# Operação do SQL Server

## Instância

- produto: Microsoft SQL Server 2022;
- edição: Express;
- sistema operacional: Ubuntu 22.04 LTS;
- serviço: `mssql-server`;
- porta interna: TCP 1433;
- exposição pública: nenhuma.

## Princípios de acesso

O SQL Server deve aceitar conexões somente no endereço local da VPS. A API Java será
executada no mesmo servidor e utilizará `127.0.0.1:1433`.

O SSMS no computador administrativo deve se conectar por um túnel SSH. A porta 1433
não deve ser adicionada ao UFW nem ao firewall gerenciado da Hostinger.

Comando validado no PowerShell do Windows:

```powershell
ssh -o ExitOnForwardFailure=yes -i "$env:USERPROFILE\.ssh\inventorymed_vps_gabriel_ed25519" -N -L 127.0.0.1:15433:127.0.0.1:1433 gabriel@179.236.237.36
```

No SSMS, o servidor é `127.0.0.1,15433`. A janela do túnel deve permanecer aberta
durante a administração.

A porta local 14330 não é utilizada porque estava ocupada pelo Docker Desktop no
computador administrativo. O uso de `ExitOnForwardFailure=yes` faz a conexão falhar
explicitamente se a porta escolhida já estiver ocupada.

## Administração local

As ferramentas oficiais estão disponíveis em:

```text
/opt/mssql-tools18/bin/sqlcmd
/opt/mssql-tools18/bin/bcp
```

Atalhos em `/usr/local/bin` permitem executar `sqlcmd` e `bcp` diretamente.

Uma consulta local nunca deve receber a senha na linha de comando. Exemplo:

```bash
sqlcmd -S localhost -U sa -C
```

## Limites iniciais

- limite do processo SQL Server: 4096 MB;
- backups: `/var/opt/mssql/backups`;
- arquivos de dados e log: diretórios padrão gerenciados pelo SQL Server;
- edição Express: respeitar os limites técnicos e de tamanho por banco da edição.

O limite de memória é um teto, não uma reserva fixa. Ele preserva recursos para Java,
Nginx, sistema operacional e administração gráfica.

## Credenciais

- `sa` é utilizado somente na configuração inicial;
- a senha fica no gerenciador de senhas do responsável e nunca no Git;
- será criado um login nominal de DBA;
- a aplicação receberá login próprio, com permissões mínimas e sem `sysadmin`;
- credenciais serão fornecidas à API por arquivo de ambiente protegido, fora do
  repositório.

## Verificação

```bash
systemctl is-active mssql-server
sudo ss -lntp | grep 1433
sudo cat /var/opt/mssql/mssql.conf
sqlcmd -S localhost -U sa -C -Q "SELECT SERVERPROPERTY('Edition');"
```

No Windows, a porta local do túnel deve pertencer ao processo `ssh`:

```powershell
Get-NetTCPConnection -LocalPort 15433 -State Listen
```

## Atualizações

Atualizações do pacote não devem ser aplicadas sem:

1. backup validado;
2. leitura das notas da versão;
3. janela de manutenção;
4. teste de inicialização e consulta após a atualização.
