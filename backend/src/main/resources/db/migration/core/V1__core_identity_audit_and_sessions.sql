CREATE TABLE dbo.hospital (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_hospital PRIMARY KEY
        CONSTRAINT df_hospital_id DEFAULT NEWSEQUENTIALID(),
    name NVARCHAR(180) NOT NULL,
    short_name NVARCHAR(40) NOT NULL,
    city NVARCHAR(120) NOT NULL,
    database_name VARCHAR(128) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_hospital_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_hospital_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_hospital_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_hospital_name UNIQUE (name),
    CONSTRAINT uq_hospital_database_name UNIQUE (database_name),
    CONSTRAINT ck_hospital_database_name
        CHECK (database_name NOT LIKE '%[^a-z0-9_]%' AND LEN(database_name) BETWEEN 3 AND 128)
);

CREATE TABLE dbo.app_user (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_app_user PRIMARY KEY
        CONSTRAINT df_app_user_id DEFAULT NEWSEQUENTIALID(),
    full_name NVARCHAR(160) NOT NULL,
    email NVARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_app_user_active DEFAULT 1,
    password_changed_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_app_user_password_changed_at DEFAULT SYSUTCDATETIME(),
    last_login_at DATETIMEOFFSET(7) NULL,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_app_user_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_app_user_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_app_user_email UNIQUE (email)
);

CREATE TABLE dbo.system_user_role (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_system_user_role PRIMARY KEY
        CONSTRAINT df_system_user_role_id DEFAULT NEWSEQUENTIALID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    role VARCHAR(40) NOT NULL,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_system_user_role_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_system_user_role_user
        FOREIGN KEY (user_id) REFERENCES dbo.app_user (id),
    CONSTRAINT ck_system_user_role_role CHECK (role IN ('ADMIN_SISTEMA')),
    CONSTRAINT uq_system_user_role_user_role UNIQUE (user_id, role)
);

CREATE INDEX ix_system_user_role_user
    ON dbo.system_user_role (user_id);

CREATE TABLE dbo.hospital_membership (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_hospital_membership PRIMARY KEY
        CONSTRAINT df_hospital_membership_id DEFAULT NEWSEQUENTIALID(),
    hospital_id UNIQUEIDENTIFIER NOT NULL,
    user_id UNIQUEIDENTIFIER NOT NULL,
    role VARCHAR(40) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_hospital_membership_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_hospital_membership_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_hospital_membership_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_hospital_membership_hospital
        FOREIGN KEY (hospital_id) REFERENCES dbo.hospital (id),
    CONSTRAINT fk_hospital_membership_user
        FOREIGN KEY (user_id) REFERENCES dbo.app_user (id),
    CONSTRAINT ck_hospital_membership_role
        CHECK (role IN (
            'ADMIN_HOSPITAL',
            'RESPONSAVEL_CLINICO',
            'MEDICO',
            'ENFERMAGEM',
            'RECEPCAO'
        )),
    CONSTRAINT uq_hospital_membership_hospital_user UNIQUE (hospital_id, user_id)
);

CREATE INDEX ix_hospital_membership_user
    ON dbo.hospital_membership (user_id, active);

CREATE TABLE dbo.audit_event (
    id BIGINT IDENTITY(1, 1) NOT NULL
        CONSTRAINT pk_audit_event PRIMARY KEY,
    hospital_id UNIQUEIDENTIFIER NULL,
    actor_user_id UNIQUEIDENTIFIER NULL,
    event_type VARCHAR(80) NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    source_ip VARCHAR(64) NULL,
    user_agent NVARCHAR(512) NULL,
    details_json NVARCHAR(MAX) NULL,
    occurred_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_audit_event_occurred_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_audit_event_hospital
        FOREIGN KEY (hospital_id) REFERENCES dbo.hospital (id),
    CONSTRAINT fk_audit_event_user
        FOREIGN KEY (actor_user_id) REFERENCES dbo.app_user (id),
    CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED')),
    CONSTRAINT ck_audit_event_details_json
        CHECK (details_json IS NULL OR ISJSON(details_json) = 1)
);

CREATE INDEX ix_audit_event_occurred_at
    ON dbo.audit_event (occurred_at DESC);

CREATE INDEX ix_audit_event_hospital_occurred_at
    ON dbo.audit_event (hospital_id, occurred_at DESC)
    WHERE hospital_id IS NOT NULL;

-- Estrutura oficial do Spring Session JDBC para SQL Server. A sessão autenticada
-- permanece no backend e o navegador recebe somente o identificador em cookie HttpOnly.
CREATE TABLE dbo.SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME NVARCHAR(100) NULL,
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1
    ON dbo.SPRING_SESSION (SESSION_ID);

CREATE INDEX SPRING_SESSION_IX2
    ON dbo.SPRING_SESSION (EXPIRY_TIME);

CREATE INDEX SPRING_SESSION_IX3
    ON dbo.SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE dbo.SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME NVARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES IMAGE NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK
        PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK
        FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES dbo.SPRING_SESSION (PRIMARY_ID)
        ON DELETE CASCADE
);
