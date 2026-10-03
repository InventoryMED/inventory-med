# Regras de desenvolvimento do Inventory MED

Antes de alterar código, leia integralmente `docs/architecture.md`. Esse documento é a
fonte oficial da arquitetura do projeto.

Leia também `docs/development/quality-standards.md` antes de implementar ou revisar
código.

## Regras obrigatórias

- Preserve o monólito modular: uma API Java/Spring Boot e um frontend Angular.
- A implantação inicial é direta no Ubuntu 22.04, gerenciada por systemd e Nginx.
- Não introduza Docker na VPS, Kubernetes, microserviços, filas, Redis ou novos
  componentes de infraestrutura sem decisão arquitetural explícita.
- O SQL Server nunca deve ser publicado na internet.
- O navegador acessa somente o Nginx; `/api` é encaminhado para a API local.
- Dados operacionais de cada hospital ficam em banco próprio.
- Nunca crie paciente global ou consulta clínica entre hospitais.
- Hospital e permissões são derivados da sessão autenticada, nunca aceitos livremente
  do navegador.
- Use Flyway para mudanças de esquema. Não dependa de alterações SQL manuais.
- Não use `sa` na aplicação e não coloque segredos no Git.
- Controllers não contêm regra de negócio e entidades JPA não são respostas da API.
- O frontend não usa `localStorage` como banco de dados.
- Alterações clínicas relevantes devem ser auditáveis.
- Não registre senhas ou dados clínicos sensíveis em logs.
- Preserve arquivos do protótipo até que a substituição correspondente esteja validada.
- Não aceite componentes gigantes, métodos extensos, duplicação, `any` desnecessário,
  SQL concatenado, regra de negócio no Angular ou atalhos sem teste e documentação.
- Preserve o sistema visual definido; não introduza aparência genérica, emojis como
  ícones, gradientes decorativos ou componentes inconsistentes.
- Para cada alteração lógica, crie ou atualize um registro em `docs/changes/` seguindo
  `docs/changes/TEMPLATE.md`.
- Registre comandos executados na VPS, serviços reiniciados, validação e recuperação.
- Explique termos e comandos em linguagem direta para que o responsável pelo projeto
  consiga reproduzir e compreender a mudança.

## Antes de concluir uma alteração

- execute os testes afetados;
- verifique o isolamento hospitalar;
- atualize a documentação se o comportamento público mudar;
- confirme que nenhum segredo foi incluído no diff;
- compare a solução com `docs/architecture.md`.
- confirme que o registro da alteração está completo.
- execute formatação, análise estática, testes e build das áreas afetadas.
