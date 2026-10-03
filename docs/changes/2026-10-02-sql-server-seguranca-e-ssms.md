# Segurança da instância SQL e acesso por SSMS

**Data:** 2026-10-02
**Responsável:** Gabriel Pereira
**Status:** concluída

## Resumo

A instância SQL Server foi limitada a 4096 MB, recebeu diretório dedicado de backup e
passou a escutar somente no endereço local da VPS. O acesso pelo SSMS foi validado por
um túnel SSH na porta local 15433.

## Motivo

Evitar exposição pública do banco, preservar memória para os demais componentes e
permitir administração gráfica segura pelo computador autorizado.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `/var/opt/mssql/mssql.conf` | modificado na VPS | define memória, endereço local e diretório de backup |
| `/var/opt/mssql/backups` | criado na VPS | armazena backups sob propriedade do serviço SQL |
| `docs/operations/sql-server.md` | modificado | documenta o túnel SSH e a porta local escolhida |
| `docs/changes/2026-10-02-sql-server-express.md` | modificado | remove pendências concluídas |
| `docs/changes/2026-10-02-sql-server-seguranca-e-ssms.md` | criado | registra a configuração aplicada |

## Banco de dados

- bancos de usuário afetados: nenhum;
- migrações: nenhuma;
- objetos afetados: nenhum.

## Comandos executados na VPS

```bash
sudo install -d -m 750 -o mssql -g mssql /var/opt/mssql/backups
sudo /opt/mssql/bin/mssql-conf set filelocation.defaultbackupdir /var/opt/mssql/backups
sudo /opt/mssql/bin/mssql-conf set memory.memorylimitmb 4096
sudo /opt/mssql/bin/mssql-conf set network.ipaddress 127.0.0.1
sudo systemctl restart mssql-server
```

## Serviços afetados

- `mssql-server`: reiniciado para aplicar os limites.

## Configurações

- memória máxima do processo: 4096 MB;
- endereço de escuta: `127.0.0.1`;
- porta interna: 1433;
- porta local do túnel no Windows: 15433;
- porta pública do banco: nenhuma;
- diretório de backup: `/var/opt/mssql/backups`.

## Validação

- consulta local com `sqlcmd` concluída;
- túnel confirmado como processo `ssh` na porta 15433;
- conexão do SSMS concluída usando `127.0.0.1,15433`;
- conflito anterior identificado: Docker Desktop ocupava `127.0.0.1:14330` enquanto o
  primeiro túnel estava vinculado somente ao IPv6 `::1`;
- porta 1433 permaneceu ausente do UFW.

## Recuperação

As configurações individuais podem ser removidas com `mssql-conf unset`, seguidas de
reinicialização controlada do serviço. A porta pública não deve ser aberta como forma de
contornar problemas no túnel.

## Pendências

- criar e testar login nominal de DBA;
- deixar `sa` reservado para recuperação e não para uso cotidiano;
- criar política e teste de backup;
- criar bancos e login restrito da aplicação por migrações.
