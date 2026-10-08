# Precauções e isolamento estruturados na prescrição

**Data:** 2026-10-07
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída localmente

## Resumo

A tela de prescrição passou a permitir a seleção estruturada de precaução padrão,
contato, gotículas, aerossóis e isolamento protetor. O médico informa o motivo ou
patógeno, a duração ou reavaliação e o aprazamento. O backend valida a seleção, gera o
texto clínico para impressão, orientações assistenciais e itens separados para
conferência da CCIH e da auditoria.

Nenhuma diária, EPI ou procedimento é faturado automaticamente. A resposta indica
somente o que deve ser conferido pela equipe responsável antes do faturamento.

## Motivo

Padronizar as orientações de biossegurança na prescrição, reduzir omissões de proteção
da equipe e registrar claramente a indicação e a duração do isolamento. A estrutura
também destaca germes multirresistentes que exigem confirmação microbiológica e
acompanhamento pela CCIH.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/IsolationPrecautionCatalog.java` | criado | Contrato do catálogo de tipos, durações, aprazamentos e modelos. |
| `backend/src/main/java/br/com/inventorymed/clinical/IsolationPrecautionCatalogService.java` | criado | Define opções e modelos seguros de precaução. |
| `backend/src/main/java/br/com/inventorymed/clinical/IsolationPrecautionRequest.java` | criado | Define e limita os dados aceitos pela API. |
| `backend/src/main/java/br/com/inventorymed/clinical/IsolationPrecautionResponse.java` | criado | Separa conteúdo clínico de itens para conferência de auditoria. |
| `backend/src/main/java/br/com/inventorymed/clinical/IsolationPrecautionValidator.java` | criado | Valida identificadores, catálogo, motivo, duração e aprazamento. |
| `backend/src/main/java/br/com/inventorymed/clinical/IsolationPrecautionService.java` | criado | Gera e normaliza o plano no backend e rejeita campos desconhecidos. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Normaliza o novo campo ao salvar a prescrição no banco hospitalar da sessão. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia somente aos papéis médicos autorizados. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Adiciona o tipo seguro `ISOLATION_PRECAUTIONS_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V10__structured_isolation_precautions.sql` | criado | Publica nova versão do modelo de prescrição com o campo estruturado. |
| `backend/src/test/java/br/com/inventorymed/clinical/IsolationPrecautionServiceTest.java` | criado | Testa conteúdo clínico, alertas CCIH, validações e formato permitido. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Valida a V10 e o salvamento no banco exclusivo do hospital selecionado. |
| `frontend/src/app/features/medical/isolation-precautions.component.ts` | criado | Controla o formulário reativo e solicita validação ao backend. |
| `frontend/src/app/features/medical/isolation-precautions.component.html` | criado | Implementa a interface clínica, modelos e prévias. |
| `frontend/src/app/features/medical/isolation-precautions.component.scss` | criado | Aplica o sistema visual existente e os estados responsivos. |
| `frontend/src/app/features/medical/isolation-precautions.component.spec.ts` | criado | Testa modelo, validação e limpeza do formulário. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Adiciona os contratos tipados do novo módulo. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Adiciona as chamadas de catálogo e prévia. |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | Testa URLs, corpo e proteção CSRF das novas chamadas. |
| `frontend/src/app/app.ts` | modificado | Integra estado, validação, salvamento e linhas de impressão. |
| `frontend/src/app/app.html` | modificado | Inclui o formulário e o bloco próprio no documento impresso. |
| `frontend/src/app/app.spec.ts` | modificado | Testa estado vazio e linha de impressão das precauções. |

## Banco de dados

- bancos afetados: cada banco hospitalar, sem tabela ou paciente global;
- migração: `V10__structured_isolation_precautions.sql`;
- objetos afetados: restrição `ck_form_field_type`, nova versão publicada do modelo
  `PRESCRIÇÃO MÉDICA PADRÃO`, seção `PRECAUCOES_ISOLAMENTO` e campo `PLANO`;
- documentos antigos permanecem vinculados às versões anteriores e não são alterados;
- o hospital é derivado exclusivamente da sessão autenticada.

## Comandos executados na VPS

Nenhum.

## Serviços afetados

Nenhum serviço da VPS foi reiniciado nesta alteração local.

## Configurações

Nenhuma variável ou segredo novo.

## Validação

- `npm test -- --watch=false`: 27 testes aprovados em 7 arquivos;
- `npm run build`: concluído; permanecem os avisos já conhecidos de orçamento do
  bundle inicial e de `app.scss`;
- `mvn clean test` com Java 21 e SQL Server 2022 descartável: 39 testes aprovados;
- Flyway aplicou V1 a V10 em bancos hospitalares descartáveis e validou o isolamento;
- `npx prettier --check "src/**/*.{ts,html,scss}"`: aprovado;
- `git diff --check`: aprovado;
- verificação do diff e dos 13 arquivos novos por padrões de segredo: nenhum segredo
  identificado;
- `docker compose up --build -d api`: imagem local reconstruída e API iniciada;
- `GET /api/v1/actuator/health`: estado local `UP`;
- endpoint protegido de catálogo sem sessão autenticada: resposta `401`, conforme esperado;
- frontend local em `http://127.0.0.1:4200/`: resposta HTTP `200`.

## Referências clínicas consultadas

- Protocolo de Precauções e Isolamento em Serviços de Saúde da Anvisa, versão 1;
- Nota Técnica GVIMS/GGTES/Anvisa nº 04/2020, atualizada em 24/06/2024;
- Guideline for Isolation Precautions do CDC e seus resumos oficiais.

Essas referências orientaram a separação entre precaução padrão e precauções baseadas
na transmissão. O protocolo institucional e a CCIH continuam responsáveis pela
homologação final e por situações específicas de cada hospital.

## Recuperação

Antes da implantação, a mudança pode ser desfeita revertendo o commit. Depois que a V10
for aplicada em um hospital, não se deve apagar nem editar a migração. Uma correção deve
ser feita por nova migração Flyway, publicando outra versão do formulário. Em caso de
falha de implantação, o script de produção restaura o JAR e o frontend anteriores; o
registro clínico existente permanece preservado.

## Pendências

- homologar textos, modelos, critérios de suspensão e equipamentos com a CCIH e o
  responsável clínico de cada hospital;
- mapear códigos TUSS/SIGTAP e regras contratuais por hospital antes de qualquer uso em
  faturamento; o sistema atual apenas sinaliza itens para conferência;
- revisar visualmente o PDF com combinações longas de múltiplas precauções;
- o arquivo de requisito recebido termina incompleto na linha 110, no meio do primeiro
  exemplo; se houver exemplos ou regras posteriores, eles ainda precisam ser enviados e
  avaliados.
