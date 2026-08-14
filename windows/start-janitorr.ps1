[CmdletBinding()]
param(
    [string]$ConfigPath,
    [string]$JarPath,
    [string]$JavaPath,
    [switch]$Check,
    [switch]$Background,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ApplicationArguments
)

$ErrorActionPreference = "Stop"

if (-not $ConfigPath) {
    $ConfigPath = Join-Path $PSScriptRoot "application.yml"
}
if (-not $JarPath) {
    $JarPath = Join-Path $PSScriptRoot "janitorr.jar"
}

function Get-JavaInfo([string]$JavaExecutable) {
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $JavaExecutable
    $startInfo.Arguments = "-version"
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    try {
        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $startInfo
        [void]$process.Start()
        $versionOutput = $process.StandardOutput.ReadToEnd() + $process.StandardError.ReadToEnd()
        $process.WaitForExit()

        if ($process.ExitCode -ne 0 -or $versionOutput -notmatch 'version\s+"(?<major>\d+)') {
            return $null
        }

        return [PSCustomObject]@{
            Path = (Resolve-Path -LiteralPath $JavaExecutable).Path
            Major = [int]$Matches.major
        }
    } catch {
        return $null
    } finally {
        if ($process) {
            $process.Dispose()
        }
    }
}

function Resolve-JavaExecutable([string]$RequestedJavaPath) {
    if ($RequestedJavaPath) {
        $requested = Get-JavaInfo (Resolve-Path -LiteralPath $RequestedJavaPath -ErrorAction Stop).Path
        if (-not $requested) {
            throw "The requested Java executable could not be started: $RequestedJavaPath"
        }
        if ($requested.Major -lt 25) {
            throw "Janitorr requires Java 25 or newer; the requested executable is Java $($requested.Major): $($requested.Path)"
        }
        return $requested.Path
    }

    $candidates = @()

    if ($env:JANITORR_JAVA_HOME) {
        $candidates += Join-Path $env:JANITORR_JAVA_HOME "bin\java.exe"
    }
    if ($env:JAVA_HOME) {
        $candidates += Join-Path $env:JAVA_HOME "bin\java.exe"
    }

    $javaCommand = Get-Command "java.exe" -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $candidates += $javaCommand.Source
    }

    $searchPatterns = @(
        (Join-Path $env:USERPROFILE ".gradle\jdks\*\bin\java.exe"),
        (Join-Path $env:ProgramFiles "Java\*\bin\java.exe"),
        (Join-Path $env:ProgramFiles "Eclipse Adoptium\*\bin\java.exe"),
        (Join-Path $env:ProgramFiles "Microsoft\jdk-*\bin\java.exe"),
        (Join-Path $env:LOCALAPPDATA "Programs\Eclipse Adoptium\*\bin\java.exe")
    )

    foreach ($pattern in $searchPatterns) {
        $candidates += Get-ChildItem -Path $pattern -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
    }

    $discovered = @($candidates | Where-Object { $_ -and (Test-Path -LiteralPath $_ -PathType Leaf) } |
        Select-Object -Unique | ForEach-Object { Get-JavaInfo $_ } | Where-Object { $_ })
    $compatible = $discovered | Where-Object { $_.Major -ge 25 } | Sort-Object Major | Select-Object -First 1

    if ($compatible) {
        return $compatible.Path
    }

    if ($discovered) {
        $found = ($discovered | ForEach-Object { "Java $($_.Major) at $($_.Path)" }) -join "; "
        throw "Janitorr requires Java 25 or newer. Found only: $found. Install a 64-bit Java 25+ JDK or pass -JavaPath."
    }

    throw "Java was not found. Install a 64-bit Java 25+ JDK or pass -JavaPath."
}

function ConvertTo-WindowsCommandLineArgument([string]$Argument) {
    if ($null -eq $Argument -or $Argument.Length -eq 0) {
        return '""'
    }
    if ($Argument -notmatch '[\s"]') {
        return $Argument
    }

    $result = New-Object System.Text.StringBuilder
    [void]$result.Append('"')
    $backslashes = 0
    foreach ($character in $Argument.ToCharArray()) {
        if ($character -eq '\') {
            $backslashes++
        } elseif ($character -eq '"') {
            [void]$result.Append(('\' * (($backslashes * 2) + 1)))
            [void]$result.Append('"')
            $backslashes = 0
        } else {
            [void]$result.Append(('\' * $backslashes))
            [void]$result.Append($character)
            $backslashes = 0
        }
    }
    [void]$result.Append(('\' * ($backslashes * 2)))
    [void]$result.Append('"')
    return $result.ToString()
}

function Move-PreviousConsoleLog([string]$Path, [string]$Timestamp) {
    if (Test-Path -LiteralPath $Path -PathType Leaf) {
        $item = Get-Item -LiteralPath $Path
        if ($item.Length -gt 0) {
            $archiveName = "{0}.{1}{2}" -f $item.BaseName, $Timestamp, $item.Extension
            Move-Item -LiteralPath $Path -Destination (Join-Path $item.DirectoryName $archiveName) -Force
        } else {
            Remove-Item -LiteralPath $Path -Force
        }
    }
}

$resolvedJar = (Resolve-Path -LiteralPath $JarPath -ErrorAction Stop).Path
$resolvedConfig = (Resolve-Path -LiteralPath $ConfigPath -ErrorAction Stop).Path
$java = Resolve-JavaExecutable $JavaPath

$janitorrHome = [System.IO.Path]::GetFullPath($PSScriptRoot).TrimEnd('\', '/')
$env:JANITORR_HOME = $janitorrHome.Replace('\', '/')
$logsDirectory = Join-Path $janitorrHome "logs"
[void](New-Item -ItemType Directory -Path $logsDirectory -Force)

$configUri = ([System.Uri]$resolvedConfig).AbsoluteUri

if ($Check) {
    Write-Host "Janitorr Windows setup is valid."
    Write-Host "Java:  $java"
    Write-Host "JAR:   $resolvedJar"
    Write-Host "Config: $resolvedConfig"
    exit 0
}

Write-Host "Starting Janitorr with config $resolvedConfig"
$javaArguments = @(
    "-Dfile.encoding=UTF-8"
    "-Dspring.config.additional-location=optional:$configUri"
    "-jar"
    $resolvedJar
) + @($ApplicationArguments | Where-Object { $null -ne $_ -and $_ -ne "" })

if ($Background) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $standardOutputLog = Join-Path $logsDirectory "janitorr-console.log"
    $standardErrorLog = Join-Path $logsDirectory "janitorr-error.log"
    Move-PreviousConsoleLog $standardOutputLog $timestamp
    Move-PreviousConsoleLog $standardErrorLog $timestamp

    $argumentLine = ($javaArguments | ForEach-Object { ConvertTo-WindowsCommandLineArgument $_ }) -join " "
    $process = Start-Process `
        -FilePath $java `
        -ArgumentList $argumentLine `
        -WorkingDirectory $janitorrHome `
        -WindowStyle Hidden `
        -RedirectStandardOutput $standardOutputLog `
        -RedirectStandardError $standardErrorLog `
        -Wait `
        -PassThru
    exit $process.ExitCode
}

& $java @javaArguments
exit $LASTEXITCODE
