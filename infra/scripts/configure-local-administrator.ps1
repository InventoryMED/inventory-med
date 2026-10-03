[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$environmentPath = Join-Path $repositoryRoot '.env'
$environmentExamplePath = Join-Path $repositoryRoot '.env.example'

if (-not (Test-Path -LiteralPath $environmentPath)) {
    Copy-Item -LiteralPath $environmentExamplePath -Destination $environmentPath
    Write-Host 'Arquivo .env criado a partir de .env.example.'
}

$administratorEmail = Read-Host 'E-mail do administrador geral [admin@inventorymed.local]'
if ([string]::IsNullOrWhiteSpace($administratorEmail)) {
    $administratorEmail = 'admin@inventorymed.local'
}
if ($administratorEmail -notmatch '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+$') {
    throw 'O e-mail informado não é válido.'
}

$securePassword = Read-Host 'Senha temporária do administrador geral' -AsSecureString
$secureConfirmation = Read-Host 'Confirme a senha temporária' -AsSecureString
$password = [System.Net.NetworkCredential]::new('', $securePassword).Password
$confirmation = [System.Net.NetworkCredential]::new('', $secureConfirmation).Password

try {
    if ($password -cne $confirmation) {
        throw 'As senhas informadas não são iguais.'
    }
    if (
        $password.Length -lt 12 -or
        $password -cnotmatch '[A-Z]' -or
        $password -cnotmatch '[a-z]' -or
        $password -notmatch '[0-9]' -or
        $password -notmatch '[@#%+=._-]' -or
        $password -notmatch '^[A-Za-z0-9@#%+=._-]+$'
    ) {
        throw 'Use pelo menos 12 caracteres, maiúscula, minúscula, número e um dos símbolos @ # % + = . _ -.'
    }

    $keyBytes = New-Object byte[] 32
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $random.GetBytes($keyBytes)
    }
    finally {
        $random.Dispose()
    }
    $encryptionKey = [Convert]::ToBase64String($keyBytes)

    $lines = [System.Collections.Generic.List[string]]::new()
    foreach ($line in [IO.File]::ReadAllLines($environmentPath)) {
        if ($line -notmatch '^(BOOTSTRAP_DOCTOR_EMAIL|BOOTSTRAP_DOCTOR_PASSWORD)=') {
            $lines.Add($line)
        }
    }

    function Set-EnvironmentValue {
        param(
            [Parameter(Mandatory)] [string] $Name,
            [Parameter(Mandatory)] [string] $Value
        )

        $replacement = "${Name}=${Value}"
        for ($index = 0; $index -lt $lines.Count; $index++) {
            if ($lines[$index] -match "^$([Regex]::Escape($Name))=") {
                $lines[$index] = $replacement
                return
            }
        }
        $lines.Add($replacement)
    }

    Set-EnvironmentValue -Name 'BOOTSTRAP_ENABLED' -Value 'true'
    Set-EnvironmentValue -Name 'BOOTSTRAP_SYSTEM_ADMIN_EMAIL' -Value $administratorEmail
    Set-EnvironmentValue -Name 'BOOTSTRAP_SYSTEM_ADMIN_PASSWORD' -Value $password
    Set-EnvironmentValue -Name 'TENANT_PROVISIONING_ENABLED' -Value 'true'
    Set-EnvironmentValue -Name 'TENANT_CREDENTIAL_ENCRYPTION_KEY' -Value $encryptionKey

    $utf8WithoutBom = [Text.UTF8Encoding]::new($false)
    [IO.File]::WriteAllLines($environmentPath, $lines, $utf8WithoutBom)
    Write-Host 'Administrador inicial e chave local configurados no .env.'
    Write-Host 'A senha e a chave não foram exibidas nem gravadas no Git.'
}
finally {
    $password = $null
    $confirmation = $null
}
