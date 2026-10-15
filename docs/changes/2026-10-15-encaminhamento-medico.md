# Encaminhamento médico para atenção especializada

**Data:** 2026-10-15
**Responsável:** equipe Inventory MED
**Status:** concluída localmente

## Resumo

Foi criada a aba `ENCAMINHAMENTO` para o médico emitir uma guia de referência para
especialista ou outra área da saúde. A tela reutiliza os dados da internação, permite
registrar prioridade, prazo recomendado, justificativa clínica, documentos entregues e
destino, além de gerar prévia, finalizar, consultar o histórico da internação e imprimir
em A4.

## Motivo

Organizar o encaminhamento pós-internação e manter autoria, conteúdo e contexto clínico
auditáveis no banco exclusivo do hospital, sem transformar o frontend em fonte de regra
de negócio.

## Arquivos alterados

| Área | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/Referral*.java` | criado | Contrato, catálogo, normalização e regras do encaminhamento. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Validação do novo tipo estruturado antes da persistência. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Endpoints autenticados de catálogo e prévia. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/` | modificado | Novos tipos `REFERRAL` e `REFERRAL_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V22__medical_referrals.sql` | criado | Publicação do modelo padrão em cada banco hospitalar. |
| `frontend/src/app/features/medical/referral-workspace.component.*` | criado | Formulário, histórico, prévia e impressão. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Contratos TypeScript do módulo. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Comunicação tipada com a API. |
| `frontend/src/app/app.*` e navegação clínica | modificado | Acesso pelo leito, admissão provisória e abas clínicas. |
| testes de backend e frontend | criado/modificado | Regras, contrato, navegação e isolamento hospitalar. |
| `docs/architecture.md` | modificado | Inclusão do encaminhamento nos invariantes clínicos. |

## Banco de dados

- bancos afetados: banco operacional de cada hospital;
- migração: `V22__medical_referrals.sql`;
- objetos afetados: restrições de tipo de formulário/documento/campo e modelo publicado
  de encaminhamento;
- nenhum dado clínico é armazenado no banco central;
- nenhuma consulta entre bancos hospitalares foi criada.

## Regras de segurança e negócio

- hospital, paciente, internação e autor são derivados da sessão e da internação ativa;
- apenas `MEDICO` e `RESPONSAVEL_CLINICO` acessam os endpoints;
- especialidades, prioridades, prazos, destinos e anexos são aceitos somente quando
  pertencem ao catálogo do backend;
- a justificativa clínica é obrigatória;
- anexos entregues são opcionais;
- finalização exige confirmação explícita do médico;
- documento finalizado é imutável e a operação é auditada;
- SISREG, CROSS e convênios são apenas destinos declarados: não existe integração nem
  protocolo automático nesta versão.

O desenho considera que sistemas de regulação recebem e processam solicitações em fluxo
próprio, conforme as informações oficiais do Ministério da Saúde:

- https://www.gov.br/saude/pt-br/composicao/saes/drac/regulacao/regulacao-do-acesso/sistemas-de-informacao
- https://wiki.saude.gov.br/SISREG/index.php/P%C3%A1gina_principal

## Comandos executados na VPS

Nenhum. A alteração foi realizada e validada localmente.

## Serviços afetados

- API Spring Boot;
- frontend Angular;
- SQL Server na próxima execução do Flyway durante a implantação.

## Ativação local

- a imagem da API foi reconstruída com Docker Compose;
- a API local respondeu `UP` em `http://127.0.0.1:8080/api/v1/actuator/health`;
- o frontend foi iniciado em `http://127.0.0.1:4200/`;
- nenhuma alteração foi enviada à VPS.

## Configurações

Nenhuma variável de ambiente nova.

## Validação

- backend: `./mvnw verify`, com 90 testes aprovados, incluindo regras do
  encaminhamento, endpoints, migrações Flyway e isolamento entre hospitais;
- frontend: 64 testes aprovados, cobrindo componente, serviço e navegação;
- build de produção do frontend aprovado; permanecem apenas os avisos de orçamento de
  bundle já existentes no projeto;
- verificação de formatação do frontend aprovada pelo Prettier;
- `git diff --check` aprovado e inspeção sem indício de segredo incluído no diff.

## Recuperação

Se a versão ainda não tiver sido implantada, basta não publicar o commit. Depois da
implantação, a aplicação pode voltar ao pacote anterior. A migração `V22` é aditiva; o
modelo e os documentos já criados devem ser preservados, não apagados manualmente.

## Pendências

- homologação clínica do formulário e da impressão;
- definição dos protocolos e prazos específicos de cada rede reguladora;
- integração futura com SISREG, e-SUS Regulação, CROSS ou convênios, se houver contrato e
  interface oficial disponível;
- inclusão do número do conselho profissional no perfil autenticado antes de imprimir
  esse dado como parte da autoria.
