# Endurecimento do acesso SSH

**Data:** 2026-10-02
**Responsável:** Gabriel Pereira
**Status:** concluída

## Resumo

O SSH da VPS passou a aceitar somente o usuário nominal `gabriel`, autenticado por chave
pública. O login direto de `root` e as autenticações por senha foram bloqueados.

## Motivo

Reduzir a exposição da conta mais privilegiada do servidor e impedir ataques de força
bruta contra senhas no serviço SSH.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `/etc/ssh/sshd_config.d/00-inventorymed-hardening.conf` | criado na VPS | define a política segura do SSH |
| `docs/security/vps-hardening.md` | criado | documenta a proteção básica da VPS |
| `docs/changes/2026-10-02-endurecimento-ssh.md` | criado | registra a alteração aplicada |
| `docs/changes/2026-10-02-chave-ssh-usuario-gabriel.md` | modificado | remove pendências concluídas |

## Banco de dados

- bancos afetados: nenhum;
- migrações: nenhuma;
- objetos afetados: nenhum.

## Comandos executados na VPS

```bash
sudo nano /etc/ssh/sshd_config.d/00-inventorymed-hardening.conf
sudo sshd -t
sudo systemctl reload ssh
sudo sshd -T
```

## Serviços afetados

- `ssh`: configuração validada e recarregada sem reinicializar a VPS.

## Configurações

- login de `root`: bloqueado no SSH;
- autenticação por senha: desabilitada no SSH;
- autenticação por chave: habilitada;
- usuário permitido: `gabriel`;
- máximo de tentativas por conexão: três;
- encaminhamento gráfico X11: desabilitado.

## Validação

- nova sessão autenticada com a chave nominal de `gabriel`;
- tentativa de autenticação por senha como `root` rejeitada com
  `Permission denied (publickey)`;
- a sessão administrativa preexistente foi mantida durante a validação.

## Recuperação

Em uma sessão administrativa ainda aberta, corrigir ou remover o arquivo dedicado,
executar `sudo sshd -t` e somente então recarregar o serviço. Se não houver sessão
disponível, utilizar o console de recuperação da Hostinger.

## Pendências

- verificar atualizações automáticas;
- remover a chave pública antiga autorizada para `root`.
