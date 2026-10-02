# Arquitetura do Inventory MED

## Decisões iniciais

- Front-end: Angular.
- API: Java 21 e Spring Boot.
- Banco: SQL Server Express.
- Migrações: Flyway.
- Autenticação: Spring Security com access token JWT curto.
- Implantação inicial: contêineres Docker atrás de proxy HTTPS.

## Isolamento entre hospitais

O Inventory MED é multi-hospital, mas não compartilha dados clínicos entre unidades.

1. O usuário autentica com sua conta.
2. A API lista somente os hospitais aos quais ele possui vínculo ativo.
3. Se existir um único vínculo, o token já nasce limitado àquele hospital.
4. Se houver mais de um vínculo, o usuário escolhe a unidade e recebe um novo token com `hospital_id` e função.
5. Endpoints clínicos exigem token com hospital selecionado.
6. Todas as consultas clínicas usam o `hospital_id` do token, nunca um hospital informado livremente pelo navegador.
7. Pacientes são cadastrados separadamente por hospital; não existe paciente global compartilhado.
8. Chaves estrangeiras compostas no SQL Server bloqueiam vínculos entre entidades de hospitais diferentes.

O vínculo do profissional com mais de um hospital dá acesso independente a cada unidade; ele não cria relação entre pacientes, quartos, leitos, internações, prescrições ou evoluções.

## Modelo principal

- `hospital`: unidade hospitalar.
- `app_user`: conta do profissional.
- `hospital_membership`: vínculo e função do profissional em cada hospital.
- `room` e `bed`: estrutura física exclusiva do hospital.
- `patient`: paciente pertencente exclusivamente à unidade que o cadastrou.
- `admission`: internação do paciente em um leito do mesmo hospital.
- `prescription` e `prescription_item`: prescrição versionada da internação.
- `evolution`: evolução médica da internação.
- `audit_event`: trilha de operações sensíveis.
- `refresh_token`: base para renovação segura de sessão, a ser ativada na etapa seguinte.

## Segurança prevista

- Senhas com BCrypt.
- Tokens de acesso curtos e hospitalares.
- HTTPS obrigatório em produção.
- SQL Server sem porta pública.
- Segredos somente em variáveis de ambiente.
- Auditoria de login e alterações clínicas.
- Backup automatizado e teste periódico de restauração.
- Menor privilégio para usuário do banco; a aplicação não usará `sa` em produção.
