# Estrutura inicial dos hospitais de João Pinheiro

**Data:** 2026-10-04
**Responsável:** equipe Inventory MED
**Status:** planejada para execução na VPS

## Resumo

Foi preparado um inicializador idempotente para cadastrar cinco quartos e quatro leitos
em cada uma das unidades `HOSPITAL DE JOÃO PINHEIRO` e `UPA DE JOÃO PINHEIRO`.

## Motivo

As unidades precisam iniciar a operação com uma estrutura mínima de quartos e leitos,
mantendo cada conjunto de dados em seu banco hospitalar exclusivo.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `infra/scripts/initialize-joao-pinheiro-rooms.sh` | criado | autentica pela API e cria somente unidades, quartos e leitos ausentes |
| `docs/changes/2026-10-04-estrutura-inicial-joao-pinheiro.md` | criado | registra a criação operacional e sua validação |

## Banco de dados

- bancos afetados: bancos exclusivos dos dois hospitais;
- migrações: nenhuma;
- dados planejados em cada hospital:
  - unidade `CLÍNICA MÉDICA`;
  - quartos `QUARTO 101` a `QUARTO 105`;
  - leitos `A`, `B`, `C` e `D` em cada quarto;
  - estado inicial dos leitos: `AVAILABLE`;
- nenhum dado é inserido diretamente: todas as operações passam pelos casos de uso da
  API, validação de `ADMIN_SISTEMA` e auditoria central.

## Comandos executados na VPS

Após enviar e validar o script:

```bash
bash /home/gabriel/initialize-joao-pinheiro-rooms.sh
```

O e-mail e a senha do administrador são solicitados no terminal. A senha não aparece,
não entra em argumentos de processo, não é registrada e a sessão é encerrada ao final.
A comunicação com a API ocorre por `127.0.0.1` dentro da VPS e chega ao servidor por SSH.

## Serviços afetados

Nenhum serviço precisa ser reiniciado.

## Configurações

Nenhuma.

## Validação

- análise sintática do script: pendente;
- autenticação e autorização pela API: pendentes;
- isolamento e contagem final por hospital: pendentes;
- visualização no acesso médico: pendente.

## Recuperação

O script é idempotente e não apaga registros. Uma eventual remoção dos dados criados não
deve ser feita por SQL. Quartos e leitos devem ser desativados pelo caso de uso
administrativo apropriado para preservar auditoria e histórico.

## Pendências

- executar na VPS;
- confirmar cinco quartos e vinte leitos em cada banco;
- validar a visualização no frontend médico.
