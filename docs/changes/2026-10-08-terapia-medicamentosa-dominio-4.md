# Terapia medicamentosa estruturada — Domínio 4

**Data:** 2026-10-08
**Responsável:** equipe Inventory MED
**Status:** concluída no ambiente local

## Resumo

A prescrição passou a possuir um bloco estruturado para antimicrobianos,
profilaxias hospitalares de TEV e LAMG, reconciliação de medicamentos de uso
contínuo e analgesia/sintomáticos. Os cinco formulários livres anteriores foram
substituídos por esse bloco; `DEMAIS MEDICAMENTOS` continua disponível para itens
que não pertencem ao domínio.

O backend valida as combinações e produz o texto da prescrição, as linhas da
grade de aprazamento, os alertas e os itens de insumos/equipamentos para
conferência. O frontend somente coleta a entrada tipada e mostra a resposta da
API.

## Motivo

Reduzir ambiguidade em medicamentos com regras diferentes de aprazamento e
registrar barreiras explícitas de segurança, incluindo dia do antimicrobiano,
foco infeccioso, situação da CCIH, revisão da função renal, risco hemorrágico,
reconciliação medicamentosa e gatilho obrigatório para medicamentos `SN`.

O arquivo de direcionamento recebido termina no meio do exemplo JSON, na chave
`clinica_contexto`. Por isso, o contrato de entrada foi definido somente a partir
dos requisitos legíveis do documento, sem inventar campos clínicos posteriores.

## Comportamento implementado

- antimicrobianos exigem D1–D999, foco infeccioso, situação/referência da CCIH,
  TFG ou clearance de creatinina e confirmação de revisão do ajuste renal;
- o sistema não calcula nem recomenda automaticamente uma dose renal;
- bolus e infusão rápida exigem diluente e equipo e geram itens para conferência;
- profilaxia farmacológica de TEV exige plaquetas e informação sobre sangramento
  ativo;
- profilaxia farmacológica ativa é bloqueada com plaquetas abaixo de 50.000/mm³
  ou sangramento ativo; ainda é possível registrar a suspensão e seu motivo;
- reconciliação informa se o medicamento foi mantido, ajustado ou suspenso;
- `SN` e `SN / FIXO` exigem gatilho clínico e intervalo mínimo;
- o PDF recebe os títulos 11, 12, 13 e 14 exatamente como solicitado;
- itens de cobrança são somente uma lista de conferência, sem lançamento
  financeiro automático e sem códigos TUSS/SIGTAP inventados.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicationTherapyCatalog.java` | criado | Contrato do catálogo controlado. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicationTherapyCatalogService.java` | criado | Opções de medicamentos, vias, aprazamentos, CCIH, função renal, profilaxias e reconciliação. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicationTherapyRequest.java` | criado | Entrada tipada e limites estruturais. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicationTherapyResponse.java` | criado | Saída da prescrição, linhas impressas e conferência. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicationTherapyValidator.java` | criado | Travas de segurança e coerência no backend. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicationTherapyService.java` | criado | Normalização e montagem dos quatro módulos. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Endpoints autenticados de catálogo e prévia. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Normaliza o novo tipo antes de persistir no banco hospitalar. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Inclui `MEDICATION_THERAPY_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V14__structured_medication_therapy.sql` | criado | Publica nova versão do modelo e desativa os cinco campos livres substituídos. |
| `backend/src/test/java/br/com/inventorymed/clinical/MedicationTherapyServiceTest.java` | criado | Testa saída, função renal, plaquetas, SN e campos indevidos. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Testa migração, ordem e persistência no banco hospitalar. |
| `frontend/src/app/features/medical/medication-therapy.component.ts` | criado | Formulário reativo tipado e integração com a API. |
| `frontend/src/app/features/medical/medication-therapy.component.html` | criado | Interface dos quatro módulos e campos condicionais. |
| `frontend/src/app/features/medical/medication-therapy.component.scss` | criado | Aparência coerente com o sistema visual clínico. |
| `frontend/src/app/features/medical/medication-therapy.component.spec.ts` | criado | Testa criação e limpeza dos grupos. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Contratos TypeScript do domínio. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Chamadas ao catálogo e à prévia. |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | Testa os contratos HTTP e CSRF. |
| `frontend/src/app/app.ts` | modificado | Integra validação, salvamento, impressão e substituição dos campos livres. |
| `frontend/src/app/app.html` | modificado | Posiciona o bloco após cuidados críticos e inclui a saída impressa. |
| `frontend/src/app/app.spec.ts` | modificado | Testa estado inicial e ordem das linhas impressas. |

## Banco de dados

- bancos afetados: cada banco operacional hospitalar, de forma independente;
- migração: `V14__structured_medication_therapy.sql`;
- objetos afetados: restrição de tipos de campo, versão publicada do modelo,
  seção `TERAPIA_MEDICAMENTOSA` e campo `PLANO`;
- os campos livres `ANALGESIA`, `SINTOMATICOS`, `PROFILAXIA`, `ATB` e
  `USO_CONTINUO` ficam inativos somente na nova versão;
- documentos e snapshots antigos não são modificados;
- não há acesso cruzado entre hospitais e o hospital continua derivado da sessão.

## Comandos executados na VPS

Nenhum. A alteração foi realizada e validada somente no ambiente local.

## Serviços afetados

- API Java/Spring Boot;
- frontend Angular;
- bancos hospitalares quando a API executar a migração Flyway V14.

## Configurações

Nenhuma variável nova e nenhum segredo alterado.

## Validação

- `npm run build`: concluído; somente os avisos de orçamento já conhecidos;
- `npm test -- --watch=false`: 37 testes aprovados, incluindo carregamento,
  falha da API e nova tentativa do catálogo;
- teste unitário `MedicationTherapyServiceTest`: 5 testes aprovados em Java 21;
- `mvn clean verify` em Java 21: 55 testes aprovados, sem falhas ou erros;
- `mvn -Dtest=CoreSecurityIntegrationTests test` após a revisão de autorização:
  11 testes de integração aprovados;
- Flyway V14: aplicada com sucesso em três bancos hospitalares descartáveis e
  validada novamente em banco já atualizado;
- integração: confirmou a ordem da nova seção, a inativação dos cinco campos
  livres legados somente na nova versão, a normalização antes da persistência e
  o bloqueio do endpoint para perfil não médico;
- `git diff --check`: concluído sem erros de whitespace;
- varredura dos arquivos alterados: nenhum segredo literal ou chave privada
  incluído;
- nenhum dado real foi usado nos testes.

### Referências consultadas

- Ministério da Saúde e Anvisa, Protocolo de Segurança na Prescrição, Uso e
  Administração de Medicamentos:
  `https://www.gov.br/anvisa/pt-br/centraisdeconteudo/publicacoes/servicosdesaude/publicacoes/protocolo-de-seguranca-na-prescricao-uso-e-administracao-de-medicamentos`;
- CDC, Core Elements of Hospital Antibiotic Stewardship Programs:
  `https://www.cdc.gov/antibiotic-use/hcp/core-elements/hospital.html`;
- DailyMed, informação oficial de vancomicina e função renal:
  `https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=99e523d8-9bde-43cb-8434-497015e5dcbd`;
- DailyMed, informação oficial de enoxaparina, sangramento e plaquetas:
  `https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=b4728749-8453-4f07-9fbc-a1f3e9d2708b`;
- ANS, Padrão TISS/TUSS vigente:
  `https://www.gov.br/ans/pt-br/assuntos/prestadores/padrao-para-troca-de-informacao-de-saude-suplementar-2013-tiss/padrao-tiss-maio-2025`.

## Recuperação

Antes da implantação, a alteração pode ser desfeita revertendo o código. Depois
que V14 for aplicada, ela não deve ser apagada ou alterada: uma nova migração deve
publicar outra versão do modelo e desativar o campo estruturado. O retorno dos
artefatos da versão anterior restaura a aplicação, enquanto o esquema V14
permanece compatível e os documentos históricos continuam preservados.

## Pendências

- homologar listas, textos, limites e travas com infectologia, farmácia clínica,
  CCIH, enfermagem, auditoria, comissão de farmácia e responsável técnico de cada
  hospital;
- definir como o hospital parametrizará quais antimicrobianos exigem autorização
  prévia da CCIH;
- o documento de entrada está truncado no início do JSON; confirmar se existiam
  campos adicionais depois de `clinica_contexto`;
- os números 11–14 solicitados neste domínio se sobrepõem ao número 14 já usado no
  Domínio 3; definir a numeração global antes da homologação final do PDF;
- integrar códigos TUSS/SIGTAP e regras contratuais somente após criar e homologar
  um módulo financeiro próprio; esta entrega não lança cobrança;
- homologar visualmente a impressão com prescrições extensas;
- publicar na VPS somente após aprovação funcional.
