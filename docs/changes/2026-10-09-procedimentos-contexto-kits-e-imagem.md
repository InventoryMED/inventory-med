# Contexto, kits e segurança na tela de procedimentos

**Data:** 2026-10-09
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída

## Resumo

A tela de procedimentos passou a organizar a solicitação pelo contexto clínico, permitir
busca por nome ou referência de faturamento, oferecer kits rápidos e exigir lateralidade,
modo de execução, indicação clínica e confirmação de uso de imagem. O backend valida todas
essas regras, produz o texto estruturado do documento e gera alertas de auditoria.

## Motivo

Reduzir digitação repetitiva, impedir registros incompletos e deixar explícitos os itens que
precisam de conferência clínica, documental e de faturamento antes da finalização.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureCatalog.java` | modificado | ampliou o catálogo com contextos, kits, insumos e classificação de grande porte |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureCatalogService.java` | modificado | cadastrou contextos, modos, lateralidades, kits rápidos e vínculos determinísticos conhecidos |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureRequest.java` | modificado | incluiu contexto clínico, confirmação de imagem e referência do anexo no PEP |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureValidator.java` | modificado | concentrou as novas travas de lateralidade, contexto, modo e anexo no backend |
| `backend/src/main/java/br/com/inventorymed/clinical/BedsideProcedureService.java` | modificado | estruturou contexto, códigos, kits, alertas laboratoriais e controle pós-procedimento |
| `backend/src/main/resources/db/migration/tenant/V16__procedure_context_and_quick_kits.sql` | criado | publicou nova versão imutável do formulário de procedimentos em cada banco hospitalar |
| `backend/src/test/java/br/com/inventorymed/clinical/BedsideProcedureServiceTest.java` | modificado | cobriu kits, contextos, anexo de imagem, código TUSS e alertas de grande porte |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | atualizou o contrato integrado e validou a migração nos bancos hospitalares isolados |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | refletiu o novo contrato tipado no Angular |
| `frontend/src/app/features/medical/bedside-procedure-workspace.component.ts` | modificado | implementou busca, aplicação de kits e montagem segura do payload |
| `frontend/src/app/features/medical/bedside-procedure-workspace.component.html` | modificado | reorganizou os campos e adicionou contexto, kits e avisos clínicos |
| `frontend/src/app/features/medical/bedside-procedure-workspace.component.scss` | modificado | manteve o padrão visual do sistema e adaptou a tela para tamanhos menores |
| `frontend/src/app/features/medical/bedside-procedure-workspace.component.spec.ts` | modificado | atualizou o catálogo simulado e a verificação do componente |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | atualizou o teste do contrato HTTP da tela de procedimentos |

## Banco de dados

- bancos afetados: bancos operacionais de cada hospital;
- migrações: `V16__procedure_context_and_quick_kits.sql`;
- objetos afetados: nova versão publicada em `form_template_version`, `form_section` e
  `form_field` para o formulário `PROCEDURE`;
- documentos clínicos finalizados anteriormente permanecem inalterados;
- nenhum dado é compartilhado entre hospitais.

## Comandos executados na VPS

Nenhum. A alteração não foi implantada na VPS.

## Serviços afetados

- API Java/Spring Boot;
- frontend Angular;
- bancos SQL Server exclusivos dos hospitais por meio do Flyway.

No ambiente local, a API foi reconstruída e reiniciada com Docker Compose apenas para
validação. Nenhum serviço de produção foi reiniciado.

## Configurações

Nenhuma variável de ambiente nova foi criada.

## Validação

- `npm run build`: concluído; somente os avisos de orçamento de bundle já conhecidos;
- `npm test -- --watch=false`: 42 testes aprovados;
- testes completos da API em Java 21 com SQL Server descartável: 65 testes aprovados;
- Flyway: 16 migrações aplicadas com sucesso em bancos hospitalares descartáveis;
- isolamento hospitalar e autorização: preservados pelos testes integrados;
- compilação da imagem local da API: concluída com sucesso;
- verificação de segredos no diff: nenhum segredo adicionado.

## Recuperação

O código pode retornar à versão anterior por reversão do commit. Como versões publicadas
de formulários são imutáveis, a reversão do banco deve ser feita por uma nova migração que
retire a versão 2 e publique novamente a definição anterior; não se deve apagar a V16 nem
alterar registros clínicos existentes manualmente.

## Pendências

- o sistema ainda não possui armazenamento seguro de arquivos clínicos; por isso, quando a
  guiagem por imagem é marcada, a tela exige a referência do anexo já armazenado no PEP. O
  upload binário deverá ser desenvolvido junto com controle de acesso, criptografia,
  retenção e auditoria;
- referências CBHPM/SIGTAP não existentes no catálogo atual foram marcadas para revisão.
  Nenhum código foi inventado e nenhuma cobrança é gerada automaticamente.
