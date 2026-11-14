$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$codexRoot = Join-Path $projectRoot 'codex'
if (-not $codexRoot.StartsWith(($projectRoot + [IO.Path]::DirectorySeparatorChar),
        [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Unexpected project layout.'
}
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a JDK with jpackage first.' }
$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
if (-not (Test-Path -LiteralPath $jpackage)) { throw 'JAVA_HOME does not contain jpackage.exe.' }
if (@(Get-Process -Name 'Study Agent','Study Agent CLI' -ErrorAction SilentlyContinue).Count -gt 0) {
    throw 'Close Study Agent and its CLI before updating. Replacing a JAR used by a running app can break lazy class loading.'
}

New-Item -ItemType Directory -Force -Path (Join-Path $codexRoot 'build') | Out-Null
Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'mvnw.cmd') -B -ntp clean package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build or tests failed.' }
} finally { Pop-Location }

$inputDir = Join-Path $codexRoot 'build\package-input'
$imageDir = Join-Path $codexRoot ('build\app-image-' + [guid]::NewGuid().ToString('N'))
$zipTemp = Join-Path $codexRoot ('build\Study-Agent-' + [guid]::NewGuid().ToString('N') + '.zip')
$zipTarget = Join-Path $projectRoot 'Study-Agent-Windows-x64.zip'
New-Item -ItemType Directory -Force -Path $inputDir,$imageDir | Out-Null
Copy-Item -LiteralPath (Join-Path $codexRoot 'build\target\study-agent.jar') `
    -Destination (Join-Path $inputDir 'study-agent.jar') -Force
& $jpackage --type app-image --name 'Study Agent' --input $inputDir `
    --main-jar study-agent.jar --main-class eecs3311.studyagent.Main `
    --dest $imageDir --java-options '"-Dstudyagent.root=$ROOTDIR"' `
    --add-launcher ('Study Agent CLI=' + (Join-Path $projectRoot 'packaging\cli-launcher.properties'))
if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }
$imageApp = Join-Path $imageDir 'Study Agent'
& (Join-Path $PSScriptRoot 'update-app.ps1') -SourceJar (Join-Path $inputDir 'study-agent.jar') `
    -AppDirectory (Join-Path $imageApp 'app')
Copy-Item -LiteralPath (Join-Path $projectRoot '.env.example') `
    -Destination (Join-Path $imageApp '.env.example') -Force

# This ZIP contains only the runnable image. Credentials and user data stay local.
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory($imageApp, $zipTemp)
Move-Item -LiteralPath $zipTemp -Destination $zipTarget -Force

$dataDir = Join-Path $projectRoot 'data'
$before = if (Test-Path -LiteralPath $dataDir) {
    @(Get-ChildItem -LiteralPath $dataDir -Recurse -File | Get-FileHash -Algorithm SHA256 |
        Sort-Object Path | ForEach-Object { $_.Path + ':' + $_.Hash })
} else { @() }
$destination = Join-Path $projectRoot 'runtime'
if (-not (Test-Path -LiteralPath $destination)) {
    Copy-Item -LiteralPath (Join-Path $imageApp 'runtime') -Destination $destination -Recurse
}
$applicationDirectory = Join-Path $projectRoot 'app'
New-Item -ItemType Directory -Force -Path $applicationDirectory | Out-Null
foreach ($name in 'Study Agent.cfg','Study Agent CLI.cfg') {
    $target = Join-Path $applicationDirectory $name
    if (-not (Test-Path -LiteralPath $target)) {
        Copy-Item -LiteralPath (Join-Path $imageApp ('app\' + $name)) -Destination $target
    }
}
& (Join-Path $PSScriptRoot 'update-app.ps1') -SourceJar (Join-Path $inputDir 'study-agent.jar') `
    -AppDirectory $applicationDirectory
foreach ($name in 'Study Agent.exe','Study Agent CLI.exe','.env.example') {
    $target = Join-Path $projectRoot $name
    if ($name -eq '.env.example' -or -not (Test-Path -LiteralPath $target)) {
        Copy-Item -LiteralPath (Join-Path $imageApp $name) -Destination $target -Force
    }
}
$after = if (Test-Path -LiteralPath $dataDir) {
    @(Get-ChildItem -LiteralPath $dataDir -Recurse -File | Get-FileHash -Algorithm SHA256 |
        Sort-Object Path | ForEach-Object { $_.Path + ':' + $_.Hash })
} else { @() }
if ($before.Count -ne $after.Count -or
        ($before.Count -gt 0 -and (Compare-Object $before $after))) {
    throw 'User data changed while updating the app.'
}
Write-Output "Windows GUI, CLI, and ZIP updated in $projectRoot; $($after.Count) data files preserved."
