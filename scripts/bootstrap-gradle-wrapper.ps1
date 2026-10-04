param(
    [string]$GradleVersion = "9.5.0"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$wrapperDirectory = Join-Path $projectRoot "gradle\wrapper"
$wrapperJar = Join-Path $wrapperDirectory "gradle-wrapper.jar"
$wrapperUrl = "https://services.gradle.org/distributions/gradle-$GradleVersion-wrapper.jar"

New-Item -ItemType Directory -Path $wrapperDirectory -Force | Out-Null
Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperJar

Write-Host "Downloaded the Gradle $GradleVersion wrapper to $wrapperJar"
Write-Host "Verify the checksum against the matching .sha256 file before committing in a supply-chain-sensitive project."

