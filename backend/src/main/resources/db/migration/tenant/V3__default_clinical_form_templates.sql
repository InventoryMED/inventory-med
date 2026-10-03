SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

DECLARE @system_actor UNIQUEIDENTIFIER = '00000000-0000-0000-0000-000000000000';
DECLARE @prescription_template UNIQUEIDENTIFIER = NEWID();
DECLARE @prescription_version UNIQUEIDENTIFIER = NEWID();
DECLARE @prescription_general UNIQUEIDENTIFIER = NEWID();
DECLARE @prescription_medications UNIQUEIDENTIFIER = NEWID();
DECLARE @prescription_notes UNIQUEIDENTIFIER = NEWID();

INSERT INTO dbo.form_template (id, kind, name, active, created_by)
VALUES (@prescription_template, 'PRESCRIPTION', N'PRESCRIÇÃO MÉDICA PADRÃO', 1, @system_actor);

INSERT INTO dbo.form_template_version
    (id, template_id, version_number, status, created_by, published_by, published_at)
VALUES
    (@prescription_version, @prescription_template, 1, 'PUBLISHED', @system_actor, @system_actor, SYSUTCDATETIME());

INSERT INTO dbo.form_section (id, version_id, section_key, title, display_order, active)
VALUES
    (@prescription_general, @prescription_version, 'ORIENTACOES', N'ORIENTAÇÕES GERAIS', 10, 1),
    (@prescription_medications, @prescription_version, 'MEDICAMENTOS', N'MEDICAMENTOS', 20, 1),
    (@prescription_notes, @prescription_version, 'OBSERVACOES', N'OBSERVAÇÕES', 30, 1);

INSERT INTO dbo.form_field
    (id, section_id, field_key, label, field_type, required, display_order, active, placeholder, max_length)
VALUES
    (NEWID(), @prescription_general, 'DIETA', N'DIETA', 'SHORT_TEXT', 0, 10, 1, N'DIGITE OU SELECIONE UM MODELO DE DIETA', 500),
    (NEWID(), @prescription_general, 'SINAIS_VITAIS', N'SINAIS VITAIS', 'MEDICATION_LINE', 0, 20, 1, NULL, NULL),
    (NEWID(), @prescription_general, 'DXT', N'DXT', 'MEDICATION_LINE', 0, 30, 1, NULL, NULL),
    (NEWID(), @prescription_medications, 'HIDRATACAO', N'HIDRATAÇÃO', 'MEDICATION_LINE', 0, 10, 1, NULL, NULL),
    (NEWID(), @prescription_medications, 'ANALGESIA', N'ANALGESIA', 'MEDICATION_LINE', 0, 20, 1, NULL, NULL),
    (NEWID(), @prescription_medications, 'SINTOMATICOS', N'SINTOMÁTICOS', 'MEDICATION_LINE', 0, 30, 1, NULL, NULL),
    (NEWID(), @prescription_medications, 'PROFILAXIA', N'PROFILAXIA', 'MEDICATION_LINE', 0, 40, 1, NULL, NULL),
    (NEWID(), @prescription_medications, 'ATB', N'ANTIBIÓTICOS', 'MEDICATION_LINE', 0, 50, 1, NULL, NULL),
    (NEWID(), @prescription_medications, 'USO_CONTINUO', N'MEDICAÇÕES DE USO CONTÍNUO', 'MEDICATION_LINE', 0, 60, 1, NULL, NULL),
    (NEWID(), @prescription_medications, 'DEMAIS_MEDICAMENTOS', N'DEMAIS MEDICAMENTOS', 'MEDICATION_LINE', 0, 70, 1, NULL, NULL),
    (NEWID(), @prescription_notes, 'OBSERVACOES', N'OBSERVAÇÕES E COMUNICAR ANORMALIDADES', 'LONG_TEXT', 0, 10, 1, NULL, 4000);

DECLARE @evolution_template UNIQUEIDENTIFIER = NEWID();
DECLARE @evolution_version UNIQUEIDENTIFIER = NEWID();
DECLARE @evolution_monitoring UNIQUEIDENTIFIER = NEWID();
DECLARE @evolution_subjective UNIQUEIDENTIFIER = NEWID();
DECLARE @evolution_physical UNIQUEIDENTIFIER = NEWID();
DECLARE @evolution_exams UNIQUEIDENTIFIER = NEWID();
DECLARE @evolution_plan UNIQUEIDENTIFIER = NEWID();

INSERT INTO dbo.form_template (id, kind, name, active, created_by)
VALUES (@evolution_template, 'EVOLUTION', N'EVOLUÇÃO MÉDICA PADRÃO', 1, @system_actor);

INSERT INTO dbo.form_template_version
    (id, template_id, version_number, status, created_by, published_by, published_at)
VALUES
    (@evolution_version, @evolution_template, 1, 'PUBLISHED', @system_actor, @system_actor, SYSUTCDATETIME());

INSERT INTO dbo.form_section (id, version_id, section_key, title, display_order, active)
VALUES
    (@evolution_monitoring, @evolution_version, 'ACOMPANHAMENTO', N'IDENTIFICAÇÃO E ACOMPANHAMENTO', 10, 1),
    (@evolution_subjective, @evolution_version, 'SUBJETIVO', N'SUBJETIVO', 20, 1),
    (@evolution_physical, @evolution_version, 'EXAME_FISICO', N'EXAME FÍSICO', 30, 1),
    (@evolution_exams, @evolution_version, 'EXAMES', N'EXAMES COMPLEMENTARES', 40, 1),
    (@evolution_plan, @evolution_version, 'CONDUTA', N'CONDUTA MÉDICA', 50, 1);

DECLARE @position_field UNIQUEIDENTIFIER = NEWID();
DECLARE @accompaniment_field UNIQUEIDENTIFIER = NEWID();
DECLARE @consciousness_field UNIQUEIDENTIFIER = NEWID();
DECLARE @orientation_field UNIQUEIDENTIFIER = NEWID();
DECLARE @chief_complaint_field UNIQUEIDENTIFIER = NEWID();
DECLARE @shift_events_field UNIQUEIDENTIFIER = NEWID();
DECLARE @food_field UNIQUEIDENTIFIER = NEWID();
DECLARE @urinary_field UNIQUEIDENTIFIER = NEWID();
DECLARE @urine_field UNIQUEIDENTIFIER = NEWID();
DECLARE @intestinal_field UNIQUEIDENTIFIER = NEWID();
DECLARE @stool_field UNIQUEIDENTIFIER = NEWID();
DECLARE @sleep_field UNIQUEIDENTIFIER = NEWID();

INSERT INTO dbo.form_field
    (id, section_id, field_key, label, field_type, required, display_order, active, placeholder, max_length)
VALUES
    (@position_field, @evolution_monitoring, 'POSICAO', N'POSIÇÃO / ATIVIDADE NO LEITO', 'SINGLE_SELECT', 0, 10, 1, NULL, NULL),
    (@accompaniment_field, @evolution_monitoring, 'ACOMPANHAMENTO', N'ACOMPANHAMENTO', 'SINGLE_SELECT', 0, 20, 1, NULL, NULL),
    (@consciousness_field, @evolution_monitoring, 'CONSCIENCIA', N'NÍVEL DE CONSCIÊNCIA', 'SINGLE_SELECT', 0, 30, 1, NULL, NULL),
    (@orientation_field, @evolution_monitoring, 'ORIENTACAO', N'ORIENTAÇÃO TEMPOROESPACIAL', 'SINGLE_SELECT', 0, 40, 1, NULL, NULL),
    (@chief_complaint_field, @evolution_subjective, 'QUEIXA_PRINCIPAL', N'QUEIXA PRINCIPAL ATUAL', 'MULTI_SELECT', 0, 10, 1, NULL, NULL),
    (@shift_events_field, @evolution_subjective, 'INTERCORRENCIAS', N'INTERCORRÊNCIAS NO PLANTÃO', 'MULTI_SELECT', 0, 20, 1, NULL, NULL),
    (@food_field, @evolution_subjective, 'ACEITACAO_ALIMENTAR', N'ACEITAÇÃO ALIMENTAR', 'SINGLE_SELECT', 0, 30, 1, NULL, NULL),
    (@urinary_field, @evolution_subjective, 'ELIMINACAO_URINARIA', N'ELIMINAÇÃO URINÁRIA', 'SINGLE_SELECT', 0, 40, 1, NULL, NULL),
    (@urine_field, @evolution_subjective, 'ASPECTO_URINA', N'ASPECTO DA URINA', 'SINGLE_SELECT', 0, 50, 1, NULL, NULL),
    (@intestinal_field, @evolution_subjective, 'ELIMINACAO_INTESTINAL', N'ELIMINAÇÃO INTESTINAL', 'SINGLE_SELECT', 0, 60, 1, NULL, NULL),
    (@stool_field, @evolution_subjective, 'ASPECTO_FEZES', N'ASPECTO DAS FEZES', 'SINGLE_SELECT', 0, 70, 1, NULL, NULL),
    (@sleep_field, @evolution_subjective, 'PADRAO_SONO', N'PADRÃO DE SONO', 'SINGLE_SELECT', 0, 80, 1, NULL, NULL),
    (NEWID(), @evolution_subjective, 'ADMISSAO', N'ADMISSÃO', 'LONG_TEXT', 0, 90, 1, NULL, 10000),
    (NEWID(), @evolution_subjective, 'EVOLUCAO', N'EVOLUÇÃO', 'LONG_TEXT', 0, 100, 1, NULL, 10000),
    (NEWID(), @evolution_subjective, 'ATB_ATUAL', N'ANTIBIOTICOTERAPIA ATUAL', 'LONG_TEXT', 0, 110, 1, NULL, 2000),
    (NEWID(), @evolution_subjective, 'ATB_PREVIA', N'ANTIBIOTICOTERAPIA PRÉVIA', 'LONG_TEXT', 0, 120, 1, NULL, 2000),
    (NEWID(), @evolution_physical, 'SSVV', N'SINAIS VITAIS', 'SHORT_TEXT', 0, 10, 1, N'SATO₂, FC, FR, PA E TAX', 500),
    (NEWID(), @evolution_physical, 'ECTOSCOPIA', N'ECTOSCOPIA', 'SHORT_TEXT', 0, 20, 1, NULL, 1000),
    (NEWID(), @evolution_physical, 'NEUROLOGICO', N'NEUROLÓGICO', 'LONG_TEXT', 0, 30, 1, NULL, 3000),
    (NEWID(), @evolution_physical, 'AR', N'APARELHO RESPIRATÓRIO', 'LONG_TEXT', 0, 40, 1, NULL, 3000),
    (NEWID(), @evolution_physical, 'ACV', N'APARELHO CARDIOVASCULAR', 'LONG_TEXT', 0, 50, 1, NULL, 3000),
    (NEWID(), @evolution_physical, 'ABDOME', N'ABDOME', 'LONG_TEXT', 0, 60, 1, NULL, 3000),
    (NEWID(), @evolution_physical, 'MMII', N'MEMBROS INFERIORES', 'LONG_TEXT', 0, 70, 1, NULL, 3000),
    (NEWID(), @evolution_physical, 'PERFUSAO', N'PERFUSÃO CAPILAR EM MMSS E MMII', 'SHORT_TEXT', 0, 80, 1, NULL, 1000),
    (NEWID(), @evolution_exams, 'TABELA_EXAMES', N'TABELA DE EXAMES', 'CLINICAL_TABLE', 0, 10, 1, NULL, NULL),
    (NEWID(), @evolution_exams, 'OUTROS_EXAMES', N'OUTROS EXAMES COMPLEMENTARES', 'LONG_TEXT', 0, 20, 1, NULL, 5000),
    (NEWID(), @evolution_plan, 'CONDUTA', N'CONDUTA MÉDICA', 'LONG_TEXT', 0, 10, 1, N'CADA LINHA SERÁ APRESENTADA COM HÍFEN', 10000);

INSERT INTO dbo.form_field_option (id, field_id, option_value, option_label, display_order, active)
VALUES
    (NEWID(), @position_field, N'ACAMADO', N'ACAMADO EM DECÚBITO DORSAL', 10, 1),
    (NEWID(), @position_field, N'POLTRONA', N'SENTADO NA POLTRONA', 20, 1),
    (NEWID(), @position_field, N'DEAMBULANDO', N'DEAMBULANDO NO QUARTO', 30, 1),
    (NEWID(), @position_field, N'RESTRITO', N'RESTRITO AO LEITO POR CONTENÇÃO MECÂNICA', 40, 1),
    (NEWID(), @accompaniment_field, N'ACOMPANHADO', N'ACOMPANHADO POR FAMILIAR / CUIDADOR', 10, 1),
    (NEWID(), @accompaniment_field, N'DESACOMPANHADO', N'DESACOMPANHADO NO MOMENTO', 20, 1),
    (NEWID(), @consciousness_field, N'VIGIL', N'VIGIL / ACORDADO', 10, 1),
    (NEWID(), @consciousness_field, N'SONOLENTO', N'SONOLENTO (DESPERTA AO CHAMADO)', 20, 1),
    (NEWID(), @consciousness_field, N'TORPOROSO', N'TORPOROSO (DESPERTA APENAS COM ESTÍMULO VIGOROSO)', 30, 1),
    (NEWID(), @consciousness_field, N'COMATOSO', N'COMATOSO / SEDADO', 40, 1),
    (NEWID(), @orientation_field, N'ORIENTADO', N'ORIENTADO NO TEMPO E NO ESPAÇO', 10, 1),
    (NEWID(), @orientation_field, N'DESORIENTADO_TEMPO', N'DESORIENTADO NO TEMPO', 20, 1),
    (NEWID(), @orientation_field, N'DESORIENTADO_ESPACO', N'DESORIENTADO NO ESPAÇO', 30, 1),
    (NEWID(), @orientation_field, N'DESORIENTADO_GLOBAL', N'GLOBALMENTE DESORIENTADO', 40, 1),
    (NEWID(), @chief_complaint_field, N'SEM_QUEIXAS', N'NEGA NOVAS QUEIXAS ATIVAS', 10, 1),
    (NEWID(), @chief_complaint_field, N'DOR', N'QUEIXA-SE DE DOR', 20, 1),
    (NEWID(), @chief_complaint_field, N'DISPNEIA', N'QUEIXA-SE DE FALTA DE AR / DISPNEIA', 30, 1),
    (NEWID(), @chief_complaint_field, N'NAUSEAS', N'QUEIXA-SE DE NÁUSEAS / ENJOO', 40, 1),
    (NEWID(), @chief_complaint_field, N'TONTURA', N'QUEIXA-SE DE TONTURA / MAL-ESTAR', 50, 1),
    (NEWID(), @shift_events_field, N'SEM_INTERCORRENCIAS', N'SEM INTERCORRÊNCIAS RELATADAS', 10, 1),
    (NEWID(), @shift_events_field, N'FEBRE', N'PICO FEBRIL', 20, 1),
    (NEWID(), @shift_events_field, N'HIPOTENSAO', N'EPISÓDIO DE HIPOTENSÃO', 30, 1),
    (NEWID(), @shift_events_field, N'AGITACAO', N'AGITAÇÃO PSICOMOTORA', 40, 1),
    (NEWID(), @shift_events_field, N'QUEDA_SATURACAO', N'QUEDA DA SATURAÇÃO / NECESSIDADE DE O₂', 50, 1),
    (NEWID(), @shift_events_field, N'VOMITO_DIARREIA', N'EPISÓDIO DE VÔMITO / DIARREIA', 60, 1),
    (NEWID(), @food_field, N'BOA', N'BOA (>75% DA REFEIÇÃO)', 10, 1),
    (NEWID(), @food_field, N'PARCIAL', N'PARCIAL / REGULAR (~50%)', 20, 1),
    (NEWID(), @food_field, N'RECUSA', N'INAPETENTE / RECUSA ALIMENTAR (<25%)', 30, 1),
    (NEWID(), @food_field, N'JEJUM', N'EM JEJUM PARA PROCEDIMENTO / EXAME', 40, 1),
    (NEWID(), @urinary_field, N'ESPONTANEA', N'ESPONTÂNEA', 10, 1),
    (NEWID(), @urinary_field, N'SVD', N'POR SONDA VESICAL DE DEMORA (SVD)', 20, 1),
    (NEWID(), @urinary_field, N'SVA', N'POR SONDA VESICAL DE ALÍVIO (SVA)', 30, 1),
    (NEWID(), @urinary_field, N'ANURIA', N'AUSENTE / ANÚRIA', 40, 1),
    (NEWID(), @urine_field, N'CLARA', N'CLARA / CITRINA', 10, 1),
    (NEWID(), @urine_field, N'CONCENTRADA', N'CONCENTRADA / COLÚRICA', 20, 1),
    (NEWID(), @urine_field, N'HEMATURICA', N'HEMATÚRICA (COM SANGUE)', 30, 1),
    (NEWID(), @urine_field, N'PIURICA', N'PIÚRICA / TURVA', 40, 1),
    (NEWID(), @intestinal_field, N'PRESENTE', N'PRESENTES E PRESERVADAS NAS ÚLTIMAS 24H', 10, 1),
    (NEWID(), @intestinal_field, N'AUSENTE', N'AUSENTES', 20, 1),
    (NEWID(), @intestinal_field, N'DIARREIA', N'EPISÓDIOS DIARREICOS', 30, 1),
    (NEWID(), @stool_field, N'FORMADAS', N'PASTOSAS / FORMADAS', 10, 1),
    (NEWID(), @stool_field, N'LIQUIDAS', N'LÍQUIDAS', 20, 1),
    (NEWID(), @stool_field, N'ESCIBALAS', N'ESCÍBALAS (ENDURECIDAS)', 30, 1),
    (NEWID(), @stool_field, N'SANGUE', N'MELENA / ENTERORRAGIA (COM SANGUE)', 40, 1),
    (NEWID(), @sleep_field, N'PRESERVADO', N'PRESERVADO / DORMIU BEM', 10, 1),
    (NEWID(), @sleep_field, N'INSONIA', N'INSÔNIA / AGITADO DURANTE A NOITE', 20, 1);
