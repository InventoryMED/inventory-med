SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

ALTER TABLE dbo.form_template DROP CONSTRAINT ck_form_template_kind;
ALTER TABLE dbo.form_template ADD CONSTRAINT ck_form_template_kind
    CHECK (kind IN ('PRESCRIPTION', 'EVOLUTION', 'PROCEDURE'));

ALTER TABLE dbo.clinical_document DROP CONSTRAINT ck_clinical_document_kind;
ALTER TABLE dbo.clinical_document ADD CONSTRAINT ck_clinical_document_kind
    CHECK (kind IN ('PRESCRIPTION', 'EVOLUTION', 'PROCEDURE'));

ALTER TABLE dbo.form_field DROP CONSTRAINT ck_form_field_type;
ALTER TABLE dbo.form_field ADD CONSTRAINT ck_form_field_type CHECK (field_type IN (
    'SHORT_TEXT', 'LONG_TEXT', 'INTEGER', 'DECIMAL', 'DATE', 'TIME',
    'SINGLE_SELECT', 'MULTI_SELECT', 'BOOLEAN', 'MEDICATION_LINE', 'CLINICAL_TABLE',
    'DIET_PLAN', 'NURSING_CARE_PLAN', 'MONITORING_PLAN', 'VENTILATORY_SUPPORT_PLAN',
    'REHABILITATION_PLAN', 'ISOLATION_PRECAUTIONS_PLAN', 'THERAPEUTIC_SUPPORT_PLAN',
    'CRITICAL_CARE_PLAN', 'MEDICATION_THERAPY_PLAN', 'PROCEDURE_PLAN'
));

DECLARE @system_actor UNIQUEIDENTIFIER = '00000000-0000-0000-0000-000000000000';
DECLARE @template_id UNIQUEIDENTIFIER = NEWID();
DECLARE @version_id UNIQUEIDENTIFIER = NEWID();
DECLARE @section_id UNIQUEIDENTIFIER = NEWID();

INSERT INTO dbo.form_template (id, kind, name, active, created_by)
VALUES (@template_id, 'PROCEDURE', N'PROCEDIMENTOS BEIRA-LEITO PADRÃO', 1, @system_actor);

INSERT INTO dbo.form_template_version
    (id, template_id, version_number, status, created_by, published_by, published_at)
VALUES
    (@version_id, @template_id, 1, 'PUBLISHED', @system_actor, @system_actor, SYSUTCDATETIME());

INSERT INTO dbo.form_section (id, version_id, section_key, title, display_order, active)
VALUES (
    @section_id,
    @version_id,
    'PROCEDURE',
    N'PROCEDIMENTOS E INTERVENÇÕES BEIRA-LEITO',
    10,
    1
);

INSERT INTO dbo.form_field
    (id, section_id, field_key, label, field_type, required, display_order, active, placeholder, max_length)
VALUES (
    NEWID(),
    @section_id,
    'RECORD',
    N'REGISTRO E PLANEJAMENTO DE PROCEDIMENTOS',
    'PROCEDURE_PLAN',
    1,
    10,
    1,
    N'SELECIONE O PROCEDIMENTO, CONFIRA A TÉCNICA E REGISTRE A RASTREABILIDADE',
    NULL
);
