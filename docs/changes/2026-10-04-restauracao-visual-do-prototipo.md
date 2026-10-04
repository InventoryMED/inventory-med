# Restauração visual do protótipo aprovado

**Data:** 2026-10-04
**Responsável:** Codex
**Status:** concluída

## Resumo

O fluxo médico voltou a utilizar o mesmo layout de quartos, leitos, prescrição e
evolução que serviu de referência no GitHub Pages. O painel médico genérico criado na
etapa de integração foi removido. Os dados reais continuam sendo obtidos da API e são
adaptados para os componentes visuais preservados do protótipo.

As páginas administrativas de ordenação da estrutura hospitalar e de configuração
dinâmica dos formulários clínicos também foram removidas do frontend. O painel básico
de hospitais, usuários e acessos foi mantido.

## Motivo

O frontend integrado havia se afastado do desenho aprovado na apresentação. A
alteração restabelece a referência visual sem desfazer autenticação, isolamento por
hospital ou persistência clínica no backend.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `frontend/src/app/app.html` | modificado | remove o painel médico alternativo e preserva o layout original |
| `frontend/src/app/app.ts` | modificado | conecta o layout original ao workspace e às operações reais da API |
| `frontend/src/app/demo-store.ts` | modificado | recebe a projeção temporária dos dados reais para o layout legado |
| `frontend/src/app/models.ts` | modificado | identifica a internação exibida no leito |
| `frontend/src/app/features/administration/administration.*` | modificado | remove os módulos de estrutura e formulários do menu |
| `frontend/src/app/features/administration/structure-administration.component.*` | removido | exclui a tela de ordenação de unidades, quartos e leitos |
| `frontend/src/app/features/administration/form-template-administration.component.*` | removido | exclui a tela de configuração de campos clínicos |
| `frontend/src/app/features/medical/medical-workspace.component.*` | removido | exclui o frontend alternativo que divergia do protótipo |

## Banco de dados

- bancos afetados: nenhum;
- migrações: nenhuma;
- objetos afetados: nenhum.

As estruturas já criadas no backend foram preservadas, mas os configuradores removidos
não são mais apresentados no frontend.

## Comandos executados na VPS

Nenhum. A alteração foi aplicada somente no ambiente local.

## Serviços afetados

- frontend Angular local;
- API e SQL Server não foram reiniciados nem modificados.

## Configurações

Nenhuma.

## Validação

- formatação do frontend executada com Prettier;
- 13 testes do frontend concluídos sem falhas;
- build Angular concluído com sucesso;
- conferência visual da tela de acesso realizada em `http://127.0.0.1:4200/`;
- busca por referências aos componentes removidos concluída.

## Recuperação

Reverter o commit desta alteração restaura o painel médico alternativo e os dois
configuradores administrativos removidos.

## Pendências

- validar com o responsável do projeto o fluxo médico completo usando uma conta real;
- publicar na VPS somente após a aprovação visual local.
