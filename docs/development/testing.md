# Testes locais

## Objetivo

Validar o código contra tecnologias equivalentes às de produção sem alterar a VPS nem
usar dados reais. Os testes de backend iniciam um SQL Server 2022 descartável; os testes
do frontend executam no ambiente isolado do Angular.

## Backend com Java 21 instalado

Pré-requisitos:

- Java 21;
- Docker Desktop ativo;
- acesso ao mecanismo Docker local.

```powershell
cd 'C:\Inventory MED\backend'
.\mvnw.cmd clean test
```

O Testcontainers cria um SQL Server temporário, executa a migração Flyway, roda os
testes e remove o contêiner ao final.

## Backend sem Java instalado no Windows

No computador atual, Maven e Java podem rodar em um contêiner somente para teste:

```powershell
cd 'C:\Inventory MED'

docker run --rm `
  --add-host host.docker.internal:host-gateway `
  -e TESTCONTAINERS_RYUK_DISABLED=true `
  -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal `
  --mount type=bind,src=/var/run/docker.sock,dst=/var/run/docker.sock `
  -v inventorymed-maven-cache:/root/.m2 `
  -v "${PWD}:/workspace" `
  -w /workspace/backend `
  maven:3.9.11-eclipse-temurin-21 `
  mvn clean test
```

`TESTCONTAINERS_RYUK_DISABLED` é necessário apenas porque o Maven está dentro de outro
contêiner no Docker Desktop. As classes de teste continuam encerrando o SQL Server
temporário. Ao final, confirme que não restou um contêiner de teste:

```powershell
docker ps --format "table {{.Names}}\t{{.Image}}"
```

Os contêineres permanentes `inventory-med-api-1` e
`inventory-med-sqlserver-1` pertencem ao ambiente local e não devem ser removidos por
essa verificação.

## Frontend

```powershell
cd 'C:\Inventory MED\frontend'
npm test
npm run build
```

## Critério para concluir uma etapa

- todos os testes terminam com código de saída zero;
- o build de produção do Angular é concluído;
- `git diff --check` não encontra erro de espaço em branco;
- nenhuma senha, cookie, chave ou dado de paciente aparece em logs ou arquivos
  versionados;
- a alteração possui registro em `docs/changes/`.

Docker não faz parte da implantação oficial na VPS. Ele é apenas uma ferramenta de
desenvolvimento e teste local.
