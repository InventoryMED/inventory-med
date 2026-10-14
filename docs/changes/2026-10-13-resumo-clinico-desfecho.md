# Resumo clínico obrigatório para desfechos da internação

**Data:** 2026-10-13
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída

## Resumo

Foi criada a aba `RESUMO CLÍNICO` para documentar alta hospitalar, transferência para
outro hospital ou óbito. Alta, transferência externa e óbito somente encerram a
internação e liberam o leito depois que um resumo compatível estiver finalizado.

O formulário apresenta os dados já conhecidos da internação, campos específicos para
cada desfecho, histórico imutável e uma versão própria para impressão. A autoria, data,
hospital e documento utilizado no encerramento permanecem auditáveis.

## Motivo

O encerramento de uma internação precisa preservar a síntese clínica e os dados do
desfecho, sem depender apenas de um motivo curto de alta e sem permitir que o frontend
libere um leito sem a documentação clínica exigida.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalSummary*.java` | criado | Catálogo, contrato, validação e normalização do resumo clínico no backend. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Normaliza o novo campo estruturado antes de persistir o documento. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalRequests.java` | modificado | Recebe o identificador do resumo usado no encerramento. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo, prévia e encaminha o vínculo do resumo na alta. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceService.java` | modificado | Impede encerramento sem resumo finalizado e compatível e registra o vínculo na auditoria. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormKind.java` | modificado | Inclui `CLINICAL_SUMMARY`. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Inclui `CLINICAL_SUMMARY_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V20__clinical_discharge_summary.sql` | criado | Atualiza restrições, vincula o resumo à admissão e publica o modelo inicial. |
| `backend/src/test/java/br/com/inventorymed/clinical/ClinicalSummaryServiceTest.java` | criado | Valida os três desfechos, opções inválidas e alta interna sem encerramento. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Testa migração, bloqueio sem resumo, encerramento válido e isolamento hospitalar. |
| `frontend/src/app/features/medical/clinical-summary-workspace.component.*` | criado | Implementa formulário, histórico, prévia, impressão e conclusão do desfecho. |
| `frontend/src/app/features/medical/clinical-documentation-tabs.component.*` | modificado | Inclui a aba `RESUMO CLÍNICO`. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Adiciona contratos tipados do resumo clínico. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Integra catálogo, prévia e encerramento vinculado ao resumo. |
| `frontend/src/app/features/medical/provisional-admissions.component.*` | modificado | Disponibiliza o resumo para admissões ainda sem leito. |
| `frontend/src/app/app.ts` e `frontend/src/app/app.html` | modificado | Integram rota, botão no leito e continuação obrigatória do fluxo de desfecho. |
| `frontend/src/app/models.ts` | modificado | Registra a nova tela na navegação tipada. |
| `frontend/src/app/**/*.spec.ts` | modificado | Cobre navegação, serviço, emissão provisória e componente novo. |
| `docs/architecture.md` | modificado | Formaliza a regra arquitetural do resumo obrigatório e a assinatura ainda pendente. |

## Banco de dados

- bancos afetados: todos os bancos operacionais hospitalares;
- migração: `V20__clinical_discharge_summary.sql`;
- objetos afetados: restrições de `form_template`, `clinical_document` e `form_field`;
- coluna criada: `admission.outcome_summary_document_id`;
- relacionamento criado: resumo finalizado associado ao desfecho da admissão;
- modelo publicado: `RESUMO CLÍNICO DE DESFECHO PADRÃO`.

A migração é executada pelo Flyway. Não há comando SQL manual a ser aplicado no SSMS.

## Comandos executados na VPS

Nenhum. Esta alteração foi implementada e validada localmente e ainda não foi publicada.

## Serviços afetados

- `inventory-med-api`: será atualizado quando a versão for publicada;
- Nginx: sem alteração de configuração;
- SQL Server: receberá a migração V20 pelo Flyway na inicialização da API;
- frontend Angular: receberá a nova aba e o novo fluxo na publicação.

## Configurações

Nenhuma variável de ambiente nova foi criada e nenhum segredo foi registrado.

## Validação

- testes unitários do serviço de resumo clínico: aprovados;
- teste de integração com SQL Server real, Flyway e isolamento por hospital: aprovado;
- testes do frontend: aprovados;
- build de produção do Angular: aprovado;
- formatação e verificação de diferenças: aprovadas.

## Recuperação

Antes da publicação, gerar backup dos bancos hospitalares. Se a versão precisar ser
retirada, restaurar a versão anterior da API e do frontend pelo mecanismo de releases e
restaurar os bancos a partir do backup anterior à V20. A migração não deve ser removida
ou revertida manualmente em produção.

## Pendências

- integrar um provedor homologado para assinatura digital ICP-Brasil; até lá o sistema
  registra autoria e data na auditoria interna, sem alegar assinatura certificada;
- validar os códigos de motivo de saída com a competência SIGTAP e os contratos de cada
  hospital antes de qualquer integração de faturamento;
- ampliar futuramente a composição assistida do texto com dados estruturados de toda a
  internação; nesta versão o texto inicial usa somente admissão e diagnóstico já
  cadastrados e sempre exige revisão médica.
