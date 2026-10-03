# Padrões obrigatórios de qualidade

**Data:** 2026-10-02

**Responsável:** equipe Inventory MED

**Status:** concluída

## Resumo

Foram definidos critérios obrigatórios de qualidade para Java, Angular, banco, testes,
segurança, experiência e identidade visual.

## Motivo

O projeto precisa evitar soluções improvisadas, código difícil de manter e interfaces
genéricas. A qualidade deve ser verificável e fazer parte da definição de pronto.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/development/quality-standards.md` | criado | definiu padrões técnicos e visuais |
| `docs/architecture.md` | modificado | tornou os padrões parte da arquitetura oficial |
| `AGENTS.md` | modificado | tornou a leitura e aplicação dos padrões obrigatória |
| `README.md` | modificado | adicionou o documento ao mapa do projeto |
| `docs/changes/2026-10-02-padroes-de-qualidade.md` | criado | registrou esta decisão |

## Banco de dados

- bancos alterados: nenhum;
- migrações executadas: nenhuma;
- dados alterados: nenhum.

## Comandos executados na VPS

Nenhum. A VPS ainda não foi provisionada.

## Serviços afetados

Nenhum.

## Configurações

Nenhuma configuração de execução ou segredo foi alterado.

## Validação

- padrões ligados à arquitetura oficial;
- critérios de backend, frontend, banco, segurança e design documentados;
- definição de pronto criada;
- documentação verificada com `git diff --check`.

## Recuperação

Esta etapa altera somente documentação. A situação anterior pode ser recuperada pelo
histórico do Git.

## Pendências

- configurar formatadores e analisadores automáticos;
- configurar testes e build obrigatórios na integração contínua;
- implementar o sistema visual no novo frontend;
- revisar cada módulo futuro usando a definição de pronto.
