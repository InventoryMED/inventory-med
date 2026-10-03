# Arquitetura oficial do Inventory MED

**Versão:** 1.0

**Data da decisão:** 02/10/2026

**Status:** arquitetura oficial a ser seguida

Este documento é a fonte de verdade técnica do Inventory MED. O protótipo existente
serve como referência visual e funcional, mas código novo deve respeitar esta
arquitetura. Qualquer mudança estrutural precisa ser registrada neste documento antes
da implementação.

## 1. Objetivo

Construir um sistema web hospitalar simples de entender, instalar e manter, responsável
por:

- autenticação de profissionais;
- seleção da unidade hospitalar autorizada;
- gerenciamento de quartos e leitos;
- admissão, alta e transferência de pacientes;
- prescrições médicas e impressão;
- evoluções médicas e impressão;
- auditoria das operações sensíveis.

O sistema não será dividido em microserviços. A primeira versão será um monólito
modular, executado em uma única VPS.

## 2. Princípios obrigatórios

1. **Simplicidade:** cada tecnologia deve ter uma função clara.
2. **Uma única API:** todo o backend será uma aplicação Java.
3. **Uma única entrada pública:** navegador acessa somente Nginx por HTTP/HTTPS.
4. **Banco privado:** SQL Server nunca terá a porta 1433 exposta à internet.
5. **Isolamento hospitalar:** dados clínicos de hospitais diferentes não são
   armazenados no mesmo banco operacional.
6. **Git como fonte oficial:** nenhuma alteração definitiva será feita apenas na VPS.
7. **Migrações versionadas:** mudanças no banco serão executadas pelo Flyway.
8. **Segredos fora do código:** senhas e chaves ficam em arquivo protegido na VPS.
9. **Auditoria:** alterações clínicas relevantes identificam autor, data e unidade.
10. **Evolução controlada:** não serão adicionados Docker, Kubernetes, Redis, filas ou
    microserviços sem necessidade comprovada e nova decisão arquitetural.

## 3. Visão geral

```text
                         INTERNET
                             |
                    portas públicas 80/443
                             |
                             v
                    +-----------------+
                    |      NGINX      |
                    | domínio e HTTPS |
                    +--------+--------+
                             |
                +------------+-------------+
                |                          |
                v                          v
       / arquivos estáticos          /api/v1/*
       +------------------+       +------------------+
       | FRONTEND ANGULAR |       | API JAVA         |
       | HTML/CSS/JS      |       | SPRING BOOT      |
       +------------------+       +---------+--------+
                                            |
                                   127.0.0.1:1433
                                            |
                                            v
                                  +-------------------+
                                  | SQL SERVER 2022   |
                                  | EXPRESS           |
                                  +-------------------+
```

### Endereços

Durante a preparação inicial:

- frontend: `http://IP-DA-VPS/`;
- API: `http://IP-DA-VPS/api/v1/`;
- saúde da aplicação: `http://IP-DA-VPS/api/v1/actuator/health`.

Depois da configuração do domínio:

- sistema: `https://app.inventorymed.com.br/`;
- API: `https://app.inventorymed.com.br/api/v1/`.

O navegador nunca acessa diretamente Java ou SQL Server. O Nginx entrega o Angular e
encaminha `/api` internamente para a API em `127.0.0.1:8080`.

## 4. Tecnologias oficiais

| Camada | Tecnologia | Responsabilidade |
| --- | --- | --- |
| Servidor | Ubuntu Server 22.04 LTS | Sistema operacional compatível com SQL Server 2022 |
| Proxy web | Nginx | HTTPS, frontend e encaminhamento da API |
| Frontend | Angular | Interface utilizada no navegador |
| Backend | Java 21 + Spring Boot | Regras, segurança e acesso aos dados |
| Banco | SQL Server 2022 Express | Persistência dos dados |
| Migrações | Flyway | Versionamento da estrutura dos bancos |
| Serviços | systemd | Inicialização e reinício automático da API |
| Código | Git + GitHub | Histórico e fonte oficial do sistema |
| Certificado | Let's Encrypt | HTTPS gratuito |

### O que não será usado inicialmente

- Docker na VPS;
- Kubernetes;
- microserviços;
- mensageria;
- Redis;
- servidor de frontend separado;
- painel de hospedagem como cPanel ou CyberPanel.

Esses componentes só serão introduzidos se uma necessidade real justificar a
complexidade.

## 5. Organização física da VPS

```text
/srv/inventory-med/source/          clone do repositório Git
/opt/inventory-med/api/             arquivo executável da API
/var/www/inventory-med/             build estático do Angular
/etc/inventory-med/api.env          configurações e segredos da API
/var/opt/mssql/                     arquivos administrados pelo SQL Server
/var/opt/mssql/backups/             backups locais temporários
/var/log/nginx/                     logs de acesso e erro do Nginx
```

O arquivo `/etc/inventory-med/api.env` será propriedade de `root`, permissão `600`, e
nunca será enviado ao GitHub.

## 6. Serviços em execução

| Serviço systemd | Processo | Acesso |
| --- | --- | --- |
| `nginx` | site e proxy reverso | público em 80/443 |
| `inventory-med-api` | aplicação Java | privado em 127.0.0.1:8080 |
| `mssql-server` | SQL Server Express | privado em 127.0.0.1:1433 |

Comandos de diagnóstico:

```bash
systemctl status nginx
systemctl status inventory-med-api
systemctl status mssql-server
journalctl -u inventory-med-api -f
```

## 7. Arquitetura do banco de dados

Uma única instância do SQL Server conterá bancos separados.

```text
SQL SERVER EXPRESS
|
+-- inventory_med_core
|   +-- usuários
|   +-- hospitais
|   +-- vínculos de usuário com hospital
|   +-- sessões
|   +-- auditoria de autenticação
|
+-- inventory_med_hospital_<id>
    +-- quartos
    +-- leitos
    +-- pacientes
    +-- admissões
    +-- transferências e altas
    +-- prescrições e itens
    +-- evoluções
    +-- auditoria clínica
```

Cada hospital terá seu próprio banco `inventory_med_hospital_<id>`. Não haverá tabela
global de pacientes e não haverá chave estrangeira entre bancos hospitalares.

### Regras de isolamento

1. O usuário autentica no banco `inventory_med_core`.
2. A API consulta somente os vínculos hospitalares ativos daquele usuário.
3. Com um único vínculo, a unidade é selecionada automaticamente.
4. Com vários vínculos, o usuário escolhe uma unidade.
5. A unidade escolhida fica registrada na sessão do servidor.
6. A API resolve internamente qual banco hospitalar utilizar.
7. O frontend nunca informa nome de banco, conexão ou credencial.
8. Cada banco hospitalar terá um usuário SQL próprio, com permissão apenas naquele
   banco.
9. Nenhuma consulta clínica poderá juntar bancos de hospitais diferentes.

O SQL Server Express limita cada banco a 10 GB. O uso será monitorado; uma mudança de
edição ou banco será avaliada antes que qualquer unidade se aproxime desse limite.

## 8. Backend

O backend será um monólito modular. Haverá um único processo Java e um único arquivo
`.jar` para implantar.

### Módulos

```text
br.com.inventorymed
+-- common
+-- config
+-- security
+-- identity
+-- administration
+-- hospitals
+-- beds
+-- patients
+-- admissions
+-- formtemplates
+-- prescriptions
+-- evolutions
+-- transfers
+-- discharges
+-- audit
```

Cada módulo pode conter:

- `controller`: recebe e responde HTTP;
- `service`: executa os casos de uso e transações;
- `repository`: acessa o banco;
- `entity`: modelo persistido;
- `dto`: dados de entrada e saída.

### Regras do backend

- controllers não contêm regra de negócio;
- toda regra de negócio, autorização, validação e transição de estado é executada no
  backend;
- validações do frontend existem apenas para usabilidade e nunca substituem a validação
  do backend;
- entidades JPA não são devolvidas diretamente ao navegador;
- entradas são validadas com Jakarta Validation;
- erros possuem um formato JSON único;
- datas são armazenadas com tipo apropriado, nunca como texto quando forem datas reais;
- toda operação clínica valida a unidade selecionada na sessão;
- prescrições e evoluções criadas não são sobrescritas silenciosamente: mudanças
  relevantes geram versão ou evento de auditoria;
- Flyway é o único meio normal de alterar a estrutura dos bancos;
- a aplicação não utiliza a conta `sa`.

### Administração e conteúdo configurável

O backend disponibilizará casos de uso administrativos para:

- criar, editar, ativar e desativar hospitais;
- criar usuários e administrar seus vínculos e papéis por hospital;
- criar, renomear, ordenar, ativar e desativar quartos e leitos;
- administrar catálogos e opções usados nos formulários;
- criar campos e seções configuráveis de prescrição e evolução;
- criar rascunhos, publicar novas versões e aposentar modelos.

Itens que já possuem histórico clínico não serão apagados fisicamente. Serão
desativados, arquivados, cancelados ou substituídos por nova versão, conforme o caso.

Prescrições e evoluções emitidas podem ser alteradas por profissional clínico autorizado
por meio de retificação ou nova versão. O sistema nunca sobrescreve o registro anterior:
a alteração identifica autor, data, motivo e conteúdo anterior. Permissão administrativa
não equivale a autoria médica.

### Invariante dos documentos clínicos

Prescrições e evoluções seguem um modelo de histórico somente acrescentável:

```text
RASCUNHO -> FINALIZADO -> RETIFICADO, SUBSTITUÍDO OU CANCELADO
```

- enquanto estiver em rascunho, somente o autor ou pessoa autorizada pode editar ou
  descartar o documento;
- após a finalização, o conteúdo original torna-se imutável;
- a interface oferece a ação `ALTERAR/RETIFICAR` para profissional clínico autorizado;
- a ação exige confirmação explícita e justificativa da alteração;
- a identidade vem da sessão autenticada e não pode ser escolhida ou modificada no
  formulário;
- quando a sessão não tiver garantia recente suficiente, a ação exige reautenticação;
- correção de evolução gera retificação vinculada ao documento original e assinada pelo
  autor da retificação;
- mudança de prescrição gera nova versão vinculada à anterior e define claramente qual
  versão está vigente;
- cancelamento exige motivo e não remove o conteúdo;
- todas as versões permanecem acessíveis na linha do tempo autorizada;
- não existirá endpoint comum de exclusão física para documentos finalizados;
- o banco impedirá exclusão acidental por relacionamentos e permissões;
- a aplicação não concederá a administradores permissão para reescrever autoria ou
  conteúdo clínico.

A impressão e a visualização identificam número da versão, situação, autor e data. Uma
versão substituída permanece consultável e aparece como não vigente, evitando que duas
prescrições sejam interpretadas como simultaneamente ativas.

Uma eventual eliminação por obrigação legal ou política formal de retenção não será uma
operação do painel administrativo. Exigirá procedimento específico, autorização,
auditoria e validação jurídica.

Modelos configuráveis aceitarão somente tipos de campo previamente implementados e
seguros. Não será permitido inserir código JavaScript, HTML executável, SQL ou regras de
negócio arbitrárias pelo painel.

## 9. Autenticação e autorização

A primeira versão usará sessão armazenada no backend e cookie seguro. O Angular não
armazenará senha nem token de autenticação.

Fluxo:

```text
login -> sessão autenticada -> seleção do hospital -> acesso aos módulos autorizados
```

Regras:

- senha armazenada com hash forte;
- cookie `HttpOnly`, `Secure` e `SameSite` em produção;
- proteção CSRF habilitada;
- expiração por inatividade;
- encerramento de sessão no logout;
- papéis por hospital, não somente por usuário;
- tentativas e sucessos de login auditados;
- usuário desativado perde acesso a todas as unidades;
- vínculo desativado remove apenas o acesso àquela unidade.

Papéis iniciais:

- `ADMIN_SISTEMA`;
- `ADMIN_HOSPITAL`;
- `RESPONSAVEL_CLINICO`;
- `MEDICO`;
- `ENFERMAGEM`;
- `RECEPCAO`.

O `ADMIN_SISTEMA` gerencia a plataforma inteira: hospitais, administradores hospitalares,
configurações globais, segurança, auditoria e operação técnica. Ele não recebe acesso
clínico rotineiro nem autoria médica por causa desse poder administrativo.

O `ADMIN_HOSPITAL` pode ser o gestor ou responsável de TI da unidade. Ele administra
usuários, papéis permitidos, quartos, leitos, catálogos e configurações somente de seu
hospital. O `RESPONSAVEL_CLINICO` aprova a publicação de modelos que afetem a atividade
médica.

Nenhum administrador hospitalar pode promover a si próprio ou outra pessoa a
`ADMIN_SISTEMA`. As permissões exatas serão definidas por caso de uso e negadas por
padrão.

Quando o `ADMIN_SISTEMA` precisar prestar suporte dentro de uma unidade, deverá entrar em
modo de suporte com hospital explícito, motivo, prazo e auditoria. Não existirá uma tela
que misture dados clínicos de hospitais diferentes.

### Proteção das contas administrativas

- MFA obrigatório para todos os administradores;
- passkey/WebAuthn preferencial e TOTP como alternativa inicial;
- conta individual, sem usuários administrativos compartilhados;
- reautenticação para criar administrador, alterar MFA, publicar modelo clínico,
  desativar hospital ou executar outra ação crítica;
- limitação progressiva de tentativas e alerta de comportamento suspeito;
- sessão administrativa curta e renovação do identificador após autenticação;
- encerramento de todas as sessões após troca de senha ou recuperação de conta;
- notificação de mudança de senha, MFA, papel ou conta administrativa;
- conta de emergência separada, guardada fora do uso diário e auditada a cada acesso;
- todas as autorizações verificadas pelo backend em toda requisição, com negação por
  padrão;
- toda ação administrativa registra sucesso ou falha, autor, unidade, data, endereço de
  origem, objeto afetado, valores relevantes anteriores e novos e justificativa quando
  exigida.

## 10. Frontend

O Angular será organizado por funcionalidades.

```text
src/app
+-- core
|   +-- auth
|   +-- guards
|   +-- interceptors
|   +-- layout
|   +-- api
|
+-- shared
|   +-- components
|   +-- models
|   +-- validators
|
+-- features
    +-- login
    +-- hospital-selection
    +-- administration
    +-- beds
    +-- admissions
    +-- prescriptions
    +-- evolutions
```

Regras:

- componentes não acessam diretamente armazenamento do navegador para simular banco;
- toda informação persistente vem da API;
- o frontend pode ocultar botões sem permissão e validar campos para melhorar a
  experiência, mas o backend repete e decide todas as validações e autorizações;
- menus e respostas da API não constituem autorização: cada requisição é validada no
  servidor;
- `localStorage` pode guardar apenas preferências não sensíveis;
- estado inicial será feito com serviços e Signals, sem biblioteca global adicional;
- formulários clínicos usam Reactive Forms e validação;
- impressão possui componentes e estilos próprios;
- textos visíveis seguem o padrão em caixa alta já definido para o produto;
- interface é responsiva, mas a impressão segue formato próprio de documento.

### Formulários configuráveis

O frontend renderiza definições recebidas da API. A definição informa seção, ordem,
tipo, rótulo, opções e restrições visuais. Ao salvar, o backend carrega a versão do
modelo, valida novamente todos os campos e rejeita conteúdo incompatível.

Uma versão publicada de modelo é imutável. Alterações criam nova versão e atingem
somente documentos futuros. Cada prescrição ou evolução guarda a versão utilizada e um
retrato dos campos apresentados naquele momento.

## 11. Implantação

O GitHub é a fonte oficial. A VPS executa uma versão do repositório, mas não será o
único local onde o código existe.

Fluxo inicial, mantido simples:

```text
alteração -> testes -> commit -> GitHub -> script de implantação na VPS
```

O script de implantação deverá:

1. buscar a versão autorizada no GitHub;
2. executar os testes;
3. compilar a API;
4. compilar o Angular;
5. copiar o `.jar` para `/opt/inventory-med/api/`;
6. copiar o frontend para `/var/www/inventory-med/`;
7. reiniciar `inventory-med-api`;
8. validar `/api/v1/actuator/health`.

Uma implantação não executará comandos SQL manuais; as alterações estruturais serão
aplicadas pelo Flyway.

## 12. Configuração de recursos

Configuração da primeira VPS KVM 4 com 16 GB de RAM:

- SQL Server com limite de memória configurado para não consumir a VPS inteira;
- Java com heap máximo inicial próximo de 2 GB, ajustado por medição;
- Nginx servindo arquivos estáticos;
- rotação automática de logs;
- espaço livre monitorado.

Os valores serão ajustados usando métricas reais, não por suposição.

### Capacidade inicial de referência

A primeira implantação será dimensionada para:

- pelo menos 20 hospitais;
- aproximadamente 15 usuários cadastrados por hospital;
- aproximadamente 300 usuários cadastrados no total;
- uso predominantemente transacional e textual, sem armazenamento de vídeos ou exames
  de imagem;
- até 100 sessões simultaneamente ativas como cenário operacional de referência;
- 300 sessões autenticadas e teste de pico com pelo menos 150 usuários executando
  operações antes da entrada em produção.

Esse dimensionamento não é garantia baseada apenas no plano contratado. A validação será
feita com teste de carga e monitoramento de CPU, memória, disco, conexões, tempo de
resposta, erros e tamanho de cada banco. Os critérios e gatilhos estão em
[`operations/capacity-plan.md`](operations/capacity-plan.md).

O KVM 4 será usado inicialmente para desenvolvimento, homologação e primeiros pilotos.
A meta funcional permanece em pelo menos 20 hospitais, mas a VPS não será aprovada para
essa carga apenas por estimativa. Testes e métricas determinarão quando fazer upgrade
para KVM 8 ou mover o SQL Server para servidor separado, sem alterar os módulos do
produto.

## 13. Segurança da infraestrutura

- acesso SSH por chave, sem compartilhamento de senha root;
- usuário administrativo próprio com `sudo`;
- login SSH de root e autenticação por senha desabilitados após validação da chave;
- firewall permitindo somente SSH controlado, HTTP e HTTPS;
- TLS com renovação automática;
- SQL Server e Java escutando somente em `127.0.0.1`;
- atualizações de segurança do Ubuntu;
- segredos fora do repositório;
- princípio do menor privilégio no SQL Server;
- dados reais somente depois da validação de autenticação, auditoria, backup e
  restauração.

### Administração pelo SSMS

O administrador poderá usar o SQL Server Management Studio no Windows para consultar e
administrar os bancos da VPS. A porta 1433 continuará fechada para a internet.

O acesso será feito por túnel SSH:

```text
SSMS -> 127.0.0.1:15433 -> túnel SSH criptografado -> SQL Server 127.0.0.1:1433
```

Será criada uma conta administrativa nominal para o responsável pelo sistema. A conta
`sa` ficará reservada para recuperação e não será usada diariamente nem pela API.

O SSMS poderá ser usado para inspeção, consultas, backups, restaurações e diagnósticos.
Alterações permanentes na estrutura das tabelas deverão ser registradas em migração
Flyway para que possam ser reproduzidas em qualquer instalação.

## 14. Backup e recuperação

Serão mantidas duas camadas independentes:

1. backup diário `.bak` de cada banco SQL Server;
2. cópia criptografada dos backups para armazenamento fora da VPS.

O snapshot semanal do provedor será complementar. Backup sem teste de restauração não
será considerado suficiente.

Rotina mínima:

- backup diário;
- retenção definida antes da entrada em produção;
- verificação automática de sucesso;
- teste de restauração mensal;
- documentação do procedimento de recuperação.

## 15. Logs e diagnóstico

Na primeira versão não haverá plataforma externa de observabilidade.

- API: `journalctl -u inventory-med-api`;
- Nginx: `/var/log/nginx/`;
- SQL Server: logs próprios em `/var/opt/mssql/log/`;
- saúde: `/api/v1/actuator/health`;
- auditoria funcional: tabelas próprias nos bancos.

Dados clínicos e senhas não podem ser escritos nos logs.

## 16. Ambientes

### Fase inicial

A primeira VPS será ambiente de desenvolvimento e homologação. Usará somente dados
fictícios.

### Produção

Antes de pacientes reais, será necessário:

- decidir se haverá uma segunda VPS de produção;
- configurar domínio e HTTPS;
- revisar permissões;
- validar isolamento hospitalar;
- concluir auditoria;
- executar teste de restauração;
- revisar obrigações de privacidade e LGPD;
- definir suporte, retenção e resposta a incidentes.

## 17. Critério para mudança arquitetural

Uma tecnologia ou serviço novo só será incorporado quando:

1. existir um problema concreto que a arquitetura atual não resolva bem;
2. o benefício e o custo de manutenção forem explicados;
3. a decisão for registrada no repositório;
4. a mudança mantiver o isolamento entre hospitais e a segurança dos dados.

Até que isso aconteça, esta arquitetura deve ser seguida.

## 18. Transparência e documentação de alterações

Cada alteração lógica no projeto deve possuir um registro em `docs/changes/`. O
registro deve informar, em linguagem direta:

- objetivo da alteração;
- arquivos criados, modificados ou removidos;
- tabelas e migrações afetadas;
- comandos executados na VPS;
- configurações e serviços afetados;
- como a alteração foi validada;
- como desfazer ou recuperar;
- pendências conhecidas.

Os comandos importantes da VPS também devem existir em scripts versionados dentro de
`infra/` sempre que isso for possível. A documentação nunca deve conter senhas, chaves
privadas ou dados reais de pacientes.

## 19. Qualidade de engenharia e produto

Código que apenas funciona não é suficiente. Toda entrega deve atender aos padrões de
[`development/quality-standards.md`](development/quality-standards.md).

Regras essenciais:

- código organizado por funcionalidade e responsabilidade;
- nomes consistentes e claros, sem abreviações obscuras;
- nenhuma regra de negócio duplicada ou escondida no frontend;
- nenhuma solução temporária sem registro e prazo de remoção;
- testes proporcionais ao risco, com prioridade para autorização, isolamento hospitalar
  e histórico clínico;
- formatação, análise estática, testes e build executados na integração contínua;
- interface construída a partir de um sistema visual consistente, não de componentes
  genéricos reunidos sem critério;
- estados de carregamento, vazio, erro, sucesso e falta de permissão tratados;
- acessibilidade, responsividade e impressão verificadas;
- mudança só é considerada concluída quando código, testes e documentação concordam.
