# Imutabilidade de prescrições e evoluções

**Data:** 2026-10-02

**Responsável:** equipe Inventory MED

**Status:** concluída

## Resumo

Foi oficializada a regra de que prescrições e evoluções finalizadas nunca podem ser
alteradas ou apagadas pelo fluxo normal do sistema.

## Motivo

Documentos clínicos precisam preservar autoria, conteúdo e sequência histórica. Uma
alteração invisível prejudicaria a segurança do paciente, a rastreabilidade e a defesa
dos profissionais e hospitais.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/architecture.md` | modificado | adicionou o ciclo de vida imutável dos documentos clínicos |
| `docs/product/configurable-forms.md` | modificado | separou evolução do modelo e imutabilidade do documento emitido |
| `docs/changes/2026-10-02-imutabilidade-documentos-clinicos.md` | criado | registrou a decisão |

## Banco de dados

- bancos alterados: nenhum;
- migrações executadas: nenhuma;
- dados alterados: nenhum.

A implementação futura deverá incluir versão, estado, vínculo com documento anterior,
autor, data e motivo. As contas da aplicação não receberão permissão de exclusão física
dos documentos finalizados.

## Comandos executados na VPS

Nenhum. A VPS ainda não foi provisionada.

## Serviços afetados

Nenhum.

## Configurações

Nenhuma.

## Validação

- regra incluída na arquitetura oficial;
- comportamento de rascunho, finalização, retificação, substituição e cancelamento
  documentado;
- proteção contra exclusão administrativa registrada;
- documentação verificada com `git diff --check`.

## Recuperação

Esta etapa altera somente documentação. A situação anterior pode ser recuperada pelo
histórico do Git.

## Pendências

- implementar estados e versionamento nas entidades;
- criar migrações Flyway;
- implementar autorizações e auditoria;
- criar testes que comprovem a impossibilidade de apagar ou sobrescrever documentos
  finalizados.
