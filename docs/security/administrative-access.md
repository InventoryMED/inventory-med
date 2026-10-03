# Modelo de administração e segurança

## Objetivo

Permitir a configuração completa do Inventory MED sem transformar uma única conta em
um acesso irrestrito e impossível de controlar.

## Perfis administrativos

### ADMIN_SISTEMA

Responsável pela operação geral do Inventory MED.

Pode:

- criar e desativar hospitais;
- criar o primeiro administrador hospitalar;
- administrar os próprios operadores de sistema;
- gerenciar configurações globais técnicas;
- acompanhar disponibilidade, backups e alertas da plataforma;
- consultar auditorias administrativas autorizadas;
- prestar suporte por fluxo controlado.

Não recebe automaticamente acesso aos dados clínicos dos hospitais. Qualquer acesso
excepcional deve utilizar modo de suporte, com hospital selecionado, justificativa,
prazo, autorização aplicável e auditoria destacada.

### ADMIN_HOSPITAL

Responsável somente por uma unidade.

Pode:

- criar, editar e desativar usuários da unidade;
- atribuir papéis permitidos dentro daquela unidade;
- criar, renomear, ordenar e desativar quartos e leitos;
- configurar catálogos e modelos da unidade;
- designar outros administradores hospitalares quando possuir permissão específica;
- consultar auditorias administrativas da própria unidade.

Não pode:

- acessar outro hospital;
- criar ou desativar hospitais;
- conceder `ADMIN_SISTEMA`;
- alterar configurações globais de segurança;
- alterar ou apagar o conteúdo de uma prescrição ou evolução emitida;
- apagar histórico utilizado por internações;
- desativar a auditoria.

### RESPONSAVEL_CLINICO

Responsável por revisar e publicar configurações que interferem no trabalho médico,
como modelos de prescrição e evolução. Pode ser uma pessoa diferente do administrador
hospitalar, permitindo dupla verificação das mudanças de maior risco.

## Matriz inicial de responsabilidades

| Ação | Sistema | Hospital | Clínico |
| --- | --- | --- | --- |
| Criar hospital | sim | não | não |
| Criar administrador hospitalar | sim | sim, se autorizado na própria unidade | não |
| Criar usuário comum | sim, por suporte controlado | sim, na própria unidade | não |
| Alterar configuração global | sim | não | não |
| Criar quarto e leito | não | sim, na própria unidade | não |
| Criar rascunho de modelo | não | sim, na própria unidade | sim |
| Publicar modelo clínico | não | solicita | aprova |
| Alterar documento clínico emitido | não | não | não |
| Retificar documento próprio | não | não | conforme regra clínica e autoria |

Essa matriz será detalhada em permissões atômicas no backend. O nome do papel facilita
o gerenciamento, mas a autorização real será feita pela permissão exigida pelo caso de
uso.

## Escopo e troca de contexto

O `ADMIN_SISTEMA` enxerga um painel global com cadastro de hospitais, situação dos
serviços, segurança e auditorias permitidas. Para executar suporte operacional dentro de
um hospital, ele deve selecionar explicitamente a unidade e ativar o modo de suporte.

Enquanto esse modo estiver ativo:

- o hospital selecionado aparece permanentemente na tela;
- a sessão recebe escopo somente daquela unidade;
- o motivo e o prazo são registrados;
- operações são destacadas na auditoria;
- a troca para outra unidade encerra o contexto anterior;
- nunca há consulta clínica combinando hospitais.

## Exclusão, edição e histórico

Nem todo botão chamado “excluir” executará exclusão física.

- hospital com histórico: desativar;
- usuário: desativar e encerrar sessões;
- quarto ou leito utilizado: desativar;
- opção de catálogo utilizada: desativar;
- modelo em rascunho não utilizado: pode ser excluído;
- modelo publicado: aposentar e criar nova versão;
- prescrição ou evolução emitida: retificar, versionar ou cancelar com justificativa.

Isso impede que uma alteração administrativa modifique o significado de documentos
antigos.

## Autenticação administrativa

Requisitos mínimos:

- MFA obrigatório;
- passkey/WebAuthn como opção preferencial;
- TOTP como alternativa inicial;
- códigos de recuperação individuais, protegidos e de uso único;
- nenhuma conta compartilhada;
- senha longa e verificada contra senhas comprometidas;
- limitação de tentativas por conta e origem;
- proteção contra enumeração de usuários;
- sessão curta e cookie `HttpOnly`, `Secure` e `SameSite`;
- reautenticação para ações críticas;
- recuperação de conta tratada como ação de alto risco;
- notificação de mudanças em senha, fatores e permissões.

O administrador que também trabalha como médico deve usar elevação administrativa
explícita ou conta administrativa separada para reduzir o risco de executar uma ação
crítica durante o uso clínico diário.

## Autorização

O backend aplicará:

- negação por padrão;
- verificação em toda requisição;
- escopo obrigatório de hospital;
- permissões por ação, não apenas por tela;
- bloqueio de promoção indevida de privilégios;
- validação de propriedade e estado do objeto;
- proteção contra troca de identificadores na URL ou no JSON;
- testes automatizados da matriz de permissões.

Ocultar um botão no Angular não protege uma operação. Mesmo que alguém monte uma
requisição manualmente, o backend precisa recusá-la.

## Auditoria administrativa

Serão registrados:

- login, logout, falhas e bloqueios;
- criação, alteração, desativação e recuperação de usuários;
- concessão e retirada de permissões;
- alteração de MFA;
- criação e desativação de hospitais, quartos e leitos;
- mudanças e publicação de modelos;
- exportações e consultas administrativas sensíveis;
- tentativas de acessar hospital ou recurso sem autorização;
- uso de conta de emergência.

Os eventos terão identidade do autor, data e hora confiáveis, hospital, ação, objeto,
resultado, origem, correlação e valores anteriores/novos quando apropriado. A aplicação
não terá permissão para apagar a trilha de auditoria e uma cópia será protegida fora do
banco operacional.

Senhas, cookies, tokens, chaves e conteúdo clínico desnecessário não serão incluídos nos
logs de segurança.

## Proteções adicionais

- HTTPS obrigatório;
- cabeçalhos de segurança;
- CSRF para operações autenticadas por cookie;
- limite de tamanho e frequência das requisições;
- validação e normalização de toda entrada;
- consultas parametrizadas;
- dependências verificadas e atualizadas;
- backups criptografados e testes de restauração;
- alertas para mudanças administrativas críticas;
- procedimento de resposta a incidente;
- revisão periódica de usuários e permissões.

## Referências de segurança

- [OWASP Authorization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP Multifactor Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Multifactor_Authentication_Cheat_Sheet.html)
- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [NIST SP 800-63B-4](https://pages.nist.gov/800-63-4/sp800-63b.html)
- [Guia de segurança da ANPD](https://www.gov.br/anpd/pt-br/centrais-de-conteudo/materiais-educativos-e-publicacoes/anonimizado___guia_orientat-_seg_da_inf_p_atpp.pdf)
