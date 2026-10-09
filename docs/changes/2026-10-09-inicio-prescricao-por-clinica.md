# Início da prescrição por clínica e modelo

**Data:** 2026-10-09
**Responsável:** equipe Inventory MED
**Status:** concluída

## Resumo

A abertura de uma nova prescrição agora apresenta três caminhos organizados: criar em
branco, selecionar a clínica e escolher um modelo compatível. As clínicas e a relação
entre clínica e modelo são fornecidas pela API, sem regra clínica fixa no Angular.

## Motivo

Adequar a tela de prescrição ao fluxo clínico solicitado e impedir que o frontend
decida sozinho quais modelos podem ser utilizados em cada contexto assistencial.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/PrescriptionStarterCatalog.java` | criado | contrato do catálogo de clínicas e modelos iniciais |
| `backend/src/main/java/br/com/inventorymed/clinical/PrescriptionStarterCatalogService.java` | criado | definição backend das opções e compatibilidades |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | endpoint médico autenticado para consultar o catálogo |
| `backend/src/test/java/br/com/inventorymed/clinical/PrescriptionStarterCatalogServiceTest.java` | criado | teste das clínicas e compatibilidades entregues |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | teste de autorização do novo endpoint para perfis médicos |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | tipos do contrato da API |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | consulta do catálogo de início da prescrição |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | teste da chamada HTTP tipada |
| `frontend/src/app/app.ts` | modificado | carregamento, seleção e tratamento dos estados do catálogo |
| `frontend/src/app/app.html` | modificado | novo arranjo visual solicitado |
| `frontend/src/app/app.scss` | modificado | responsividade e estados visuais do novo fluxo |
| `frontend/src/app/app.spec.ts` | modificado | teste da filtragem dos modelos conforme a clínica |

## Banco de dados

- bancos afetados: nenhum;
- migrações: nenhuma;
- objetos afetados: nenhum.

## Comandos executados na VPS

Nenhum. A alteração não foi implantada neste trabalho.

## Serviços afetados

- API Java: novo endpoint `GET /api/v1/clinical/prescriptions/start-options`;
- frontend Angular: novo fluxo inicial da prescrição.

## Configurações

Nenhuma.

## Validação

- `mvn verify` no backend: 62 testes aprovados antes do acréscimo do teste de endpoint;
- testes backend afetados após o acréscimo: 13 aprovados, incluindo autorização do
  endpoint e catálogo;
- `npm test -- --watch=false`: 42 testes frontend aprovados;
- `npm run build`: build concluído; permaneceram apenas os avisos preexistentes de
  orçamento do pacote inicial e do arquivo global de estilos;
- Prettier executado nos arquivos Angular alterados.

## Recuperação

Reverter os arquivos listados restaura o seletor anterior. Não existe migração de banco
para desfazer.

## Pendências

- os conteúdos clínicos completos de cada modelo pré-pronto ainda precisam de definição
  e aprovação pela equipe clínica responsável antes de serem cadastrados;
- as clínicas sem modelo aprovado aparecem corretamente sem opção pré-pronta, mantendo
  disponível a criação em branco.
