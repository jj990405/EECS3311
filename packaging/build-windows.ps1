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
foreach ($name in 'app','runtime') {
    $destination = Join-Path $projectRoot $name
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    Copy-Item -Path (Join-Path $imageApp ($name + '\*')) -Destination $destination -Recurse -Force
}
foreach ($name in 'Study Agent.exe','Study Agent CLI.exe','.env.example') {
    Copy-Item -LiteralPath (Join-Path $imageApp $name) -Destination (Join-Path $projectRoot $name) -Force
}
$after = if (Test-Path -LiteralPath $dataDir) {
    @(Get-ChildItem -LiteralPath $dataDir -Recurse -File | Get-FileHash -Algorithm SHA256 |
        Sort-Object Path | ForEach-Object { $_.Path + ':' + $_.Hash })
} else { @() }
if ($before.Count -ne $after.Count -or (Compare-Object $before $after)) {
    throw 'User data changed while updating the app.'
}
Write-Output "Windows GUI, CLI, and ZIP updated in $projectRoot; $($after.Count) data files preserved."
