# Procedimento de alterações do Inventory MED

Este procedimento existe para que o responsável pelo sistema consiga saber o que foi
alterado, onde foi alterado, por que foi alterado e como verificar o resultado.

## Regra principal

Cada solicitação que resulte em mudança no código, banco ou VPS deve gerar um registro
em `docs/changes/`.

Uma alteração pode envolver vários arquivos quando eles fazem parte do mesmo objetivo.
Não é necessário criar um documento para cada linha de código, mas nenhuma mudança
funcional ou operacional pode ficar sem registro.

## Nome dos registros

Use o formato:

```text
AAAA-MM-DD-descricao-curta.md
```

Exemplo:

```text
2026-10-02-instalacao-sql-server.md
```

Se houver mais de uma alteração com a mesma descrição no dia, acrescente um número ao
final.

## Conteúdo obrigatório

Todo registro deve conter:

1. **Resumo:** resultado obtido em linguagem simples.
2. **Motivo:** problema ou necessidade atendida.
3. **Arquivos:** lista de arquivos criados, modificados ou removidos.
4. **Banco de dados:** bancos, tabelas, colunas, índices e migrações afetados.
5. **VPS:** comandos executados e explicação de cada grupo de comandos.
6. **Serviços:** serviços instalados, configurados ou reiniciados.
7. **Configurações:** nomes das variáveis alteradas, nunca seus valores secretos.
8. **Validação:** testes e verificações realizados, com resultado.
9. **Recuperação:** como voltar à situação anterior ou restaurar o serviço.
10. **Pendências:** o que ainda não foi concluído.

## Mudanças no banco

- toda mudança estrutural deve ter uma migração Flyway;
- o registro deve apontar o arquivo da migração;
- comandos de diagnóstico podem ser executados no SSMS;
- correções manuais permanentes devem ser transformadas em script versionado;
- nunca colocar senhas ou dados reais no documento;
- antes de mudança destrutiva, criar e validar backup.

## Mudanças na VPS

Sempre que possível, comandos reutilizáveis serão transformados em scripts em
`infra/scripts/`. Arquivos do Nginx e do systemd ficarão respectivamente em
`infra/nginx/` e `infra/systemd/`.

O registro deve explicar:

- o que o comando faz;
- em qual diretório foi executado;
- se exigiu `sudo`;
- qual serviço foi afetado;
- qual foi o resultado esperado e observado.

## Entrega de cada etapa

Ao final de cada alteração, o resumo para o responsável deve informar:

- o resultado entregue;
- onde consultar a documentação;
- quais serviços estão funcionando;
- como testar;
- eventuais riscos ou pendências.

O modelo oficial está em [`changes/TEMPLATE.md`](changes/TEMPLATE.md).
