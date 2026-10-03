# Retificação após finalização

**Data:** 2026-10-02

**Responsável:** equipe Inventory MED

**Status:** concluída

## Resumo

Foi definido que prescrições e evoluções finalizadas podem ser alteradas por profissional
clínico autorizado. A alteração cria uma nova versão ou retificação e nunca sobrescreve
o documento anterior.

## Motivo

Na prática clínica pode ser necessário corrigir ou atualizar um documento já finalizado.
O sistema deve permitir isso sem perder autoria, conteúdo anterior ou rastreabilidade.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/architecture.md` | modificado | definiu confirmação, autoria e vigência da nova versão |
| `docs/product/configurable-forms.md` | modificado | documentou a experiência de alteração após finalização |
| `docs/changes/2026-10-02-retificacao-apos-finalizacao.md` | criado | registrou a decisão |

## Banco de dados

- bancos alterados: nenhum;
- migrações executadas: nenhuma;
- dados alterados: nenhum.

A implementação futura deverá armazenar documento original, nova versão, vínculo com a
versão anterior, situação de vigência, autor, data, justificativa e evento de auditoria.

## Comandos executados na VPS

Nenhum. A VPS ainda não foi provisionada.

## Serviços afetados

Nenhum.

## Configurações

Nenhuma.

## Validação

- possibilidade de alteração após finalização documentada;
- preservação integral da versão anterior mantida;
- confirmação, justificativa e autoria obrigatórias;
- distinção entre versão vigente e substituída registrada;
- documentação verificada com `git diff --check`.

## Recuperação

Esta etapa altera somente documentação. A situação anterior pode ser recuperada pelo
histórico do Git.

## Pendências

- definir janela ou condições de reautenticação;
- definir permissões clínicas atômicas;
- implementar versionamento no banco e no backend;
- criar visualização da linha do tempo e identificação nas impressões;
- criar testes de concorrência para impedir duas versões vigentes.
