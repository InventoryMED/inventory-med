# Cuidados de enfermagem estruturados na prescrição

**Data:** 2026-10-06  
**Responsável:** Codex e responsável pelo Inventory MED  
**Status:** concluída

## Resumo

A prescrição passou a oferecer um item estruturado de cuidados de enfermagem. O
prescritor pode selecionar posicionamento, higiene, proteção da pele, curativos,
drenos, aspiração, manutenção de dispositivos e balanço hídrico. A API valida as
seleções e produz o resumo e o detalhamento que serão persistidos com o documento.

## Motivo

Padronizar cuidados recorrentes, facilitar a checagem assistencial e destacar pontos
que precisam de conferência de qualidade, materiais e contrato sem transferir regra de
negócio clínica para o navegador.

## Decisões de segurança e domínio

- o Angular apenas coleta escolhas do catálogo entregue pela API;
- a API rejeita opções, campos e combinações que não pertencem ao catálogo oficial;
- a cobertura de ferida complexa exige frequência de troca;
- o conteúdo persistido inclui a entrada normalizada e a saída calculada pelo backend;
- alertas de faturamento e qualidade são pontos de conferência e não representam
  autorização automática, garantia de cobrança ou certificação;
- o recurso usa o hospital da sessão e o banco hospitalar já selecionado;
- somente os perfis médicos autorizados a criar prescrições acessam catálogo e prévia;
- documentos clínicos anteriores continuam associados à versão histórica do modelo.

## Arquivos alterados

| Arquivo | Operação | Explicação |
| --- | --- | --- |
| `backend/src/main/java/br/com/inventorymed/clinical/NursingCarePrescriptionRequest.java` | criado | Contrato tipado das seleções de cuidados. |
| `backend/src/main/java/br/com/inventorymed/clinical/NursingCarePrescriptionCatalog.java` | criado | Catálogo oficial entregue ao frontend. |
| `backend/src/main/java/br/com/inventorymed/clinical/NursingCarePrescriptionResponse.java` | criado | Resumo, detalhamento e itens de conferência. |
| `backend/src/main/java/br/com/inventorymed/clinical/NursingCarePrescriptionService.java` | criado | Validação, normalização e geração determinística do item. |
| `backend/src/main/java/br/com/inventorymed/clinical/ClinicalDocumentService.java` | modificado | Normaliza o novo tipo antes de persistir o documento. |
| `backend/src/main/java/br/com/inventorymed/clinical/MedicalWorkspaceController.java` | modificado | Expõe catálogo e prévia com escopo autenticado. |
| `backend/src/main/java/br/com/inventorymed/formtemplates/FormFieldType.java` | modificado | Adiciona `NURSING_CARE_PLAN`. |
| `backend/src/main/resources/db/migration/tenant/V6__structured_nursing_care_prescription.sql` | criado | Publica nova versão do modelo de prescrição em cada banco hospitalar. |
| `backend/src/test/java/br/com/inventorymed/clinical/NursingCarePrescriptionServiceTest.java` | criado | Testa geração, validações e rejeição de campos desconhecidos. |
| `frontend/src/app/features/medical/nursing-care-prescription.component.*` | criado | Interface estruturada, responsiva e integrada ao sistema visual. |
| `frontend/src/app/features/medical/medical.models.ts` | modificado | Tipos do catálogo, rascunho e resposta. |
| `frontend/src/app/features/medical/medical.service.ts` | modificado | Chamadas ao catálogo e à prévia. |
| `frontend/src/app/features/medical/medical.service.spec.ts` | modificado | Testa os novos contratos HTTP. |
| `frontend/src/app/app.ts` | modificado | Valida, persiste, imprime e limpa o item junto à prescrição. |
| `frontend/src/app/app.html` | modificado | Posiciona o item após a dieta e inclui seu bloco de impressão. |

## Banco de dados

- bancos afetados: todos os bancos hospitalares existentes e futuros;
- migração: `V6__structured_nursing_care_prescription.sql`;
- objetos afetados: restrição `ck_form_field_type`, nova versão publicada do modelo
  `PRESCRIÇÃO MÉDICA PADRÃO`, seção `CUIDADOS_ENFERMAGEM` e campo `CUIDADOS`;
- a versão anteriormente publicada é aposentada, não apagada;
- documentos existentes e respectivos retratos do modelo não são alterados.

## Comandos executados na VPS

Versão implantada: `18392cb9278b`.

```bash
cd /home/gabriel/inventory-med-upload
tar -xzf inventory-med-18392cb9278b.tar.gz
cd 18392cb9278b
sudo bash infra/scripts/install-release.sh "$PWD" 18392cb9278b
```

O instalador publicou o JAR e o frontend, reiniciou `inventory-med-api`, recarregou o
Nginx e manteve a versão somente após a verificação de saúde. O Flyway executará `V6`
em cada banco hospitalar quando a aplicação abrir sua primeira conexão após a versão.

## Serviços afetados

- API Spring Boot;
- frontend Angular;
- migrações Flyway dos bancos hospitalares.

## Configurações

Nenhuma variável de ambiente nova.

## Validação

- `npm test -- --watch=false`: 17 testes aprovados;
- `npm run build`: concluído; permanecem apenas os avisos de orçamento já conhecidos;
- `mvn clean test` em Java 21 e SQL Server 2022 descartável: 20 testes aprovados;
- Flyway aplicou as seis migrações em bancos hospitalares descartáveis;
- `git diff --check`: sem erro de espaço em branco;
- verificação de segredos no diff: nenhum segredo adicionado;
- frontend público: HTTP `200`, com o mesmo artefato `main-QTSWAJLW.js` gerado localmente;
- saúde pública e interna da API: `UP`;
- endpoints de dieta e cuidados: protegidos com HTTP `401` sem sessão;
- `inventory-med-api` e `nginx`: ativos após a implantação;
- a tentativa adicional de executar `nginx -t` sem `sudo` confirmou a sintaxe, mas não
  pôde ler o PID protegido; a validação privilegiada já faz parte do instalador.

## Recuperação

Se ainda não houver documento criado com a nova versão, uma migração posterior pode
aposentar a versão 3 da prescrição e republicar a anterior. Se já existirem documentos,
eles devem ser preservados; a recuperação deve ocorrer por nova versão do modelo, nunca
por remoção ou alteração retroativa do histórico.

## Pendências

- validar o fluxo autenticado e o PDF com um usuário médico antes do uso assistencial;
- validar o catálogo com enfermagem, controle de infecção, auditoria e faturamento de
  cada hospital antes do uso assistencial definitivo.
