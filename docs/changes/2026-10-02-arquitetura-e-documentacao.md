# Arquitetura e documentação obrigatória

**Data:** 2026-10-02

**Responsável:** equipe Inventory MED

**Status:** concluída

## Resumo

Foi definida a arquitetura oficial e criado um procedimento obrigatório para registrar
todas as mudanças futuras. Também foi documentado o acesso seguro ao SQL Server pelo
SSMS usando túnel SSH.

## Motivo

O projeto precisava de uma estrutura simples, compreensível e estável, além de um
histórico que permita ao responsável saber onde e por que cada alteração ocorreu.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/architecture.md` | modificado | tornou-se a fonte oficial da arquitetura |
| `AGENTS.md` | criado | tornou as regras arquiteturais obrigatórias |
| `README.md` | modificado | passou a apontar para os documentos oficiais |
| `docs/change-management.md` | criado | definiu como documentar mudanças |
| `docs/operations/ssms-access.md` | criado | documentou o acesso pelo SSMS |
| `docs/changes/TEMPLATE.md` | criado | criou o modelo dos próximos registros |
| `docs/changes/2026-10-02-arquitetura-e-documentacao.md` | criado | registrou esta alteração |

## Banco de dados

- bancos afetados: nenhum;
- migrações executadas: nenhuma;
- dados alterados: nenhum.

Foi apenas definida a decisão futura de utilizar um banco central e um banco operacional
independente para cada hospital.

## Comandos executados na VPS

Nenhum. A VPS ainda não foi provisionada.

## Serviços afetados

Nenhum serviço foi instalado ou reiniciado.

## Configurações

Nenhum segredo ou configuração de execução foi alterado.

## Validação

- documentação revisada no repositório;
- verificação de formatação executada com `git diff --check`;
- regras obrigatórias registradas no arquivo `AGENTS.md`.

## Recuperação

Como não houve alteração em execução, a recuperação consiste em reverter os arquivos de
documentação pelo histórico do Git.

## Pendências

- contratar e provisionar a VPS;
- gerar a chave SSH;
- instalar e configurar os componentes;
- criar o usuário administrativo SQL nominal;
- validar o primeiro acesso pelo SSMS.
