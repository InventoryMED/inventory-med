# VPS inicial KVM 4

**Data:** 2026-10-02

**Responsável:** equipe Inventory MED

**Status:** concluída

## Resumo

Foi decidido iniciar o desenvolvimento, a homologação e os primeiros pilotos em uma VPS
Hostinger KVM 4, mantendo a arquitetura funcional preparada para pelo menos vinte
hospitais.

## Motivo

O KVM 4 atende a fase atual com menor custo. Não há necessidade de contratar a capacidade
máxima antes de existir carga real, desde que o crescimento seja medido e planejado.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/architecture.md` | modificado | registrou o KVM 4 como primeira VPS |
| `docs/operations/capacity-plan.md` | modificado | ajustou recursos e estratégia de crescimento |
| `docs/changes/2026-10-02-vps-inicial-kvm4.md` | criado | registrou a decisão |

## Banco de dados

- bancos alterados: nenhum;
- migrações executadas: nenhuma;
- dados alterados: nenhum.

## Comandos executados na VPS

Nenhum. A VPS ainda não foi provisionada.

## Serviços afetados

Nenhum.

## Configurações

A referência inicial passou a ser:

- 4 vCPUs;
- 16 GB de RAM;
- 200 GB NVMe;
- 16 TB mensais de tráfego;
- Java com heap inicial próximo de 2 GB.

Nenhuma configuração foi aplicada em servidor nesta etapa.

## Validação

- KVM 4 registrado como ambiente inicial;
- meta de vinte hospitais preservada;
- upgrade condicionado a teste e métricas;
- caminho para KVM 8 ou separação do banco mantido;
- documentação verificada com `git diff --check`.

## Recuperação

Esta etapa altera somente documentação. A referência anterior pode ser recuperada pelo
histórico do Git.

## Pendências

- contratar e provisionar o KVM 4;
- instalar Ubuntu 22.04;
- configurar acesso SSH;
- instalar os componentes da arquitetura;
- executar testes antes de cada etapa de expansão;
- definir o momento de upgrade com base nas métricas reais.
