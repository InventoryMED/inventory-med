# Implantação na VPS

## Objetivo

Publicar o Inventory MED na VPS sem eliminar o ambiente local. O desenvolvimento e os
testes continuam em `localhost`; somente uma versão aprovada e vinculada a um commit Git
é enviada ao servidor.

## Ambientes

| Ambiente | Endereço | Dados | Finalidade |
| --- | --- | --- | --- |
| Local | `http://127.0.0.1:4200` | fictícios | desenvolvimento e testes |
| GitHub Pages | endereço do Pages | fictícios | demonstração estática |
| VPS por IP | `http://179.236.237.36` | somente fictícios | homologação pública |
| Produção clínica | domínio HTTPS a definir | reais após aprovação | uso hospitalar |

O IP público usa HTTP nesta primeira etapa. Por isso, `SESSION_COOKIE_SECURE=false` é
aceito somente na homologação. Não cadastrar pacientes reais. Com domínio e HTTPS, o
valor passa obrigatoriamente para `true`.

## O que roda na VPS

- Nginx público nas portas autorizadas;
- Angular estático em `/var/www/inventory-med/current`;
- API Java somente em `127.0.0.1:8080`;
- SQL Server somente em `127.0.0.1:1433`;
- serviço `inventory-med-api` controlado pelo systemd;
- segredos somente em `/etc/inventory-med/api.env`, proprietário `root`, modo `600`.

O navegador nunca acessa diretamente as portas 8080 ou 1433.

## Configuração do frontend

- `ng build` usa `environment.production.ts` e acessa a API real em `/api/v1`;
- `ng build --configuration pages` usa `environment.pages.ts` e mantém a demonstração
  sem backend;
- `ng serve` usa `environment.ts` e encaminha `/api` para a API local.

As telas de leitos, admissão, prescrição e evolução já estão presentes, mas os dados
operacionais ainda vêm do armazenamento demonstrativo. Na homologação, a autenticação e
a seleção de hospital são reais; essa limitação aparece visivelmente na interface.

## 1. Gerar uma versão no Windows

Depois de testes, revisão, commit e envio ao GitHub:

```powershell
cd 'C:\Inventory MED'
.\infra\scripts\build-release.ps1
```

O script:

1. exige repositório Git sem alterações pendentes;
2. executa os testes e o build Angular;
3. executa `mvn clean verify` em Java 21 com SQL Server descartável;
4. gera `artifacts/releases/inventory-med-COMMIT.tar.gz`.

`artifacts/` é ignorado pelo Git.

## 2. Liberar a chave SSH nesta sessão do Windows

```powershell
Start-Service ssh-agent
ssh-add "$env:USERPROFILE\.ssh\inventorymed_vps_gabriel_ed25519"
```

A frase secreta é digitada no Windows e não é enviada ao repositório.

## 3. Enviar o pacote

```powershell
.\infra\scripts\upload-release.ps1 `
  -ArchivePath '.\artifacts\releases\inventory-med-COMMIT.tar.gz'
```

O envio usa o usuário `gabriel` e grava em
`/home/gabriel/inventory-med-upload/`. Nenhuma instalação privilegiada ocorre nessa
etapa.

## 4. Preparar a VPS na primeira implantação

Entre por SSH, substitua `COMMIT` e execute:

```bash
cd /home/gabriel/inventory-med-upload
tar -xzf inventory-med-COMMIT.tar.gz
cd COMMIT
sudo bash infra/scripts/provision-vps.sh
sudo bash infra/scripts/configure-vps-environment.sh
```

O primeiro script instala Java 21 e Nginx, cria o usuário de serviço e configura o
firewall HTTP. O segundo pede no terminal:

- a senha atual de `sa`, usada apenas para provisionar o banco;
- uma senha inicial para Lucas Galante.

Ele gera internamente senhas diferentes para o login de execução e para o login de
migração, cria `inventory_med_core` e grava o arquivo protegido de ambiente. A senha de
`sa` não entra em argumento de processo, arquivo permanente ou Git.

## 5. Instalar a versão

Ainda dentro da pasta `COMMIT`:

```bash
sudo bash infra/scripts/install-release.sh "$PWD" COMMIT
```

O script instala JAR e frontend, reinicia a API, recarrega o Nginx e valida a saúde. Se
a nova API não ficar saudável, os links da versão anterior são restaurados.

## 6. Validar externamente

No Windows:

```powershell
Invoke-RestMethod http://179.236.237.36/api/v1/actuator/health
Start-Process http://179.236.237.36
```

Validar login, escolha de hospital, atualização da página e logout. Depois do primeiro
acesso bem-sucedido, na VPS:

```bash
sudo bash infra/scripts/disable-bootstrap.sh
```

Isso desativa a criação inicial, remove a senha inicial do arquivo de ambiente e
reinicia a API.

## Diagnóstico

```bash
systemctl status inventory-med-api --no-pager
journalctl -u inventory-med-api -n 100 --no-pager
systemctl status nginx --no-pager
nginx -t
curl http://127.0.0.1:8080/api/v1/actuator/health
sudo ufw status
```

## Reversão

`install-release.sh` preserva versões anteriores e restaura automaticamente os links se
a validação falhar. Para reversão manual, aponte
`/opt/inventory-med/api/inventory-med-api.jar` e `/var/www/inventory-med/current` para a
versão anterior, reinicie a API e recarregue o Nginx.

Migrações de banco não devem ser revertidas apagando tabelas. Se uma implantação futura
alterar dados de produção, a recuperação exigirá backup validado e procedimento próprio.
