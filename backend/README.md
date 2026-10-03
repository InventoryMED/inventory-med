# Inventory MED API

API do Inventory MED construída com Java 21, Spring Boot 4, Spring Security,
Spring Session JDBC, Spring Data JPA, Flyway e SQL Server 2022.

## Responsabilidade atual

Esta primeira base implementa somente o núcleo central:

- hospitais e nome do banco operacional correspondente;
- usuários e vínculos por hospital;
- perfil global `ADMIN_SISTEMA`;
- autenticação por sessão armazenada no SQL Server;
- seleção segura da unidade ativa;
- auditoria de autenticação;
- criação de hospitais com banco e login SQL exclusivos;
- criação de usuários gerais ou vinculados a um hospital;
- troca obrigatória da senha inicial;
- proteção CSRF para todas as operações de escrita.

Pacientes e documentos clínicos não ficam no banco central. A estrutura inicial de cada
banco hospitalar já é criada pelo Flyway; os módulos clínicos serão acrescentados em
migrações próprias.

## Executar localmente

Na raiz do repositório:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose logs -f api
```

Serviços locais:

- API: `http://127.0.0.1:8080/api/v1`;
- saúde: `http://127.0.0.1:8080/api/v1/actuator/health`;
- SQL Server: `127.0.0.1:14330`.

Docker é usado apenas no desenvolvimento e nos testes locais. A implantação oficial
na VPS executará Java, SQL Server e Nginx diretamente no Ubuntu.

## Autenticação

O navegador não recebe JWT nem armazena credencial no `localStorage` ou
`sessionStorage`. O fluxo é:

1. `GET /api/v1/auth/csrf` obtém a proteção CSRF;
2. `POST /api/v1/auth/login` valida e-mail e senha;
3. o backend cria uma sessão no SQL Server e envia apenas o cookie
   `INVENTORYMED_SESSION`, marcado como `HttpOnly`;
4. quando o usuário possui mais de um vínculo, `POST /api/v1/auth/select-hospital`
   seleciona a unidade;
5. uma conta com senha inicial usa `POST /api/v1/auth/change-password` antes de acessar
   qualquer outro módulo;
6. `GET /api/v1/auth/me` restaura o estado visível após recarregar a página;
7. `POST /api/v1/auth/logout` invalida a sessão no servidor.

O `GET /auth/csrf` cria o cookie legível `XSRF-TOKEN`. Nas chamadas de escrita, o
Angular copia esse valor para o cabeçalho `X-XSRF-TOKEN`. O cookie da sessão continua
`HttpOnly` e nunca é lido pelo frontend.

Os perfis e vínculos são reconsultados em toda requisição autenticada. Desativar o
usuário ou remover seu último vínculo revoga a sessão existente.

## Banco de dados

As migrações do banco central ficam em:

```text
src/main/resources/db/migration/core
```

O Flyway as executa ao iniciar a API. O Hibernate usa `ddl-auto=validate`: ele confere
o mapeamento, mas nunca cria ou altera tabelas automaticamente.

Na VPS, `DB_USERNAME` é o login de execução com acesso aos dados, enquanto
`DB_MIGRATION_USERNAME` é usado exclusivamente pelo Flyway para alterar o schema. No
ambiente local, as credenciais de migração herdam as credenciais do datasource.

## Administrador inicial controlado

O bootstrap é desabilitado por padrão e só roda quando `BOOTSTRAP_ENABLED=true`. Ele
cria de forma idempotente apenas a primeira conta `ADMIN_SISTEMA`; hospitais e demais
usuários são criados pelo painel. A senha inicial é temporária e deve ser trocada no
primeiro acesso. Na VPS, ela fica apenas durante a ativação no arquivo protegido
`/etc/inventory-med/api.env` e é removida logo depois.

## Bancos hospitalares

Ao criar um hospital, a API registra a unidade como `PROVISIONING`, cria o banco e o
login SQL exclusivos, aplica `db/migration/tenant`, limita o login ao banco criado e só
então ativa a unidade. A senha técnica do hospital é guardada criptografada no banco
central; a chave de criptografia fica somente na configuração segura da API.

## Testes

Os testes de integração iniciam um SQL Server 2022 descartável por Testcontainers e
validam migrações e segurança contra o banco real.

Com Java 21, Maven e Docker disponíveis:

```powershell
cd backend
.\mvnw.cmd clean test
```

No ambiente atual, Java/Maven também podem ser executados pelo contêiner Maven. Isso
continua sendo uma ferramenta de desenvolvimento local, não o formato de produção.
