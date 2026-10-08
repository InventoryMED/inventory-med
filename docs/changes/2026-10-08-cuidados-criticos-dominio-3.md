# Prescrição estruturada de cuidados críticos — Domínio 3

**Data:** 2026-10-08
**Responsável:** equipe Inventory MED
**Status:** concluída

## Resumo

A prescrição passou a contar com um módulo estruturado para drogas vasoativas e
inotrópicos, sedação e analgesia contínuas, bloqueio neuromuscular, antídotos,
reversores e medicamentos de emergência. O backend valida acesso vascular,
monitorização pressórica, BIC, VMI, meta RASS/BIS e a associação obrigatória de
sedação profunda antes de um bloqueador neuromuscular.

O documento impresso recebe os tópicos numerados 14, 15 e 16 e as respectivas
linhas na grade de aprazamento. Insumos e equipamentos são apresentados somente
para conferência assistencial e contratual, sem faturamento automático.

## Motivo

Transformar o direcionamento clínico recebido em uma entrada tipada, auditável e
validada no servidor, reduzindo campos livres e impedindo combinações incompatíveis
com as barreiras de segurança definidas para medicamentos de alta vigilância.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/CriticalCareCatalog.java` | criado | Define o contrato do catálogo controlado. |
| `backend/src/main/java/br/com/inventorymed/clinical/CriticalCareCatalogService.java` | criado | Centraliza medicamentos, vias, metas, monitorização e modalidades permitidas. |
| `backend/src/main/java/br/com/inventorymed/clinical/CriticalCarePrescriptionRequest.java` | criado | Define a entrada tipada e seus limites. |
| `backend/src/main/java/br/com/inventorymed/clinical/CriticalCarePrescriptionResponse.java` | criado | Define resumo, detalhamento, linhas de impressão e conferência. |
| `backend/src/main/java/br/com/inventorymed/clinical/CriticalCarePrescriptionValidator.java` | criado | Implementa as travas clínicas e estruturais no backend. |
| `backend/src/main/java/br/com/inventorymed/clinical/CriticalCarePrescriptionService.java` | criado | Normaliza o documento e gera texto, linhas e itens para revisão. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Valida e normaliza o novo campo antes da persistência. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia apenas aos perfis médicos autenticados. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Inclui `CRITICAL_CARE_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V13__structured_critical_care_prescription.sql` | criado | Publica uma nova versão da prescrição em cada banco hospitalar. |
| `backend/src/test/java/br/com/inventorymed/clinical/CriticalCarePrescriptionServiceTest.java` | criado | Testa saída, BNM, CVC e rejeição de campos indevidos. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Testa a ordem e a persistência normalizada no banco hospitalar. |
| `frontend/src/app/features/medical/critical-care-prescription.component.ts` | criado | Implementa os formulários reativos tipados e a validação pela API. |
| `frontend/src/app/features/medical/critical-care-prescription.component.html` | criado | Organiza visualmente os módulos 14, 15 e 16. |
| `frontend/src/app/features/medical/critical-care-prescription.component.scss` | criado | Reutiliza o sistema visual da prescrição. |
| `frontend/src/app/features/medical/critical-care-prescription.component.spec.ts` | criado | Testa criação e limpeza dos grupos. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Adiciona contratos tipados do novo domínio. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Adiciona chamadas ao catálogo e à prévia. |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | Testa as chamadas HTTP e o cabeçalho antifalsificação. |
| `frontend/src/app/app.ts` | modificado | Integra estado, validação, salvamento e linhas impressas. |
| `frontend/src/app/app.html` | modificado | Posiciona o módulo após o suporte terapêutico e inclui a saída impressa. |
| `frontend/src/app/app.spec.ts` | modificado | Cobre estado inicial e ordenação das linhas. |

## Banco de dados

- bancos afetados: um banco operacional independente por hospital;
- migração: `V13__structured_critical_care_prescription.sql`;
- objetos afetados: restrição de tipos, versão publicada, seção e campo do formulário;
- a versão publicada anterior é preservada e aposentada; documentos históricos
  continuam vinculados ao respectivo snapshot;
- não existe leitura cruzada nem paciente global.

## Comandos executados na VPS

Nenhum. A mudança foi implementada e validada no ambiente local.

## Serviços afetados

- API Java/Spring Boot;
- frontend Angular;
- bancos hospitalares na próxima inicialização da API, pela execução do Flyway.

## Configurações

Nenhuma variável nova e nenhum segredo alterado.

## Validação

- `npm run build`: concluído; permanecem somente os avisos conhecidos de orçamento;
- `npm test -- --watch=false`: 32 testes aprovados;
- `docker build --target build -t inventory-med-backend-check .`: compilação Java aprovada;
- suíte completa da API em contêiner local: 50 testes aprovados, sem falhas;
- o teste de integração criou bancos hospitalares independentes e aplicou as 13
  migrações, incluindo V13, em cada banco;
- API local reconstruída com `docker compose up --build -d api`;
- `GET /api/v1/actuator/health`: estado `UP`;
- catálogo de cuidados críticos sem autenticação: acesso recusado com HTTP 401;
- frontend local carregado sem erros no console do navegador;
- a validação do catálogo e da prévia exige sessão médica autenticada;
- hospital não é enviado pelo navegador e o salvamento usa o banco derivado da sessão;
- os textos orientam conferência; não há lançamento financeiro automático.

### Referências de segurança consultadas

- Ministério da Saúde e Anvisa, Protocolo de Segurança na Prescrição, Uso e
  Administração de Medicamentos:
  `https://www.gov.br/anvisa/pt-br/centraisdeconteudo/publicacoes/servicosdesaude/publicacoes/protocolo-de-seguranca-na-prescricao-uso-e-administracao-de-medicamentos`;
- DailyMed, bula oficial de rocurônio, com exigência de ventilação disponível e
  sedação/anestesia adequada:
  `https://dailymed.nlm.nih.gov/dailymed/fda/fdaDrugXsl.cfm?setid=a54a79f7-5207-43d8-a139-7d57557caa50&type=display`;
- DailyMed, bula oficial de propofol, com descarte do produto e troca do sistema
  de administração em até 12 horas:
  `https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=10272dc1-657d-4798-a34a-b6341b92e560&version=14`;
- DailyMed, bula oficial de nitroprussiato, com proteção contra luz:
  `https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=6fbfb86c-da8d-4741-b1e0-7000e2bbad5f`.

## Recuperação

Antes da implantação, a mudança pode ser removida revertendo o código. Depois de
V13 ser aplicada, a migração não deve ser editada nem apagada: uma nova migração
deve publicar outra versão e desativar o campo, preservando o histórico. O retorno
dos artefatos da versão anterior restaura a aplicação e o esquema permanece
compatível.

## Pendências

- homologar catálogo, limites e textos com responsáveis técnicos, farmácia,
  enfermagem, UTI, segurança do paciente e comissão de farmácia e terapêutica;
- parametrizar protocolos próprios de cada hospital antes do uso assistencial;
- cadastrar contratos e códigos TUSS/SIGTAP somente em um módulo financeiro futuro,
  após validação formal; esta entrega gera apenas itens para conferência;
- homologar visualmente a impressão com combinações extensas;
- publicar na VPS somente após aprovação funcional.
