# Acesso gráfico à VPS

## Objetivo

Permitir que o responsável técnico visualize e administre a VPS por uma área de
trabalho gráfica, usando o cliente **Conexão de Área de Trabalho Remota** do Windows.

## Componentes

- **XFCE:** ambiente gráfico leve para Ubuntu;
- **XRDP:** servidor compatível com o protocolo RDP;
- **XorgXRDP:** integração da sessão gráfica com o XRDP;
- **usuário `gabriel`:** conta nominal usada no desktop e nas tarefas administrativas.

O acesso gráfico como `root` não deve ser utilizado.

## Conexão

No Windows, abrir `mstsc` e informar o IP público da VPS. Na tela do XRDP:

- sessão: `Xorg`;
- usuário: `gabriel`;
- senha: a senha local definida para esse usuário.

Senhas não devem ser registradas neste repositório.

## Segurança de rede

A porta TCP 3389 não deve ficar aberta para toda a internet. A regra de firewall deve
permitir RDP somente a partir do IP público autorizado do responsável técnico. A porta
22 deve permanecer disponível até que o acesso SSH por chave esteja novamente validado.

Se o IP de origem mudar, a regra de RDP deve ser atualizada antes de uma nova conexão.
O primeiro acesso validado exigiu essa atualização porque o endereço público da conexão
mudou entre duas sessões administrativas.

## Verificação

Na VPS:

```bash
systemctl status xrdp --no-pager
ss -lntp | grep 3389
ufw status numbered
```

O XRDP deve aparecer como `active (running)` e o firewall deve mostrar a origem
autorizada explicitamente.

## Observações operacionais

- o desktop gráfico é uma conveniência administrativa e não participa da execução da
  aplicação Inventory MED;
- Java, SQL Server, Nginx e a aplicação continuarão executando como serviços do Linux;
- pacotes e atualizações devem continuar sendo instalados pelo gerenciador `apt`;
- o novo kernel somente entra em uso depois da reinicialização da VPS.
