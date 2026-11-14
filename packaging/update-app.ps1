param(
    [Parameter(Mandatory = $true)][string]$SourceJar,
    [Parameter(Mandatory = $true)][string]$AppDirectory
)
$ErrorActionPreference = 'Stop'

function Get-JarHash([string]$Path) {
    $stream = [IO.File]::OpenRead($Path)
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-', '').ToLowerInvariant() }
    finally { $sha.Dispose(); $stream.Dispose() }
}

# A running launcher keeps its original classpath. Never overwrite that JAR.
$source = (Resolve-Path -LiteralPath $SourceJar).Path
$app = (Resolve-Path -LiteralPath $AppDirectory).Path
$project = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
if (-not $app.StartsWith($project + [IO.Path]::DirectorySeparatorChar,
        [StringComparison]::OrdinalIgnoreCase)) { throw 'Application directory must be inside this project.' }
$cfgNames = @('Study Agent.cfg', 'Study Agent CLI.cfg')
$configs = @{}
foreach ($name in $cfgNames) {
    $path = Join-Path $app $name
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing launcher configuration: $name" }
    $text = [IO.File]::ReadAllText($path)
    if ([regex]::Matches($text, '(?m)^app\.classpath=.*$').Count -ne 1) {
        throw "Expected one application classpath in $name."
    }
    $configs[$name] = $text
}
$hash = Get-JarHash $source
$jarName = 'study-agent-' + $hash.Substring(0, 16) + '.jar'
$destination = Join-Path $app $jarName
if (Test-Path -LiteralPath $destination) {
    if ((Get-JarHash $destination) -ne $hash) {
        throw 'A different build already occupies this immutable version name.'
    }
} else {
    $temp = Join-Path $app ('.new-jar-' + [guid]::NewGuid().ToString('N') + '.tmp')
    try {
        Copy-Item -LiteralPath $source -Destination $temp
        if ((Get-JarHash $temp) -ne $hash) {
            throw 'Source JAR changed during publication; retry after the build finishes.'
        }
        Move-Item -LiteralPath $temp -Destination $destination
    } finally {
        if (Test-Path -LiteralPath $temp) { Remove-Item -LiteralPath $temp }
    }
}
$encoding = New-Object Text.UTF8Encoding($false)
foreach ($name in $cfgNames) {
    $cfg = Join-Path $app $name
    $replacement = 'app.classpath=$APPDIR\' + $jarName
    $text = [regex]::Replace($configs[$name], '(?m)^app\.classpath=.*$',
        [System.Text.RegularExpressions.MatchEvaluator]{ param($match) $replacement })
    $next = Join-Path $app ('.next-cfg-' + [guid]::NewGuid().ToString('N') + '.tmp')
    try {
        [IO.File]::WriteAllText($next, $text, $encoding)
        $backup = Join-Path $app ('.previous-config-' + [guid]::NewGuid().ToString('N') + '.cfg')
        [IO.File]::Replace($next, $cfg, $backup)
    } finally {
        if (Test-Path -LiteralPath $next) { Remove-Item -LiteralPath $next }
    }
}
Write-Output "Published $jarName. Existing JARs retained; restart to use the new version."
