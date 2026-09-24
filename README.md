# Inventory MED — protótipo

Protótipo navegável de um sistema hospitalar para visualização de quartos e leitos, cadastro de paciente em leito vazio e preenchimento de prescrição médica.

> Este projeto usa somente dados fictícios e não possui validade clínica. Não há back-end, autenticação real ou banco de dados nesta versão.

## Executar localmente

Requisitos: Node.js 24 e npm.

```bash
cd frontend
npm install
npm start
```

Abra `http://localhost:4200`.

O formulário de demonstração já vem preenchido. Qualquer senha não vazia permite continuar.

## Fluxo demonstrável

1. Entrada do profissional.
2. Seleção do hospital.
3. Listagem expansível de quartos.
4. Visualização dos quartos e respectivos leitos, inicialmente sem pacientes.
5. Confirmação para iniciar uma prescrição ao selecionar um leito vazio.
6. Cadastro do paciente na tela atual, com peso e informações clínicas opcionais.
7. Abertura da prescrição em uma nova guia, mantendo os dados do paciente editáveis.
8. Escolha entre prescrição do zero ou modelos demonstrativos de Admissão, PAC, CAD, TVP, TEP e Box de Emergência.
9. Prescrição digital com dieta, dados vitais, orientações de DXT, hidratação e itens medicamentosos.
10. Hidratação com modelos demonstrativos, via, frequência e aprazamento ACM, SN ou FIXO.
11. Seções editáveis de analgesia, sintomáticos, profilaxia, ATB, uso contínuo e demais medicamentos.
12. Modelos demonstrativos por seção, com inclusão e remoção de linhas conforme necessário.
13. Observações e comunicação de anormalidades em linhas independentes, com criação da prescrição ao final do formulário.

Os dados alterados durante a demonstração ficam salvos no `localStorage` do navegador. Use o botão **Restaurar demonstração** para voltar ao estado inicial.

## Publicação

O workflow em `.github/workflows/deploy-pages.yml` testa, compila e publica automaticamente o front-end no GitHub Pages a cada envio para a branch `main`.

No repositório GitHub, configure **Settings → Pages → Source** como **GitHub Actions**.

## Evolução prevista

A camada local de dados está isolada em `frontend/src/app/demo-store.ts`. Ela poderá ser substituída por uma API Java/Spring Boot conectada ao SQL Server sem reconstruir as telas principais.
