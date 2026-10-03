# Ambiente de desenvolvimento local

O ambiente recomendado usa o front-end Angular diretamente no Windows e executa a API e um SQL Server Express isolado no Docker. Assim, não é necessário instalar Java ou Maven na máquina.

## 1. Pré-requisitos

- Node.js 24 e npm;
- Docker Desktop com o mecanismo WSL 2;
- Git.

Valide no PowerShell:

```powershell
node --version
npm --version
docker --version
docker compose version
```

## 2. Configuração local

Na raiz `C:\Inventory MED`:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
```

Se o `.env` já possui as senhas do SQL Server, configure o administrador e gere a chave
de criptografia sem exibi-los no terminal:

```powershell
.\infra\scripts\configure-local-administrator.ps1
```

No `.env`, substitua pelo menos estas credenciais:

- `MSSQL_SA_PASSWORD` e `DB_PASSWORD` devem possuir exatamente o mesmo valor;
- `BOOTSTRAP_ENABLED=true` autoriza a criação idempotente da primeira conta de
  administrador geral;
- `BOOTSTRAP_SYSTEM_ADMIN_EMAIL` define o e-mail dessa conta;
- `BOOTSTRAP_SYSTEM_ADMIN_PASSWORD` é uma senha inicial temporária com pelo menos 12
  caracteres, maiúscula, minúscula, número e símbolo;
- `TENANT_PROVISIONING_ENABLED=true` habilita a criação de um banco exclusivo quando o
  administrador cadastra um hospital;
- `TENANT_CREDENTIAL_ENCRYPTION_KEY` deve ser uma chave aleatória de 32 bytes em Base64.

Para gerar a chave no PowerShell sem reutilizar uma senha, execute:

```powershell
$keyBytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($keyBytes)
[Convert]::ToBase64String($keyBytes)
```

Copie somente o resultado para `TENANT_CREDENTIAL_ENCRYPTION_KEY`. Depois que a conta
administrativa for criada e a senha inicial trocada, defina `BOOTSTRAP_ENABLED=false` e
remova o valor de `BOOTSTRAP_SYSTEM_ADMIN_PASSWORD`.

O arquivo `.env` é ignorado pelo Git. Não envie essas senhas ao repositório.

## 3. API e banco de dados

Com o Docker Desktop aberto:

```powershell
cd 'C:\Inventory MED'
docker compose up --build -d
docker compose ps
docker compose logs -f api
```

Quando a API estiver pronta, interrompa apenas a visualização dos logs com `Ctrl+C` e valide:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/v1/actuator/health
```

O retorno esperado contém `status` igual a `UP`.

## 4. Front-end

Em outro PowerShell:

```powershell
cd 'C:\Inventory MED\frontend'
npm install
npm start
```

Acesse `http://127.0.0.1:4200` e entre com:

- e-mail: o valor de `BOOTSTRAP_SYSTEM_ADMIN_EMAIL` no `.env`;
- senha: o valor temporário de `BOOTSTRAP_SYSTEM_ADMIN_PASSWORD` no `.env`.

O primeiro acesso exige a troca dessa senha. Depois, o painel geral permite criar o
primeiro hospital e, em seguida, usuários vinculados à unidade.

O Angular encaminha requisições `/api` para `http://127.0.0.1:8080`. O navegador não precisa acessar a porta do SQL Server.

## 5. Encerrar

Encerre o Angular com `Ctrl+C`. Para parar os contêineres:

```powershell
cd 'C:\Inventory MED'
docker compose down
```

Esse comando preserva o banco no volume Docker. `docker compose down -v` também apaga todo o banco local e só deve ser usado quando a intenção for reiniciar os dados de desenvolvimento do zero.

## Portas

| Serviço | Endereço local |
| --- | --- |
| Angular | `http://127.0.0.1:4200` |
| API | `http://127.0.0.1:8080/api/v1` |
| Saúde da API | `http://127.0.0.1:8080/api/v1/actuator/health` |
| SQL Server do projeto | `127.0.0.1:14330` |

A porta externa `14330` evita conflito com instalações locais do SQL Server que normalmente utilizam `1433`. Dentro da rede Docker, a API continua acessando o banco na porta padrão `1433`.

## Estado atual da integração

- login e seleção de hospital usam a API real;
- a autenticação usa sessão armazenada no SQL Server e cookie `HttpOnly`;
- o navegador não guarda JWT, senha ou token de autenticação no `sessionStorage`;
- operações de escrita usam proteção CSRF;
- a seleção do hospital fica na sessão do servidor e o vínculo é revalidado em cada
  requisição;
- o painel do administrador geral, a criação de hospitais, o provisionamento dos bancos
  e a criação de usuários já usam a API e o SQL Server reais;
- quartos, leitos, pacientes, prescrições e evoluções permanecem temporariamente na
  implementação visual anterior e não estão liberados para dados reais até a migração
  de cada módulo.
