# Reordenação das seções estruturadas da prescrição

**Data:** 2026-10-07
**Responsável:** equipe Inventory MED
**Status:** concluída

## Resumo

As seções finais estruturadas da prescrição passaram a seguir a ordem
`REABILITAÇÃO MULTIDISCIPLINAR`, `PRECAUÇÕES E ISOLAMENTO` e, por último,
`HIDRATAÇÃO, CONTROLE GLICÊMICO E HEMOTERAPIA`. A mesma sequência é usada na
tela, na validação, nos dados enviados à API, na impressão e nas linhas da tabela
impressa.

## Motivo

Adequar o fluxo visual e a impressão à sequência definida pelo responsável do
produto, mantendo o formulário publicado no banco coerente com a interface.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `frontend/src/app/app.html` | modificado | Reordena os componentes na tela e os blocos detalhados da impressão. |
| `frontend/src/app/app.ts` | modificado | Reordena validação, composição do documento e linhas da tabela impressa. |
| `frontend/src/app/app.spec.ts` | modificado | Verifica a ordem das linhas estruturadas impressas. |
| `backend/src/main/resources/db/migration/tenant/V12__reorder_structured_prescription_sections.sql` | criado | Publica nova versão do formulário com as três seções na ordem definida. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Confirma a ordem das seções em banco hospitalar isolado após as migrações. |
| `docs/changes/2026-10-07-reordenacao-secoes-estruturadas-prescricao.md` | criado | Registra objetivo, impacto, validação e recuperação. |

## Banco de dados

- bancos afetados: bancos operacionais independentes de cada hospital;
- migrações: `V12__reorder_structured_prescription_sections.sql`;
- objetos afetados: `form_template_version`, `form_section`, `form_field` e
  `form_field_option`;
- a migração aposenta a versão publicada anterior, clona seu conteúdo e publica
  uma nova versão com ordens 19, 20 e 21 para reabilitação, isolamento e suporte
  terapêutico, respectivamente;
- documentos anteriores continuam vinculados à versão original e não são
  alterados.

## Comandos executados na VPS

Nenhum. A alteração foi implementada e validada apenas no ambiente local.

## Serviços afetados

- frontend Angular;
- API Java/Spring Boot apenas pelo acréscimo da migração Flyway;
- bancos hospitalares na próxima implantação.

## Configurações

Nenhuma variável ou segredo foi criado ou alterado.

## Validação

- `npm test -- --watch=false`: 30 testes do frontend aprovados;
- `npm run build`: concluído; permaneceram somente os avisos já conhecidos de
  orçamento do bundle inicial e do arquivo `app.scss`;
- `mvn clean test` em contêiner local: 46 testes aprovados, sem falhas;
- Flyway validou e aplicou as 12 migrações em bancos hospitalares isolados;
- o teste de integração confirmou exatamente a sequência `REABILITACAO`,
  `PRECAUCOES_ISOLAMENTO`, `SUPORTE_TERAPEUTICO` no formulário publicado;
- formatação, verificação do diff e inspeção de segredos foram executadas antes do
  commit.

## Recuperação

Antes da implantação, o commit pode ser revertido. Depois que a V12 for aplicada,
ela não deve ser editada nem removida. Uma reversão da ordem deve ser feita por uma
nova migração que publique outra versão do formulário, preservando as versões e os
documentos clínicos anteriores.

## Pendências

- nenhuma.
