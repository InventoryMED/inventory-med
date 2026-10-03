# Acesso médico e configuração administrativa por hospital

**Data:** 2026-10-03
**Responsável:** equipe Inventory MED
**Status:** concluída no ambiente local

## Resumo

O acesso médico deixou de usar os dados demonstrativos e passou a trabalhar com o
banco exclusivo do hospital selecionado na sessão. O administrador geral agora possui
interfaces para gerenciar usuários, acessos hospitalares, unidades de internação,
quartos, leitos e versões configuráveis de prescrições e evoluções.

## Motivo

Disponibilizar a primeira operação real do sistema sem misturar dados entre hospitais e
permitir que a estrutura e os formulários clínicos evoluam por configuração controlada,
sem colocar regras de negócio no Angular.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/tenant/V2__clinical_workspace_and_configurable_forms.sql` | criado | cria pacientes, internações, documentos clínicos e definições versionadas de formulários no banco hospitalar |
| `backend/src/main/resources/db/migration/tenant/V3__default_clinical_form_templates.sql` | criado | publica modelos iniciais de prescrição e evolução para novos e atuais bancos hospitalares |
| `backend/src/main/java/br/com/inventorymed/tenancy/` | modificado/criado | abre conexões isoladas por hospital, executa Flyway e usa a credencial exclusiva da unidade |
| `backend/src/main/java/br/com/inventorymed/hospitals/` | criado | implementa criação, edição, ordem e ativação de unidades, quartos e leitos |
| `backend/src/main/java/br/com/inventorymed/formtemplates/` | criado | implementa rascunho, publicação, versionamento, ordem e ativação de campos e opções |
| `backend/src/main/java/br/com/inventorymed/clinical/` | criado | implementa área médica, admissão, transferência, alta e documentos clínicos validados pelo backend |
| `backend/src/main/java/br/com/inventorymed/administration/` | modificado | adiciona ativação e desativação de contas e perfis hospitalares |
| `frontend/src/app/features/medical/` | criado | apresenta quartos, leitos, pacientes e formulários clínicos reais do hospital da sessão |
| `frontend/src/app/features/administration/` | modificado/criado | adiciona gestão visual de estrutura hospitalar, acessos e formulários clínicos |
| `frontend/src/app/app.ts` e `frontend/src/app/app.html` | modificado | substitui o bloqueio demonstrativo pela área médica real |

## Banco de dados

- bancos afetados: um banco exclusivo de cada hospital, nunca o conjunto de hospitais;
- migrações: `V2__clinical_workspace_and_configurable_forms.sql` e
  `V3__default_clinical_form_templates.sql`;
- objetos afetados: `care_unit`, `room`, `bed`, `patient`, `admission`,
  `form_template`, `form_template_version`, `form_section`, `form_field`,
  `form_field_option`, `clinical_document` e `clinical_audit_event`;
- documentos finalizados não possuem rota de sobrescrita; somente rascunhos do próprio
  autor podem ser atualizados;
- o hospital é obtido da sessão autenticada. Os endpoints clínicos não recebem um
  identificador livre de hospital do navegador.

## Comandos executados na VPS

Nenhum. Esta alteração foi implementada e validada somente no ambiente local.

## Serviços afetados

- API local reconstruída e reiniciada pelo Docker Compose;
- frontend local mantido pelo servidor Angular em `127.0.0.1:4200`;
- SQL Server local permaneceu restrito a `127.0.0.1:14330`.

## Configurações

Nenhuma variável nova. Permanecem necessárias as configurações de conexão do banco
central, credencial técnica de provisionamento e chave de criptografia das credenciais
hospitalares já documentadas.

## Validação

- build Java 21 concluído com sucesso;
- 11 testes de integração do backend concluídos sem falhas contra SQL Server 2022
  descartável;
- Flyway aplicou com sucesso as versões V1, V2 e V3 em banco hospitalar descartável;
- validação SQL independente confirmou 13 tabelas, dois modelos publicados e 38 campos;
- 13 testes do frontend concluídos sem falhas;
- build de produção do Angular concluído com sucesso;
- saúde da API local retornou `UP` após a reconstrução.

## Recuperação

O código pode ser revertido pelo Git antes de implantação. Em banco que já recebeu as
migrações, não apagar tabelas nem documentos clínicos. A recuperação deve restaurar o
backup anterior ou aplicar uma nova migração corretiva, preservando histórico e
auditoria.

## Pendências

- implementar o fluxo auditável de retificação de documento finalizado;
- implementar a consulta de auditoria clínica e administrativa na interface;
- concluir o escopo administrativo do perfil `ADMIN_HOSPITAL`;
- exigir MFA antes de permitir dados clínicos reais em produção;
- criar o layout definitivo de impressão usando os documentos persistidos.
