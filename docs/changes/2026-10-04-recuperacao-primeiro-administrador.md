# Recuperação do primeiro administrador geral

**Data:** 2026-10-04
**Responsável:** equipe Inventory MED
**Status:** concluída

## Resumo

Foi criado um procedimento seguro para cadastrar o primeiro `ADMIN_SISTEMA` quando a
configuração inicial da VPS não contém as variáveis de bootstrap e o banco central ainda
não possui uma conta administrativa.

## Motivo

A implantação pública estava saudável, mas não existia nenhum registro com o papel
`ADMIN_SISTEMA`. O login administrativo retornava credenciais inválidas porque a conta
nunca havia sido criada.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `infra/scripts/recover-system-administrator.sh` | criado | cria de forma controlada o primeiro administrador geral e remove a senha temporária da configuração |
| `docs/changes/2026-10-04-recuperacao-primeiro-administrador.md` | criado | registra diagnóstico, execução, validação e recuperação |

## Banco de dados

- banco afetado: `inventory_med_core`;
- migrações: nenhuma;
- objetos afetados: `dbo.app_user` e `dbo.system_user_role`, somente pelo bootstrap da
  aplicação;
- o script se recusa a executar se já existir um `ADMIN_SISTEMA` ou se o e-mail já
  pertencer a outro usuário.

## Comandos executados na VPS

Diagnóstico sem exibição de senha ou hash:

```bash
/opt/mssql-tools18/bin/sqlcmd \
  -S 127.0.0.1 \
  -U "$DB_MIGRATION_USERNAME" \
  -d inventory_med_core \
  -C -W \
  -Q "SET NOCOUNT ON; SELECT u.email, u.active, u.must_change_password, r.role FROM dbo.app_user u INNER JOIN dbo.system_user_role r ON r.user_id = u.id;"
```

Execução da recuperação:

```bash
sudo bash infra/scripts/recover-system-administrator.sh
```

O script solicita e-mail e senha temporária diretamente no terminal, reinicia a API,
confirma a criação da conta, desativa novamente o bootstrap e remove a senha temporária
do arquivo de ambiente.

## Serviços afetados

- `inventory-med-api`: reiniciado para criar a conta e novamente para retirar o
  bootstrap;
- Nginx e SQL Server não são reiniciados.

## Configurações

- `BOOTSTRAP_ENABLED`;
- `BOOTSTRAP_SYSTEM_ADMIN_EMAIL`;
- `BOOTSTRAP_SYSTEM_ADMIN_PASSWORD`, presente somente durante a criação e removida ao
  final.

Nenhum valor secreto é registrado no repositório ou impresso pelo script.

## Validação

- consulta inicial confirmou ausência de `ADMIN_SISTEMA`;
- análise sintática do script;
- confirmação de serviço saudável feita automaticamente em cada reinício;
- confirmação automática de uma conta ativa com o papel `ADMIN_SISTEMA`;
- confirmação de login e troca de senha: pendente da execução na VPS pelo responsável.

## Recuperação

O script mantém uma cópia temporária protegida do arquivo de ambiente. Se ocorrer falha
depois de iniciar a alteração, a configuração anterior é restaurada e a API é
reiniciada. A cópia temporária é removida ao encerrar.

Como a criação da conta é aditiva, uma conta já criada não é apagada automaticamente.
Se a execução for interrompida depois da gravação no banco, deve-se primeiro consultar
o papel administrativo antes de repetir qualquer recuperação.

## Pendências

- validar o primeiro login e a troca obrigatória da senha na interface pública.
