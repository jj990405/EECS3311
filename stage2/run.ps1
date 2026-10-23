param(
    [ValidateSet('gui', 'cli', 'test')][string]$Mode = 'gui',
    [string]$DataDirectory = (Join-Path $PSScriptRoot 'data')
)

$ErrorActionPreference = 'Stop'
$javaBin = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $javaBin = Join-Path $env:JAVA_HOME 'bin'
} else {
    $javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javacCommand) {
        $javaBin = Split-Path $javacCommand.Source
    } else {
        $pluginRoot = Join-Path $env:USERPROFILE '.p2\pool\plugins'
        if (Test-Path $pluginRoot) {
            $candidate = Get-ChildItem $pluginRoot -Directory -Filter 'org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_*' |
                Sort-Object Name -Descending |
                Where-Object { Test-Path (Join-Path $_.FullName 'jre\bin\javac.exe') } |
                Select-Object -First 1
            if ($candidate) { $javaBin = Join-Path $candidate.FullName 'jre\bin' }
        }
    }
}
if (-not $javaBin) { throw 'JDK 17+ not found. Set JAVA_HOME to a JDK installation.' }

$buildDir = Join-Path $PSScriptRoot 'build\classes'
$pdfboxJar = Join-Path $PSScriptRoot 'lib\pdfbox-app-3.0.8.jar'
if (-not (Test-Path $pdfboxJar)) { throw 'Missing lib\pdfbox-app-3.0.8.jar.' }
$classpath = "$buildDir;$pdfboxJar"
New-Item -ItemType Directory -Force -Path $buildDir | Out-Null
$sources = @(Get-ChildItem (Join-Path $PSScriptRoot 'src\main\java') -Filter '*.java' -Recurse | ForEach-Object FullName)
& (Join-Path $javaBin 'javac.exe') --release 17 -encoding UTF-8 -cp $pdfboxJar -d $buildDir @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($Mode -eq 'test') {
    $tests = @(Get-ChildItem (Join-Path $PSScriptRoot 'src\test\java') -Filter '*.java' -Recurse | ForEach-Object FullName)
    & (Join-Path $javaBin 'javac.exe') --release 17 -encoding UTF-8 -cp $classpath -d $buildDir @tests
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & (Join-Path $javaBin 'java.exe') "-Dproject2.root=$PSScriptRoot" -cp $classpath eecs3311.studyagent.AgentControllerTest
} elseif ($Mode -eq 'cli') {
    & (Join-Path $javaBin 'java.exe') "-Dproject2.root=$PSScriptRoot" -cp $classpath eecs3311.studyagent.Main --cli --data $DataDirectory
} else {
    & (Join-Path $javaBin 'java.exe') "-Dproject2.root=$PSScriptRoot" -cp $classpath eecs3311.studyagent.Main --data $DataDirectory
}
exit $LASTEXITCODE
