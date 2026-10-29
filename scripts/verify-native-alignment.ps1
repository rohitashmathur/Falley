param([Parameter(Mandatory=$true)][string]$Apk)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=[IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Apk))
try {
    foreach ($entry in $archive.Entries) {
        if (!$entry.FullName.EndsWith('.so')) { continue }
        $stream=$entry.Open(); $buffer=[IO.MemoryStream]::new()
        try { $stream.CopyTo($buffer); $nativeBytes=$buffer.ToArray() }
        finally { $stream.Dispose(); $buffer.Dispose() }
        if ($nativeBytes.Length -lt 64 -or $nativeBytes[0] -ne 127 -or $nativeBytes[1] -ne 69 -or $nativeBytes[2] -ne 76 -or $nativeBytes[3] -ne 70 -or $nativeBytes[5] -ne 1) { throw 'Unexpected Android native ELF format.' }
        $is64=$nativeBytes[4] -eq 2
        if (!$is64 -and $nativeBytes[4] -ne 1) { throw 'Unknown Android ELF class.' }
        if ($is64) { $tableOffset=[BitConverter]::ToUInt64($nativeBytes,32); $entrySize=[BitConverter]::ToUInt16($nativeBytes,54); $entryCount=[BitConverter]::ToUInt16($nativeBytes,56) }
        else { $tableOffset=[BitConverter]::ToUInt32($nativeBytes,28); $entrySize=[BitConverter]::ToUInt16($nativeBytes,42); $entryCount=[BitConverter]::ToUInt16($nativeBytes,44) }
        $alignments=@()
        for ($index=0;$index -lt $entryCount;$index++) {
            $offset=[int]($tableOffset+$index*$entrySize)
            if ($offset -lt 0 -or $offset+$entrySize -gt $nativeBytes.Length) { throw 'Invalid Android ELF program header.' }
            if ([BitConverter]::ToUInt32($nativeBytes,$offset) -ne 1) { continue }
            if ($is64) {
                $alignment=[BitConverter]::ToUInt64($nativeBytes,$offset+48)
                $fileOffset=[BitConverter]::ToUInt64($nativeBytes,$offset+8)
                $virtualAddress=[BitConverter]::ToUInt64($nativeBytes,$offset+16)
                if ($alignment -lt 16384 -or $fileOffset%16384 -ne $virtualAddress%16384) { throw ('64-bit native library needs 16 KiB ELF alignment: '+$entry.FullName) }
            } else { $alignment=[BitConverter]::ToUInt32($nativeBytes,$offset+28) }
            $alignments+=$alignment
        }
        if (!$alignments.Count) { throw 'An Android native library has no load segments.' }
        [pscustomobject]@{file=$entry.FullName;elfBits=$(if($is64){64}else{32});loadAlignments=$alignments;requires16KiB=$is64;verified=$true}
    }
} finally { $archive.Dispose() }
