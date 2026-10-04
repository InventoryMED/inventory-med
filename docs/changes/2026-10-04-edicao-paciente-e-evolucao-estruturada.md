# Edição de paciente e evolução clínica estruturada

**Data:** 2026-10-04  
**Responsável:** equipe Inventory MED  
**Status:** concluída e publicada na VPS em homologação pública

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

Versão publicada: `5befd995ad1a`.

O pacote `inventory-med-5befd995ad1a.tar.gz` foi gerado pelo fluxo oficial, enviado
para `/home/gabriel/inventory-med-upload/` e conferido antes da instalação. O SHA-256
local e remoto foi:

```text
8e87b9a98977eb692c7e46d49572f4cc98c5b65f831ab07c681bf58f86295411
```

Comandos executados no computador de desenvolvimento:

```powershell
git push -u origin codex/medical-admin-configuration
.\infra\scripts\build-release.ps1
.\infra\scripts\upload-release.ps1 `
  -ArchivePath '.\artifacts\releases\inventory-med-5befd995ad1a.tar.gz'
```

Comandos executados na VPS, sem valores secretos:

```bash
cd /home/gabriel/inventory-med-upload
tar -xzf inventory-med-5befd995ad1a.tar.gz
cd 5befd995ad1a
sudo bash infra/scripts/install-release.sh "$PWD" 5befd995ad1a
```

O instalador atualizou os links do JAR e do frontend, reiniciou a API, validou o Nginx
e confirmou a saúde antes de concluir. A versão anterior permaneceu disponível para
recuperação.

## Serviços afetados

- API local reconstruída e reiniciada no Docker Compose;
- `inventory-med-api` reiniciado na VPS;
- `nginx` recarregado na VPS;
- `mssql-server` não foi reiniciado nem publicado na internet;
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
- pacote remoto com SHA-256 idêntico ao arquivo local;
- instalador informou `Versão 5befd995ad1a instalada e saudável`;
- `inventory-med-api`, `nginx` e `mssql-server` ativos na VPS;
- frontend público em `http://179.236.237.36/` respondeu HTTP 200 e foi inspecionado
  visualmente com o texto institucional atualizado;
- saúde pública respondeu `UP`;
- `/api/v1/auth/me` respondeu HTTP 401 sem uma sessão autenticada;
- cabeçalhos CSP, `X-Frame-Options: DENY` e `X-Content-Type-Options: nosniff`
  presentes;
- portas públicas 8080 e 1433 permaneceram fechadas;
- listeners internos confirmados em `127.0.0.1:8080` e `127.0.0.1:1433`;
- a migração hospitalar V4 será aplicada pelo Flyway ao banco de cada unidade quando
  a API abrir o contexto clínico daquela unidade.

## Recuperação

Antes da produção, o código pode ser revertido pelo Git. Depois de V4 aplicada em um
banco hospitalar, não editar nem apagar o histórico Flyway e não reativar manualmente a
versão anterior. Uma correção deverá ser feita por nova migração que publique outra
versão do modelo, preservando os documentos já finalizados.

## Pendências

- definir o comportamento clínico e administrativo do botão AIH;
- validar com uma sessão médica a abertura da unidade e a aplicação da V4 no banco
  hospitalar da VPS;
- realizar a aprovação visual do formulário e do PDF pelo responsável;
- tratar em etapa própria os avisos de orçamento de tamanho do frontend;
- configurar domínio, HTTPS, backup externo e teste de restauração antes de usar dados
  reais; enquanto o acesso permanecer por HTTP no IP, o ambiente continua classificado
  como homologação pública pela arquitetura oficial.
