# Publicação automatizada na VPS

**Data:** 2026-10-07
**Responsável:** Codex e equipe Inventory MED
**Status:** concluída em ambiente local

## Resumo

Foi criado um comando único para validar, empacotar, enviar, instalar e conferir uma
versão do Inventory MED na VPS. A publicação continua vinculada a um commit existente
no GitHub e utiliza o rollback já implementado no instalador remoto.

## Motivo

Reduzir os passos manuais e o risco de enviar um pacote incorreto, mantendo visíveis as
validações, a versão publicada e o procedimento de recuperação.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `infra/scripts/deploy-production.ps1` | criado | Orquestra Git, testes, build, envio, instalação, saúde e comprovante local; localiza Git e Docker em instalações conhecidas. |
| `infra/scripts/upload-release.ps1` | modificado | Restringe parâmetros e exige chave, host conhecido e autenticação não interativa. |
| `infra/scripts/build-release.ps1` | modificado | Usa codificação UTF-8 compatível com Windows PowerShell 5.1. |
| `docs/operations/vps-deployment.md` | modificado | Documenta o novo comando, pré-condições e limites. |
| `docs/changes/2026-10-07-publicacao-automatizada-vps.md` | criado | Registra esta alteração operacional. |

## Banco de dados

- bancos afetados: nenhum nesta alteração;
- migrações: nenhuma;
- objetos afetados: nenhum.

Migrações que façam parte de uma versão futura continuarão sendo aplicadas pelo Flyway
na inicialização da API, nunca por SQL manual dentro do script de publicação.

## Comandos executados na VPS

Nenhum. O novo script foi criado e validado localmente, mas não foi usado para publicar
uma versão nesta alteração.

Quando executado, ele confere o hash do pacote, extrai a versão e chama:

```bash
sudo bash /home/gabriel/inventory-med-upload/COMMIT/infra/scripts/install-release.sh \
  /home/gabriel/inventory-med-upload/COMMIT COMMIT
```

O instalador troca os links do JAR e do frontend, reinicia a API, valida Nginx e saúde
local e restaura os links anteriores se a nova API falhar.

## Serviços afetados

Nenhum durante a criação do script. Em uma publicação real:

- `inventory-med-api` é reiniciado;
- `nginx` é validado e recarregado;
- `mssql-server` não é reiniciado.

## Configurações

Nenhuma variável ou segredo foi criado. O script usa a chave SSH local já existente e
as configurações protegidas que permanecem em `/etc/inventory-med/api.env`.

## Validação

- análise sintática dos scripts PowerShell;
- verificação do fluxo de interrupção quando o repositório contém alterações sem commit;
- descoberta automática do Git do ambiente Codex validada com o diretório removido
  temporariamente do `PATH`;
- arquivos PowerShell gravados com marcador UTF-8 para preservar mensagens em português
  no Windows PowerShell 5.1;
- revisão dos caminhos remotos e da conferência SHA-256;
- autenticação SSH não interativa validada em modo somente leitura com a chave nominal;
- nenhum segredo foi incluído nos scripts ou na documentação;
- nenhuma implantação foi executada durante esta alteração.

## Recuperação

Se o novo orquestrador apresentar problema, o fluxo manual documentado em
`docs/operations/vps-deployment.md` continua disponível. Durante uma instalação real,
`install-release.sh` restaura automaticamente o JAR e o frontend anteriores se a API
não ficar saudável. Migrações já aplicadas exigem migração compensatória ou restauração
validada; tabelas nunca devem ser apagadas manualmente.

## Pendências

- executar a primeira publicação assistida com um commit aprovado;
- configurar domínio e HTTPS antes de classificar a VPS como produção clínica.
