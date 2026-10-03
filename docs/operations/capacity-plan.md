# Plano de capacidade para vinte hospitais

## Cenário esperado

- pelo menos 20 hospitais;
- 15 usuários cadastrados por hospital;
- aproximadamente 300 usuários cadastrados no total;
- prescrições, evoluções e dados de leitos predominantemente textuais;
- frontend, API e SQL Server na mesma VPS;
- VPS inicial KVM 4, com 4 vCPUs, 16 GB de RAM, 200 GB NVMe e 16 TB mensais de
  tráfego.

Usuário cadastrado não significa usuário simultâneo. O dimensionamento inicial considera
até 100 sessões ativas em uso normal. Antes da produção, o teste manterá 300 sessões
autenticadas e pelo menos 150 usuários executando operações simultaneamente.

## Avaliação inicial

### Tráfego de internet

O tráfego não deverá ser o primeiro limite. Mesmo em uma hipótese alta de 100 MB por
usuário por dia:

```text
300 usuários x 100 MB x 30 dias = aproximadamente 900 GB por mês
```

Isso representa uma pequena parte dos 16 TB mensais. O cálculo será revisado caso o
sistema passe a armazenar imagens, anexos grandes ou vídeos.

### CPU e memória

Angular será servido como arquivo estático e consumirá poucos recursos. O uso principal
virá da API Java e do SQL Server.

Configuração inicial prevista:

- Java com heap máximo inicial próximo de 2 GB;
- SQL Server com limite configurado;
- Nginx com consumo reduzido;
- memória restante para sistema operacional, cache e tarefas administrativas.

O SQL Server Express possui limites próprios e não consegue aproveitar indefinidamente
um aumento de CPU ou RAM da VPS. Portanto, crescer de plano ajuda a API e o sistema
operacional, mas pode não resolver um gargalo específico do banco.

### Banco e armazenamento

Cada hospital terá banco operacional separado. SQL Server Express limita cada banco a
10 GB. Com vinte hospitais, os bancos não compartilham esse limite, mas todos disputam
os mesmos recursos da instância. O limite de memória do mecanismo Express passa a ser um
risco mais importante que a quantidade de RAM total da VPS.

O disco também armazenará:

- Ubuntu e programas;
- arquivos do SQL Server;
- logs de transação;
- frontend e API;
- logs da aplicação;
- backups locais temporários.

Backups permanentes serão enviados para fora da VPS. Não manteremos várias cópias locais
até ocupar o disco.

## Validação antes de produção

Será executado teste de carga com cenários reais:

1. login e seleção de hospital;
2. listagem de quartos e leitos;
3. admissão de paciente;
4. consulta e alteração de prescrição;
5. criação de evolução;
6. operações administrativas;
7. impressão ou preparação dos documentos.

O teste precisa confirmar:

- isolamento entre hospitais;
- inexistência de duas admissões simultâneas no mesmo leito;
- inexistência de duas versões vigentes da mesma prescrição;
- resposta estável sob carga;
- ausência de erros e esgotamento de conexões;
- recuperação normal depois do pico.

## Métricas acompanhadas

- CPU da VPS;
- memória total e memória dos processos;
- espaço e velocidade do disco;
- tráfego mensal;
- conexões do pool da API;
- conexões e esperas do SQL Server;
- tempo médio e percentil 95 das requisições;
- taxa de erros;
- tamanho dos bancos e logs;
- duração e sucesso dos backups.

## Gatilhos de atenção

- CPU acima de 70% por períodos prolongados;
- memória acima de 80% de maneira contínua;
- disco acima de 70%;
- banco hospitalar acima de 6 GB;
- banco hospitalar acima de 8 GB exige plano de migração imediato;
- aumento persistente do tempo de resposta;
- esgotamento frequente do pool de conexões;
- falha ou crescimento excessivo dos backups;
- uso inesperado de tráfego.

## Caminho de crescimento

1. medir e corrigir consultas, índices ou configurações inadequadas;
2. separar banco e aplicação em servidores diferentes quando o uso compartilhado se
   aproximar dos gatilhos;
3. aumentar recursos da VPS se o gargalo estiver em Java, memória geral ou disco;
4. migrar do SQL Server Express para edição ou serviço de banco apropriado antes de
   atingir seus limites;
5. manter a mesma API e frontend durante a mudança, evitando reconstrução do produto.

O desenho de conexão e configuração da API deve permitir que o banco seja movido para
outro servidor apenas alterando configuração protegida, sem reescrever regras de
negócio.

## Conclusão

O KVM 4 será a primeira VPS e é adequado para desenvolvimento, homologação e pilotos.
Ele não é considerado automaticamente aprovado para vinte hospitais. A entrada de novas
unidades seguirá os resultados dos testes e das métricas reais. Quando necessário,
faremos upgrade para KVM 8 ou separaremos o SQL Server. O SQL Server Express será o
componente acompanhado com maior atenção.
