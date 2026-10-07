# Monitorização e controles globais na prescrição

**Data:** 2026-10-07
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída em ambiente local

## Resumo

A prescrição passou a possuir um bloco estruturado de monitorização e controles
globais. O médico pode prescrever sinais vitais, escalas clínicas, DXT, protocolos
glicêmicos, balanço hídrico, controle de diurese e débitos e monitorização invasiva.

A API valida as escolhas, monta o texto clínico armazenado no documento e gera uma
lista separada de insumos e alertas para conferência. Essa lista não autoriza nem lança
cobranças automaticamente.

## Motivo

Substituir o bloco simples de dados vitais e as orientações fixas de DXT por um fluxo
estruturado, explícito e auditável, conforme a especificação clínica fornecida para a
prescrição.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/MonitoringPrescriptionCatalog.java` | criado | Define as opções permitidas pelo backend. |
| `backend/src/main/java/br/com/inventorymed/clinical/MonitoringPrescriptionRequest.java` | criado | Define o contrato estruturado recebido pela API. |
| `backend/src/main/java/br/com/inventorymed/clinical/MonitoringPrescriptionResponse.java` | criado | Separa texto clínico e itens de auditoria. |
| `backend/src/main/java/br/com/inventorymed/clinical/MonitoringPrescriptionService.java` | criado | Centraliza validação, geração do texto e estimativas para conferência. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia somente para perfis médicos autorizados pela sessão hospitalar. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Normaliza o bloco antes de armazená-lo no documento clínico. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Inclui o tipo de campo `MONITORING_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V7__structured_monitoring_and_global_controls.sql` | criado | Publica uma nova versão do formulário em cada banco hospitalar e preserva versões anteriores. |
| `backend/src/test/java/br/com/inventorymed/clinical/MonitoringPrescriptionServiceTest.java` | criado | Testa enfermaria, terapia intensiva, validações e rejeição de campos desconhecidos. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Testa a migração V7 e o armazenamento normalizado no banco exclusivo do hospital. |
| `frontend/src/app/features/medical/monitoring-prescription.component.*` | criado | Implementa o formulário visual em quatro grupos e a prévia validada pela API. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Adiciona os contratos tipados da monitorização. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Adiciona chamadas ao catálogo e à prévia da API. |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | Testa os novos contratos HTTP e a proteção CSRF. |
| `frontend/src/app/app.ts` | modificado | Integra validação, salvamento, impressão e linhas horárias de SSVV e DXT. |
| `frontend/src/app/app.html` | modificado | Substitui dados vitais/DXT fixos pelo novo bloco e atualiza a impressão. |
| `frontend/src/app/app.scss` | modificado | Organiza a impressão da monitorização e preserva o quadro de check-in do DXT. |
| `frontend/src/app/app.spec.ts` | modificado | Atualiza a expectativa de inicialização da prescrição. |
| `frontend/src/app/features/medical/nursing-care-prescription.component.*` | modificado | Remove a seleção duplicada de balanço hídrico da interface de cuidados de enfermagem. |

## Banco de dados

- bancos afetados quando a versão for implantada: todos os bancos operacionais dos hospitais;
- migração: `V7__structured_monitoring_and_global_controls.sql`;
- objetos afetados: restrição `ck_form_field_type`, `form_template_version`,
  `form_section`, `form_field` e `form_field_option`;
- documentos e versões antigas permanecem inalterados;
- o novo documento armazena a solicitação validada, o texto estruturado e os itens de
  conferência no JSON auditável da prescrição.

## Comandos executados na VPS

Nenhum. Esta alteração não foi implantada na VPS.

## Serviços afetados

- API Spring Boot, quando implantada;
- frontend Angular servido pelo Nginx, quando implantado;
- SQL Server somente por migrações Flyway executadas pela API.

## Configurações

Nenhuma variável nova e nenhum segredo foram adicionados.

## Validação

- `npm test -- --watch=false`: 18 testes aprovados;
- `npm run build`: concluído; permaneceram apenas os avisos de orçamento já conhecidos
  do bundle inicial (593,01 kB para limite de 500 kB) e de `app.scss` (50,10 kB
  para limite de 48 kB);
- `mvn clean test` com Java 21 e SQL Server 2022 descartável: 24 testes aprovados;
- Flyway aplicou as sete migrações em bancos hospitalares descartáveis e confirmou a
  versão V7;
- o teste de integração salvou uma prescrição com monitorização no banco do hospital da
  sessão e leu o texto normalizado;
- nenhuma conexão de banco ou identificador de hospital foi aceito pelo navegador;
- itens financeiros são apresentados somente como estimativas para conferência.
- estrutura e posição do novo bloco foram inspecionadas no frontend local; o fluxo
  autenticado completo e o PDF ainda dependem da homologação abaixo.
- API local respondeu `UP`, e o catálogo clínico recusou acesso sem sessão com HTTP 401.

## Recuperação

Antes da implantação, basta reverter os arquivos desta alteração. Depois que a V7 for
aplicada em produção, não se deve apagar o histórico do Flyway. A recuperação deve usar
uma migração compensatória ou a restauração validada dos bancos hospitalares, seguida da
reinstalação da versão anterior da API e do frontend.

## Pendências

- homologar o conteúdo clínico, as nomenclaturas e os protocolos com a direção técnica,
  enfermagem, farmácia e núcleo de segurança do paciente;
- validar com faturamento os textos de conferência de materiais conforme os contratos
  de cada hospital;
- realizar inspeção visual da prescrição e do PDF com dados fictícios;
- implantar somente após aprovação explícita da versão local.
