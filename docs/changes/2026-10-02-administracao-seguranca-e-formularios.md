# Administração, segurança e formulários configuráveis

**Data:** 2026-10-02

**Responsável:** equipe Inventory MED

**Status:** concluída

## Resumo

Foram definidos os perfis administrativos, as regras para gerenciar hospitais,
usuários, quartos e leitos, e o modelo seguro de campos configuráveis para prescrições
e evoluções. Também foi reforçado que as regras de negócio e autorizações pertencem ao
backend.

## Motivo

O produto precisa ser administrável sem alteração de código, mantendo isolamento entre
hospitais, proteção contra elevação de privilégio e preservação do histórico clínico.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/architecture.md` | modificado | incorporou administração, formulários dinâmicos e segurança administrativa |
| `docs/security/administrative-access.md` | criado | detalhou perfis, matriz de acesso, MFA e auditoria |
| `docs/product/configurable-forms.md` | criado | definiu construção e versionamento de formulários |
| `README.md` | modificado | adicionou os novos documentos ao mapa do projeto |
| `docs/changes/2026-10-02-administracao-seguranca-e-formularios.md` | criado | registrou esta decisão |

## Banco de dados

- bancos alterados: nenhum;
- migrações executadas: nenhuma;
- dados alterados: nenhum.

Foram definidas regras futuras para tabelas de permissões, auditoria e modelos
versionados. A implementação ocorrerá por migrações Flyway próprias.

## Comandos executados na VPS

Nenhum. A VPS ainda não foi provisionada.

## Serviços afetados

Nenhum.

## Configurações

Nenhuma configuração de execução ou segredo foi alterado.

## Validação

- documentação confrontada com a arquitetura oficial;
- isolamento hospitalar mantido;
- autorização definida como responsabilidade exclusiva do backend;
- documentos clínicos emitidos protegidos contra edição silenciosa;
- registro verificado com `git diff --check`.

## Recuperação

Como esta etapa altera somente documentação, é possível recuperar a situação anterior
revertendo estes arquivos pelo histórico do Git.

## Pendências

- detalhar permissões atômicas de cada módulo;
- escolher e implementar o mecanismo de MFA;
- implementar tabelas e migrações;
- criar telas administrativas;
- criar testes automatizados de isolamento e autorização.
