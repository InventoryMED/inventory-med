# Acesso seguro ao SQL Server da VPS pelo SSMS

## Objetivo

Permitir que o responsável administre os bancos no SQL Server Management Studio do
Windows sem publicar a porta 1433 na internet.

## Funcionamento

```text
SSMS no Windows
    |
    | conecta em 127.0.0.1,15433
    v
túnel SSH criptografado
    |
    | encaminha para 127.0.0.1:1433 na VPS
    v
SQL Server Express
```

Para o SSMS, parecerá que o SQL Server está instalado no próprio Windows. Na realidade,
todo o tráfego será enviado de forma criptografada para a VPS.

## Pré-requisitos

- SQL Server Management Studio instalado no Windows;
- cliente OpenSSH do Windows;
- chave privada SSH armazenada somente no computador do responsável;
- chave pública correspondente autorizada na VPS;
- usuário administrativo SQL nominal criado na VPS.

Não será necessário abrir a porta 1433 no firewall da Hostinger nem no Ubuntu.

## Abrir o túnel

Em um PowerShell, execute:

```powershell
ssh -o ExitOnForwardFailure=yes `
  -i "$env:USERPROFILE\.ssh\inventorymed_vps_gabriel_ed25519" `
  -N `
  -L 127.0.0.1:15433:127.0.0.1:1433 `
  gabriel@179.236.237.36
```

Significado:

- `-i`: informa qual chave privada será usada;
- `-N`: abre a conexão sem iniciar um terminal remoto;
- `-L`: cria o encaminhamento local;
- `15433`: porta que ficará disponível apenas no Windows;
- `127.0.0.1:1433`: endereço privado do SQL Server dentro da VPS.

O PowerShell deve permanecer aberto enquanto o SSMS estiver conectado. Para encerrar o
túnel, pressione `Ctrl+C`.

## Conectar no SSMS

Use os seguintes campos:

| Campo | Valor |
| --- | --- |
| Tipo de servidor | Mecanismo de Banco de Dados |
| Nome do servidor | `127.0.0.1,15433` |
| Autenticação | Autenticação do SQL Server |
| Login | conta administrativa nominal fornecida na implantação |
| Senha | senha guardada pelo responsável, nunca no GitHub |
| Criptografia | Obrigatória |

Durante a implantação inicial poderá ser necessário marcar temporariamente **Confiar no
certificado do servidor**. Essa decisão será registrada na documentação da instalação.

## Bancos esperados

```text
inventory_med_core
inventory_med_hospital_<id>
```

O primeiro contém usuários, hospitais, vínculos e sessões. Cada hospital possui um
banco operacional independente.

## Operações permitidas pelo SSMS

- consultar tabelas e dados autorizados;
- verificar espaço utilizado;
- executar scripts de diagnóstico versionados;
- acompanhar sessões e bloqueios;
- executar backup e restauração de maneira controlada;
- conferir o histórico de migrações do Flyway.

## Regra para alterações estruturais

O SSMS não será usado para modificar tabelas de maneira definitiva pelo designer.
Alterações como criar tabela, adicionar coluna ou índice devem existir como arquivos
Flyway no GitHub. Dessa forma, todos sabem exatamente quando e por que o banco mudou.

Se uma correção emergencial for executada pelo SSMS, ela deve ser imediatamente:

1. registrada em `docs/changes/`;
2. convertida em script versionado;
3. validada nos demais ambientes.

## Diagnóstico do túnel

Com o túnel aberto, no PowerShell:

```powershell
Test-NetConnection 127.0.0.1 -Port 15433
```

O campo `TcpTestSucceeded` deve retornar `True`.

Na VPS, o SQL Server será verificado com:

```bash
systemctl status mssql-server
sudo ss -lntp | grep 1433
```

O firewall não deverá possuir uma regra pública liberando `1433/tcp`.
