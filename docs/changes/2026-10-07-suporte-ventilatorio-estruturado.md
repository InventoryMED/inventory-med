# Suporte ventilatório estruturado na prescrição

**Data:** 2026-10-07
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída em ambiente local

## Resumo

A tela de prescrição passou a possuir um bloco estruturado para ar ambiente,
oxigenoterapia de baixo fluxo, cânula nasal de alto fluxo, ventilação não invasiva e
ventilação mecânica invasiva. O médico seleciona o suporte e informa somente os
parâmetros aplicáveis ao dispositivo ou modo escolhido.

A API valida limites e compatibilidade, produz o texto clínico, prepara as linhas da
prescrição impressa e mantém uma lista separada de insumos e equipamentos para revisão
de auditoria. Nenhum item é lançado automaticamente no faturamento.

## Motivo

Transformar a especificação clínica fornecida em um fluxo explícito, executável e
auditável, evitando texto livre para parâmetros ventilatórios críticos e mantendo as
regras clínicas no backend.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/VentilatorySupportCatalog.java` | criado | Define o catálogo tipado entregue ao frontend. |
| `backend/src/main/java/br/com/inventorymed/clinical/VentilatorySupportCatalogService.java` | criado | Centraliza dispositivos, modos, interfaces, frequências, metas e modelos permitidos. |
| `backend/src/main/java/br/com/inventorymed/clinical/VentilatorySupportRequest.java` | criado | Define o contrato estruturado e limites básicos da entrada. |
| `backend/src/main/java/br/com/inventorymed/clinical/VentilatorySupportResponse.java` | criado | Separa texto clínico, linhas impressas e revisão de auditoria. |
| `backend/src/main/java/br/com/inventorymed/clinical/VentilatorySupportValidator.java` | criado | Valida no backend faixas, combinações, frequência, aprazamento e campos condicionais. |
| `backend/src/main/java/br/com/inventorymed/clinical/VentilatorySupportService.java` | criado | Gera resumo, detalhamento, linhas da prescrição e itens para conferência. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia somente aos perfis médicos da unidade selecionada na sessão. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Normaliza e valida o plano ventilatório antes de salvar no banco do hospital. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Inclui o tipo `VENTILATORY_SUPPORT_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V8__structured_ventilatory_support.sql` | criado | Publica uma nova versão do formulário sem alterar documentos anteriores. |
| `backend/src/test/java/br/com/inventorymed/clinical/VentilatorySupportServiceTest.java` | criado | Testa baixo fluxo, CNAF, VNI, VMI, rejeições e campos desconhecidos. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Valida a V8 e o armazenamento normalizado no banco hospitalar da sessão. |
| `frontend/src/app/features/medical/ventilatory-support.component.*` | criado | Implementa o formulário responsivo, modelos, parâmetros condicionais e prévia. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Adiciona contratos tipados do suporte ventilatório. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Adiciona chamadas ao catálogo e à validação da API. |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | Testa contratos HTTP e proteção CSRF. |
| `frontend/src/app/app.ts` | modificado | Integra validação, salvamento, limpeza e linhas da impressão. |
| `frontend/src/app/app.html` | modificado | Posiciona o novo bloco na prescrição e no documento impresso. |
| `frontend/src/app/app.spec.ts` | modificado | Testa inicialização e inclusão das linhas ventilatórias na impressão. |

## Banco de dados

- bancos afetados quando a versão for implantada: todos os bancos operacionais dos hospitais;
- migração: `V8__structured_ventilatory_support.sql`;
- objetos afetados: restrição `ck_form_field_type`, `form_template_version`,
  `form_section`, `form_field` e `form_field_option`;
- pacientes e documentos clínicos permanecem no banco exclusivo de cada hospital;
- versões anteriores dos formulários e prescrições finalizadas não são alteradas;
- a nova prescrição armazena a solicitação validada, o texto gerado, as linhas de
  execução e os itens de auditoria no JSON do documento clínico.

## Comandos executados na VPS

Nenhum. Esta alteração não foi implantada na VPS.

## Serviços afetados

- API Spring Boot, quando implantada;
- frontend Angular servido pelo Nginx, quando implantado;
- SQL Server somente pela migração Flyway executada pela API.

## Configurações

Nenhuma variável nova e nenhum segredo foram adicionados.

## Validação

- `npm test -- --watch=false`: 21 testes aprovados em 5 arquivos;
- `npm run build`: concluído; permaneceram somente os avisos já conhecidos de orçamento
  do bundle inicial (626,62 kB para limite de 500 kB) e de `app.scss` (50,10 kB para
  limite de 48 kB);
- `mvn clean test` com Java 21 e SQL Server 2022 descartável: 29 testes aprovados;
- Flyway aplicou as oito migrações nos bancos hospitalares descartáveis e confirmou a V8;
- o teste integrado salvou o suporte ventilatório no banco exclusivo do hospital da
  sessão e leu o texto normalizado;
- campos desconhecidos, parâmetros incompatíveis, faixas inválidas e aprazamentos
  incoerentes são rejeitados pelo backend;
- o navegador não envia identificador de hospital ou conexão de banco;
- itens de gases, materiais e equipamentos são sugestões para conferência, sem geração
  automática de cobrança.

## Recuperação

Antes da implantação, basta reverter os arquivos desta alteração. Depois que a V8 for
aplicada em produção, o histórico do Flyway não deve ser removido. A recuperação exige
migração compensatória ou restauração validada dos bancos hospitalares, seguida da
reinstalação da versão anterior da API e do frontend.

## Pendências

- homologar conteúdo, limites, nomenclaturas e modelos com direção técnica, medicina
  intensiva, fisioterapia respiratória e segurança do paciente;
- validar com faturamento e contratos de cada hospital quais itens de revisão podem ser
  efetivamente cobrados e qual evidência operacional é necessária;
- realizar inspeção visual do fluxo autenticado e do PDF com dados fictícios;
- implantar na VPS somente após aprovação explícita desta versão local.
