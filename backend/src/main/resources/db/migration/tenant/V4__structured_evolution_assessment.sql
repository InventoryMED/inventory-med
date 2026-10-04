SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

DECLARE @system_actor UNIQUEIDENTIFIER = '00000000-0000-0000-0000-000000000000';
DECLARE @template UNIQUEIDENTIFIER;
DECLARE @version UNIQUEIDENTIFIER = NEWID();
DECLARE @version_number INT;

SELECT @template = id
FROM dbo.form_template
WHERE kind = 'EVOLUTION' AND name = N'EVOLUÇÃO MÉDICA PADRÃO';

IF @template IS NOT NULL
BEGIN
    SELECT @version_number = ISNULL(MAX(version_number), 0) + 1
    FROM dbo.form_template_version
    WHERE template_id = @template;

    UPDATE dbo.form_template_version
    SET status = 'RETIRED'
    WHERE template_id = @template AND status = 'PUBLISHED';

    INSERT INTO dbo.form_template_version
        (id, template_id, version_number, status, created_by, published_by, published_at)
    VALUES
        (@version, @template, @version_number, 'PUBLISHED', @system_actor, @system_actor, SYSUTCDATETIME());

    DECLARE @follow_up UNIQUEIDENTIFIER = NEWID();
    DECLARE @physiology UNIQUEIDENTIFIER = NEWID();
    DECLARE @neurological UNIQUEIDENTIFIER = NEWID();
    DECLARE @sedation UNIQUEIDENTIFIER = NEWID();
    DECLARE @respiratory UNIQUEIDENTIFIER = NEWID();
    DECLARE @cardiovascular UNIQUEIDENTIFIER = NEWID();
    DECLARE @physical UNIQUEIDENTIFIER = NEWID();
    DECLARE @exams UNIQUEIDENTIFIER = NEWID();
    DECLARE @plan UNIQUEIDENTIFIER = NEWID();

    INSERT INTO dbo.form_section (id, version_id, section_key, title, display_order, active)
    VALUES
        (@follow_up, @version, 'ACOMPANHAMENTO', N'IDENTIFICAÇÃO E ACOMPANHAMENTO', 10, 1),
        (@physiology, @version, 'FISIOLOGICO', N'FUNÇÕES FISIOLÓGICAS', 20, 1),
        (@neurological, @version, 'NEUROLOGICO', N'NEUROLÓGICO', 30, 1),
        (@sedation, @version, 'SEDACAO', N'SEDAÇÃO', 40, 1),
        (@respiratory, @version, 'RESPIRATORIO', N'RESPIRATÓRIO', 50, 1),
        (@cardiovascular, @version, 'CARDIOVASCULAR', N'STATUS HEMODINÂMICO E CARDIOVASCULAR', 60, 1),
        (@physical, @version, 'EXAME_FISICO', N'EXAME FÍSICO', 70, 1),
        (@exams, @version, 'EXAMES', N'EXAMES COMPLEMENTARES', 80, 1),
        (@plan, @version, 'CONDUTA', N'CONDUTA MÉDICA', 90, 1);

    DECLARE @position UNIQUEIDENTIFIER = NEWID();
    DECLARE @accompaniment UNIQUEIDENTIFIER = NEWID();
    DECLARE @hygiene UNIQUEIDENTIFIER = NEWID();
    DECLARE @food UNIQUEIDENTIFIER = NEWID();
    DECLARE @urinary UNIQUEIDENTIFIER = NEWID();
    DECLARE @urine UNIQUEIDENTIFIER = NEWID();
    DECLARE @intestinal UNIQUEIDENTIFIER = NEWID();
    DECLARE @stool UNIQUEIDENTIFIER = NEWID();
    DECLARE @sleep UNIQUEIDENTIFIER = NEWID();
    DECLARE @consciousness UNIQUEIDENTIFIER = NEWID();
    DECLARE @orientation UNIQUEIDENTIFIER = NEWID();
    DECLARE @interaction UNIQUEIDENTIFIER = NEWID();
    DECLARE @sedation_dynamics UNIQUEIDENTIFIER = NEWID();
    DECLARE @respiratory_pattern UNIQUEIDENTIFIER = NEWID();
    DECLARE @ventilatory_support UNIQUEIDENTIFIER = NEWID();
    DECLARE @hemodynamic UNIQUEIDENTIFIER = NEWID();
    DECLARE @pressure_profile UNIQUEIDENTIFIER = NEWID();

    INSERT INTO dbo.form_field
        (id, section_id, field_key, label, field_type, required, display_order, active, placeholder, max_length)
    VALUES
        (@position, @follow_up, 'POSICAO', N'POSIÇÃO / ATIVIDADE NO LEITO', 'SINGLE_SELECT', 0, 10, 1, NULL, NULL),
        (@accompaniment, @follow_up, 'ACOMPANHAMENTO', N'ACOMPANHAMENTO', 'SINGLE_SELECT', 0, 20, 1, NULL, NULL),
        (@hygiene, @follow_up, 'HIGIENE', N'HIGIENE PESSOAL', 'SINGLE_SELECT', 0, 30, 1, NULL, NULL),
        (NEWID(), @physiology, 'ADMISSAO', N'ADMISSÃO', 'LONG_TEXT', 0, 10, 1, NULL, 10000),
        (@food, @physiology, 'ACEITACAO_ALIMENTAR', N'ACEITAÇÃO ALIMENTAR', 'SINGLE_SELECT', 0, 20, 1, NULL, NULL),
        (@urinary, @physiology, 'ELIMINACAO_URINARIA', N'ELIMINAÇÃO URINÁRIA', 'SINGLE_SELECT', 0, 30, 1, NULL, NULL),
        (NEWID(), @physiology, 'VOLUME_URINARIO', N'QUANTIDADE DA DIURESE EM ML/24H', 'DECIMAL', 0, 40, 1, NULL, NULL),
        (@urine, @physiology, 'ASPECTO_URINA', N'ASPECTO DA URINA', 'SINGLE_SELECT', 0, 50, 1, NULL, NULL),
        (@intestinal, @physiology, 'ELIMINACAO_INTESTINAL', N'ELIMINAÇÃO INTESTINAL', 'SINGLE_SELECT', 0, 60, 1, NULL, NULL),
        (NEWID(), @physiology, 'DIAS_SEM_EVACUAR', N'DIAS SEM EVACUAR', 'INTEGER', 0, 70, 1, NULL, NULL),
        (@stool, @physiology, 'ASPECTO_FEZES', N'ASPECTO DAS FEZES', 'SINGLE_SELECT', 0, 80, 1, NULL, NULL),
        (@sleep, @physiology, 'PADRAO_SONO', N'PADRÃO DE SONO', 'SINGLE_SELECT', 0, 90, 1, NULL, NULL),
        (NEWID(), @physiology, 'ATB_ATUAL', N'ANTIBIOTICOTERAPIA ATUAL', 'LONG_TEXT', 0, 100, 1, NULL, 2000),
        (NEWID(), @physiology, 'ATB_PREVIA', N'ANTIBIOTICOTERAPIA PRÉVIA', 'LONG_TEXT', 0, 110, 1, NULL, 2000),
        (NEWID(), @physiology, 'EVOLUCAO', N'EVOLUÇÃO', 'LONG_TEXT', 0, 120, 1, NULL, 10000),
        (@consciousness, @neurological, 'CONSCIENCIA', N'NÍVEL DE CONSCIÊNCIA', 'SINGLE_SELECT', 0, 10, 1, NULL, NULL),
        (@orientation, @neurological, 'ORIENTACAO', N'ORIENTAÇÃO TEMPOROESPACIAL', 'SINGLE_SELECT', 0, 20, 1, NULL, NULL),
        (@interaction, @neurological, 'INTERACAO', N'INTERAÇÃO / CONTACTUANTE', 'SINGLE_SELECT', 0, 30, 1, NULL, NULL),
        (NEWID(), @neurological, 'OBSERVACOES', N'OBSERVAÇÕES NEUROLÓGICAS', 'LONG_TEXT', 0, 40, 1, NULL, 3000),
        (NEWID(), @sedation, 'MEDICAMENTOS', N'MEDICAMENTOS SEDATIVOS', 'MEDICATION_LINE', 0, 10, 1, NULL, NULL),
        (@sedation_dynamics, @sedation, 'DINAMICA', N'STATUS E DINÂMICA DA SEDAÇÃO', 'SINGLE_SELECT', 0, 20, 1, NULL, NULL),
        (@respiratory_pattern, @respiratory, 'PADRAO', N'PADRÃO RESPIRATÓRIO', 'MULTI_SELECT', 0, 10, 1, NULL, NULL),
        (@ventilatory_support, @respiratory, 'SUPORTE', N'VIA AÉREA E SUPORTE VENTILATÓRIO', 'SINGLE_SELECT', 0, 20, 1, NULL, NULL),
        (NEWID(), @respiratory, 'OBSERVACOES', N'OBSERVAÇÕES RESPIRATÓRIAS', 'LONG_TEXT', 0, 30, 1, NULL, 3000),
        (@hemodynamic, @cardiovascular, 'ESTABILIDADE', N'ESTABILIDADE HEMODINÂMICA GERAL', 'SINGLE_SELECT', 0, 10, 1, NULL, NULL),
        (@pressure_profile, @cardiovascular, 'PERFIL_PRESSORICO', N'PERFIL PRESSÓRICO', 'SINGLE_SELECT', 0, 20, 1, NULL, NULL),
        (NEWID(), @cardiovascular, 'PAM_ALVO', N'PAM DENTRO DO ALVO (PAM ≥ 65 MMHG)', 'BOOLEAN', 0, 30, 1, NULL, NULL),
        (NEWID(), @cardiovascular, 'DROGAS_VASOATIVAS', N'SUPORTE VASOATIVO / INOTRÓPICO', 'MEDICATION_LINE', 0, 40, 1, NULL, NULL),
        (NEWID(), @cardiovascular, 'OBSERVACOES', N'OBSERVAÇÕES CARDIOVASCULARES', 'LONG_TEXT', 0, 50, 1, NULL, 3000),
        (NEWID(), @physical, 'SSVV', N'SINAIS VITAIS', 'SHORT_TEXT', 0, 10, 1, N'SATO₂, FC, FR, PA E TAX', 500),
        (NEWID(), @physical, 'ECTOSCOPIA', N'ECTOSCOPIA', 'SHORT_TEXT', 0, 20, 1, NULL, 1000),
        (NEWID(), @physical, 'ABDOME', N'ABDOME', 'LONG_TEXT', 0, 30, 1, NULL, 3000),
        (NEWID(), @physical, 'MMII', N'MEMBROS INFERIORES', 'LONG_TEXT', 0, 40, 1, NULL, 3000),
        (NEWID(), @physical, 'PERFUSAO', N'PERFUSÃO CAPILAR EM MMSS E MMII', 'SHORT_TEXT', 0, 50, 1, NULL, 1000),
        (NEWID(), @exams, 'TABELA_EXAMES', N'TABELA DE EXAMES', 'CLINICAL_TABLE', 0, 10, 1, NULL, NULL),
        (NEWID(), @exams, 'OUTROS_EXAMES', N'OUTROS EXAMES COMPLEMENTARES', 'LONG_TEXT', 0, 20, 1, NULL, 5000),
        (NEWID(), @plan, 'CONDUTA', N'CONDUTA MÉDICA', 'LONG_TEXT', 0, 10, 1, N'CADA LINHA SERÁ APRESENTADA COM HÍFEN', 10000);

    INSERT INTO dbo.form_field_option (id, field_id, option_value, option_label, display_order, active)
    VALUES
        (NEWID(), @position, N'ACAMADO', N'ACAMADO EM DECÚBITO DORSAL', 10, 1),
        (NEWID(), @position, N'POLTRONA', N'SENTADO NA POLTRONA', 20, 1),
        (NEWID(), @position, N'DEAMBULANDO', N'DEAMBULANDO NO QUARTO', 30, 1),
        (NEWID(), @position, N'RESTRITO', N'RESTRITO AO LEITO POR CONTENÇÃO MECÂNICA', 40, 1),
        (NEWID(), @accompaniment, N'ACOMPANHADO', N'ACOMPANHADO POR FAMILIAR / CUIDADOR', 10, 1),
        (NEWID(), @accompaniment, N'DESACOMPANHADO', N'DESACOMPANHADO NO MOMENTO', 20, 1),
        (NEWID(), @hygiene, N'BOA', N'BOA HIGIENE', 10, 1),
        (NEWID(), @hygiene, N'MA', N'MÁ HIGIENE', 20, 1),
        (NEWID(), @food, N'BOA', N'BOA (>75% DA REFEIÇÃO)', 10, 1),
        (NEWID(), @food, N'PARCIAL', N'PARCIAL / REGULAR (~50%)', 20, 1),
        (NEWID(), @food, N'RECUSA', N'INAPETENTE / RECUSA ALIMENTAR (<25%)', 30, 1),
        (NEWID(), @food, N'JEJUM', N'EM JEJUM PARA PROCEDIMENTO / EXAME', 40, 1),
        (NEWID(), @urinary, N'ESPONTANEA', N'ESPONTÂNEA', 10, 1),
        (NEWID(), @urinary, N'SVD', N'POR SONDA VESICAL DE DEMORA (SVD)', 20, 1),
        (NEWID(), @urinary, N'SVA', N'POR SONDA VESICAL DE ALÍVIO (SVA)', 30, 1),
        (NEWID(), @urinary, N'ANURIA', N'AUSENTE / ANÚRIA', 40, 1),
        (NEWID(), @urine, N'CLARA', N'CLARA / CITRINA', 10, 1),
        (NEWID(), @urine, N'CONCENTRADA', N'CONCENTRADA / COLÚRICA', 20, 1),
        (NEWID(), @urine, N'HEMATURICA', N'HEMATÚRICA (COM SANGUE)', 30, 1),
        (NEWID(), @urine, N'PIURICA', N'PIÚRICA / TURVA', 40, 1),
        (NEWID(), @intestinal, N'PRESENTE', N'PRESENTES E PRESERVADAS NAS ÚLTIMAS 24H', 10, 1),
        (NEWID(), @intestinal, N'AUSENTE', N'AUSENTES', 20, 1),
        (NEWID(), @intestinal, N'DIARREIA', N'EPISÓDIOS DIARREICOS', 30, 1),
        (NEWID(), @stool, N'FORMADAS', N'PASTOSAS / FORMADAS', 10, 1),
        (NEWID(), @stool, N'LIQUIDAS', N'LÍQUIDAS', 20, 1),
        (NEWID(), @stool, N'ESCIBALAS', N'ESCÍBALAS (ENDURECIDAS)', 30, 1),
        (NEWID(), @stool, N'SANGUE', N'MELENA / ENTERORRAGIA (COM SANGUE)', 40, 1),
        (NEWID(), @sleep, N'PRESERVADO', N'PRESERVADO / DORMIU BEM', 10, 1),
        (NEWID(), @sleep, N'INSONIA', N'INSÔNIA / AGITADO DURANTE A NOITE', 20, 1),
        (NEWID(), @consciousness, N'VIGIL', N'VIGIL / ACORDADO', 10, 1),
        (NEWID(), @consciousness, N'SONOLENTO', N'SONOLENTO (DESPERTA AO CHAMADO)', 20, 1),
        (NEWID(), @consciousness, N'TORPOROSO', N'TORPOROSO (DESPERTA APENAS COM ESTÍMULO VIGOROSO)', 30, 1),
        (NEWID(), @consciousness, N'COMATOSO', N'COMATOSO / SEDADO', 40, 1),
        (NEWID(), @orientation, N'ORIENTADO', N'ORIENTADO NO TEMPO E NO ESPAÇO', 10, 1),
        (NEWID(), @orientation, N'DESORIENTADO_TEMPO', N'DESORIENTADO NO TEMPO', 20, 1),
        (NEWID(), @orientation, N'DESORIENTADO_ESPACO', N'DESORIENTADO NO ESPAÇO', 30, 1),
        (NEWID(), @orientation, N'DESORIENTADO_GLOBAL', N'GLOBALMENTE DESORIENTADO', 40, 1),
        (NEWID(), @interaction, N'COOPERATIVO', N'CONTACTUANTE E COOPERATIVO', 10, 1),
        (NEWID(), @interaction, N'NAO_VERBAL', N'CONTACTUANTE NÃO-VERBAL (INTERAGE POR GESTOS / OLHAR)', 20, 1),
        (NEWID(), @interaction, N'APATICO', N'POUCO CONTACTUANTE / APÁTICO', 30, 1),
        (NEWID(), @interaction, N'NAO_CONTACTUANTE', N'NÃO CONTACTUANTE', 40, 1),
        (NEWID(), @interaction, N'SEDACAO_CONTINUA', N'INCONTACTÁVEL POR SEDAÇÃO CONTÍNUA', 50, 1),
        (NEWID(), @sedation_dynamics, N'DOSE_ESTAVEL', N'MANUTENÇÃO EM DOSE ESTÁVEL', 10, 1),
        (NEWID(), @sedation_dynamics, N'DESMAME', N'EM PROCESSO DE DESMAME / REDUÇÃO GRADUAL DE DOSE', 20, 1),
        (NEWID(), @sedation_dynamics, N'ESCALONAMENTO', N'EM ESCALONAMENTO / AUMENTO DE DOSE (POR AGITAÇÃO OU ASSINCRONIA COM O VENTILADOR)', 30, 1),
        (NEWID(), @sedation_dynamics, N'PAUSA_PROGRAMADA', N'PAUSA PROGRAMADA DA SEDAÇÃO (TESTE DO DESPERTAR DIÁRIO)', 40, 1),
        (NEWID(), @sedation_dynamics, N'SUSPENSA_24H', N'SEDAÇÃO SUSPENSA NAS ÚLTIMAS 24H', 50, 1),
        (NEWID(), @respiratory_pattern, N'SEM_ESFORCO', N'SEM SINAIS DE ESFORÇO RESPIRATÓRIO (EXPANSIBILIDADE PRESERVADA E SIMÉTRICA)', 10, 1),
        (NEWID(), @respiratory_pattern, N'MUSCULATURA_ACESSORIA', N'USO DE MUSCULATURA ACESSÓRIA (TIRAGEM INTERCOSTAL, SUBCOSTAL OU SUPRACLAVICULAR)', 20, 1),
        (NEWID(), @respiratory_pattern, N'BAN', N'BATIMENTO DE ASAS DO NARIZ (BAN)', 30, 1),
        (NEWID(), @respiratory_pattern, N'DISSOCIACAO', N'DISSOCIAÇÃO TORACOABDOMINAL', 40, 1),
        (NEWID(), @respiratory_pattern, N'GEMIDO', N'GEMIDO EXPIRATÓRIO', 50, 1),
        (NEWID(), @ventilatory_support, N'AA', N'AR AMBIENTE (AA)', 10, 1),
        (NEWID(), @ventilatory_support, N'CATETER_O2', N'CATETER NASAL DE O₂', 20, 1),
        (NEWID(), @ventilatory_support, N'MASCARA_SIMPLES', N'MÁSCARA SIMPLES DE O₂', 30, 1),
        (NEWID(), @ventilatory_support, N'VENTURI', N'MÁSCARA DE VENTURI', 40, 1),
        (NEWID(), @ventilatory_support, N'VNI', N'VENTILAÇÃO NÃO INVASIVA (VNI / CPAP / BIPAP)', 50, 1),
        (NEWID(), @ventilatory_support, N'VMI', N'VENTILAÇÃO MECÂNICA INVASIVA (VMI)', 60, 1),
        (NEWID(), @ventilatory_support, N'TRAQUEOSTOMIA', N'TRAQUEOSTOMIA EM AR AMBIENTE / EM MÁSCARA DE TQT / TUBO EM T', 70, 1),
        (NEWID(), @hemodynamic, N'ESTAVEL', N'HEMODINAMICAMENTE ESTÁVEL', 10, 1),
        (NEWID(), @hemodynamic, N'ESTAVEL_MEDICACAO', N'HEMODINAMICAMENTE ESTÁVEL SOB MEDICAÇÃO', 20, 1),
        (NEWID(), @hemodynamic, N'LIMITROFE', N'HEMODINAMICAMENTE LIMÍTROFE', 30, 1),
        (NEWID(), @hemodynamic, N'INSTAVEL', N'HEMODINAMICAMENTE INSTÁVEL', 40, 1),
        (NEWID(), @pressure_profile, N'NORMOTENSO', N'NORMOTENSO', 10, 1),
        (NEWID(), @pressure_profile, N'HIPOTENSO', N'HIPOTENSO', 20, 1),
        (NEWID(), @pressure_profile, N'HIPERTENSO', N'HIPERTENSO', 30, 1);
END;
