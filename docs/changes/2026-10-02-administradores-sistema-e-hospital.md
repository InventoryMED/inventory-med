# Administradores de sistema e hospital

**Data:** 2026-10-02

**Responsável:** equipe Inventory MED

**Status:** concluída

## Resumo

Foram oficializados dois níveis administrativos: `ADMIN_SISTEMA`, responsável pela
plataforma inteira, e `ADMIN_HOSPITAL`, responsável somente pela unidade à qual está
vinculado.

## Motivo

O Inventory MED precisa ser operado centralmente e, ao mesmo tempo, permitir que cada
hospital tenha autonomia para administrar usuários, estrutura física e configurações
sem acessar outras unidades.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/architecture.md` | modificado | definiu os dois níveis e o modo de suporte |
| `docs/security/administrative-access.md` | modificado | detalhou responsabilidades e limites |
| `docs/changes/2026-10-02-administradores-sistema-e-hospital.md` | criado | registrou a decisão |

## Banco de dados

- bancos alterados: nenhum;
- migrações executadas: nenhuma;
- dados alterados: nenhum.

A implementação futura precisará representar papéis globais separadamente de papéis
hospitalares e registrar o contexto de suporte.

## Comandos executados na VPS

Nenhum. A VPS ainda não foi provisionada.

## Serviços afetados

Nenhum.

## Configurações

Nenhuma.

## Validação

- responsabilidades globais e hospitalares separadas;
- promoção para administrador do sistema bloqueada ao administrador hospitalar;
- acesso clínico global não concedido automaticamente;
- modo de suporte hospitalar auditado e com prazo documentado;
- isolamento entre hospitais preservado;
- documentação verificada com `git diff --check`.

## Recuperação

Esta etapa altera somente documentação. A situação anterior pode ser recuperada pelo
histórico do Git.

## Pendências

- detalhar permissões atômicas;
- implementar papéis globais e hospitalares no banco;
- implementar modo de suporte temporário;
- criar painéis administrativos separados;
- criar testes de elevação de privilégio e isolamento.
