# Receitas médicas ambulatoriais e de alta

**Data:** 2026-10-13
**Responsável:** Codex e Gabriel Pereira
**Status:** concluída localmente

## Resumo

Foi criada a aba `RECEITAS MÉDICAS`, acessível pelo leito e pela admissão provisória.
O médico pode montar linhas de medicamentos, gerar prévia, imprimir, finalizar uma
receita auditável e duplicar a última receita da mesma internação para nova revisão.

## Motivo

Facilitar a emissão de receitas para compra externa, continuidade do tratamento ou
renovação de medicamento sem misturar esse documento com a prescrição hospitalar.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalRecipe*.java` | criado | Implementa contrato, catálogo, normalização, checagem textual de alergias e regras de impressão/finalização. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Valida e normaliza receitas no contexto real do hospital e da internação. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia somente para papéis médicos. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/*` | modificado | Inclui o documento e o campo estruturado de receita médica. |
| `backend/src/main/resources/db/migration/tenant/V21__medical_recipes.sql` | criado | Publica o modelo padrão em cada banco hospitalar. |
| `frontend/src/app/features/medical/medical-recipe-workspace.component.*` | criado | Implementa formulário, histórico, duplicação, prévia e impressão A4. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Define contratos tipados da receita. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Integra o Angular aos endpoints do backend. |
| `frontend/src/app/features/medical/clinical-documentation-tabs.component.*` | modificado | Inclui a nova aba no conjunto de documentos clínicos. |
| `frontend/src/app/app.*` | modificado | Inclui menu do leito, rota e tela da receita. |
| `frontend/src/app/features/medical/provisional-admissions.component.*` | modificado | Disponibiliza a receita para internação ainda sem leito. |
| `backend/src/test/**` e `frontend/src/app/**/*.spec.ts` | criado/modificado | Cobrem regras, isolamento, rota, interface e bloqueios. |
| `docs/architecture.md` | modificado | Registra a versão arquitetural 1.6 e as regras do novo documento. |

## Banco de dados

- bancos afetados: bancos operacionais de cada hospital;
- migração: `V21__medical_recipes.sql`;
- objetos afetados: restrições de tipo de `form_template`, `clinical_document` e
  `form_field`, além do novo modelo publicado `RECEITUÁRIO MÉDICO PADRÃO`;
- não foi criada tabela global de paciente e não existe consulta entre hospitais.

## Regras de segurança e conformidade

- hospital, internação, paciente, alergias e autoria são derivados no backend;
- somente `MEDICO` e `RESPONSAVEL_CLINICO` acessam o documento;
- receita finalizada é imutável e auditável;
- correspondência textual clara com alergia bloqueia a finalização;
- a checagem textual não é declarada como validação farmacológica completa;
- Notificação B permanece bloqueada para finalização até integração com SNCR,
  numeração controlada e assinatura ICP-Brasil;
- qualquer prévia de Notificação B recebe marca de rascunho não válido.

Referências oficiais consultadas:

- `https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2026/inicio-de-nova-etapa-do-sncr-o-que-muda-a-partir-de-30-de-setembro`;
- `https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/anvisa-publica-documentacao-tecnica-para-integracao-de-sistemas-de-prescricao-eletronica-ao-sncr`;
- `https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2020/medicamentos-controlados-receitas-com-assinatura-digital`;
- `https://www.gov.br/anvisa/pt-br/centraisdeconteudo/publicacoes/medicamentos/controlados/perguntas-e-respostas-rdc-1000-2025-1-ed.pdf`.

## Comandos executados na VPS

Nenhum. A alteração não foi publicada na VPS.

## Serviços afetados

- local: frontend Angular e API Spring Boot;
- produção: nenhum serviço foi reiniciado.

## Configurações

Nenhuma variável de ambiente nova foi adicionada.

## Validação

- `npm test -- --watch=false`: 60 testes aprovados;
- `npm run build`: build Angular aprovado, mantendo apenas os avisos de orçamento já
  existentes;
- `docker compose build api`: compilação Java 21 aprovada;
- `mvn verify` em Java 21 e SQL Server 2022 descartável: 85 testes aprovados;
- Flyway aplicou as 21 migrações em bancos hospitalares descartáveis;
- teste de integração confirmou uma receita no primeiro hospital e zero no segundo;
- teste de integração confirmou bloqueio da finalização de Notificação B.

## Recuperação

Antes de uma publicação, basta reverter os arquivos desta alteração. Após a migração
`V21`, não se deve apagar manualmente o histórico Flyway. A recuperação deve usar uma
nova migração para desativar o modelo e os pontos de entrada, preservando documentos já
finalizados.

## Pendências

- integrar catálogo oficial de medicamentos/princípios ativos para autocomplete e
  análise farmacológica além da comparação textual;
- integrar SNCR, numeração controlada e assinatura eletrônica qualificada antes de
  habilitar Notificação B válida;
- incluir CRM/UF e demais dados do emitente no cadastro profissional, sem inventar
  valores na impressão;
- definir o identificador único do paciente antes de permitir duplicação ou consulta de
  receitas entre internações do mesmo hospital;
- realizar homologação jurídica, médica e farmacêutica dos modelos impressos antes do
  uso assistencial em produção.
