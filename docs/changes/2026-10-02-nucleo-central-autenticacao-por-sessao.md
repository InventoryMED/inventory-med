# Núcleo central e autenticação por sessão

**Data:** 2026-10-02 a 2026-10-03
**Responsável:** Codex e Gabriel Pereira
**Status:** concluída localmente

## Resumo

A API anterior, baseada em JWT e um único banco com dados clínicos compartilhados, foi
substituída pela fundação da arquitetura oficial. O banco central agora contém somente
identidade, hospitais, vínculos, auditoria e sessões. A autenticação usa sessão no
servidor, cookie protegido e CSRF. O Angular foi adaptado para esse fluxo e não guarda
mais token de autenticação no navegador. O contrato HTTP passa a usar o prefixo
versionado `/api/v1`.

## Motivo

O sistema precisa separar fisicamente os dados operacionais de cada hospital e manter
as regras de autorização no backend. JWT persistido no `sessionStorage` e tabelas
clínicas em um banco compartilhado contrariavam essas decisões e aumentavam a área de
risco.

## Arquivos alterados

| Arquivo ou grupo | Operação | Explicação |
| --- | --- | --- |
| `backend/pom.xml` | modificado | adiciona Spring Session JDBC e remove o servidor JWT |
| `backend/src/main/resources/db/migration/core/V1__core_identity_audit_and_sessions.sql` | criado | cria o primeiro esquema do banco central |
| `backend/src/main/java/br/com/inventorymed/auth/` | modificado | login, seleção de hospital, consulta da sessão e CSRF sem JWT |
| `backend/src/main/java/br/com/inventorymed/security/` | criado/modificado | principal serializável, sessão, revalidação de autorização e respostas seguras |
| `backend/src/main/java/br/com/inventorymed/audit/` | criado | registra autenticação e seleção de unidade |
| `backend/src/main/java/br/com/inventorymed/identity/` | modificado | papéis oficiais, banco operacional do hospital e perfil global |
| `backend/src/main/resources/application*.yml` | modificado | sessão JDBC, cookie seguro e migração central |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | criado | testes integrados com SQL Server 2022 real e descartável |
| `frontend/src/app/auth/` | modificado | remove JWT/sessionStorage e usa sessão HttpOnly com CSRF |
| `frontend/src/app/app.ts` | modificado | restaura a sessão após recarregar e efetua logout no backend |
| `compose.yaml` e `.env.example` | modificado | banco local `inventory_med_core` e bootstrap explícito sem JWT |
| `backend/README.md` e `docs/local-development.md` | modificado | documenta o novo fluxo local |
| `docs/development/testing.md` | criado | registra os comandos e critérios de validação local |
| `docs/architecture.md` e `docs/operations/ssms-access.md` | modificado | corrige o túnel SSMS validado para a porta local 15433 |

Foram removidos os componentes JWT e a antiga migração única que misturava dados
centrais e clínicos.

## Banco de dados

- banco afetado nos testes: instância SQL Server descartável;
- banco previsto para implantação: `inventory_med_core`;
- migração: `V1__core_identity_audit_and_sessions.sql`;
- tabelas criadas: `hospital`, `app_user`, `system_user_role`,
  `hospital_membership`, `audit_event`, `SPRING_SESSION` e
  `SPRING_SESSION_ATTRIBUTES`;
- nenhuma tabela de paciente, internação, prescrição ou evolução foi criada no banco
  central.

Cada hospital registra o nome de seu futuro banco operacional próprio. A criação e as
migrações desses bancos serão tratadas no módulo operacional.

## Comandos executados na VPS

Nenhum. Esta alteração foi desenvolvida e validada localmente. Ela ainda não foi
implantada no servidor.

## Serviços afetados

- API Java local;
- frontend Angular local;
- SQL Server descartável de testes;
- composição Docker usada somente no desenvolvimento.

## Configurações

- `DB_URL`;
- `DB_USERNAME`;
- `DB_PASSWORD`;
- `DB_POOL_MAX_SIZE`;
- `DB_POOL_MIN_IDLE`;
- `SESSION_TIMEOUT`;
- `SESSION_COOKIE_SECURE`;
- `ALLOWED_ORIGINS`;
- `BOOTSTRAP_ENABLED`;
- `BOOTSTRAP_DOCTOR_EMAIL`;
- `BOOTSTRAP_DOCTOR_PASSWORD`.

Nenhum valor secreto foi registrado nesta documentação.

## Segurança aplicada

- sessão persistida no SQL Server e cookie `INVENTORYMED_SESSION` com `HttpOnly`;
- `Secure` habilitado por padrão e desabilitado somente no perfil local sem HTTPS;
- `SameSite=Lax` e proteção CSRF em operações de escrita;
- cookie `XSRF-TOKEN` lido pelo Angular e enviado no cabeçalho `X-XSRF-TOKEN`,
  conforme a integração SPA oficial do Spring Security;
- rotação do identificador da sessão e do token CSRF no login e na troca de unidade;
- respostas genéricas para credenciais inválidas;
- auditoria de login bem-sucedido, falha e seleção de hospital;
- papéis globais e vínculos hospitalares revalidados no banco em toda requisição;
- revogação da sessão quando o usuário é desativado ou perde seu último acesso;
- papel hospitalar não é aceito como autoridade permanente armazenada na sessão;
- CORS restrito a origens configuradas e com credenciais explícitas.

## Validação

- `mvn clean test` com Java 21 e SQL Server 2022 via Testcontainers: 6 testes
  aprovados;
- cenários integrados: migração central sem tabelas clínicas, bloqueio sem CSRF,
  sessão com seleção automática, seleção entre várias unidades, auditoria de
  credencial inválida e revogação de vínculo;
- build Angular: aprovado;
- testes Angular: 10 aprovados;
- ambiente Docker local: API e SQL Server saudáveis;
- fluxo real: login de desenvolvimento, seleção de hospital, consulta da sessão e
  logout com retorno 204; consulta após logout bloqueada com 401.

## Recuperação

Antes de qualquer implantação, a reversão consiste em restaurar os arquivos pelo Git.
Após implantação, nunca se deve apagar manualmente tabelas ou executar downgrade no
banco de produção. A recuperação deverá usar backup validado e uma versão anterior da
aplicação dentro de uma janela de manutenção.

## Pendências

- criar o login SQL de menor privilégio usado pela aplicação na VPS;
- criar `inventory_med_core` na VPS por procedimento versionado;
- instalar Java 21 e publicar a API como serviço `systemd`;
- configurar Nginx e HTTPS antes de habilitar cookie `Secure` em produção;
- criar o provisionamento e as migrações dos bancos operacionais independentes;
- migrar quartos, leitos, pacientes, prescrições e evoluções do armazenamento
  demonstrativo para a API.
