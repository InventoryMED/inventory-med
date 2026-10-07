[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $repositoryRoot

$status = git status --porcelain
if ($status) {
    throw 'O repositório deve estar sem alterações pendentes antes de gerar uma versão.'
}

$commit = (git rev-parse --short=12 HEAD).Trim()
$releaseRoot = Join-Path $repositoryRoot "artifacts\releases\$commit"
$archivePath = Join-Path $repositoryRoot "artifacts\releases\inventory-med-$commit.tar.gz"

if (Test-Path -LiteralPath $releaseRoot) {
    throw "A pasta da versão já existe: $releaseRoot"
}

docker run --rm `
    --mount "type=bind,src=$(Join-Path $repositoryRoot 'frontend'),dst=/workspace" `
    -v inventorymed-node-modules:/workspace/node_modules `
    -v inventorymed-npm-cache:/root/.npm `
    -w /workspace `
    node:24-alpine `
    sh -c 'npm ci && npm test -- --watch=false && npm run build'
if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes ou no build do frontend.' }

docker run --rm `
    --add-host host.docker.internal:host-gateway `
    -e TESTCONTAINERS_RYUK_DISABLED=true `
    -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
    --mount type=bind,src=/var/run/docker.sock,dst=/var/run/docker.sock `
    -v inventorymed-maven-cache:/root/.m2 `
    -v "${repositoryRoot}:/workspace" `
    -w /workspace/backend `
    maven:3.9.11-eclipse-temurin-21 `
    mvn clean verify
if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes ou no empacotamento da API.' }

New-Item -ItemType Directory -Path (Join-Path $releaseRoot 'frontend') -Force | Out-Null
$apiJar = Get-ChildItem (Join-Path $repositoryRoot 'backend\target\inventory-med-api-*.jar') |
    Where-Object Name -NotLike '*.original' |
    Select-Object -First 1
if (-not $apiJar) { throw 'O JAR gerado da API não foi encontrado.' }
Copy-Item -LiteralPath $apiJar.FullName -Destination (Join-Path $releaseRoot 'inventory-med-api.jar')
Copy-Item `
    -Path (Join-Path $repositoryRoot 'frontend\dist\frontend\browser\*') `
    -Destination (Join-Path $releaseRoot 'frontend') `
    -Recurse
Copy-Item (Join-Path $repositoryRoot 'infra') $releaseRoot -Recurse

@(
    "commit=$commit"
    "generated_at=$([DateTimeOffset]::Now.ToString('O'))"
) | Set-Content -Encoding utf8 (Join-Path $releaseRoot 'release-info.txt')

New-Item -ItemType Directory -Path (Split-Path $archivePath) -Force | Out-Null
tar -czf $archivePath -C (Split-Path $releaseRoot) $commit
if ($LASTEXITCODE -ne 0) { throw 'Falha ao compactar a versão.' }

Write-Host "Versão gerada: $releaseRoot"
Write-Host "Pacote para envio: $archivePath"
