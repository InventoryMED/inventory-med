# Ativação segura do provisionamento hospitalar

**Data:** 2026-10-04
**Responsável:** equipe Inventory MED
**Status:** concluída

## Resumo

Foi criado um procedimento operacional para habilitar, em uma VPS já configurada, a
criação automática dos bancos exclusivos de cada hospital sem recriar o banco central
nem alterar as credenciais existentes da aplicação.

## Motivo

O painel administrativo estava acessível, mas informava que o provisionamento de
hospitais não estava habilitado. A inspeção protegida do arquivo de ambiente confirmou
que as quatro configurações de provisionamento ainda não existiam na VPS.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `infra/scripts/enable-tenant-provisioning.sh` | criado | cria/atualiza o login técnico restrito, gera segredos e habilita o provisionamento |
| `docs/operations/vps-deployment.md` | modificado | documenta a ativação posterior em instalações existentes |
| `docs/changes/2026-10-04-ativacao-provisionamento-hospitalar.md` | criado | registra diagnóstico, segurança, validação e recuperação |

## Banco de dados

- banco central consultado: `inventory_med_core`;
- migrações: nenhuma;
- objetos de esquema afetados: nenhum;
- objeto de servidor criado ou atualizado: login
  `inventorymed_tenant_provisioner`;
- permissões concedidas ao login: `CREATE ANY DATABASE` e `ALTER ANY LOGIN`;
- a conta `sa` é usada somente durante a preparação e sua senha não fica em arquivo ou
  argumento de processo;
- a ativação é recusada se já houver credenciais hospitalares criptografadas, evitando
  substituir a chave usada por hospitais existentes.

## Comandos executados na VPS

Diagnóstico inicial sem revelar valores secretos:

```bash
sudo awk -F= '
/^TENANT_PROVISIONING_ENABLED=/ { print $1 "=" $2 }
/^(DB_PROVISIONING_URL|DB_PROVISIONING_USERNAME|DB_PROVISIONING_PASSWORD|TENANT_CREDENTIAL_ENCRYPTION_KEY)=/ { print $1 "=CONFIGURADO" }
' /etc/inventory-med/api.env
```

Ativação:

```bash
sudo bash infra/scripts/enable-tenant-provisioning.sh
```

O script solicita a senha atual de `sa` de forma oculta, gera a senha técnica e a chave
AES-256, atualiza o arquivo protegido, reinicia a API e verifica sua saúde.

Na VPS, o arquivo validado foi executado a partir de
`/home/gabriel/enable-tenant-provisioning.sh`. O SQL Server confirmou o contexto
`master`, e o script concluiu informando que o provisionamento estava habilitado e a
API saudável. Nenhum valor secreto foi exibido ou registrado.

## Serviços afetados

- `inventory-med-api`: reiniciado uma vez depois da configuração;
- SQL Server permanece ativo e acessível somente localmente;
- Nginx não é reiniciado.

## Configurações

- `TENANT_PROVISIONING_ENABLED`;
- `DB_PROVISIONING_URL`;
- `DB_PROVISIONING_USERNAME`;
- `DB_PROVISIONING_PASSWORD`;
- `TENANT_CREDENTIAL_ENCRYPTION_KEY`.

Os valores secretos são gerados na VPS, armazenados somente no arquivo de ambiente
protegido e nunca registrados no Git ou no terminal.

## Validação

- ausência inicial das configurações confirmada;
- análise sintática do script executada com `bash -n` na VPS;
- SHA-256 do arquivo enviado idêntico ao arquivo versionado;
- verificação automática das duas permissões do login técnico aprovada;
- saúde da API verificada automaticamente após o reinício e externamente com resultado
  `UP`;
- serviços `inventory-med-api` e `mssql-server` ativos;
- porta SQL `1433` escutando somente em `127.0.0.1` e inacessível publicamente;
- criação dos bancos hospitalares pelo painel: pendente da execução pelo responsável.

## Recuperação

Antes da alteração, o script cria uma cópia temporária protegida do arquivo de ambiente.
Se a API não ficar saudável, restaura a configuração anterior e reinicia o serviço. A
cópia temporária e o arquivo SQL que contém a senha gerada são removidos ao encerrar.

O login técnico pode permanecer criado caso uma falha ocorra depois do comando SQL. Ele
não será usado pela aplicação enquanto as variáveis não estiverem habilitadas; uma nova
execução controlada rotacionará sua senha.

## Pendências

- reprovisionar os dois hospitais legados pelo painel;
- validar que cada hospital recebe banco, login e migrações independentes.
