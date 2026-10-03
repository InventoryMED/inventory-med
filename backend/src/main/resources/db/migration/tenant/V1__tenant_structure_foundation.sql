CREATE TABLE dbo.tenant_metadata (
    singleton_id TINYINT NOT NULL
        CONSTRAINT pk_tenant_metadata PRIMARY KEY,
    hospital_id UNIQUEIDENTIFIER NOT NULL,
    hospital_name NVARCHAR(180) NOT NULL,
    schema_version INT NOT NULL CONSTRAINT df_tenant_metadata_schema_version DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_tenant_metadata_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_tenant_metadata_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_tenant_metadata_singleton CHECK (singleton_id = 1)
);

CREATE TABLE dbo.care_unit (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_care_unit PRIMARY KEY
        CONSTRAINT df_care_unit_id DEFAULT NEWSEQUENTIALID(),
    name NVARCHAR(120) NOT NULL,
    code VARCHAR(30) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_care_unit_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_care_unit_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_care_unit_updated_at DEFAULT SYSUTCDATETIME(),
    row_version BIGINT NOT NULL CONSTRAINT df_care_unit_row_version DEFAULT 0,
    CONSTRAINT uq_care_unit_code UNIQUE (code),
    CONSTRAINT ck_care_unit_code
        CHECK (code NOT LIKE '%[^A-Z0-9_-]%' AND LEN(code) BETWEEN 2 AND 30)
);

CREATE TABLE dbo.room (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_room PRIMARY KEY
        CONSTRAINT df_room_id DEFAULT NEWSEQUENTIALID(),
    care_unit_id UNIQUEIDENTIFIER NOT NULL,
    name NVARCHAR(120) NOT NULL,
    code VARCHAR(30) NOT NULL,
    floor_name NVARCHAR(80) NULL,
    active BIT NOT NULL CONSTRAINT df_room_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_room_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_room_updated_at DEFAULT SYSUTCDATETIME(),
    row_version BIGINT NOT NULL CONSTRAINT df_room_row_version DEFAULT 0,
    CONSTRAINT fk_room_care_unit
        FOREIGN KEY (care_unit_id) REFERENCES dbo.care_unit (id),
    CONSTRAINT uq_room_care_unit_code UNIQUE (care_unit_id, code),
    CONSTRAINT ck_room_code
        CHECK (code NOT LIKE '%[^A-Z0-9_-]%' AND LEN(code) BETWEEN 1 AND 30)
);

CREATE INDEX ix_room_care_unit_active
    ON dbo.room (care_unit_id, active);

CREATE TABLE dbo.bed (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_bed PRIMARY KEY
        CONSTRAINT df_bed_id DEFAULT NEWSEQUENTIALID(),
    room_id UNIQUEIDENTIFIER NOT NULL,
    code VARCHAR(30) NOT NULL,
    status VARCHAR(24) NOT NULL CONSTRAINT df_bed_status DEFAULT 'AVAILABLE',
    active BIT NOT NULL CONSTRAINT df_bed_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_bed_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_bed_updated_at DEFAULT SYSUTCDATETIME(),
    row_version BIGINT NOT NULL CONSTRAINT df_bed_row_version DEFAULT 0,
    CONSTRAINT fk_bed_room
        FOREIGN KEY (room_id) REFERENCES dbo.room (id),
    CONSTRAINT uq_bed_room_code UNIQUE (room_id, code),
    CONSTRAINT ck_bed_code
        CHECK (code NOT LIKE '%[^A-Z0-9_-]%' AND LEN(code) BETWEEN 1 AND 30),
    CONSTRAINT ck_bed_status
        CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'CLEANING', 'MAINTENANCE', 'BLOCKED'))
);

CREATE INDEX ix_bed_room_status_active
    ON dbo.bed (room_id, status, active);

CREATE TABLE dbo.clinical_audit_event (
    id BIGINT IDENTITY(1, 1) NOT NULL
        CONSTRAINT pk_clinical_audit_event PRIMARY KEY,
    actor_user_id UNIQUEIDENTIFIER NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(80) NULL,
    outcome VARCHAR(20) NOT NULL,
    source_ip VARCHAR(64) NULL,
    details_json NVARCHAR(MAX) NULL,
    occurred_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_clinical_audit_event_occurred_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT ck_clinical_audit_event_outcome
        CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED')),
    CONSTRAINT ck_clinical_audit_event_details_json
        CHECK (details_json IS NULL OR ISJSON(details_json) = 1)
);

CREATE INDEX ix_clinical_audit_event_occurred_at
    ON dbo.clinical_audit_event (occurred_at DESC);
