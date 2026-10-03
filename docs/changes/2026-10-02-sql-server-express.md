# Instalação do SQL Server 2022 Express

**Data:** 2026-10-02
**Responsável:** Gabriel Pereira
**Status:** concluída

## Resumo

O SQL Server 2022 Express e as ferramentas `mssql-tools18` foram instalados na VPS. A
edição, o serviço e uma consulta local foram validados, sem liberar a porta do banco no
firewall.

## Motivo

Disponibilizar o banco relacional escolhido para o Inventory MED com administração por
SSMS e isolamento de rede compatível com a arquitetura do sistema.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `/etc/apt/sources.list.d/mssql-server-2022.list` | criado na VPS | repositório assinado do SQL Server 2022 |
| `/etc/apt/sources.list.d/microsoft-prod.list` | criado na VPS | repositório assinado das ferramentas Microsoft |
| `/usr/share/keyrings/microsoft-prod.gpg` | criado na VPS | chave de verificação dos pacotes Microsoft |
| `docs/operations/sql-server.md` | criado | documenta operação e acesso seguro ao banco |
| `docs/changes/2026-10-02-sql-server-express.md` | criado | registra a instalação |

## Banco de dados

- instância instalada: SQL Server 2022 Express;
- bancos de sistema validados: `master`, `tempdb`, `model` e `msdb`;
- bancos da aplicação: ainda não criados;
- migrações: nenhuma.

## Comandos executados na VPS

Foram adicionados os repositórios oficiais Microsoft, instalados `mssql-server`,
`mssql-tools18` e `unixodbc-dev`, executado o assistente `mssql-conf setup` e realizada
uma consulta com `sqlcmd`.

As senhas não foram incluídas nos comandos documentados nem neste registro.

## Serviços afetados

- `mssql-server`: instalado, habilitado e ativo.

## Configurações

- edição: Express;
- plataforma: Linux x64 sobre Ubuntu 22.04.5;
- porta padrão: TCP 1433;
- regra pública no UFW: nenhuma;
- usuário `sa`: senha definida e armazenada pelo responsável fora do repositório.

## Validação

- `mssql-server` retornou `enabled` e `active`;
- log confirmou `Express Edition (64-bit)`;
- porta 1433 identificada pelo sistema operacional;
- UFW confirmou ausência de liberação da porta 1433;
- `sqlcmd` conectou localmente e listou os bancos de sistema.

Durante a primeira tentativa, o APT recusou o repositório por não encontrar a chave
associada. A entrada foi corrigida com `signed-by=/usr/share/keyrings/microsoft-prod.gpg`
antes da instalação. Nenhum pacote não verificado foi instalado.

## Recuperação

Antes de remover ou reinstalar o SQL Server, devem ser preservados os backups e
confirmados os caminhos de dados. A remoção não deve ser feita automaticamente em um
ambiente que contenha dados.

## Pendências

- criar login nominal de DBA;
- criar bancos e login restrito da aplicação por migrações controladas.
