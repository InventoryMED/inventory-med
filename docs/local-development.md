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
Copy-Item .env.example .env
notepad .env
```

No `.env`, substitua pelo menos estas credenciais:

- `MSSQL_SA_PASSWORD` e `DB_PASSWORD` devem possuir exatamente o mesmo valor;
- `JWT_SECRET` deve ser um segredo longo, aleatório e ter pelo menos 32 caracteres;
- `BOOTSTRAP_DOCTOR_PASSWORD` será a senha inicial de Lucas Galante.

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
Invoke-RestMethod http://127.0.0.1:8080/api/actuator/health
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

- e-mail: `lucas.galante@inventorymed.local`;
- senha: o valor de `BOOTSTRAP_DOCTOR_PASSWORD` no `.env`.

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
| API | `http://127.0.0.1:8080/api` |
| Saúde da API | `http://127.0.0.1:8080/api/actuator/health` |
| SQL Server do projeto | `127.0.0.1:14330` |

A porta externa `14330` evita conflito com instalações locais do SQL Server que normalmente utilizam `1433`. Dentro da rede Docker, a API continua acessando o banco na porta padrão `1433`.

## Estado atual da integração

- login e seleção de hospital usam a API real;
- o token de acesso fica no `sessionStorage` e é enviado automaticamente à API;
- cada seleção de hospital gera um token limitado à unidade;
- quartos, leitos, pacientes, prescrições e evoluções ainda usam o armazenamento demonstrativo do navegador e serão migrados módulo por módulo para a API.
