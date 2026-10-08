# Procedimentos e intervenções beira-leito

**Data:** 2026-10-08
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída

## Resumo

Foi criado um fluxo clínico próprio para solicitar ou registrar procedimentos
beira-leito. O acesso parte do leito ocupado, abre uma página independente, permite
usar modelos técnicos, exige os dados de segurança aplicáveis, mantém os registros da
internação no banco exclusivo do hospital e gera uma folha A4 para impressão ou PDF.

O sistema diferencia explicitamente um procedimento apenas solicitado de um
procedimento efetivamente realizado. Textos técnicos pré-preenchidos em uma solicitação
são apresentados como plano para conferência e não como prova de execução.

## Motivo

Atender ao registro estruturado de CVC, via aérea invasiva, traqueostomia, drenagem e
punções, com lateralidade, indicação clínica, CID-10, rastreabilidade de dispositivo,
controle pós-procedimento e revisão de insumos. O objetivo é melhorar segurança,
legibilidade e auditoria sem transformar referências financeiras não verificadas em
cobranças automáticas.

## Regras implementadas

- acesso restrito a `MEDICO` e `RESPONSAVEL_CLINICO`;
- hospital derivado da sessão e persistência pelo executor do banco hospitalar;
- até 10 procedimentos por documento, com identificadores únicos;
- indicação, CID-10, sítio, lateralidade, assepsia, barreira estéril, prioridade e
  monitorização validados pelo backend;
- lateralidade direita ou esquerda obrigatória em sítios pares;
- CVC, IOT, drenagem torácica e traqueostomia exigem exame de controle ou justificativa;
- procedimento realizado exige data/hora, técnica e resultado;
- data/hora realizada é apresentada no fuso clínico `America/Sao_Paulo`;
- procedimento realizado com dispositivo exige marca, calibre, lote e registro Anvisa;
- conteúdo persistido é novamente normalizado e validado pelo backend;
- documento finalizado é imutável e gera evento na auditoria clínica existente;
- códigos TUSS/SIGTAP e insumos são exibidos somente para conferência humana;
- nenhuma cobrança, taxa, honorário ou insumo é lançado automaticamente.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormKind.java` | modificado | Adiciona o tipo de documento `PROCEDURE`. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Adiciona o campo estruturado `PROCEDURE_PLAN`. |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureCatalog.java` | criado | Contrato do catálogo de procedimentos e modelos. |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureCatalogService.java` | criado | Define procedimentos, padrões técnicos e referências para conferência. |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureRequest.java` | criado | Contrato tipado de entrada. |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureResponse.java` | criado | Contrato da prévia estruturada e da revisão de auditoria. |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureValidator.java` | criado | Centraliza as regras clínicas e de consistência no backend. |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureService.java` | criado | Gera a prévia e normaliza o documento que será persistido. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Integra a normalização de `PROCEDURE_PLAN` à persistência clínica. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia somente aos perfis médicos autorizados. |
| `backend/src/main/resources/db/migration/tenant/V15__bedside_procedures.sql` | criado | Publica o modelo de procedimentos em cada banco hospitalar. |
| `backend/src/test/java/br/com/inventorymed/clinical/BedsideProcedureServiceTest.java` | criado | Testa formatação, lateralidade, rastreabilidade, controle e formato estrito. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Testa autorização, migração, persistência e isolamento entre hospitais. |
| `frontend/src/app/models.ts` | modificado | Adiciona a tela `procedure`. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Adiciona contratos tipados de procedimento e documento clínico. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Adiciona catálogo, prévia, histórico e retorno tipado da criação. |
| `frontend/src/app/features/medical/bedside-procedure-workspace.component.*` | criado | Implementa formulário reativo, histórico, prévia e impressão. |
| `frontend/src/app/app.ts` | modificado | Abre a nova página em guia própria a partir do leito. |
| `frontend/src/app/app.html` | modificado | Adiciona o botão `PROCEDIMENTOS` e hospeda a página clínica. |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | Testa os novos contratos HTTP. |
| `frontend/src/app/features/medical/bedside-procedure-workspace.component.spec.ts` | criado | Testa carregamento, estado vazio e escopo da internação. |
| `frontend/src/app/app.spec.ts` | modificado | Testa a abertura do fluxo pelo leito ocupado. |

## Banco de dados

- bancos afetados: todos os bancos operacionais dos hospitais;
- migração: `V15__bedside_procedures.sql`;
- objetos afetados: restrições de tipo de `form_template`, `form_field` e
  `clinical_document`, além de um novo modelo publicado de procedimento;
- banco central: sem nova tabela e sem paciente global;
- a migração será aplicada pelo Flyway, sem execução SQL manual.

## Comandos executados na VPS

Nenhum. Esta alteração foi implementada e validada somente no ambiente local.

## Serviços afetados

- API Spring Boot: novos endpoints em `/api/v1/clinical/procedures` e novo tipo de
  documento clínico;
- frontend Angular: nova página acessível pelo card do leito;
- SQL Server: migração Flyway dos bancos hospitalares;
- Nginx: nenhuma alteração.

## Configurações

Nenhuma variável de ambiente nova foi criada. Nenhum segredo foi adicionado.

## Validação

- `mvn clean verify` em Java 21: 61 testes aprovados, sem falhas ou erros;
- dentro da bateria completa, 6 testes unitários do serviço de procedimentos e 11
  testes de integração de segurança e isolamento foram aprovados;
- 40 testes do frontend aprovados;
- build de produção Angular concluído;
- prévia de impressão renderizada em PDF A4 retrato e inspecionada visualmente, sem
  cortes, sobreposições ou conteúdo fora das margens;
- avisos de orçamento já existentes permanecem visíveis no build do Angular.

## Recuperação

Antes da implantação, o procedimento seguro é manter backup dos bancos hospitalares e
o pacote da versão anterior. Em caso de falha após a migração, restaurar o pacote
anterior da API e do frontend não apaga os novos registros. Se for indispensável
remover fisicamente o novo tipo e o modelo, restaurar os bancos a partir do backup,
pois a migração não executa exclusão automática de documento clínico.

## Pendências

- validar com faturamento de cada hospital a vigência dos códigos TUSS/SIGTAP e as
  regras contratuais antes de qualquer futura automação financeira;
- validar os textos técnicos e os modelos com a direção clínica antes da implantação
  em produção.

## Referências oficiais consultadas

- Anvisa - rastreabilidade de dispositivos médicos e direito do paciente:
  `https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2024/etiqueta-de-rastreabilidade-de-dispositivos-medicos-e-direito-do-paciente-saiba-mais/`;
- Anvisa - Identificação Única de Dispositivos Médicos (UDI):
  `https://www.gov.br/anvisa/pt-br/assuntos/produtosparasaude/udi`;
- DATASUS - módulo de pesquisa do SIGTAP:
  `https://wiki.datasus.gov.br/sigtap/index.php/M%C3%B3dulo_Pesquisa`;
- ANS - sistema de codificação CBHPM:
  `https://fhir-hm.ans.gov.br/CodeSystem-cbhpm.html`.
