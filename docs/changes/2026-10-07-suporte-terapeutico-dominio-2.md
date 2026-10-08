# Prescrição estruturada de suporte terapêutico — Domínio 2

**Data:** 2026-10-07
**Responsável:** equipe Inventory MED
**Status:** concluída

## Resumo

A tela de prescrição passou a oferecer um módulo estruturado para hidratação e
soluções, controle glicêmico e insulinoterapia, além de hemocomponentes,
hemoderivados e transfusões. O backend valida as combinações clínicas, produz o
texto padronizado da prescrição e apresenta insumos e alertas para conferência de
auditoria, sem lançar cobranças automaticamente.

## Motivo

Substituir campos livres e controles separados por uma entrada clínica guiada,
com opções padronizadas, barreiras de segurança, impressão uniforme e informações
de apoio à conferência assistencial e de faturamento.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/TherapeuticSupportCatalog.java` | criado | Define o contrato do catálogo controlado. |
| `backend/src/main/java/br/com/inventorymed/clinical/TherapeuticSupportCatalogService.java` | criado | Mantém as opções permitidas de contexto, soluções, aditivos, DXT e produtos sanguíneos. |
| `backend/src/main/java/br/com/inventorymed/clinical/TherapeuticSupportRequest.java` | criado | Define a entrada tipada do domínio. |
| `backend/src/main/java/br/com/inventorymed/clinical/TherapeuticSupportResponse.java` | criado | Define resumo, detalhamento, linhas de impressão e dados de conferência. |
| `backend/src/main/java/br/com/inventorymed/clinical/TherapeuticSupportValidator.java` | criado | Centraliza limites e combinações clínicas permitidas. |
| `backend/src/main/java/br/com/inventorymed/clinical/TherapeuticSupportService.java` | criado | Gera o texto numerado, as linhas do PDF e a conferência de insumos e alertas. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Valida e normaliza o novo campo antes de salvar um documento clínico. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia somente a perfis clínicos autenticados. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Inclui o tipo `THERAPEUTIC_SUPPORT_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V11__structured_therapeutic_support.sql` | criado | Publica nova versão do formulário de prescrição em cada banco hospitalar. |
| `backend/src/test/java/br/com/inventorymed/clinical/TherapeuticSupportServiceTest.java` | criado | Testa geração, limites e barreiras de segurança do módulo. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Testa autorização, isolamento hospitalar e persistência do novo campo. |
| `frontend/src/app/features/medical/therapeutic-support.component.ts` | criado | Implementa formulário reativo tipado e obtém validação/prévia do backend. |
| `frontend/src/app/features/medical/therapeutic-support.component.html` | criado | Organiza os três blocos assistenciais e a conferência final. |
| `frontend/src/app/features/medical/therapeutic-support.component.scss` | criado | Aplica o sistema visual existente ao novo módulo. |
| `frontend/src/app/features/medical/therapeutic-support.component.spec.ts` | criado | Testa estado vazio, regras automáticas e emissão dos dados. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Adiciona os contratos tipados do catálogo, requisição e resposta. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Integra catálogo e prévia com a API. |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | Valida as novas chamadas HTTP. |
| `frontend/src/app/features/medical/monitoring-prescription.component.ts` | modificado | Retira o controle glicêmico antigo para manter uma única fonte de verdade. |
| `frontend/src/app/features/medical/monitoring-prescription.component.html` | modificado | Remove o bloco DXT anterior e renumera os grupos restantes. |
| `frontend/src/app/app.ts` | modificado | Integra estado, validação, salvamento e impressão do suporte terapêutico. |
| `frontend/src/app/app.html` | modificado | Posiciona o módulo na prescrição e usa as novas linhas no documento impresso. |
| `frontend/src/app/app.spec.ts` | modificado | Cobre persistência do estado vazio e formação das linhas de impressão. |

## Banco de dados

- bancos afetados: um banco operacional independente por hospital;
- migrações: `V11__structured_therapeutic_support.sql`;
- objetos afetados: versões e campos dos formulários clínicos;
- a migração duplica a última versão publicada da prescrição, aposenta a anterior
  e adiciona o campo `SUPORTE_TERAPEUTICO.PLANO`, preservando os documentos
  históricos;
- não existe paciente global nem leitura cruzada entre hospitais.

## Comandos executados na VPS

Nenhum. A alteração foi implementada e validada apenas no ambiente local.

## Serviços afetados

- API Java/Spring Boot;
- frontend Angular;
- bancos hospitalares, na próxima execução do Flyway durante a implantação.

## Configurações

Nenhuma variável de ambiente nova foi criada e nenhum segredo foi alterado.

## Validação

- `npm test -- --watch=false`: 30 testes do frontend aprovados;
- `docker compose build api`: imagem da API compilada com sucesso;
- `mvn clean test` em contêiner local: 46 testes aprovados, sem falhas;
- o teste de integração criou bancos hospitalares independentes e aplicou as 11
  migrações em cada um;
- a autorização do catálogo e da prévia usa a sessão autenticada e o hospital não
  é recebido livremente do navegador;
- inspeção de segredos no diff: nenhum valor sensível incluído;
- `npm run build`: concluído; o Angular manteve os avisos já conhecidos de
  orçamento do bundle inicial (679,05 kB) e de `app.scss` (50,10 kB), sem erro;
- `npx prettier --check "src/**/*.{ts,html,scss}"`: todos os arquivos aprovados;
- `git diff --check`: aprovado, sem erros de espaços em branco;
- inspeção de 25 arquivos alterados ou novos: nenhum possível segredo encontrado.
- API local reconstruída e iniciada com `docker compose up --build -d api`;
- `GET /api/v1/actuator/health`: estado `UP`;
- catálogo do suporte terapêutico sem autenticação: acesso recusado com HTTP 401;
- frontend local em `http://127.0.0.1:4200/`: resposta HTTP 200.

## Recuperação

Antes de uma implantação, a alteração pode ser desfeita revertendo o commit. Após
a migração V11 ser aplicada em algum hospital, ela não deve ser editada nem
apagada: uma nova migração deve publicar outra versão do formulário e desativar o
campo, preservando o histórico clínico já gravado. Em produção, retornar os
artefatos da versão anterior restaura a aplicação, mas o esquema V11 permanece
compatível.

## Pendências

- homologar as opções, limites e textos com farmácia, enfermagem, hemoterapia,
  corpo clínico e segurança do paciente;
- validar formalmente o protocolo institucional de insulina, hipoglicemia e
  eletrólitos concentrados de cada hospital antes do uso assistencial;
- cadastrar os códigos TUSS/SIGTAP e regras contratuais de cada hospital antes de
  automatizar qualquer lançamento financeiro; nesta versão são apenas itens para
  conferência;
- revisar visualmente o PDF com combinações extensas e em diferentes formatos de
  papel;
- publicar na VPS somente após a aprovação funcional do responsável pelo projeto.
