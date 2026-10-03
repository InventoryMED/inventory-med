# Administração real e bancos hospitalares isolados

**Data:** 2026-10-03
**Responsável:** Codex e Gabriel Pereira
**Status:** concluída e validada localmente; ainda não implantada na VPS

## Resumo

Foi criada a primeira entrega do sistema operacional real. O administrador geral agora
possui um painel conectado à API para cadastrar hospitais e usuários. Cada novo hospital
recebe um banco SQL Server e um login SQL exclusivos, com estrutura criada pelo Flyway.
Usuários podem receber perfil geral ou perfil vinculado a um hospital. Senhas iniciais
precisam ser trocadas antes que qualquer módulo seja acessado.

A versão pública atual da VPS não foi alterada. As telas clínicas antigas ficam
bloqueadas quando o frontend está conectado à API, até que leitos, admissões,
prescrições e evoluções possuam persistência real no banco hospitalar.

## Motivo

O Inventory MED precisa sair da fase visual sem permitir que dados temporários do
navegador sejam confundidos com registros verdadeiros. A fundação também precisava
aplicar a decisão de um banco central e um banco independente para cada hospital, com
regras e autorização concentradas no backend.

## Arquivos alterados

| Arquivo ou grupo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/resources/db/migration/core/V2__administration_and_tenant_provisioning.sql` | criado | adiciona estados de provisionamento, concorrência otimista, troca obrigatória de senha e credencial hospitalar criptografada |
| `backend/src/main/resources/db/migration/tenant/V1__tenant_structure_foundation.sql` | criado | inicia cada banco hospitalar com unidade, quarto, leito, metadados e auditoria clínica |
| `backend/src/main/java/br/com/inventorymed/administration/` | criado | casos de uso e contratos do administrador geral |
| `backend/src/main/java/br/com/inventorymed/tenancy/` | criado | provisiona banco, login, permissões, Flyway e credencial criptografada |
| `backend/src/main/java/br/com/inventorymed/auth/` | modificado | inclui troca obrigatória da senha inicial |
| `backend/src/main/java/br/com/inventorymed/security/` | modificado | política forte de senha e bloqueio dos demais endpoints antes da troca |
| `backend/src/main/java/br/com/inventorymed/bootstrap/` | modificado | bootstrap passa a criar somente o primeiro administrador geral |
| `frontend/src/app/features/administration/` | criado | painel de hospitais, provisionamento, usuários e perfis |
| `frontend/src/app/auth/` | modificado | suporta perfil geral e troca da senha inicial |
| `frontend/src/app/app.*` | modificado | integra o painel, carrega-o sob demanda e impede acesso clínico temporário no modo real |
| `frontend/src/app/demo-store.ts` | modificado | remove persistência clínica em `localStorage` |
| `compose.yaml` e `.env.example` | modificado | adiciona configurações locais de administração e provisionamento |
| `infra/sql/provision-core-database.sql` | modificado | cria login técnico exclusivo para provisionar bancos e logins hospitalares |
| `infra/scripts/configure-vps-environment.sh` | modificado | prepara administrador geral, credencial de provisionamento e chave de criptografia |
| `infra/scripts/configure-local-administrator.ps1` | criado | solicita a senha sem exibi-la e prepara o administrador e a chave do ambiente local |
| `infra/scripts/disable-bootstrap.sh` | modificado | remove a senha temporária do administrador geral |
| `docs/architecture.md` | modificado | versão 1.1 descreve o fluxo automático de criação de hospital |
| `docs/local-development.md`, `backend/README.md` e `docs/operations/vps-deployment.md` | modificado | atualiza execução, segurança e limitações atuais |

## Banco de dados

- banco central: migração `V2__administration_and_tenant_provisioning.sql`;
- novas colunas em `hospital`: `technical_code`, `status`, `provisioning_error`,
  `provisioned_at` e `row_version`;
- novas colunas em `app_user`: `must_change_password` e `row_version`;
- nova tabela central: `hospital_database_secret`;
- banco de cada hospital: migração inicial `V1__tenant_structure_foundation.sql`;
- tabelas hospitalares iniciais: `tenant_metadata`, `care_unit`, `room`, `bed` e
  `clinical_audit_event`;
- pacientes e documentos clínicos ainda não foram criados nessa entrega.

Hospitais centrais anteriores à migração ficam inativos em
`PROVISIONING_FAILED`. O administrador geral pode acionar `CRIAR BANCO` para aplicar o
novo isolamento sem fingir que a unidade já possui estrutura operacional.

## Segurança aplicada

- somente `ADMIN_SISTEMA` acessa `/api/v1/administration/**`;
- nomes de banco e login são gerados e validados no backend;
- cada login hospitalar recebe leitura, escrita e execução somente no próprio banco;
- senha do login hospitalar é gerada aleatoriamente e armazenada criptografada com
  AES-256-GCM;
- a chave de criptografia fica fora do Git e do SQL Server;
- senha inicial do usuário exige troca antes do acesso a outros endpoints;
- política mínima de senha validada no backend;
- criação de hospital, nova tentativa de provisionamento, criação de usuário e troca de
  senha geram eventos de auditoria;
- as respostas administrativas nunca devolvem nome de banco, login ou senha SQL;
- transações de cadastro são confirmadas antes do evento de sucesso na auditoria;
- dados clínicos temporários deixaram de ser gravados no armazenamento do navegador.
- durante uma atualização coordenada, uma sessão retornada pela versão anterior da API é
  tratada como não administrativa em vez de causar falha na interface.

## Comandos executados na VPS

Nenhum. A implantação pública atual foi preservada deliberadamente até aprovação dos
testes locais e preparação dos segredos de produção.

## Serviços afetados

- API Java local;
- frontend Angular local;
- SQL Server 2022 descartável dos testes;
- Docker Compose de desenvolvimento quando a nova imagem for ativada.

## Configurações

- `BOOTSTRAP_ENABLED`;
- `BOOTSTRAP_SYSTEM_ADMIN_EMAIL`;
- `BOOTSTRAP_SYSTEM_ADMIN_PASSWORD`;
- `TENANT_PROVISIONING_ENABLED`;
- `DB_PROVISIONING_URL`;
- `DB_PROVISIONING_USERNAME`;
- `DB_PROVISIONING_PASSWORD`;
- `TENANT_CREDENTIAL_ENCRYPTION_KEY`.

Nenhum valor foi registrado no código ou nesta documentação.

## Validação

- compilação Java 21: aprovada;
- testes integrados Spring Boot com SQL Server 2022 real e descartável: 10 aprovados;
- cenários de backend: migrações central e hospitalar, banco independente, credencial
  criptografada, autorização administrativa, criação de usuário hospitalar, CSRF,
  revogação de sessão e troca obrigatória da senha;
- o provisionamento hospitalar foi testado com uma conta SQL técnica restrita a
  `CREATE ANY DATABASE` e `ALTER ANY LOGIN`, sem utilizar `sa` na aplicação;
- testes Angular: 12 aprovados;
- build Angular de produção: aprovado;
- painel administrativo separado em bloco carregado sob demanda;
- dependências npm: nenhuma vulnerabilidade informada pelo `npm audit`.

## Recuperação

Como esta mudança ainda não foi implantada, a reversão consiste em retornar à branch
anterior no Git. Depois de uma implantação, bancos hospitalares não devem ser apagados
automaticamente. Em falha de publicação, restaura-se a versão anterior da aplicação;
qualquer recuperação de banco seguirá backup e procedimento documentado.

## Pendências

- configurar os novos segredos no `.env` local e validar o fluxo manual no navegador;
- confirmar na homologação que o login técnico da VPS possui somente as permissões
  documentadas para provisionamento;
- implementar administração hospitalar de quartos e leitos;
- implementar pacientes, admissões, prescrições e evoluções nos bancos hospitalares;
- implementar MFA antes de permitir dados clínicos reais;
- implantar na VPS somente depois da aprovação explícita do ambiente local.
