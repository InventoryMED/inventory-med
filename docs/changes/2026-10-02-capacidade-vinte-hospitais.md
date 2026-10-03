# Capacidade atualizada para vinte hospitais

**Data:** 2026-10-02

**Responsável:** equipe Inventory MED

**Status:** concluída

## Resumo

O dimensionamento oficial foi alterado de seis para pelo menos vinte hospitais, com
aproximadamente 300 usuários cadastrados e até 100 sessões simultaneamente ativas como
cenário de referência.

## Motivo

O Inventory MED deve nascer preparado para uma operação diária maior. O plano anterior
subestimava a quantidade de unidades esperada.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/architecture.md` | modificado | atualizou o dimensionamento e a VPS de referência |
| `docs/operations/capacity-plan.md` | modificado | recalculou usuários, tráfego, testes e crescimento |
| `docs/changes/2026-10-02-capacidade-vinte-hospitais.md` | criado | registrou a substituição da referência anterior |

## Banco de dados

- bancos alterados: nenhum;
- migrações executadas: nenhuma;
- dados alterados: nenhum.

A avaliação identificou o SQL Server Express como principal limite potencial por
compartilhar recursos entre vinte bancos hospitalares e possuir limites próprios de
edição.

## Comandos executados na VPS

Nenhum. A VPS ainda não foi provisionada.

## Serviços afetados

Nenhum.

## Configurações

Nenhuma configuração de execução foi alterada. A referência de infraestrutura passou de
KVM 4 para KVM 8.

## Validação

- meta de vinte hospitais registrada;
- 300 contas e 100 sessões ativas consideradas;
- teste com 300 sessões autenticadas e 150 usuários operando definido;
- tráfego mensal recalculado;
- limites de disco e SQL Server considerados;
- caminho para separar banco e aplicação documentado;
- documentação verificada com `git diff --check`.

## Recuperação

Esta etapa altera somente documentação. A referência anterior permanece registrada no
histórico e pode ser restaurada pelo Git.

## Pendências

- confirmar orçamento do KVM 8;
- provisionar a VPS;
- medir latência real;
- implementar os casos de uso;
- executar teste de carga;
- validar o SQL Server Express com vinte bancos ativos;
- definir o ponto econômico para migração de edição ou serviço de banco.
