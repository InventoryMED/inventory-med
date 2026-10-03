[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ArchivePath,
    [string]$Server = '179.236.237.36',
    [string]$User = 'gabriel',
    [string]$KeyPath = "$env:USERPROFILE\.ssh\inventorymed_vps_gabriel_ed25519"
)

$ErrorActionPreference = 'Stop'
$resolvedArchive = (Resolve-Path -LiteralPath $ArchivePath).Path
$archiveName = Split-Path -Leaf $resolvedArchive
$remoteDirectory = '/home/gabriel/inventory-med-upload'

ssh -o ExitOnForwardFailure=yes -i $KeyPath "$User@$Server" "mkdir -p '$remoteDirectory'"
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível preparar a pasta remota.' }

scp -i $KeyPath $resolvedArchive "${User}@${Server}:${remoteDirectory}/${archiveName}"
if ($LASTEXITCODE -ne 0) { throw 'Não foi possível enviar o pacote.' }

Write-Host "Pacote enviado para ${remoteDirectory}/${archiveName}"
