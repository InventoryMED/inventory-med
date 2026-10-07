[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ArchivePath,
    [ValidatePattern('^[A-Za-z0-9.-]+$')]
    [string]$Server = '179.236.237.36',
    [ValidatePattern('^[a-z_][a-z0-9_-]*$')]
    [string]$User = 'gabriel',
    [string]$KeyPath = "$env:USERPROFILE\.ssh\inventorymed_vps_gabriel_ed25519"
)

$ErrorActionPreference = 'Stop'
$resolvedArchive = (Resolve-Path -LiteralPath $ArchivePath).Path
$resolvedKey = (Resolve-Path -LiteralPath $KeyPath).Path
$archiveName = Split-Path -Leaf $resolvedArchive
$remoteDirectory = "/home/$User/inventory-med-upload"
$sshOptions = @(
    '-o', 'BatchMode=yes',
    '-o', 'IdentitiesOnly=yes',
    '-o', 'StrictHostKeyChecking=yes',
    '-i', $resolvedKey
)

& ssh @sshOptions "$User@$Server" "mkdir -p '$remoteDirectory'"
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível preparar a pasta remota.' }

& scp @sshOptions $resolvedArchive "${User}@${Server}:${remoteDirectory}/${archiveName}"
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível enviar o pacote.' }

Write-Host "Pacote enviado para ${remoteDirectory}/${archiveName}"
