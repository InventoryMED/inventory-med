# Inventory MED

Sistema hospitalar multiunidade para gerenciamento de quartos e leitos, admissão de pacientes, prescrições e evoluções médicas.

O projeto entrou na fase de produto. O front-end demonstrativo continua disponível, enquanto a nova API e o banco passam a substituir os dados locais do navegador.

## Estrutura

- `frontend/`: aplicação Angular e impressão das prescrições/evoluções.
- `backend/`: API Java 21 com Spring Boot e Spring Security.
- `compose.yaml`: SQL Server Express e API para desenvolvimento local.
- `docs/architecture.md`: decisões técnicas e regras de isolamento hospitalar.

## Regra de isolamento

Cada hospital é um domínio independente. Pacientes, quartos, leitos, internações, prescrições e evoluções não são compartilhados entre hospitais. Um profissional pode possuir vínculos com várias unidades, mas acessa apenas os dados do hospital selecionado na sessão atual.

## Executar o ambiente real localmente

Requisitos: Docker Desktop com Docker Compose, Node.js 24 e npm.

```powershell
Copy-Item .env.example .env
docker compose up --build -d
cd frontend
npm install
npm start
```

- Front-end atual: `http://localhost:4200`
- API: `http://localhost:8080/api`
- Saúde da API: `http://localhost:8080/api/actuator/health`
- SQL Server do projeto: `127.0.0.1:14330`

O proxy de desenvolvimento do Angular encaminha `/api` para a API local. O build de produção usado no GitHub Pages continua em modo demonstrativo até a API ter uma URL HTTPS pública.

Consulte `docs/local-development.md` para o roteiro completo e `backend/README.md` para o fluxo de autenticação.

## Executar apenas o protótipo Angular

Requisitos: Node.js 24 e npm.

```bash
cd frontend
npm install
npm start
```

## Publicação atual

O GitHub Pages publica o protótipo Angular automaticamente. A API real será implantada em servidor próprio com HTTPS; o SQL Server ficará acessível somente pela rede privada do servidor.

> O sistema ainda está em desenvolvimento e não deve receber dados reais de pacientes até que autenticação, auditoria, backup, segurança da infraestrutura e validação clínica estejam concluídos.
