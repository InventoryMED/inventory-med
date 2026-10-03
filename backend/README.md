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
- proteção CSRF para todas as operações de escrita.

Pacientes e documentos clínicos não ficam no banco central. As migrações dos bancos
operacionais de cada hospital serão adicionadas em uma etapa própria.

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
5. `GET /api/v1/auth/me` restaura o estado visível após recarregar a página;
6. `POST /api/v1/auth/logout` invalida a sessão no servidor.

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

## Dados iniciais controlados

O bootstrap é desabilitado por padrão e só roda quando `BOOTSTRAP_ENABLED=true`. Ele
cria de forma idempotente Lucas Galante e os dois hospitais de demonstração. A senha
deve ter no mínimo 12 caracteres. Na VPS, ela fica temporariamente no arquivo protegido
`/etc/inventory-med/api.env` e é removida depois da validação inicial.

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
