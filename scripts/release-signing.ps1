param(
    [switch]$Initialize,
    [string]$JdkRoot=$env:JAVA_HOME,
    [string]$SigningDirectory
)
$ErrorActionPreference='Stop'
$root=Split-Path -Parent $PSScriptRoot
if (!$SigningDirectory) { $SigningDirectory=Join-Path $root 'releases\private-signing' }
$SigningDirectory=[IO.Path]::GetFullPath($SigningDirectory)
if ($env:OS -ne 'Windows_NT') { throw 'This local credential helper uses Windows DPAPI. On other platforms configure signing through FALLEY_UPLOAD_* environment variables and a secure secret store.' }
if (!$JdkRoot) { throw 'Set JAVA_HOME or pass JdkRoot for JDK 17.' }
$metadataFile=Join-Path $SigningDirectory 'signing.json'
$credentialFile=Join-Path $SigningDirectory 'credential.dpapi'
$keyFile=Join-Path $SigningDirectory 'falley-upload.p12'
$certificateFile=Join-Path $SigningDirectory 'upload-certificate.pem'
$present=@($metadataFile,$credentialFile,$keyFile | Where-Object { Test-Path -LiteralPath $_ })
if ($present.Count -gt 0 -and $present.Count -lt 3) { throw 'Signing setup is incomplete. Preserve all existing key files and recover the matching credential; no key will be overwritten.' }
if ($present.Count -eq 0) {
    if (!$Initialize) { throw 'No upload key exists. Run with -Initialize once, then preserve this signing directory.' }
    New-Item -ItemType Directory -Force -Path $SigningDirectory | Out-Null
    $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl=New-Object Security.AccessControl.DirectorySecurity
    $acl.SetAccessRuleProtection($true,$false)
    $rule=New-Object Security.AccessControl.FileSystemAccessRule($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow')
    $acl.SetAccessRule($rule)
    Set-Acl -LiteralPath $SigningDirectory -AclObject $acl
    $bytes=New-Object byte[] 32
    $rng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $password=[Convert]::ToBase64String($bytes)
    $env:FALLEY_UPLOAD_STORE_PASSWORD=$password
    try {
        & (Join-Path $JdkRoot 'bin\keytool.exe') -genkeypair -keystore $keyFile -storetype PKCS12 -storepass:env FALLEY_UPLOAD_STORE_PASSWORD -keypass:env FALLEY_UPLOAD_STORE_PASSWORD -alias falley-upload -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Falley Upload,O=Perf Games'
        if ($LASTEXITCODE -ne 0) { throw 'Upload key creation failed. Preserve any partial files for inspection.' }
        ConvertTo-SecureString $password -AsPlainText -Force | ConvertFrom-SecureString | Set-Content -LiteralPath $credentialFile -Encoding ascii
        @{keyStore=$keyFile;keyAlias='falley-upload';credential=$credentialFile;createdUtc=[DateTime]::UtcNow.ToString('o');protection='Windows DPAPI, current user; directory ACL restricted to current user';purpose='Play upload key; enroll in Play App Signing'} | ConvertTo-Json | Set-Content -LiteralPath $metadataFile -Encoding utf8
    } finally { $password=$null; $env:FALLEY_UPLOAD_STORE_PASSWORD=$null; [Array]::Clear($bytes,0,$bytes.Length) }
}
$metadata=Get-Content -LiteralPath $metadataFile -Raw | ConvertFrom-Json
if ([IO.Path]::GetFullPath($metadata.keyStore) -ne $keyFile -or $metadata.keyAlias -ne 'falley-upload') { throw 'Unexpected signing metadata; verify the existing upload identity before proceeding.' }
$secure=(Get-Content -LiteralPath $credentialFile -Raw).Trim() | ConvertTo-SecureString
$plain=[Net.NetworkCredential]::new('', $secure).Password
$env:FALLEY_UPLOAD_KEYSTORE=$keyFile
$env:FALLEY_UPLOAD_STORE_PASSWORD=$plain
$env:FALLEY_UPLOAD_KEY_ALIAS=$metadata.keyAlias
$env:FALLEY_UPLOAD_KEY_PASSWORD=$plain
$plain=$null
if (!(Test-Path -LiteralPath $certificateFile)) {
    & (Join-Path $JdkRoot 'bin\keytool.exe') -exportcert -rfc -keystore $keyFile -storepass:env FALLEY_UPLOAD_STORE_PASSWORD -alias $metadata.keyAlias -file $certificateFile
    if ($LASTEXITCODE -ne 0) { throw 'Upload certificate export failed.' }
}
# Deliberately return paths/alias only. Passwords are never written to console or Git.
[pscustomobject]@{Directory=$SigningDirectory;KeyStore=$keyFile;Alias=$metadata.keyAlias;Certificate=$certificateFile;Credential=$credentialFile}
