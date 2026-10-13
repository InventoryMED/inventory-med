# Solicitação estruturada de exames diagnósticos

**Data:** 2026-10-13
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída

## Resumo

Foi criado um módulo de solicitação de exames no leito do paciente, disponível também
para admissões provisórias. Ele reúne pacotes rápidos, exames de sangue e urina,
tomografia, raio-X com incidências, ultrassonografia e ECG. As solicitações podem ser
visualizadas no histórico, finalizadas e impressas.

## Motivo

Centralizar o pedido de exames na mesma internação usada pelos demais documentos
clínicos, reduzir digitação repetitiva e oferecer validações de segurança sem transferir
regra de negócio para o frontend.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/tenant/V19__diagnostic_exam_requests.sql` | criado | Inclui o tipo de documento `EXAM`, o campo estruturado e o modelo publicado inicial. |
| `backend/src/main/java/br/com/inventorymed/clinical/DiagnosticExam*.java` | criado | Define catálogo, contrato, validação, normalização, auditoria e detecção de duplicidade. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia hospital-específicos para médicos autorizados. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Normaliza o pedido no backend antes de gravar o documento clínico. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormKind.java` | modificado | Inclui o tipo clínico `EXAM`. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Inclui o campo `DIAGNOSTIC_EXAM_PLAN`. |
| `frontend/src/app/features/medical/diagnostic-exam-workspace.component.*` | criado | Implementa formulário, pacotes rápidos, histórico, prévia e impressão. |
| `frontend/src/app/features/medical/clinical-documentation-tabs.component.*` | modificado | Habilita a aba de exames diagnósticos. |
| `frontend/src/app/features/medical/provisional-admissions.component.*` | modificado | Disponibiliza exames para pacientes ainda sem leito. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Tipifica catálogo, pedido, resposta e documento de exame. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Adiciona as chamadas HTTP do módulo. |
| `frontend/src/app/app.*` | modificado | Adiciona botão no leito, rota por guia e renderização da tela. |
| `docs/architecture.md` | modificado | Registra as invariantes do novo documento clínico. |

## Banco de dados

- bancos afetados: todos os bancos hospitalares, sempre de forma isolada;
- migração: `tenant/V19__diagnostic_exam_requests.sql`;
- objetos afetados: restrições de `form_template`, `clinical_document` e `form_field`;
- dados iniciais: um modelo publicado de solicitação de exames por banco hospitalar;
- não há tabela global de pacientes nem consulta clínica entre bancos.

## Comandos executados na VPS

Nenhum. A alteração permanece no ambiente local e não foi publicada.

Na implantação futura, o serviço de instalação executará as migrações Flyway V18 e V19
em cada banco hospitalar antes de liberar a nova versão da API.

## Serviços afetados

- API Java `inventory-med-api`;
- frontend Angular servido pelo Nginx;
- SQL Server, por migração Flyway em cada banco hospitalar.

## Configurações

Nenhuma variável de ambiente ou segredo novo.

## Validação

- backend: 75 testes aprovados, incluindo SQL Server real, migrações Flyway V18/V19,
  autorização por perfil, criação do modelo, finalização, isolamento hospitalar e aviso
  de repetição em menos de 12 horas;
- frontend: 51 testes aprovados em 14 arquivos;
- formatação Prettier aprovada;
- build Angular de produção aprovado; permanecem apenas os avisos já conhecidos de
  orçamento do pacote inicial e do `app.scss`.

## Recuperação

Em caso de falha na implantação, restaurar o pacote anterior da API e do frontend pelo
procedimento de rollback. A migração V19 adiciona valores permitidos e um modelo; não
apagar solicitações finalizadas por SQL. Qualquer reversão estrutural exige backup e
validação banco a banco.

## Pendências

- validar e versionar o catálogo completo de códigos TUSS e SIGTAP com o setor de
  faturamento de cada hospital antes de uso para cobrança;
- definir, por hospital, integrações futuras com laboratório, radiologia e faturamento;
- o módulo atual gera referências para conferência e não lança cobrança automática.
