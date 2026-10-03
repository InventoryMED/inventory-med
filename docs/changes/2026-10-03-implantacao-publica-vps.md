# Implantação pública na VPS

**Data:** 2026-10-03
**Responsável:** Codex e Gabriel Pereira
**Status:** em implantação

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

Nenhum até este ponto do registro. Os comandos serão acrescentados após a implantação.

## Serviços afetados

- previstos: `inventory-med-api` e `nginx`;
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
- `npm audit --omit=dev --audit-level=high`: nenhuma vulnerabilidade encontrada;
- pendente: acesso externo pelo IP e fluxo de autenticação.

## Recuperação

O instalador mantém os artefatos anteriores e restaura os links em caso de falha de
saúde. Banco não é apagado nem rebaixado automaticamente.

## Pendências

- desbloquear a chave SSH no agente do Windows;
- executar o provisionamento inicial com `sudo`;
- validar a homologação pelo IP;
- configurar domínio e HTTPS antes de qualquer dado real.
