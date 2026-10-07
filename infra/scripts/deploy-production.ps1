[CmdletBinding()]
param(
    [switch]$ConfirmarProducao,
    [ValidatePattern('^[A-Za-z0-9.-]+$')]
    [string]$Server = '179.236.237.36',
    [ValidatePattern('^[a-z_][a-z0-9_-]*$')]
    [string]$User = 'gabriel',
    [string]$KeyPath = "$env:USERPROFILE\.ssh\inventorymed_vps_gabriel_ed25519",
    [ValidatePattern('^https?://[^\s/]+(?::\d+)?$')]
    [string]$PublicBaseUrl = 'http://179.236.237.36',
    [switch]$ReutilizarPacoteExistente
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$releaseDirectory = Join-Path $repositoryRoot 'artifacts\releases'
$deploymentDirectory = Join-Path $repositoryRoot 'artifacts\deployments'
$startedAt = [DateTimeOffset]::Now
$receiptPath = $null
$deploymentStatus = 'FAILED'
$commit = $null
$branch = $null
$archivePath = $null
$archiveHash = $null

function Assert-CommandAvailable {
    param([Parameter(Mandatory = $true)][string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "O comando '$Name' não está disponível neste computador."
    }
}

function Add-KnownCommandLocation {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string[]]$Candidates
    )

    if (Get-Command $Name -ErrorAction SilentlyContinue) { return }
    foreach ($candidate in $Candidates) {
        if ([string]::IsNullOrWhiteSpace($candidate)) { continue }
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            $directory = Split-Path -Parent $candidate
            $env:PATH = "$directory;$env:PATH"
            if (Get-Command $Name -ErrorAction SilentlyContinue) { return }
        }
    }
}

function Invoke-CheckedNativeCommand {
    param(
        [Parameter(Mandatory = $true)][scriptblock]$Command,
        [Parameter(Mandatory = $true)][string]$FailureMessage
    )

    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw $FailureMessage
    }
}

function Write-DeploymentReceipt {
    param([string]$FailureMessage)

    if (-not $receiptPath) { return }

    $receipt = [ordered]@{
        status = $deploymentStatus
        commit = $commit
        branch = $branch
        server = $Server
        publicBaseUrl = $PublicBaseUrl
        package = if ($archivePath) { Split-Path -Leaf $archivePath } else { $null }
        packageSha256 = $archiveHash
        startedAt = $startedAt.ToString('O')
        finishedAt = [DateTimeOffset]::Now.ToString('O')
        failure = $FailureMessage
        operations = @(
            'verificação do Git e envio do commit ao origin quando necessário',
            'testes e builds locais pelo build-release.ps1',
            'envio do pacote por SSH e conferência SHA-256 na VPS',
            'instalação pelo install-release.sh com rollback automático em falha de saúde local',
            'validação externa do frontend e do actuator da API'
        )
    }
    $receipt | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $receiptPath -Encoding utf8
}

try {
    if (-not $ConfirmarProducao) {
        throw 'Confirme conscientemente a publicação usando -ConfirmarProducao.'
    }

    Set-Location $repositoryRoot
    Add-KnownCommandLocation -Name 'git' -Candidates @(
        (Join-Path $env:ProgramFiles 'Git\cmd\git.exe'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Git\cmd\git.exe'),
        (Join-Path $env:USERPROFILE 'scoop\apps\git\current\cmd\git.exe'),
        (Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\native\git\cmd\git.exe')
    )
    Add-KnownCommandLocation -Name 'docker' -Candidates @(
        (Join-Path $env:ProgramFiles 'Docker\Docker\resources\bin\docker.exe'),
        (Join-Path $env:LOCALAPPDATA 'Programs\DockerDesktop\resources\bin\docker.exe')
    )
    foreach ($command in @('git', 'docker', 'ssh', 'scp', 'ssh-add', 'ssh-keygen', 'tar')) {
        Assert-CommandAvailable $command
    }

    $resolvedKey = (Resolve-Path -LiteralPath $KeyPath).Path
    $status = @(git status --porcelain=v1)
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível consultar o estado do Git.' }
    if ($status.Count -gt 0) {
        throw 'Existem alterações sem commit. Revise, documente e faça o commit antes da publicação.'
    }

    $branch = (git branch --show-current).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($branch)) {
        throw 'A publicação não é permitida com o Git em estado detached HEAD.'
    }

    $commit = (git rev-parse --short=12 HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or $commit -notmatch '^[0-9a-f]{12}$') {
        throw 'Não foi possível identificar o commit da versão.'
    }

    New-Item -ItemType Directory -Path $deploymentDirectory -Force | Out-Null
    $receiptPath = Join-Path $deploymentDirectory (
        '{0}-{1}.json' -f $startedAt.ToString('yyyyMMdd-HHmmss'), $commit
    )

    $keyDescription = & ssh-keygen -lf $resolvedKey
    $keyExitCode = $LASTEXITCODE
    $keyParts = ([string]$keyDescription).Trim() -split '\s+'
    if ($keyExitCode -ne 0 -or $keyParts.Count -lt 2) {
        throw 'Não foi possível ler a impressão digital da chave SSH.'
    }
    $keyFingerprint = $keyParts[1]
    $agentService = Get-Service -Name 'ssh-agent' -ErrorAction SilentlyContinue
    if ($agentService -and $agentService.Status -ne 'Running') {
        try {
            Start-Service -Name 'ssh-agent'
        } catch {
            throw (
                'O serviço ssh-agent está parado. Inicie-o uma vez em um PowerShell como ' +
                'administrador com: Start-Service ssh-agent'
            )
        }
    }
    $loadedKeys = (& ssh-add -l 2>$null) -join "`n"
    if ($LASTEXITCODE -ne 0 -or $loadedKeys -notmatch [regex]::Escape($keyFingerprint)) {
        Write-Host 'A chave da VPS ainda não está no agente SSH. Informe a frase secreta uma vez.'
        Invoke-CheckedNativeCommand -FailureMessage 'Não foi possível carregar a chave no agente SSH.' -Command {
            & ssh-add $resolvedKey
        }
    }

    Write-Host 'Sincronizando o commit com o GitHub...'
    Invoke-CheckedNativeCommand -FailureMessage 'Falha ao atualizar as referências do GitHub.' -Command {
        & git fetch --quiet origin
    }

    $upstreamOutput = & git rev-parse --abbrev-ref --symbolic-full-name '@{upstream}' 2>$null
    $upstreamExitCode = $LASTEXITCODE
    $upstream = ([string]$upstreamOutput).Trim()
    if ($upstreamExitCode -ne 0 -or [string]::IsNullOrWhiteSpace($upstream)) {
        Invoke-CheckedNativeCommand -FailureMessage 'Falha ao publicar e vincular a branch no GitHub.' -Command {
            & git push --set-upstream origin $branch
        }
        $upstream = "origin/$branch"
    }
    if (-not $upstream.StartsWith('origin/', [StringComparison]::Ordinal)) {
        throw "A branch atual acompanha '$upstream', mas a fonte oficial deve ser o remote origin."
    }

    $divergence = ((& git rev-list --left-right --count "${upstream}...HEAD") -join ' ').Trim() -split '\s+'
    if ($LASTEXITCODE -ne 0 -or $divergence.Count -ne 2) {
        throw 'Não foi possível comparar a branch local com o GitHub.'
    }
    $behind = [int]$divergence[0]
    $ahead = [int]$divergence[1]
    if ($behind -gt 0) {
        throw 'A branch local está atrás ou divergiu do GitHub. Sincronize e revise antes de publicar.'
    }
    if ($ahead -gt 0) {
        Invoke-CheckedNativeCommand -FailureMessage 'Falha ao enviar o commit ao GitHub.' -Command {
            & git push origin $branch
        }
    }

    $remoteCommit = (& git rev-parse $upstream).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Não foi possível ler o commit publicado no GitHub.' }
    $localCommit = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or $remoteCommit -ne $localCommit) {
        throw 'O commit local ainda não está confirmado no GitHub. A publicação foi interrompida.'
    }

    $archivePath = Join-Path $releaseDirectory "inventory-med-$commit.tar.gz"
    if (Test-Path -LiteralPath $archivePath) {
        if (-not $ReutilizarPacoteExistente) {
            throw (
                "Já existe um pacote para o commit $commit. Para reutilizá-lo conscientemente, " +
                'execute novamente com -ReutilizarPacoteExistente.'
            )
        }
        Write-Host "Reutilizando o pacote imutável do commit $commit."
    } else {
        Write-Host 'Executando testes e gerando o pacote da versão...'
        & (Join-Path $PSScriptRoot 'build-release.ps1')
        if ($LASTEXITCODE -ne 0) { throw 'A geração da versão falhou.' }
    }

    if (-not (Test-Path -LiteralPath $archivePath -PathType Leaf)) {
        throw 'O pacote esperado não foi gerado.'
    }
    $archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()

    Write-Host 'Enviando o pacote para a VPS...'
    & (Join-Path $PSScriptRoot 'upload-release.ps1') `
        -ArchivePath $archivePath `
        -Server $Server `
        -User $User `
        -KeyPath $resolvedKey
    if ($LASTEXITCODE -ne 0) { throw 'O envio do pacote falhou.' }

    $archiveName = Split-Path -Leaf $archivePath
    $remoteDirectory = "/home/$User/inventory-med-upload"
    $remotePackageDirectory = "$remoteDirectory/$commit"
    $remoteCommand = (
        "set -Eeuo pipefail; cd '$remoteDirectory'; " +
        "printf '%s  %s\n' '$archiveHash' '$archiveName' | sha256sum -c -; " +
        "tar -xzf '$archiveName'; " +
        "sudo bash '$remotePackageDirectory/infra/scripts/install-release.sh' " +
        "'$remotePackageDirectory' '$commit'"
    )
    $sshOptions = @(
        '-tt',
        '-o', 'BatchMode=yes',
        '-o', 'IdentitiesOnly=yes',
        '-o', 'StrictHostKeyChecking=yes',
        '-i', $resolvedKey
    )

    Write-Host "Instalando a versão na VPS. O sudo solicitará a senha do usuário $User."
    & ssh @sshOptions "$User@$Server" $remoteCommand
    if ($LASTEXITCODE -ne 0) {
        throw 'A instalação remota falhou. Consulte a saída acima para confirmar o rollback.'
    }

    $healthUrl = "$($PublicBaseUrl.TrimEnd('/'))/api/v1/actuator/health"
    $health = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 30
    if ($health.status -ne 'UP') {
        throw "A API instalada não respondeu UP em $healthUrl."
    }
    $frontend = Invoke-WebRequest -Uri $PublicBaseUrl -Method Get -TimeoutSec 30 -UseBasicParsing
    if ($frontend.StatusCode -ne 200) {
        throw "O frontend público respondeu HTTP $($frontend.StatusCode)."
    }

    $deploymentStatus = 'SUCCEEDED'
    Write-DeploymentReceipt -FailureMessage $null
    Write-Host ''
    Write-Host "Versão $commit publicada e validada com sucesso."
    Write-Host "Sistema: $PublicBaseUrl"
    Write-Host "Comprovante local: $receiptPath"
} catch {
    Write-DeploymentReceipt -FailureMessage $_.Exception.Message
    throw
}
