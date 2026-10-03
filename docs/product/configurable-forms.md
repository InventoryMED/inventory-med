# Formulários configuráveis de prescrição e evolução

## Objetivo

Permitir que cada hospital adapte modelos de prescrição e evolução sem alterar código e
sem comprometer documentos clínicos já emitidos.

## Conceito

O administrador trabalha em um construtor de formulários controlado. Ele poderá:

- adicionar e renomear seções;
- adicionar, renomear e ordenar campos;
- definir campos obrigatórios ou opcionais;
- cadastrar e ordenar opções de listas;
- desativar campos e opções;
- criar um rascunho a partir de um modelo existente;
- visualizar antes de publicar;
- solicitar publicação;
- aposentar uma versão antiga.

O administrador não poderá inserir código executável, SQL ou lógica arbitrária.

## Tipos de campo permitidos

A primeira versão poderá disponibilizar tipos aprovados, como:

- texto curto;
- texto longo;
- número inteiro;
- número decimal;
- data;
- hora;
- seleção única;
- seleção múltipla;
- sim/não;
- linha de medicamento com colunas previamente definidas;
- tabela clínica previamente implementada.

Novos tipos exigem implementação e validação no backend e no frontend.

## Ciclo de vida do modelo

```text
RASCUNHO -> EM_REVISAO -> PUBLICADO -> APOSENTADO
```

- rascunho pode ser alterado;
- em revisão fica bloqueado para publicação até aprovação;
- publicado é imutável;
- uma mudança em modelo publicado cria uma nova versão;
- aposentado não aparece para novos documentos, mas continua disponível no histórico.

## Persistência

O banco manterá definições versionadas de:

- modelo;
- versão;
- seção;
- campo;
- opção;
- restrições permitidas;
- ordem de exibição;
- autor, revisor e datas.

Cada documento preenchido guarda:

- identificador da versão do modelo;
- valores validados;
- retrato dos rótulos e opções usados;
- autor e data;
- versões, retificações e cancelamentos posteriores.

## Imutabilidade dos documentos emitidos

O modelo configurável pode evoluir, mas um documento já finalizado nunca muda com ele.
O sistema preservará o modelo e os valores usados naquele momento.

- rascunho pode ser editado ou descartado por usuário autorizado;
- prescrição finalizada pode ser alterada por profissional autorizado, mas a ação cria
  uma nova versão e não sobrescreve a anterior;
- evolução finalizada pode ser alterada por profissional autorizado, mas a ação cria
  uma retificação sem substituir o texto original;
- alterar ou retificar exige confirmação explícita e justificativa;
- a autoria é obtida exclusivamente da sessão autenticada;
- a nova versão registra data, hora e autor, mesmo quando ele for diferente do autor
  original;
- cancelamento altera o estado e exige motivo, mas mantém o conteúdo;
- nova versão aponta para a anterior, formando uma linha do tempo;
- administrador configura modelos, mas não altera autoria ou conteúdo clínico emitido.

Na interface, o usuário poderá trabalhar como uma edição comum. Ao confirmar, a API
executará uma operação de versionamento em uma única transação. A versão anterior ficará
marcada como substituída, e somente a versão nova ficará vigente.

O backend não disponibilizará exclusão física comum para prescrições e evoluções
finalizadas.

Campos clínicos essenciais continuarão estruturados em colunas próprias. Campos
adicionais configuráveis poderão ser armazenados em formato flexível validado pelo
backend, sem substituir os dados essenciais.

## Regra de segurança

O Angular apenas renderiza a definição recebida e oferece validação visual. Ao receber
o envio, o backend:

1. identifica hospital e usuário pela sessão;
2. verifica a permissão;
3. carrega a versão publicada correta;
4. valida tipo, tamanho, obrigatoriedade e opções;
5. rejeita campos desconhecidos;
6. salva o documento e a auditoria em uma única transação.

Uma requisição manipulada no navegador não consegue criar campo, ignorar obrigatoriedade
ou usar opção não autorizada.
