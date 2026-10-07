# Reabilitação multidisciplinar estruturada na prescrição

**Data:** 2026-10-07
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída em ambiente local

## Resumo

A prescrição passou a possuir um bloco estruturado para fisioterapia respiratória,
fisioterapia motora e mobilização precoce, fonoaudiologia e terapia ocupacional. O
médico seleciona especialidade, conduta, frequência e aprazamento e pode adicionar até
doze condutas no mesmo documento.

A API exige justificativa clínica para fisioterapia duas ou três vezes ao dia, gera o
texto clínico e as linhas da impressão e mantém os itens de auditoria separados. O
sistema não lança códigos TUSS/SIGTAP ou cobranças automaticamente.

## Motivo

Transformar a especificação multidisciplinar fornecida em uma prescrição executável e
auditável, com opções consistentes por especialidade e proteção contra combinações
inválidas ou sessões repetidas sem justificativa clínica.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/RehabilitationPrescriptionCatalog.java` | criado | Define o catálogo tipado entregue ao frontend. |
| `backend/src/main/java/br/com/inventorymed/clinical/RehabilitationPrescriptionCatalogService.java` | criado | Centraliza especialidades, condutas, frequências, aprazamentos e modelos permitidos. |
| `backend/src/main/java/br/com/inventorymed/clinical/RehabilitationPrescriptionRequest.java` | criado | Define o contrato estruturado recebido pela API. |
| `backend/src/main/java/br/com/inventorymed/clinical/RehabilitationPrescriptionResponse.java` | criado | Separa o texto clínico, linhas de impressão e itens de auditoria. |
| `backend/src/main/java/br/com/inventorymed/clinical/RehabilitationPrescriptionValidator.java` | criado | Valida especialidade, conduta, frequência, aprazamento e justificativa no backend. |
| `backend/src/main/java/br/com/inventorymed/clinical/RehabilitationPrescriptionService.java` | criado | Gera a prescrição e os itens individualizados para conferência. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia somente aos perfis médicos da unidade da sessão. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Normaliza a reabilitação antes de armazená-la no banco hospitalar. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Inclui o tipo `REHABILITATION_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V9__structured_multidisciplinary_rehabilitation.sql` | criado | Publica nova versão do formulário e preserva documentos e versões anteriores. |
| `backend/src/test/java/br/com/inventorymed/clinical/RehabilitationPrescriptionServiceTest.java` | criado | Testa geração, justificativa, compatibilidade e rejeição de campos desconhecidos. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Valida a V9 e o armazenamento no banco exclusivo do hospital da sessão. |
| `frontend/src/app/features/medical/rehabilitation-prescription.component.*` | criado | Implementa o formulário responsivo, modelos, justificativa e prévia da API. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Adiciona contratos tipados da reabilitação. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Adiciona chamadas ao catálogo e à validação da API. |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | Testa contratos HTTP e proteção CSRF. |
| `frontend/src/app/app.ts` | modificado | Integra validação, salvamento, limpeza e impressão. |
| `frontend/src/app/app.html` | modificado | Posiciona a reabilitação na prescrição e no documento impresso. |
| `frontend/src/app/app.spec.ts` | modificado | Testa inicialização e inclusão na tabela impressa. |

## Banco de dados

- bancos afetados quando a versão for implantada: todos os bancos operacionais dos hospitais;
- migração: `V9__structured_multidisciplinary_rehabilitation.sql`;
- objetos afetados: restrição `ck_form_field_type`, `form_template_version`,
  `form_section`, `form_field` e `form_field_option`;
- pacientes e documentos permanecem no banco exclusivo de cada hospital;
- prescrições finalizadas e versões de formulário anteriores permanecem inalteradas;
- o novo documento armazena solicitação, texto normalizado, linhas de execução e itens
  de auditoria no JSON da prescrição.

## Comandos executados na VPS

Nenhum. Esta alteração não foi implantada na VPS.

## Serviços afetados

- API Spring Boot, quando implantada;
- frontend Angular servido pelo Nginx, quando implantado;
- SQL Server somente pela migração Flyway executada pela API.

## Configurações

Nenhuma variável nova e nenhum segredo foram adicionados.

## Validação

- `npm test -- --watch=false`: 24 testes aprovados em 6 arquivos;
- `npm run build`: concluído; permaneceram somente os avisos já conhecidos de orçamento
  do bundle inicial (643,40 kB para limite de 500 kB) e de `app.scss` (50,10 kB para
  limite de 48 kB);
- `mvn clean test` com Java 21 e SQL Server 2022 descartável: 34 testes aprovados;
- Flyway aplicou nove migrações em bancos hospitalares descartáveis e confirmou a V9;
- o teste integrado salvou reabilitação no banco exclusivo do hospital da sessão;
- conduta de outra especialidade, aprazamento incoerente, campos desconhecidos e alta
  frequência de fisioterapia sem justificativa são rejeitados pela API;
- o navegador não informa hospital, banco ou credencial;
- códigos e cobranças são apenas itens para conferência de auditoria.

## Recuperação

Antes da implantação, basta reverter os arquivos desta alteração. Depois que a V9 for
aplicada em produção, o histórico do Flyway não deve ser removido. A recuperação exige
migração compensatória ou restauração validada dos bancos hospitalares, seguida da
reinstalação da versão anterior da API e do frontend.

## Pendências

- homologar condutas, frequências, nomenclaturas e exigência de justificativa com
  fisioterapia, fonoaudiologia, terapia ocupacional e direção técnica;
- cadastrar ou validar códigos TUSS/SIGTAP e regras contratuais por hospital antes de
  qualquer integração financeira;
- validar o fluxo de deglutição em conjunto com dieta, neurologia e pós-extubação;
- realizar inspeção visual do fluxo autenticado e do PDF com dados fictícios;
- implantar na VPS somente após aprovação explícita desta versão local.
