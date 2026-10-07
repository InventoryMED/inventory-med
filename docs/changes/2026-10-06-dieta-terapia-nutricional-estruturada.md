# Dieta e terapia nutricional estruturadas

**Data:** 2026-10-06
**Responsável:** equipe Inventory MED
**Status:** concluída

## Resumo

A seção simples de dieta da prescrição foi substituída por um formulário estruturado
para dieta oral, dieta enteral, nutrição parenteral e jejum. A API fornece o catálogo,
valida os campos condicionais e gera o resumo clínico, o detalhamento e os itens de
auditoria. O frontend somente coleta os parâmetros e apresenta o resultado validado.

Os itens financeiros são apresentados como pontos para conferência. O sistema não
realiza lançamento nem confirma cobrança automaticamente, pois cobertura, contrato e
regras institucionais precisam ser verificados pelo hospital.

## Motivo

O campo livre e os seis atalhos anteriores não representavam adequadamente via,
consistência, regime de infusão, fórmula, vazão, lavagem de sonda, metas, justificativa
de nutrição parenteral e reavaliação do jejum. A estrutura nova reduz ambiguidades e
mantém a decisão clínica com o prescritor.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/DietType.java` | criado | Define as quatro modalidades aceitas. |
| `backend/src/main/java/br/com/inventorymed/clinical/DietPrescriptionRequest.java` | criado | Contrato tipado dos parâmetros clínicos. |
| `backend/src/main/java/br/com/inventorymed/clinical/DietPrescriptionResponse.java` | criado | Contrato do resumo, detalhamento e auditoria. |
| `backend/src/main/java/br/com/inventorymed/clinical/DietPrescriptionCatalog.java` | criado | Catálogo entregue ao frontend. |
| `backend/src/main/java/br/com/inventorymed/clinical/DietPrescriptionService.java` | criado | Valida e normaliza a terapia nutricional. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Revalida e normaliza a dieta ao salvar a prescrição. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia somente a perfis médicos autorizados. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Adiciona o tipo seguro `DIET_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V5__structured_diet_prescription.sql` | criado | Publica a versão seguinte do modelo de prescrição com dieta estruturada. |
| `backend/src/test/java/br/com/inventorymed/clinical/DietPrescriptionServiceTest.java` | criado | Testa geração e validações condicionais. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Valida migração, salvamento e normalização em banco hospitalar. |
| `frontend/src/app/features/medical/diet-prescription.component.*` | criado | Componente responsivo com Reactive Forms e revisão da dieta. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Adiciona contratos tipados da dieta. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Integra catálogo e prévia da API. |
| `frontend/src/app/features/medical/medical.service.spec.ts` | criado | Verifica contratos HTTP e proteção CSRF. |
| `frontend/src/app/app.ts` | modificado | Integra o resultado validado à prescrição. |
| `frontend/src/app/app.html` | modificado | Usa o novo componente e inclui o detalhamento na impressão. |
| `frontend/src/app/app.scss` | modificado | Adiciona o bloco compacto de dieta na impressão. |

## Banco de dados

- bancos afetados: todos os bancos hospitalares, cada um de forma independente;
- migração: `V5__structured_diet_prescription.sql`;
- objetos afetados: restrição `ck_form_field_type`, `form_template_version`,
  `form_section` e `form_field`;
- a versão publicada anterior da prescrição é aposentada e preservada;
- a nova versão utiliza o tipo `DIET_PLAN`; documentos antigos e seus retratos não são
  alterados.

## Comandos executados na VPS

Nenhum. A alteração não foi implantada na VPS nesta etapa.

Quando houver implantação, o Flyway executará a migração separadamente em cada banco
hospitalar na primeira abertura da conexão correspondente.

## Serviços afetados

- `inventory-med-api`: deverá ser reiniciado pelo processo normal de implantação;
- Nginx e SQL Server não exigem alteração manual.

## Configurações

Nenhuma variável de ambiente nova.

## Validação

- `npm test`: 16 testes aprovados;
- `npm run build`: build de produção aprovado;
- `mvn clean test` em Java 21 e SQL Server 2022 descartável: 16 testes aprovados;
- Flyway aplicou as cinco migrações do banco hospitalar e confirmou a versão `v5`;
- a integração confirmou que a dieta é normalizada pelo backend antes de integrar um
  documento finalizado;
- o backend não recebe identificador ou conexão de hospital pelo navegador: o banco
  continua derivado da sessão autenticada.

## Recuperação

O pacote anterior pode ser reinstalado para desabilitar a nova interface e os novos
endpoints. A migração é aditiva: a nova versão do formulário pode permanecer no banco
sem impedir a execução do pacote anterior. Para reversão integral do estado do banco,
restaure o backup feito antes da implantação; não execute SQL manual para remover a
migração.

## Pendências

- validação clínica e administrativa do texto gerado pela equipe responsável de cada
  hospital antes de uso com pacientes reais;
- validar regras de faturamento conforme contrato, convênio e normas vigentes de cada
  instituição;
- implantação na VPS somente após aprovação visual e funcional local.
