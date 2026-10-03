# Capacidade inicial para seis hospitais

**Data:** 2026-10-02

**Responsável:** equipe Inventory MED

**Status:** concluída

## Resumo

Foi registrado o dimensionamento inicial para seis hospitais, com aproximadamente 15
usuários por unidade e 90 usuários cadastrados no total.

## Motivo

Era necessário avaliar se a VPS KVM 4 suporta o cenário inicial e estabelecer critérios
objetivos para validar e acompanhar a capacidade.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/operations/capacity-plan.md` | criado | documentou cenário, métricas, testes e crescimento |
| `docs/architecture.md` | modificado | incorporou a capacidade inicial de referência |
| `README.md` | modificado | adicionou o documento ao mapa do projeto |
| `docs/changes/2026-10-02-capacidade-seis-hospitais.md` | criado | registrou a decisão |

## Banco de dados

- bancos alterados: nenhum;
- migrações executadas: nenhuma;
- dados alterados: nenhum.

## Comandos executados na VPS

Nenhum. A VPS ainda não foi provisionada.

## Serviços afetados

Nenhum.

## Configurações

Nenhuma configuração de execução foi alterada. Foram apenas registrados valores
iniciais esperados para Java, SQL Server e monitoramento.

## Validação

- cálculo de tráfego mensal exemplificado;
- limites do SQL Server Express considerados;
- cenários de teste de carga definidos;
- gatilhos de capacidade documentados;
- isolamento e concorrência incluídos nos critérios;
- documentação verificada com `git diff --check`.

## Recuperação

Esta etapa altera somente documentação. A situação anterior pode ser recuperada pelo
histórico do Git.

## Pendências

- provisionar a VPS;
- medir latência real dos hospitais até o datacenter;
- implementar os principais casos de uso;
- criar e executar teste de carga;
- configurar alertas e coleta das métricas.
