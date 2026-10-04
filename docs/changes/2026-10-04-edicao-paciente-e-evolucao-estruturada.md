# Edição de paciente e evolução clínica estruturada

**Data:** 2026-10-04  
**Responsável:** equipe Inventory MED  
**Status:** concluída no ambiente local; aguardando aprovação visual para produção

## Resumo

O card de leito ocupado passou a oferecer edição do cadastro do paciente e um botão
AIH reservado para a funcionalidade futura. A evolução médica foi reorganizada com
seções estruturadas de acompanhamento, funções fisiológicas, neurologia, sedação,
respiração e estado hemodinâmico. A evolução preenchida agora pode ser finalizada na
API usando uma versão publicada e imutável do modelo hospitalar.

## Motivo

Adequar o fluxo médico ao levantamento clínico aprovado, permitir correções auditáveis
do paciente e manter no backend a autoridade sobre campos, opções, autorização,
isolamento hospitalar e finalização do documento.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `frontend/src/app/app.html` | modificado | atualiza o login, card do leito, modal de edição, evolução e impressão |
| `frontend/src/app/app.ts` | modificado | controla listas clínicas, vazões, edição de paciente e criação da evolução |
| `frontend/src/app/app.scss` | modificado | estiliza os novos blocos sem substituir a identidade visual aprovada |
| `frontend/src/app/app.spec.ts` | modificado | testa edição do paciente e conversão do formulário para o contrato publicado |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | adiciona chamada de atualização do paciente |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalRequests.java` | modificado | define e valida a requisição de alteração do paciente |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | expõe a atualização somente para perfis médicos autorizados |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceService.java` | modificado | atualiza o paciente no banco da unidade e grava auditoria sem dados clínicos no evento |
| `backend/src/main/resources/db/migration/tenant/V4__structured_evolution_assessment.sql` | criado | publica a versão 2 do modelo de evolução e aposenta a versão anterior |
| `backend/src/test/java/br/com/inventorymed/CoreSecurityIntegrationTests.java` | modificado | valida edição auditada e finalização da evolução estruturada |

## Regras funcionais entregues

- botão `AIH` visível e inativo até a definição do fluxo;
- ícone de edição junto ao paciente no leito ocupado;
- correção de nome, nascimento, sexo, peso, diagnóstico, comorbidades e alergias pelo
  leito ou pelo cabeçalho clínico, sempre persistida na API;
- evento `PATIENT_REGISTRATION_UPDATED` no banco hospitalar;
- higiene pessoal em condição atual do paciente;
- consciência, orientação e interação transferidas para a seção Neurológico;
- medicamentos sedativos com múltipla escolha e vazão individual em `ML/H`;
- status e dinâmica da sedação;
- seção Respiratório com abas para padrão e suporte ventilatório;
- seção hemodinâmica com estabilidade, perfil pressórico, alvo de PAM e DVA;
- quantidade em `ML/24H` exibida quando SVD é selecionada;
- remoção de queixa principal e intercorrências do plantão;
- campo Evolução imediatamente antes do Exame físico;
- botão `Criar evolução`, além da impressão;
- texto institucional do login atualizado.

## Banco de dados

- banco central: nenhuma tabela alterada;
- bancos hospitalares: migração Flyway V4 aplicada individualmente;
- a versão 1 publicada de `EVOLUÇÃO MÉDICA PADRÃO` passa para `RETIRED`;
- a versão 2 é publicada com as novas seções, campos e opções;
- documentos existentes continuam apontando para a versão e fotografia do modelo com
  que foram criados;
- a edição do paciente sempre usa o hospital da sessão, nunca um hospital informado
  pelo navegador.

## VPS

Nenhum comando foi executado na VPS e nenhuma versão foi publicada. A alteração está
somente no computador de desenvolvimento.

## Serviços afetados

- API local reconstruída e reiniciada no Docker Compose;
- SQL Server local mantido em `127.0.0.1:14330`;
- frontend local mantido em `http://127.0.0.1:4200`.

## Configurações

Nenhuma variável de ambiente ou segredo foi criado ou alterado.

## Validação

- 11 testes integrados Java aprovados em Java 21 com SQL Server 2022 descartável;
- Flyway aplicou V1 a V4 em bancos hospitalares descartáveis;
- o teste clínico finalizou uma evolução V4 com valores estruturados;
- a API rejeitou uma infusão selecionada sem a respectiva vazão;
- o perfil de recepção foi impedido de alterar o cadastro clínico do paciente;
- 15 testes Angular aprovados;
- build de produção Angular concluído;
- o build mantém apenas os avisos já conhecidos de orçamento do pacote inicial e do
  arquivo SCSS, sem erro de compilação;
- API local reconstruída e iniciada com sucesso.

## Recuperação

Antes da produção, o código pode ser revertido pelo Git. Depois de V4 aplicada em um
banco hospitalar, não editar nem apagar o histórico Flyway e não reativar manualmente a
versão anterior. Uma correção deverá ser feita por nova migração que publique outra
versão do modelo, preservando os documentos já finalizados.

## Pendências

- definir o comportamento clínico e administrativo do botão AIH;
- realizar a aprovação visual do formulário e do PDF pelo responsável;
- implantar somente após a aprovação explícita;
- tratar em etapa própria os avisos de orçamento de tamanho do frontend.
