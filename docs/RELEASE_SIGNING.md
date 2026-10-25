# Falley release signing

The beta uses a dedicated RSA-3072 **upload key**, separate from the old debug key. Enroll in Play App Signing when uploading the first bundle; Google then signs the APKs it distributes with the app-signing key. The local bundletool APK is signed with the upload key for pre-upload testing, so it is not proof of a Play-installed update.

`scripts/release-signing.ps1` initializes a key only when no identity exists. It refuses to overwrite a partial identity. The default private directory is `releases/private-signing/`, excluded from Git:

- `falley-upload.p12`: password-encrypted private upload key.
- `credential.dpapi`: its random password encrypted for the current Windows user/profile.
- `signing.json`: local alias/path metadata.
- `upload-certificate.pem`: public certificate, useful if Console requests it.

The directory ACL permits the current Windows user. Build/test scripts load passwords into their process environment, never command-line values or logs, and clear those variables in `finally`. Bundletool's password file is created only inside this restricted directory and removed in `finally`.

## Before the first Play upload

Preserve the existing identity. Back up the PKCS12 key and store its password in your trusted password manager or encrypted offline backup. **A DPAPI file alone cannot be decrypted on a different Windows profile/machine.** The release scripts continue to work on this profile without typing the password.

To retrieve the password for your own secure backup, run the following only in a private local PowerShell session, then transfer it directly to your password manager. This work does not print that password in chat, build logs or delivery receipts:

```powershell
$secret = (Get-Content -LiteralPath .\releases\private-signing\credential.dpapi -Raw).Trim() | ConvertTo-SecureString
$credential = [Net.NetworkCredential]::new('Falley upload', $secret)
# Inspect $credential.Password locally when saving your secure backup.
# Do not paste it into chat, issue reports, screenshots or Git.
```

Record the public certificate fingerprint from the release receipt. Future builds must reuse this upload identity; never delete the private-signing directory to bypass a password or signing error. An upload-key reset, if necessary, is a separate authenticated Play Console recovery process. Keep the old debug key separate: it cannot update the Play-signed installation.

## Verification

The release pipeline runs bundletool validation, strict jarsigner verification against this key/alias, and `VerifyBundleSignature.java`. The Java check reads every payload entry through signature validation and requires the preserved certificate. It rejects unsigned entries, unexpected signers and empty payloads. A successfully signed universal test APK alone is not evidence that the AAB is signed.

[Android app signing](https://developer.android.com/studio/publish/app-signing), [jarsigner verification](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jarsigner.html).


From 4.2, release builds also run R8 and resource shrinking. Keep the generated `mapping.txt` for each exact version alongside its private local AAB. `scripts/build-release.ps1` compares it to the embedded AAB mapping and preserves available R8 reports plus `release-verification.json`. These files stay local with the artifacts. See [4.2 verification notes](BETA_4_2.md).
