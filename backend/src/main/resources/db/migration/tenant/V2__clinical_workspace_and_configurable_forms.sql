SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

ALTER TABLE dbo.care_unit ADD
    display_order INT NOT NULL CONSTRAINT df_care_unit_display_order DEFAULT 0;

ALTER TABLE dbo.room ADD
    display_order INT NOT NULL CONSTRAINT df_room_display_order DEFAULT 0;

ALTER TABLE dbo.bed ADD
    display_order INT NOT NULL CONSTRAINT df_bed_display_order DEFAULT 0;

GO

ALTER TABLE dbo.care_unit ADD
    CONSTRAINT ck_care_unit_display_order CHECK (display_order >= 0);
ALTER TABLE dbo.room ADD
    CONSTRAINT ck_room_display_order CHECK (display_order >= 0);
ALTER TABLE dbo.bed ADD
    CONSTRAINT ck_bed_display_order CHECK (display_order >= 0);

CREATE TABLE dbo.patient (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_patient PRIMARY KEY
        CONSTRAINT df_patient_id DEFAULT NEWSEQUENTIALID(),
    full_name NVARCHAR(180) NOT NULL,
    birth_date DATE NULL,
    sex VARCHAR(24) NOT NULL CONSTRAINT df_patient_sex DEFAULT 'NAO_INFORMADO',
    weight_kg DECIMAL(6, 2) NULL,
    diagnosis NVARCHAR(1000) NULL,
    comorbidities NVARCHAR(2000) NULL,
    allergies NVARCHAR(2000) NULL,
    created_by UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_patient_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_patient_updated_at DEFAULT SYSUTCDATETIME(),
    row_version BIGINT NOT NULL CONSTRAINT df_patient_row_version DEFAULT 0,
    CONSTRAINT ck_patient_name CHECK (LEN(LTRIM(RTRIM(full_name))) BETWEEN 2 AND 180),
    CONSTRAINT ck_patient_sex CHECK (sex IN ('FEMININO', 'MASCULINO', 'OUTRO', 'NAO_INFORMADO')),
    CONSTRAINT ck_patient_weight CHECK (weight_kg IS NULL OR weight_kg BETWEEN 0.10 AND 500.00)
);

CREATE TABLE dbo.admission (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_admission PRIMARY KEY
        CONSTRAINT df_admission_id DEFAULT NEWSEQUENTIALID(),
    patient_id UNIQUEIDENTIFIER NOT NULL,
    current_bed_id UNIQUEIDENTIFIER NOT NULL,
    status VARCHAR(24) NOT NULL CONSTRAINT df_admission_status DEFAULT 'ACTIVE',
    admitted_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_admission_admitted_at DEFAULT SYSUTCDATETIME(),
    discharged_at DATETIMEOFFSET(7) NULL,
    discharge_reason VARCHAR(40) NULL,
    created_by UNIQUEIDENTIFIER NOT NULL,
    updated_by UNIQUEIDENTIFIER NOT NULL,
    updated_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_admission_updated_at DEFAULT SYSUTCDATETIME(),
    row_version BIGINT NOT NULL CONSTRAINT df_admission_row_version DEFAULT 0,
    CONSTRAINT fk_admission_patient FOREIGN KEY (patient_id) REFERENCES dbo.patient (id),
    CONSTRAINT fk_admission_bed FOREIGN KEY (current_bed_id) REFERENCES dbo.bed (id),
    CONSTRAINT ck_admission_status CHECK (status IN ('ACTIVE', 'DISCHARGED')),
    CONSTRAINT ck_admission_discharge_reason CHECK (
        discharge_reason IS NULL OR discharge_reason IN ('OBITO', 'TRANSFERENCIA', 'ALTA_MELHORA')
    )
);

CREATE UNIQUE INDEX uq_admission_active_bed
    ON dbo.admission (current_bed_id)
    WHERE status = 'ACTIVE';
CREATE INDEX ix_admission_patient_status ON dbo.admission (patient_id, status);

CREATE TABLE dbo.form_template (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_form_template PRIMARY KEY
        CONSTRAINT df_form_template_id DEFAULT NEWSEQUENTIALID(),
    kind VARCHAR(24) NOT NULL,
    name NVARCHAR(160) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_form_template_active DEFAULT 1,
    created_by UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_form_template_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_form_template_updated_at DEFAULT SYSUTCDATETIME(),
    row_version BIGINT NOT NULL CONSTRAINT df_form_template_row_version DEFAULT 0,
    CONSTRAINT ck_form_template_kind CHECK (kind IN ('PRESCRIPTION', 'EVOLUTION')),
    CONSTRAINT uq_form_template_kind_name UNIQUE (kind, name)
);

CREATE TABLE dbo.form_template_version (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_form_template_version PRIMARY KEY
        CONSTRAINT df_form_template_version_id DEFAULT NEWSEQUENTIALID(),
    template_id UNIQUEIDENTIFIER NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(24) NOT NULL CONSTRAINT df_form_template_version_status DEFAULT 'DRAFT',
    created_by UNIQUEIDENTIFIER NOT NULL,
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_form_template_version_created_at DEFAULT SYSUTCDATETIME(),
    published_by UNIQUEIDENTIFIER NULL,
    published_at DATETIMEOFFSET(7) NULL,
    CONSTRAINT fk_form_template_version_template FOREIGN KEY (template_id) REFERENCES dbo.form_template (id),
    CONSTRAINT ck_form_template_version_number CHECK (version_number > 0),
    CONSTRAINT ck_form_template_version_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'RETIRED')),
    CONSTRAINT uq_form_template_version UNIQUE (template_id, version_number)
);

CREATE UNIQUE INDEX uq_form_template_single_draft
    ON dbo.form_template_version (template_id)
    WHERE status = 'DRAFT';
CREATE UNIQUE INDEX uq_form_template_single_published
    ON dbo.form_template_version (template_id)
    WHERE status = 'PUBLISHED';

CREATE TABLE dbo.form_section (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_form_section PRIMARY KEY
        CONSTRAINT df_form_section_id DEFAULT NEWSEQUENTIALID(),
    version_id UNIQUEIDENTIFIER NOT NULL,
    section_key VARCHAR(60) NOT NULL,
    title NVARCHAR(160) NOT NULL,
    display_order INT NOT NULL,
    active BIT NOT NULL CONSTRAINT df_form_section_active DEFAULT 1,
    CONSTRAINT fk_form_section_version FOREIGN KEY (version_id) REFERENCES dbo.form_template_version (id),
    CONSTRAINT ck_form_section_key CHECK (section_key NOT LIKE '%[^A-Z0-9_]%' AND LEN(section_key) BETWEEN 2 AND 60),
    CONSTRAINT ck_form_section_order CHECK (display_order >= 0),
    CONSTRAINT uq_form_section_key UNIQUE (version_id, section_key)
);

CREATE TABLE dbo.form_field (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_form_field PRIMARY KEY
        CONSTRAINT df_form_field_id DEFAULT NEWSEQUENTIALID(),
    section_id UNIQUEIDENTIFIER NOT NULL,
    field_key VARCHAR(60) NOT NULL,
    label NVARCHAR(160) NOT NULL,
    field_type VARCHAR(32) NOT NULL,
    required BIT NOT NULL CONSTRAINT df_form_field_required DEFAULT 0,
    display_order INT NOT NULL,
    active BIT NOT NULL CONSTRAINT df_form_field_active DEFAULT 1,
    placeholder NVARCHAR(200) NULL,
    max_length INT NULL,
    CONSTRAINT fk_form_field_section FOREIGN KEY (section_id) REFERENCES dbo.form_section (id),
    CONSTRAINT ck_form_field_key CHECK (field_key NOT LIKE '%[^A-Z0-9_]%' AND LEN(field_key) BETWEEN 2 AND 60),
    CONSTRAINT ck_form_field_type CHECK (field_type IN (
        'SHORT_TEXT', 'LONG_TEXT', 'INTEGER', 'DECIMAL', 'DATE', 'TIME',
        'SINGLE_SELECT', 'MULTI_SELECT', 'BOOLEAN', 'MEDICATION_LINE', 'CLINICAL_TABLE'
    )),
    CONSTRAINT ck_form_field_order CHECK (display_order >= 0),
    CONSTRAINT ck_form_field_max_length CHECK (max_length IS NULL OR max_length BETWEEN 1 AND 10000),
    CONSTRAINT uq_form_field_key UNIQUE (section_id, field_key)
);

CREATE TABLE dbo.form_field_option (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_form_field_option PRIMARY KEY
        CONSTRAINT df_form_field_option_id DEFAULT NEWSEQUENTIALID(),
    field_id UNIQUEIDENTIFIER NOT NULL,
    option_value NVARCHAR(120) NOT NULL,
    option_label NVARCHAR(160) NOT NULL,
    display_order INT NOT NULL,
    active BIT NOT NULL CONSTRAINT df_form_field_option_active DEFAULT 1,
    CONSTRAINT fk_form_field_option_field FOREIGN KEY (field_id) REFERENCES dbo.form_field (id),
    CONSTRAINT ck_form_field_option_order CHECK (display_order >= 0),
    CONSTRAINT uq_form_field_option_value UNIQUE (field_id, option_value)
);

CREATE TABLE dbo.clinical_document (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_clinical_document PRIMARY KEY
        CONSTRAINT df_clinical_document_id DEFAULT NEWSEQUENTIALID(),
    admission_id UNIQUEIDENTIFIER NOT NULL,
    kind VARCHAR(24) NOT NULL,
    template_version_id UNIQUEIDENTIFIER NOT NULL,
    status VARCHAR(24) NOT NULL CONSTRAINT df_clinical_document_status DEFAULT 'DRAFT',
    version_number INT NOT NULL CONSTRAINT df_clinical_document_version DEFAULT 1,
    previous_document_id UNIQUEIDENTIFIER NULL,
    author_user_id UNIQUEIDENTIFIER NOT NULL,
    content_json NVARCHAR(MAX) NOT NULL,
    template_snapshot_json NVARCHAR(MAX) NOT NULL,
    amendment_reason NVARCHAR(1000) NULL,
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_clinical_document_created_at DEFAULT SYSUTCDATETIME(),
    finalized_at DATETIMEOFFSET(7) NULL,
    row_version BIGINT NOT NULL CONSTRAINT df_clinical_document_row_version DEFAULT 0,
    CONSTRAINT fk_clinical_document_admission FOREIGN KEY (admission_id) REFERENCES dbo.admission (id),
    CONSTRAINT fk_clinical_document_template_version FOREIGN KEY (template_version_id) REFERENCES dbo.form_template_version (id),
    CONSTRAINT fk_clinical_document_previous FOREIGN KEY (previous_document_id) REFERENCES dbo.clinical_document (id),
    CONSTRAINT ck_clinical_document_kind CHECK (kind IN ('PRESCRIPTION', 'EVOLUTION')),
    CONSTRAINT ck_clinical_document_status CHECK (status IN ('DRAFT', 'FINALIZED', 'SUPERSEDED', 'CANCELLED')),
    CONSTRAINT ck_clinical_document_version CHECK (version_number > 0),
    CONSTRAINT ck_clinical_document_content_json CHECK (ISJSON(content_json) = 1),
    CONSTRAINT ck_clinical_document_snapshot_json CHECK (ISJSON(template_snapshot_json) = 1)
);

CREATE INDEX ix_clinical_document_admission_kind
    ON dbo.clinical_document (admission_id, kind, created_at DESC);
