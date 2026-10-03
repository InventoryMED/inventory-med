# Implantação pública na VPS

**Data:** 2026-10-03
**Responsável:** Codex e Gabriel Pereira
**Status:** implantada em homologação; aguardando validação do primeiro login

## Resumo

Foi preparada a separação entre desenvolvimento local, demonstração no GitHub Pages e
homologação pública na VPS. O build de produção do Angular passa a usar a API real. A
VPS recebe um pacote versionado contendo JAR, frontend e scripts reproduzíveis.

## Motivo

Permitir testar localmente, aprovar uma versão e somente depois publicá-la no IP da VPS,
sem expor SQL Server ou Java diretamente à internet.

## Arquivos alterados

| Arquivo ou grupo | Operação | Explicação |
| --- | --- | --- |
| `frontend/src/environments/` | modificado/criado | separa produção real e demonstração Pages |
| `frontend/angular.json` | modificado | adiciona configuração `pages` |
| `.github/workflows/deploy-pages.yml` | modificado | preserva o Pages demonstrativo |
| `backend/src/main/resources/application.yml` | modificado | separa credenciais Flyway e execução e aceita endereço privado |
| `backend/src/main/java/br/com/inventorymed/bootstrap/` | modificado | bootstrap explícito e desabilitado por padrão |
| `infra/nginx/` | criado | Nginx, proxy `/api` e cabeçalhos de segurança |
| `infra/systemd/` | criado | serviço Java com restrições de sistema |
| `infra/sql/` | criado | provisiona banco e logins de menor privilégio |
| `infra/scripts/` | criado | build, envio, provisionamento, instalação e desativação do bootstrap |
| `docs/operations/vps-deployment.md` | criado | roteiro completo e recuperação |

## Banco de dados

- banco previsto: `inventory_med_core`;
- migrações Flyway: nenhuma nova nesta alteração;
- login de execução: leitura e escrita no banco central, sem DDL;
- login de migração: DDL e dados necessários ao Flyway, sem `sysadmin`;
- SQL Server permanece em `127.0.0.1:1433`.

## Comandos executados na VPS

- envio e conferência SHA-256 do pacote `inventory-med-e59449b2ce3b.tar.gz`;
- `sudo bash infra/scripts/provision-vps.sh`;
- `sudo bash infra/scripts/configure-vps-environment.sh`;
- `sudo bash infra/scripts/install-release.sh "$PWD" e59449b2ce3b`.

A versão publicada corresponde ao commit Git `e59449b2ce3b`. Nenhuma senha foi
registrada neste documento ou passada como argumento de linha de comando.

## Serviços afetados

- ativos e habilitados no boot: `inventory-med-api`, `nginx` e `mssql-server`;
- o SQL Server não será exposto nem reiniciado pela implantação da aplicação.

## Configurações

- `SERVER_ADDRESS`;
- `DB_MIGRATION_USERNAME`;
- `DB_MIGRATION_PASSWORD`;
- `SESSION_COOKIE_SECURE`;
- demais variáveis já documentadas para API e bootstrap.

Nenhum valor secreto foi incluído.

## Validação

- 6 testes integrados da API aprovados com SQL Server 2022;
- 10 testes Angular aprovados;
- builds `production` e `pages` aprovados;
- scripts Bash validados com `bash -n` e ShellCheck sem ocorrências;
- scripts PowerShell analisados pelo parser sem erro;
- provisionamento SQL validado em SQL Server Express descartável;
- API iniciada com logins separados de execução e migração, saúde `UP`, login de Lucas
  Galante aprovado e dois hospitais retornados;
- Angular atualizado para `22.2.1` e `npm audit --audit-level=low`: nenhuma
  vulnerabilidade encontrada;
- frontend externo `http://179.236.237.36/`: HTTP 200 via Nginx;
- saúde externa `http://179.236.237.36/api/v1/actuator/health`: `UP`;
- rota `/api/v1/auth/me`: HTTP 401 sem autenticação;
- token CSRF, política CSP e cabeçalhos contra framing e MIME sniffing verificados;
- SQL Server escutando somente em `127.0.0.1:1433`;
- API escutando somente em `127.0.0.1:8080`;
- somente o Nginx escutando publicamente na porta HTTP 80;
- pendente: validar o primeiro login no navegador e desativar o bootstrap.

## Recuperação

O instalador mantém os artefatos anteriores e restaura os links em caso de falha de
saúde. Banco não é apagado nem rebaixado automaticamente.

## Pendências

- validar o primeiro login de Lucas Galante e a seleção de hospital;
- executar `sudo bash infra/scripts/disable-bootstrap.sh` imediatamente depois da
  validação, removendo a senha inicial do ambiente da API;
- manter a homologação restrita a dados fictícios enquanto o acesso usar HTTP por IP;
- configurar domínio, HTTPS, política de backup e restauração testada antes de qualquer
  dado real.
