# Inventory MED

Sistema hospitalar multiunidade para gerenciamento de quartos e leitos, admissão de pacientes, prescrições e evoluções médicas.

O protótipo visual existente será preservado como referência. A nova versão será reconstruída seguindo uma arquitetura simples e oficial: uma VPS Ubuntu, Nginx, frontend Angular, uma API Java/Spring Boot e SQL Server Express instalado diretamente no servidor.

## Estrutura

- `frontend/`: aplicação Angular e impressão das prescrições/evoluções.
- `backend/`: API Java 21 com Spring Boot e Spring Security.
- `compose.yaml`: ambiente legado de desenvolvimento local, preservado enquanto a nova base é criada.
- `docs/architecture.md`: arquitetura oficial, implantação e regras de isolamento hospitalar.
- `docs/change-management.md`: procedimento obrigatório de documentação das alterações.
- `docs/operations/ssms-access.md`: acesso seguro aos bancos da VPS pelo SSMS.
- `docs/operations/capacity-plan.md`: capacidade inicial, métricas e gatilhos de crescimento.
- `docs/security/administrative-access.md`: perfis administrativos e proteções obrigatórias.
- `docs/product/configurable-forms.md`: modelo versionado de prescrição e evolução configuráveis.
- `docs/development/quality-standards.md`: critérios obrigatórios de código, testes e design.
- `docs/development/testing.md`: execução reproduzível dos testes locais.
- `docs/changes/`: histórico técnico detalhado das alterações.
- `AGENTS.md`: regras obrigatórias para futuras alterações no código.

## Regra de isolamento

Cada hospital é um domínio independente e possui banco operacional próprio. Pacientes, quartos, leitos, internações, prescrições e evoluções não são compartilhados entre hospitais. Um profissional pode possuir vínculos com várias unidades, mas acessa apenas os dados do hospital selecionado na sessão atual.

## Arquitetura oficial

Consulte [`docs/architecture.md`](docs/architecture.md) antes de implementar qualquer funcionalidade. A implantação inicial na VPS será direta no Ubuntu 22.04, sem Docker, Kubernetes ou microserviços.

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
- API: `http://localhost:8080/api/v1`
- Saúde da API: `http://localhost:8080/api/v1/actuator/health`
- SQL Server do projeto: `127.0.0.1:14330`

O proxy de desenvolvimento do Angular encaminha `/api` para a API local. O build
`production` usa a API real no mesmo endereço do frontend. O GitHub Pages utiliza a
configuração separada `pages`, que continua exclusivamente demonstrativa.

Consulte `docs/local-development.md` para o roteiro completo e `backend/README.md` para o fluxo de autenticação.

## Executar apenas o protótipo Angular

Requisitos: Node.js 24 e npm.

```bash
cd frontend
npm install
npm start
```

## Ambientes

- desenvolvimento local: API e SQL Server em Docker e Angular em `localhost:4200`;
- homologação pública: frontend e API na VPS pelo IP, somente com dados fictícios;
- demonstração estática: GitHub Pages sem acesso à API real;
- produção clínica: será liberada somente com domínio, HTTPS, backup e validação de
  segurança concluídos.

O procedimento da VPS está em
[`docs/operations/vps-deployment.md`](docs/operations/vps-deployment.md).

> O sistema ainda está em desenvolvimento e não deve receber dados reais de pacientes até que autenticação, auditoria, backup, segurança da infraestrutura e validação clínica estejam concluídos.
