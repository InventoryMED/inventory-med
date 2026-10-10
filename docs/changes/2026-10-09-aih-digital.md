# AIH digital no leito

**Data:** 2026-10-09
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída

## Resumo

O botão `AIH` do leito ocupado passou a abrir uma área própria para elaborar, revisar,
finalizar e imprimir o Laudo para Solicitação de Autorização de Internação Hospitalar.
O documento é salvo no banco exclusivo do hospital selecionado e mantém autoria,
versão, data e auditoria.

A tela preenche automaticamente os dados já disponíveis no censo da internação. CNS,
nome da mãe, prontuário, endereço e CNES permanecem opcionais e editáveis no laudo
porque esses atributos ainda não existem no cadastro persistente do paciente ou do
hospital.

## Motivo

Substituir o botão reservado de AIH por um fluxo real, compatível com o isolamento por
hospital, com geração flexível do laudo e validações de procedimentos concentradas no
backend.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/architecture.md` | modificado | Inclui procedimentos e AIH no escopo e nos módulos oficiais. |
| `backend/src/main/resources/db/migration/tenant/V17__digital_aih.sql` | criado | Adiciona o tipo documental `AIH`, o campo estruturado `AIH_PLAN` e o modelo publicado padrão em cada banco hospitalar. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormKind.java` | modificado | Reconhece AIH como documento clínico. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Reconhece o conteúdo estruturado da AIH. |
| `backend/src/main/java/br/com/inventorymed/clinical/AihCatalog.java` | criado | Define o contrato do dicionário de contextos, lateralidades, procedimentos e códigos. |
| `backend/src/main/java/br/com/inventorymed/clinical/AihCatalogService.java` | criado | Expõe códigos previamente cadastrados sem aceitar códigos de procedimento definidos pelo navegador. |
| `backend/src/main/java/br/com/inventorymed/clinical/AihRequest.java` | criado | Define a entrada opcional e limitada da AIH. |
| `backend/src/main/java/br/com/inventorymed/clinical/AihResponse.java` | criado | Define o laudo normalizado e os alertas de auditoria. |
| `backend/src/main/java/br/com/inventorymed/clinical/AihService.java` | criado | Centraliza normalização, validação condicional, mapeamento de códigos e texto do laudo. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Publica catálogo e prévia somente para profissional autenticado no hospital da sessão. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Valida e persiste a AIH no banco exclusivo do hospital. |
| `frontend/src/app/features/medical/aih-workspace.component.*` | criado | Implementa formulário, histórico, prévia e impressão em PDF pelo navegador. |
| `frontend/src/app/features/medical/clinical-documentation-tabs.component.*` | criado | Organiza a navegação do módulo de procedimentos e AIH. |
| `frontend/src/app/features/medical/bedside-procedure-workspace.component.*` | modificado | Inclui a navegação compartilhada para AIH. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Adiciona contratos tipados da AIH. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Integra o catálogo e a prévia da AIH à API. |
| `frontend/src/app/app.ts`, `frontend/src/app/app.html`, `frontend/src/app/models.ts` | modificado | Ativa o botão AIH, abre a nova guia e reconhece o fluxo `aih`. |
| `backend/src/test/java/br/com/inventorymed/clinical/AihServiceTest.java` | criado | Cobre campos opcionais, códigos, lateralidade, anexo de imagem e formato persistido. |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | Verifica migração, persistência e isolamento da AIH entre hospitais. |
| `frontend/src/app/features/medical/aih-workspace.component.spec.ts` | criado | Verifica carga, preenchimento automático e prévia flexível. |
| `frontend/src/app/features/medical/medical.service.spec.ts`, `frontend/src/app/app.spec.ts` | modificado | Verificam contratos HTTP e abertura do fluxo pelo leito. |

## Banco de dados

- bancos afetados: todos os bancos hospitalares, sem alteração no banco central;
- migração: `tenant/V17__digital_aih.sql`;
- objetos afetados: restrições de tipos de formulário/documento/campo, modelo publicado
  de AIH, seções, campos e documentos clínicos;
- isolamento: a AIH é criada pelo `TenantJdbcExecutor` usando exclusivamente o hospital
  autenticado na sessão.

## Regras aplicadas

- textos, CIDs e procedimentos da AIH podem ficar em branco;
- um procedimento selecionado é resolvido pelo dicionário mestre da API;
- procedimentos de sítio pareado exigem direita ou esquerda;
- guia por imagem exige referência do anexo no PEP e acrescenta o TUSS `40901262`;
- o navegador não escolhe banco ou hospital;
- uma AIH finalizada não é sobrescrita nem excluída pelo fluxo comum.

## Comandos executados na VPS

Nenhum. Esta alteração ainda não foi implantada na VPS.

## Serviços afetados

- `inventory-med-api`: precisará ser reiniciado durante uma implantação;
- `nginx`: não exige mudança de configuração, somente receberá os novos arquivos do
  frontend pelo processo normal de publicação;
- `mssql-server`: receberá a migração Flyway automaticamente por banco hospitalar.

## Configurações

Nenhuma variável ou segredo novo.

## Validação

- formatação Prettier aplicada aos arquivos Angular alterados;
- testes do frontend: 45 aprovados;
- build de produção do frontend: aprovado, mantendo apenas os avisos de orçamento já
  existentes;
- compilação Java 21 em contêiner local descartável: aprovada;
- testes unitários de `AihService`: 5 aprovados;
- testes completos do backend: 70 aprovados, incluindo 12 testes integrados com SQL
  Server e verificação do isolamento da AIH entre hospitais;
- `git diff --check`: aprovado;

## Recuperação

Antes da implantação, basta reverter o commit. Depois da migração em produção, o código
pode ser revertido pelo processo de release, mas os dados e o modelo de AIH devem ser
preservados. Uma remoção estrutural exigiria migração corretiva revisada e restauração de
backup se houvesse perda de dados; não executar SQL manual de exclusão.

## Pendências

- fornecer o catálogo oficial e vigente de equivalências CBHPM e SIGTAP para cada
  procedimento; códigos ausentes aparecem explicitamente como pendência de auditoria e
  não são inventados pelo sistema;
- definir os campos e o catálogo do módulo POCUS/laboratório beira-leito; a aba já está
  visível, mas permanece inativa para evitar criar regras clínicas e códigos sem fonte
  oficial;
- definir o armazenamento de arquivos clínicos para substituir a referência textual ao
  anexo no PEP por upload e guarda efetiva da imagem, mantendo autorização e auditoria;
- incluir CNS, nome da mãe, prontuário, endereço e CNES nos cadastros persistentes para
  que esses dados também sejam preenchidos automaticamente em novas AIHs.
