ALTER TABLE dbo.hospital ADD
    technical_code VARCHAR(24) NULL,
    status VARCHAR(32) NULL,
    provisioning_error NVARCHAR(1000) NULL,
    provisioned_at DATETIMEOFFSET(7) NULL,
    row_version BIGINT NOT NULL CONSTRAINT df_hospital_row_version DEFAULT 0;

EXEC sys.sp_executesql N'
    UPDATE dbo.hospital
    SET
        technical_code = ''H'' + LEFT(REPLACE(CONVERT(VARCHAR(36), id), ''-'', ''''), 16),
        status = ''PROVISIONING_FAILED'',
        provisioning_error = ''BANCO HOSPITALAR AINDA NÃO PROVISIONADO'',
        provisioned_at = NULL,
        active = 0;
';

ALTER TABLE dbo.hospital ALTER COLUMN technical_code VARCHAR(24) NOT NULL;
ALTER TABLE dbo.hospital ALTER COLUMN status VARCHAR(32) NOT NULL;

ALTER TABLE dbo.hospital ADD
    CONSTRAINT uq_hospital_technical_code UNIQUE (technical_code),
    CONSTRAINT ck_hospital_technical_code
        CHECK (technical_code NOT LIKE '%[^A-Z0-9]%' AND LEN(technical_code) BETWEEN 8 AND 24),
    CONSTRAINT ck_hospital_status
        CHECK (status IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'PROVISIONING_FAILED'));

ALTER TABLE dbo.app_user ADD
    must_change_password BIT NOT NULL
        CONSTRAINT df_app_user_must_change_password DEFAULT 0,
    row_version BIGINT NOT NULL
        CONSTRAINT df_app_user_row_version DEFAULT 0;

CREATE TABLE dbo.hospital_database_secret (
    hospital_id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_hospital_database_secret PRIMARY KEY,
    login_name VARCHAR(128) NOT NULL,
    encrypted_password VARCHAR(2000) NOT NULL,
    encryption_key_version SMALLINT NOT NULL
        CONSTRAINT df_hospital_database_secret_key_version DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_hospital_database_secret_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_hospital_database_secret_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_hospital_database_secret_hospital
        FOREIGN KEY (hospital_id) REFERENCES dbo.hospital (id),
    CONSTRAINT uq_hospital_database_secret_login UNIQUE (login_name)
);

CREATE INDEX ix_hospital_status_name
    ON dbo.hospital (status, name);

CREATE INDEX ix_app_user_active_name
    ON dbo.app_user (active, full_name);
