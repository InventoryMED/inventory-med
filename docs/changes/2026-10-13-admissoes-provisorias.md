# Admissões provisórias sem leito

**Data:** 2026-10-13
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída

## Resumo

O censo hospitalar passa a permitir a admissão de um paciente antes da disponibilidade
de um leito físico. O paciente permanece vinculado à unidade assistencial, pode receber
documentos clínicos e depois é alocado em um leito disponível sem trocar de internação
ou perder histórico. Na visão operacional, a fila foi organizada em um painel lateral
compacto, com busca, rolagem própria e detalhes de apenas um paciente por vez.

## Motivo

Permitir o início seguro do atendimento enquanto o paciente aguarda um leito, sem criar
leitos fictícios e sem duplicar prontuário operacional ou documentos no momento da
alocação.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/tenant/V18__provisional_admissions.sql` | criado | Torna o leito atual opcional, adiciona unidade e origem provisórias, CNS, nome da mãe, restrições e índices. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalRequests.java` | modificado | Define contratos validados para admissão provisória, alocação e identificação complementar. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe os casos de uso de admissão provisória e alocação. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceService.java` | modificado | Centraliza validação, transações, reserva de leito, isolamento e auditoria. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceResponse.java` | modificado | Inclui admissões sem leito e seus dados na área de trabalho hospitalar. |
| `frontend/src/app/features/medical/provisional-admissions.component.*` | criado | Apresenta a fila provisória em painel lateral compacto, pesquisável e com ações sob demanda. |
| `frontend/src/app/app.*` | modificado | Integra admissão, edição, módulos clínicos, alocação e encerramento; reduz a altura dos cards de leito com ações secundárias recolhíveis. |
| `frontend/src/app/demo-store.ts` | modificado | Mantém o comportamento equivalente no modo local de interface. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Tipifica os novos contratos da API. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Adiciona chamadas de admissão provisória e alocação. |
| `docs/architecture.md` | modificado | Registra as invariantes arquiteturais do fluxo sem leito. |

## Banco de dados

- bancos afetados: todos os bancos hospitalares, de forma isolada;
- migração: `tenant/V18__provisional_admissions.sql`;
- objetos afetados: `patient`, `admission` e índices de admissões ativas;
- `current_bed_id` passa a aceitar nulo somente quando há unidade provisória;
- o índice de leito ativo continua impedindo duas internações no mesmo leito físico e
  ignora admissões sem leito.

## Comandos executados na VPS

Nenhum. A alteração foi implementada e validada somente no ambiente local.

Na futura implantação, o serviço executará a migração Flyway automaticamente antes de
aceitar tráfego da nova versão.

## Serviços afetados

- API Java `inventory-med-api`;
- frontend Angular servido pelo Nginx;
- SQL Server, por migração Flyway dos bancos hospitalares.

## Configurações

Nenhuma variável ou segredo novo.

## Validação

- compilação do backend Java 21 executada em contêiner de desenvolvimento;
- migrações Flyway executadas em SQL Server 2022 descartável;
- testes de integração validam isolamento entre hospitais, admissão sem leito,
  alocação no mesmo identificador e auditoria;
- testes do frontend validam serviço HTTP, card provisório e preservação da internação
  na alocação, além da busca e da abertura exclusiva dos detalhes na fila lateral;
- build Angular de produção executado.

## Recuperação

Se a implantação falhar, restaurar o pacote anterior da API e do frontend pelo fluxo de
rollback já documentado. A migração V18 é aditiva e compatível com a versão anterior;
as novas colunas podem permanecer até uma correção posterior. Não remover admissões ou
pacientes diretamente por SQL. Antes de qualquer reversão estrutural, restaurar o backup
do banco hospitalar correspondente.

## Pendências

- definir formalmente se o perfil `ENFERMAGEM` poderá criar documentos hoje classificados
  como médicos (prescrição, evolução e AIH) ou apenas consultar e executar ações
  operacionais. As permissões médicas atuais foram preservadas por segurança.
