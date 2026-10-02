# Inventory MED API

API do Inventory MED construída com Java 21, Spring Boot, Spring Security, Spring Data JPA, Flyway e SQL Server.

## Executar com Docker

Na raiz do repositório:

```bash
cp .env.example .env
docker compose up --build
```

Serviços locais:

- API: `http://localhost:8080/api`
- Saúde: `http://localhost:8080/api/actuator/health`
- SQL Server: `127.0.0.1:14330` (porta externa configurável por `DB_HOST_PORT`)

O SQL Server fica restrito ao computador local no ambiente de desenvolvimento. Em produção, a porta 1433 não deve ser publicada na internet.

## Autenticação inicial

No perfil `dev`, o inicializador cria o usuário e os dois hospitais informados no `.env`.

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "lucas.galante@inventorymed.local",
  "password": "senha-configurada-no-env"
}
```

Se o profissional possuir acesso a mais de um hospital, use o token retornado para selecionar a unidade:

```http
POST /api/auth/select-hospital
Authorization: Bearer TOKEN_DO_LOGIN
Content-Type: application/json

{
  "hospitalId": "UUID_DO_HOSPITAL"
}
```

O segundo token contém o `hospital_id` selecionado e a função do profissional. Endpoints clínicos exigirão esse token hospitalar.

## Banco de dados

As mudanças estruturais ficam em `src/main/resources/db/migration` e são aplicadas pelo Flyway na inicialização.

Cada entidade clínica contém `hospital_id`. As chaves estrangeiras compostas impedem que paciente, leito, internação, prescrição ou evolução de um hospital seja relacionado a outro hospital.

## Testes

```bash
./mvnw verify
```

No Windows:

```powershell
.\mvnw.cmd verify
```
