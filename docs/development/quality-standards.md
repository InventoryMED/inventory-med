# Padrões de qualidade do Inventory MED

## Objetivo

Garantir que o Inventory MED seja seguro, compreensível, testável, agradável de usar e
possível de evoluir sem improvisações acumuladas.

## Princípios

- clareza antes de esperteza;
- solução simples antes de abstração prematura;
- regra escrita uma vez e no lugar correto;
- segurança e isolamento desde o desenho;
- nomes que expressem intenção;
- comportamento comprovado por testes;
- interface coerente com o trabalho hospitalar;
- dívida técnica sempre visível, justificada e rastreável.

## Backend Java

### Organização

- Java 21 e Spring Boot conforme a arquitetura oficial;
- monólito modular organizado por funcionalidade;
- cada módulo expõe apenas o necessário;
- controller recebe HTTP e delega;
- service/application executa caso de uso e transação;
- domínio contém regras e estados válidos;
- repository concentra persistência;
- DTOs separam API de entidades JPA;
- dependências são injetadas pelo construtor;
- configurações ficam em classes próprias e tipadas.

### Regras de implementação

- nenhum controller contém regra de negócio;
- nenhuma entidade JPA é retornada diretamente;
- nenhuma consulta SQL é construída por concatenação de entrada;
- toda entrada é validada no backend;
- hospital é obtido da sessão e nunca confiado a partir do corpo da requisição;
- transações possuem limite explícito no caso de uso;
- exceções de domínio são específicas e convertidas por tratamento central;
- respostas de erro seguem formato único e não revelam detalhes internos;
- datas, valores e estados usam tipos apropriados;
- estados clínicos usam transições explícitas;
- consultas evitam carregamento excessivo e problema N+1;
- alterações estruturais usam Flyway;
- logs não contêm senhas, segredos, sessão ou conteúdo clínico desnecessário;
- comentários explicam decisões incomuns, não repetem o código.

### Tamanho e legibilidade

- classes e métodos devem possuir uma responsabilidade reconhecível;
- métodos extensos devem ser divididos por intenção, sem fragmentação artificial;
- duplicações relevantes devem ser removidas;
- abreviações obscuras e nomes genéricos como `data`, `obj`, `util` e `manager` são
  evitados quando houver nome de domínio melhor;
- `TODO` só é aceito com justificativa e item rastreável.

### Testes

- testes unitários para regras de domínio;
- testes de integração para banco, migrações e transações;
- testes de API para autenticação, autorização e contratos;
- testes obrigatórios de isolamento entre hospitais;
- testes para impedir exclusão ou sobrescrita de documento clínico finalizado;
- testes de concorrência para leito, admissão e versão vigente quando houver risco;
- correção de defeito deve incluir teste que falhava antes da correção;
- cobertura é indicador, não substituto de cenários relevantes.

## Frontend Angular

### Organização

- Angular em modo estrito;
- funcionalidades separadas em `features`;
- infraestrutura compartilhada em `core`;
- componentes reutilizáveis realmente genéricos em `shared`;
- componentes pequenos, com responsabilidade clara;
- Reactive Forms tipados para formulários clínicos;
- serviços tipados para comunicação com a API;
- Signals e recursos nativos antes de biblioteca global de estado;
- carregamento por rota quando trouxer benefício real.

### Regras de implementação

- nenhuma regra de negócio ou autorização definitiva no navegador;
- nenhuma persistência clínica em `localStorage`;
- evitar `any`; exceções precisam de justificativa;
- modelos da API são tipados;
- subscriptions e efeitos não podem gerar vazamento de memória;
- mensagens de erro são úteis e não exibem detalhes internos;
- formulários tratam carregamento, envio, falha, sucesso e reenvio;
- ações críticas pedem confirmação apropriada;
- tela não inventa dados ausentes;
- conteúdo enviado pelo usuário é exibido de forma segura;
- sem `console.log` esquecido em produção;
- sem estilos globais casuais para corrigir um único componente.

### Testes

- testes de componentes para interações importantes;
- testes de serviços e conversão de dados;
- testes de rotas protegidas;
- testes dos estados de carregamento, vazio, erro e permissão negada;
- fluxo integrado dos casos críticos antes da entrada em produção.

## Design visual

O sistema deve parecer um produto médico construído intencionalmente, não uma coleção de
componentes genéricos.

### Identidade

- paleta principal branca, preta e azul-escuro;
- verde somente para disponibilidade ou sucesso;
- amarelo para ocupado ou atenção;
- vermelho para manutenção, erro ou risco;
- tipografia legível e hierarquia consistente;
- espaçamento baseado em escala única;
- bordas, raios e sombras discretos e padronizados;
- ícones de uma mesma família visual;
- logotipo tratado como ativo da marca, sem fundos ou recortes improvisados.

### O que evitar

- gradientes decorativos sem função;
- excesso de cartões arredondados e sombras;
- emojis como ícones de interface;
- textos promocionais ou genéricos;
- métricas fictícias para preencher tela;
- cores aleatórias por componente;
- animações que atrasem o trabalho;
- telas visualmente bonitas que escondam estado, autoria ou risco clínico.

### Experiência

- ações principais são evidentes;
- ações destrutivas ou irreversíveis são diferenciadas;
- estado do leito é compreendido sem depender somente da cor;
- navegação por teclado e foco visível;
- contraste adequado;
- rótulos associados aos campos;
- interface responsiva para os tamanhos realmente utilizados;
- impressão possui revisão separada da tela;
- conteúdo clínico prioriza leitura e precisão.

Textos de interface seguem a identidade aprovada em caixa alta. Dados fornecidos pelo
usuário e textos clínicos preservam o conteúdo original quando a transformação puder
alterar seu significado.

## API e contratos

- endpoints versionados em `/api/v1`;
- JSON com nomes consistentes;
- paginação para listas potencialmente grandes;
- filtros permitidos e limitados pelo backend;
- identificadores não sequenciais quando expostos trouxerem risco;
- idempotência onde repetição puder duplicar operação crítica;
- contrato alterado de forma compatível ou versionada;
- documentação da API atualizada junto com a implementação.

## Banco de dados

- nomes consistentes e explícitos;
- chaves, restrições e índices fazem parte do desenho;
- integridade não depende apenas do Java;
- usuário da aplicação possui somente as permissões necessárias;
- exclusões físicas são excepcionais e documentadas;
- migrações são pequenas, revisáveis e reversíveis quando tecnicamente possível;
- alterações destrutivas exigem backup e plano de recuperação;
- consultas importantes são avaliadas com dados de volume representativo.

## Segurança no desenvolvimento

- revisão de autorização em todo endpoint;
- dependências acompanhadas e atualizadas;
- análise automática de vulnerabilidades e código;
- segredos nunca entram no repositório;
- dados reais não são usados em desenvolvimento;
- mensagens, logs e arquivos de teste não contêm dados sensíveis;
- bibliotecas novas precisam de justificativa, manutenção ativa e licença compatível;
- falhas de segurança têm prioridade sobre melhorias visuais.

## Fluxo de trabalho

Cada mudança segue:

```text
requisito -> critérios de aceite -> implementação -> testes -> revisão -> documentação -> implantação
```

- trabalho relevante ocorre em branch própria;
- commits descrevem uma mudança coerente;
- integração contínua executa formatação, análise, testes e build;
- mudança arquitetural é documentada antes da implementação;
- scripts e configurações da VPS permanecem versionados;
- nenhuma correção é feita somente no servidor e esquecida no GitHub.

## Definição de pronto

Uma alteração só está pronta quando:

- atende aos critérios de aceite;
- respeita a arquitetura;
- possui autorização e isolamento corretos;
- não introduz duplicação ou atalho oculto;
- possui testes adequados ao risco;
- frontend trata todos os estados relevantes;
- build e verificações automáticas passam;
- documentação e registro da alteração foram atualizados;
- implantação e recuperação são compreendidas;
- não há segredos ou dados reais no diff.
