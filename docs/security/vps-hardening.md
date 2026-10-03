# Proteção básica da VPS

## Acesso administrativo

- o acesso SSH administrativo é realizado pelo usuário nominal `gabriel`;
- a autenticação SSH exige a chave ED25519 cadastrada;
- senhas não são aceitas pelo serviço SSH;
- o usuário `root` não pode iniciar sessão por SSH;
- tarefas privilegiadas são executadas com `sudo` e ficam associadas ao usuário nominal;
- o RDP utiliza o usuário `gabriel` e permanece restrito no firewall ao IP público
  autorizado.

O bloqueio da senha no SSH não altera a senha utilizada pelo XRDP.

## Configuração efetiva do SSH

O arquivo dedicado é:

```text
/etc/ssh/sshd_config.d/00-inventorymed-hardening.conf
```

Diretivas adotadas:

```text
PermitRootLogin no
PubkeyAuthentication yes
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitEmptyPasswords no
MaxAuthTries 3
AllowUsers gabriel
X11Forwarding no
```

Qualquer alteração deve ser validada antes de recarregar o serviço:

```bash
sudo sshd -t
```

Uma sessão administrativa existente deve permanecer aberta enquanto uma segunda sessão
testa a nova configuração.

## Firewall

A política é negar conexões de entrada que não tenham sido autorizadas. Nesta etapa:

- TCP 22: SSH com autenticação exclusiva por chave;
- TCP 3389: XRDP somente para o IP público autorizado;
- demais portas de entrada: bloqueadas.

As portas 80 e 443 serão liberadas quando o Nginx for instalado. A porta do SQL Server
não deverá ser aberta para toda a internet.

## Proteções complementares

O servidor deve manter:

- atualizações de segurança automáticas;
- Fail2ban para bloqueio temporário de tentativas repetidas;
- registros de autenticação e auditoria;
- revisão periódica de usuários, chaves e regras de firewall.
