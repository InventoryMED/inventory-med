SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

ALTER TABLE dbo.form_field DROP CONSTRAINT ck_form_field_type;

ALTER TABLE dbo.form_field ADD CONSTRAINT ck_form_field_type CHECK (field_type IN (
    'SHORT_TEXT', 'LONG_TEXT', 'INTEGER', 'DECIMAL', 'DATE', 'TIME',
    'SINGLE_SELECT', 'MULTI_SELECT', 'BOOLEAN', 'MEDICATION_LINE', 'CLINICAL_TABLE',
    'DIET_PLAN', 'NURSING_CARE_PLAN'
));

DECLARE @template_id UNIQUEIDENTIFIER;
DECLARE @previous_version_id UNIQUEIDENTIFIER;
DECLARE @new_version_id UNIQUEIDENTIFIER = NEWID();
DECLARE @version_number INT;
DECLARE @system_actor UNIQUEIDENTIFIER = '00000000-0000-0000-0000-000000000000';
DECLARE @general_section UNIQUEIDENTIFIER = NEWID();
DECLARE @nursing_section UNIQUEIDENTIFIER = NEWID();
DECLARE @medication_section UNIQUEIDENTIFIER = NEWID();
DECLARE @notes_section UNIQUEIDENTIFIER = NEWID();

SELECT TOP 1
    @template_id = t.id,
    @previous_version_id = v.id
FROM dbo.form_template t
JOIN dbo.form_template_version v ON v.template_id = t.id
WHERE t.kind = 'PRESCRIPTION'
  AND t.name = N'PRESCRIÇÃO MÉDICA PADRÃO'
  AND v.status = 'PUBLISHED'
ORDER BY v.version_number DESC;

IF @template_id IS NULL OR @previous_version_id IS NULL
    THROW 51000, 'Modelo publicado de prescrição não encontrado para criar cuidados de enfermagem.', 1;

SELECT @version_number = MAX(version_number) + 1
FROM dbo.form_template_version
WHERE template_id = @template_id;

UPDATE dbo.form_template_version
SET status = 'RETIRED'
WHERE id = @previous_version_id;

INSERT INTO dbo.form_template_version
    (id, template_id, version_number, status, created_by, published_by, published_at)
VALUES
    (@new_version_id, @template_id, @version_number, 'PUBLISHED', @system_actor, @system_actor, SYSUTCDATETIME());

INSERT INTO dbo.form_section (id, version_id, section_key, title, display_order, active)
VALUES
    (@general_section, @new_version_id, 'ORIENTACOES', N'ORIENTAÇÕES GERAIS', 10, 1),
    (@nursing_section, @new_version_id, 'CUIDADOS_ENFERMAGEM', N'CUIDADOS DE ENFERMAGEM', 15, 1),
    (@medication_section, @new_version_id, 'MEDICAMENTOS', N'MEDICAMENTOS', 20, 1),
    (@notes_section, @new_version_id, 'OBSERVACOES', N'OBSERVAÇÕES', 30, 1);

INSERT INTO dbo.form_field
    (id, section_id, field_key, label, field_type, required, display_order, active, placeholder, max_length)
SELECT
    NEWID(),
    CASE s.section_key
        WHEN 'ORIENTACOES' THEN @general_section
        WHEN 'MEDICAMENTOS' THEN @medication_section
        WHEN 'OBSERVACOES' THEN @notes_section
    END,
    f.field_key,
    f.label,
    f.field_type,
    f.required,
    f.display_order,
    f.active,
    f.placeholder,
    f.max_length
FROM dbo.form_field f
JOIN dbo.form_section s ON s.id = f.section_id
WHERE s.version_id = @previous_version_id
  AND s.section_key IN ('ORIENTACOES', 'MEDICAMENTOS', 'OBSERVACOES');

INSERT INTO dbo.form_field
    (id, section_id, field_key, label, field_type, required, display_order, active, placeholder, max_length)
VALUES
    (
        NEWID(), @nursing_section, 'CUIDADOS', N'CUIDADOS DE ENFERMAGEM',
        'NURSING_CARE_PLAN', 0, 10, 1,
        N'SELECIONE OS CUIDADOS, DISPOSITIVOS E FREQUÊNCIAS APLICÁVEIS', NULL
    );
