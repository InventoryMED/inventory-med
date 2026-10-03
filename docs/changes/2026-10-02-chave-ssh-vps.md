# Chave SSH administrativa da VPS

**Data:** 2026-10-02

**Responsável:** Gabriel Pereira

**Status:** substituída

## Resumo

Foi criada uma chave SSH ED25519 dedicada ao Inventory MED e sua parte pública foi
cadastrada durante o provisionamento da VPS Hostinger.

## Motivo

O acesso por chave é mais seguro que o uso cotidiano da senha root e permitirá
administrar a VPS sem compartilhar credenciais confidenciais.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `docs/changes/2026-10-02-chave-ssh-vps.md` | criado | registrou a criação e vinculação da chave |

A chave privada foi criada fora do repositório em:

```text
C:\Users\gabriel.pereira\.ssh\inventorymed_vps_ed25519
```

A chave pública correspondente está em:

```text
C:\Users\gabriel.pereira\.ssh\inventorymed_vps_ed25519.pub
```

## Identificação da chave

```text
Tipo: ED25519
Comentário: inventorymed-vps
Fingerprint: SHA256:kSRfSw+vuy8+s6LDL8R1a3FL2NjxRsFg6c25W7d1MGg
```

A chave privada, a senha da chave e a senha root não são registradas na documentação.

## Banco de dados

- bancos alterados: nenhum;
- migrações executadas: nenhuma;
- dados alterados: nenhum.

## Comandos executados na VPS

Nenhum. A chave foi cadastrada durante o provisionamento no painel da Hostinger.

## Serviços afetados

Nenhum serviço foi alterado nesta etapa.

## Configurações

- chave pública SSH autorizada no provisionamento;
- senha root definida e guardada pelo responsável, sem compartilhamento.

## Validação

- arquivo público localizado no computador do responsável;
- tipo e comentário conferidos;
- fingerprint calculada com `ssh-keygen`;
- chave pública cadastrada no painel da Hostinger.

A chave pública foi cadastrada na VPS, mas a passphrase da chave privada original não
foi validada durante o primeiro acesso. O cliente SSH recorreu à senha do usuário
`root`. Por isso, esta chave deixou de ser a credencial administrativa escolhida e foi
substituída por uma chave nominal do usuário `gabriel`.

## Recuperação

Se a chave privada for perdida ou suspeita de comprometimento:

1. remover imediatamente a chave pública da VPS;
2. gerar novo par de chaves;
3. cadastrar a nova chave pública;
4. registrar a rotação em `docs/changes/`.

## Pendências

- remover a chave pública original da autorização do usuário `root` após concluir o
  bloqueio do login direto desse usuário.
