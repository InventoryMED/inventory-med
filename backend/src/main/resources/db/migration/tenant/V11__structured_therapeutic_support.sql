SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

ALTER TABLE dbo.form_field DROP CONSTRAINT ck_form_field_type;

ALTER TABLE dbo.form_field ADD CONSTRAINT ck_form_field_type CHECK (field_type IN (
    'SHORT_TEXT', 'LONG_TEXT', 'INTEGER', 'DECIMAL', 'DATE', 'TIME',
    'SINGLE_SELECT', 'MULTI_SELECT', 'BOOLEAN', 'MEDICATION_LINE', 'CLINICAL_TABLE',
    'DIET_PLAN', 'NURSING_CARE_PLAN', 'MONITORING_PLAN', 'VENTILATORY_SUPPORT_PLAN',
    'REHABILITATION_PLAN', 'ISOLATION_PRECAUTIONS_PLAN', 'THERAPEUTIC_SUPPORT_PLAN'
));

DECLARE @template_id UNIQUEIDENTIFIER;
DECLARE @previous_version_id UNIQUEIDENTIFIER;
DECLARE @new_version_id UNIQUEIDENTIFIER = NEWID();
DECLARE @version_number INT;
DECLARE @system_actor UNIQUEIDENTIFIER = '00000000-0000-0000-0000-000000000000';
DECLARE @therapeutic_section_id UNIQUEIDENTIFIER = NEWID();

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
    THROW 51000, 'Modelo publicado de prescrição não encontrado para criar suporte terapêutico estruturado.', 1;

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

DECLARE @section_map TABLE (
    old_id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    new_id UNIQUEIDENTIFIER NOT NULL
);

INSERT INTO @section_map (old_id, new_id)
SELECT id, NEWID()
FROM dbo.form_section
WHERE version_id = @previous_version_id;

INSERT INTO dbo.form_section (id, version_id, section_key, title, display_order, active)
SELECT m.new_id, @new_version_id, s.section_key, s.title, s.display_order, s.active
FROM dbo.form_section s
JOIN @section_map m ON m.old_id = s.id;

INSERT INTO dbo.form_section (id, version_id, section_key, title, display_order, active)
VALUES (
    @therapeutic_section_id,
    @new_version_id,
    'SUPORTE_TERAPEUTICO',
    N'HIDRATAÇÃO, CONTROLE GLICÊMICO E HEMOTERAPIA',
    16,
    1
);

DECLARE @field_map TABLE (
    old_id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    new_id UNIQUEIDENTIFIER NOT NULL
);

INSERT INTO @field_map (old_id, new_id)
SELECT f.id, NEWID()
FROM dbo.form_field f
JOIN dbo.form_section s ON s.id = f.section_id
WHERE s.version_id = @previous_version_id;

INSERT INTO dbo.form_field
    (id, section_id, field_key, label, field_type, required, display_order, active, placeholder, max_length)
SELECT
    fm.new_id,
    sm.new_id,
    f.field_key,
    f.label,
    f.field_type,
    f.required,
    f.display_order,
    f.active,
    f.placeholder,
    f.max_length
FROM dbo.form_field f
JOIN @field_map fm ON fm.old_id = f.id
JOIN dbo.form_section s ON s.id = f.section_id
JOIN @section_map sm ON sm.old_id = s.id;

INSERT INTO dbo.form_field_option
    (id, field_id, option_value, option_label, display_order, active)
SELECT
    NEWID(),
    fm.new_id,
    o.option_value,
    o.option_label,
    o.display_order,
    o.active
FROM dbo.form_field_option o
JOIN @field_map fm ON fm.old_id = o.field_id;

INSERT INTO dbo.form_field
    (id, section_id, field_key, label, field_type, required, display_order, active, placeholder, max_length)
VALUES (
    NEWID(),
    @therapeutic_section_id,
    'PLANO',
    N'HIDRATAÇÃO, CONTROLE GLICÊMICO E HEMOTERAPIA',
    'THERAPEUTIC_SUPPORT_PLAN',
    0,
    10,
    1,
    N'SELECIONE SOLUÇÕES, CONTROLE GLICÊMICO E PRODUTOS SANGUÍNEOS',
    NULL
);
