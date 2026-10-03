# Chave SSH nominal do usuário Gabriel

**Data:** 2026-10-02
**Responsável:** Gabriel Pereira
**Status:** concluída

## Resumo

Foi criada e instalada uma nova chave SSH ED25519, protegida por passphrase, para o
usuário administrativo nominal `gabriel`. O acesso foi validado sem utilização da senha
da conta na VPS.

## Motivo

A chave cadastrada inicialmente para `root` não pôde ser utilizada porque sua
passphrase não foi validada. Além disso, a arquitetura de segurança exige uma conta
nominal com `sudo`, sem uso cotidiano do usuário `root`.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/changes/2026-10-02-chave-ssh-usuario-gabriel.md` | criado | registra a nova credencial administrativa |
| `docs/changes/2026-10-02-chave-ssh-vps.md` | modificado | registra a substituição da chave inicial |
| `docs/changes/2026-10-02-interface-grafica-xrdp.md` | modificado | confirma o kernel ativo após a reinicialização |

A chave privada permanece somente no computador do responsável em:

```text
C:\Users\gabriel.pereira\.ssh\inventorymed_vps_gabriel_ed25519
```

## Identificação da chave

```text
Tipo: ED25519
Comentário: gabriel@inventorymed-vps-01
Fingerprint: SHA256:kozaT8Rix5vsKstoZgCtvF9QRoKo/Ug96biT+sPVKLw
```

A passphrase e as senhas das contas não são registradas.

## Banco de dados

- bancos afetados: nenhum;
- migrações: nenhuma;
- objetos afetados: nenhum.

## Comandos executados na VPS

Foram criados o diretório e o arquivo padrão de autorização SSH do usuário `gabriel`,
com proprietário e permissões restritas:

```text
/home/gabriel/.ssh/authorized_keys
```

## Serviços afetados

Nenhum serviço foi reiniciado nesta etapa.

## Configurações

- diretório `.ssh`: modo `700`, usuário e grupo `gabriel`;
- arquivo `authorized_keys`: modo `600`, usuário e grupo `gabriel`;
- chave privada protegida por passphrase no computador do responsável.

## Validação

- autenticação realizada com a nova chave;
- cliente solicitou a passphrase local da chave;
- sessão aberta como `gabriel@srv2029473`;
- sistema confirmou o kernel `5.15.0-198-generic` em execução.

## Recuperação

Em caso de perda ou suspeita de comprometimento, remover a linha correspondente do
`authorized_keys`, gerar uma nova chave, instalá-la e validar uma segunda sessão antes
de invalidar a anterior.

## Pendências

- instalar e configurar proteção contra tentativas repetidas de acesso;
- remover a chave pública original da conta `root`.
