# Interface gráfica e acesso remoto à VPS

**Data:** 2026-10-02
**Responsável:** Gabriel Pereira
**Status:** concluída

## Resumo

Foi instalado o ambiente gráfico XFCE e o serviço XRDP na VPS Ubuntu 22.04. Também foi
criado o usuário nominal `gabriel`, com permissão administrativa por `sudo`, para evitar
o uso do usuário `root` na sessão gráfica.

## Motivo

O responsável técnico solicitou acesso visual à VPS por uma área de trabalho semelhante
a um computador convencional, sem abandonar a administração dos serviços Linux.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/operations/remote-desktop-access.md` | criado | documenta o acesso gráfico e seus controles de segurança |
| `docs/changes/2026-10-02-interface-grafica-xrdp.md` | criado | registra a alteração aplicada na VPS |

## Banco de dados

- bancos afetados: nenhum;
- migrações: nenhuma;
- objetos afetados: nenhum.

## Comandos executados na VPS

```bash
adduser gabriel
usermod -aG sudo gabriel
apt update
apt upgrade -y
DEBIAN_FRONTEND=noninteractive apt install -y xfce4 xfce4-goodies xrdp xorgxrdp dbus-x11
echo "startxfce4" > /home/gabriel/.xsession
chown gabriel:gabriel /home/gabriel/.xsession
chmod 600 /home/gabriel/.xsession
adduser xrdp ssl-cert
systemctl enable xrdp
systemctl restart xrdp
systemctl status xrdp --no-pager
```

O valor da senha do usuário não foi documentado.

## Serviços afetados

- `xrdp`: instalado, habilitado na inicialização e iniciado;
- pacotes base do Ubuntu: atualizados;
- kernel: versão `5.15.0-198-generic` instalada, aguardando reinicialização para entrar
  em uso.

## Configurações

- usuário `gabriel` criado com diretório próprio;
- usuário adicionado ao grupo `sudo`;
- sessão gráfica padrão definida como XFCE;
- usuário de serviço `xrdp` adicionado ao grupo `ssl-cert`;
- XRDP detectado ouvindo na porta TCP 3389.

## Validação

- instalação dos pacotes concluída sem erro fatal;
- `xrdp.service` retornou `Active: active (running)`;
- serviço configurado para iniciar automaticamente;
- porta TCP 3389 validada externamente com `Test-NetConnection`;
- regra do UFW ajustada ao IP público atual do responsável;
- conexão gráfica realizada com sucesso pelo cliente RDP do Windows e pelo usuário
  `gabriel`;
- reinicialização confirmada pelo banner do sistema com o kernel
  `5.15.0-198-generic` em execução.

## Recuperação

Se for necessário interromper o acesso gráfico sem remover os pacotes:

```bash
systemctl disable --now xrdp
```

A remoção completa do ambiente gráfico exige avaliação prévia das dependências e não
deve ser feita automaticamente em produção.

## Pendências

- atualizar a regra do RDP quando o provedor de internet alterar o IP público;
- corrigir ou substituir a chave SSH cuja passphrase não foi validada;
- depois da validação da nova chave, bloquear login SSH direto de `root` e por senha.
