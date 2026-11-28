param([string]$Task = "bootRun")
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
if (Test-Path -LiteralPath ".env") {
    foreach ($line in Get-Content -LiteralPath ".env") {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $name = $Matches[1]
            $value = $Matches[2].Trim()
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}
& .\gradlew.bat $Task --console=plain
exit $LASTEXITCODE
