SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

DECLARE @system_actor UNIQUEIDENTIFIER = '00000000-0000-0000-0000-000000000000';
DECLARE @template_id UNIQUEIDENTIFIER;
DECLARE @version_id UNIQUEIDENTIFIER = NEWID();
DECLARE @section_id UNIQUEIDENTIFIER = NEWID();
DECLARE @version_number INT;

SELECT TOP 1 @template_id = id
FROM dbo.form_template
WHERE kind = 'PROCEDURE'
  AND name = N'PROCEDIMENTOS BEIRA-LEITO PADRÃO'
ORDER BY created_at;

IF @template_id IS NOT NULL
BEGIN
    SELECT @version_number = ISNULL(MAX(version_number), 0) + 1
    FROM dbo.form_template_version
    WHERE template_id = @template_id;

    UPDATE dbo.form_template_version
    SET status = 'RETIRED'
    WHERE template_id = @template_id
      AND status = 'PUBLISHED';

    INSERT INTO dbo.form_template_version
        (id, template_id, version_number, status, created_by, published_by, published_at)
    VALUES
        (@version_id, @template_id, @version_number, 'PUBLISHED', @system_actor, @system_actor, SYSUTCDATETIME());

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
        N'CONTEXTO, SOLICITAÇÃO, EXECUÇÃO E RASTREABILIDADE DE PROCEDIMENTOS',
        'PROCEDURE_PLAN',
        1,
        10,
        1,
        N'SELECIONE O CONTEXTO, O PROCEDIMENTO E CONFIRA AS TRAVAS DE SEGURANÇA',
        NULL
    );
END;
