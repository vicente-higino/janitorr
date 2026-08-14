[CmdletBinding()]
param(
    [string]$TaskName = "Janitorr",
    [string]$ConfigPath,
    [switch]$StartNow
)

$ErrorActionPreference = "Stop"

if (-not $ConfigPath) {
    $ConfigPath = Join-Path $PSScriptRoot "application.yml"
}

$launcher = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "start-janitorr.ps1") -ErrorAction Stop).Path
$resolvedConfig = (Resolve-Path -LiteralPath $ConfigPath -ErrorAction Stop).Path
$workingDirectory = [System.IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\', '/')
$powerShell = (Get-Command "powershell.exe" -ErrorAction Stop).Source
$user = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name

# Validate Java, the JAR, and configuration path before installing a persistent task.
& $launcher -ConfigPath $resolvedConfig -Check
if ($LASTEXITCODE -ne 0) {
    throw "Janitorr validation failed with exit code $LASTEXITCODE."
}

$arguments = @(
    "-NoProfile"
    "-NonInteractive"
    "-WindowStyle Hidden"
    "-ExecutionPolicy Bypass"
    "-File `"$launcher`""
    "-ConfigPath `"$resolvedConfig`""
    "-Background"
) -join " "

$action = New-ScheduledTaskAction `
    -Execute $powerShell `
    -Argument $arguments `
    -WorkingDirectory $workingDirectory
$trigger = New-ScheduledTaskTrigger -AtLogOn -User $user
$principal = New-ScheduledTaskPrincipal -UserId $user -LogonType Interactive -RunLevel Limited
$settings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -MultipleInstances IgnoreNew `
    -RestartCount 3 `
    -RestartInterval (New-TimeSpan -Minutes 1) `
    -ExecutionTimeLimit ([TimeSpan]::Zero)

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger $trigger `
    -Principal $principal `
    -Settings $settings `
    -Description "Run Janitorr in the background after $user logs on." `
    -Force | Out-Null

if ($StartNow) {
    Start-ScheduledTask -TaskName $TaskName
}

Write-Host "Scheduled task '$TaskName' was installed for $user."
if ($StartNow) {
    Write-Host "Janitorr was started in the background."
} else {
    Write-Host "Start it with: Start-ScheduledTask -TaskName '$TaskName'"
}
Write-Host "View status with: Get-ScheduledTaskInfo -TaskName '$TaskName'"
Write-Host "Remove it with: Unregister-ScheduledTask -TaskName '$TaskName'"
