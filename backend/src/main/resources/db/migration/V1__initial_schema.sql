CREATE TABLE dbo.hospital (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_hospital PRIMARY KEY
        CONSTRAINT df_hospital_id DEFAULT NEWSEQUENTIALID(),
    name NVARCHAR(180) NOT NULL,
    short_name NVARCHAR(40) NOT NULL,
    city NVARCHAR(120) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_hospital_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_hospital_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_hospital_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_hospital_name UNIQUE (name)
);

CREATE TABLE dbo.app_user (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_app_user PRIMARY KEY
        CONSTRAINT df_app_user_id DEFAULT NEWSEQUENTIALID(),
    full_name NVARCHAR(160) NOT NULL,
    email NVARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_app_user_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_app_user_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_app_user_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT uq_app_user_email UNIQUE (email)
);

CREATE TABLE dbo.hospital_membership (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_hospital_membership PRIMARY KEY
        CONSTRAINT df_hospital_membership_id DEFAULT NEWSEQUENTIALID(),
    hospital_id UNIQUEIDENTIFIER NOT NULL,
    user_id UNIQUEIDENTIFIER NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_hospital_membership_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_hospital_membership_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_hospital_membership_hospital
        FOREIGN KEY (hospital_id) REFERENCES dbo.hospital (id),
    CONSTRAINT fk_hospital_membership_user
        FOREIGN KEY (user_id) REFERENCES dbo.app_user (id),
    CONSTRAINT ck_hospital_membership_role
        CHECK (role IN ('DOCTOR', 'NURSE', 'ADMIN')),
    CONSTRAINT uq_hospital_membership_hospital_user UNIQUE (hospital_id, user_id)
);

CREATE INDEX ix_hospital_membership_user
    ON dbo.hospital_membership (user_id, active);

CREATE TABLE dbo.room (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_room PRIMARY KEY
        CONSTRAINT df_room_id DEFAULT NEWSEQUENTIALID(),
    hospital_id UNIQUEIDENTIFIER NOT NULL,
    name NVARCHAR(100) NOT NULL,
    floor NVARCHAR(80) NULL,
    unit_name NVARCHAR(120) NOT NULL,
    active BIT NOT NULL CONSTRAINT df_room_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_room_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_room_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_room_hospital FOREIGN KEY (hospital_id) REFERENCES dbo.hospital (id),
    CONSTRAINT uq_room_hospital_id_id UNIQUE (hospital_id, id),
    CONSTRAINT uq_room_hospital_name UNIQUE (hospital_id, name)
);

CREATE TABLE dbo.bed (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_bed PRIMARY KEY
        CONSTRAINT df_bed_id DEFAULT NEWSEQUENTIALID(),
    hospital_id UNIQUEIDENTIFIER NOT NULL,
    room_id UNIQUEIDENTIFIER NOT NULL,
    code NVARCHAR(60) NOT NULL,
    operational_status VARCHAR(20) NOT NULL
        CONSTRAINT df_bed_operational_status DEFAULT 'AVAILABLE',
    active BIT NOT NULL CONSTRAINT df_bed_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_bed_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_bed_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_bed_room_same_hospital
        FOREIGN KEY (hospital_id, room_id) REFERENCES dbo.room (hospital_id, id),
    CONSTRAINT ck_bed_operational_status
        CHECK (operational_status IN ('AVAILABLE', 'CLEANING', 'MAINTENANCE')),
    CONSTRAINT uq_bed_hospital_id_id UNIQUE (hospital_id, id),
    CONSTRAINT uq_bed_room_code UNIQUE (room_id, code)
);

CREATE INDEX ix_bed_hospital_room
    ON dbo.bed (hospital_id, room_id, active);

CREATE TABLE dbo.patient (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_patient PRIMARY KEY
        CONSTRAINT df_patient_id DEFAULT NEWSEQUENTIALID(),
    hospital_id UNIQUEIDENTIFIER NOT NULL,
    full_name NVARCHAR(180) NOT NULL,
    birth_date DATE NULL,
    sex VARCHAR(30) NULL,
    weight_kg DECIMAL(6, 2) NULL,
    diagnosis NVARCHAR(1000) NULL,
    comorbidities NVARCHAR(2000) NULL,
    allergies NVARCHAR(2000) NULL,
    active BIT NOT NULL CONSTRAINT df_patient_active DEFAULT 1,
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_patient_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_patient_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_patient_hospital FOREIGN KEY (hospital_id) REFERENCES dbo.hospital (id),
    CONSTRAINT ck_patient_weight CHECK (weight_kg IS NULL OR weight_kg > 0),
    CONSTRAINT uq_patient_hospital_id_id UNIQUE (hospital_id, id)
);

CREATE INDEX ix_patient_hospital_name
    ON dbo.patient (hospital_id, full_name);

CREATE TABLE dbo.admission (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_admission PRIMARY KEY
        CONSTRAINT df_admission_id DEFAULT NEWSEQUENTIALID(),
    hospital_id UNIQUEIDENTIFIER NOT NULL,
    patient_id UNIQUEIDENTIFIER NOT NULL,
    bed_id UNIQUEIDENTIFIER NOT NULL,
    admitted_by_user_id UNIQUEIDENTIFIER NOT NULL,
    admitted_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_admission_admitted_at DEFAULT SYSUTCDATETIME(),
    discharged_at DATETIMEOFFSET(7) NULL,
    discharge_reason VARCHAR(40) NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT df_admission_status DEFAULT 'ACTIVE',
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_admission_created_at DEFAULT SYSUTCDATETIME(),
    updated_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_admission_updated_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_admission_patient_same_hospital
        FOREIGN KEY (hospital_id, patient_id) REFERENCES dbo.patient (hospital_id, id),
    CONSTRAINT fk_admission_bed_same_hospital
        FOREIGN KEY (hospital_id, bed_id) REFERENCES dbo.bed (hospital_id, id),
    CONSTRAINT fk_admission_professional_same_hospital
        FOREIGN KEY (hospital_id, admitted_by_user_id)
        REFERENCES dbo.hospital_membership (hospital_id, user_id),
    CONSTRAINT ck_admission_status CHECK (status IN ('ACTIVE', 'DISCHARGED', 'TRANSFERRED')),
    CONSTRAINT ck_admission_discharge_reason
        CHECK (
            discharge_reason IS NULL
            OR discharge_reason IN ('DEATH', 'TRANSFER', 'CLINICAL_IMPROVEMENT')
        ),
    CONSTRAINT uq_admission_hospital_id_id UNIQUE (hospital_id, id)
);

CREATE UNIQUE INDEX uq_admission_active_bed
    ON dbo.admission (hospital_id, bed_id)
    WHERE status = 'ACTIVE';

CREATE UNIQUE INDEX uq_admission_active_patient
    ON dbo.admission (hospital_id, patient_id)
    WHERE status = 'ACTIVE';

CREATE TABLE dbo.prescription (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_prescription PRIMARY KEY
        CONSTRAINT df_prescription_id DEFAULT NEWSEQUENTIALID(),
    hospital_id UNIQUEIDENTIFIER NOT NULL,
    admission_id UNIQUEIDENTIFIER NOT NULL,
    author_user_id UNIQUEIDENTIFIER NOT NULL,
    version_number INT NOT NULL CONSTRAINT df_prescription_version DEFAULT 1,
    status VARCHAR(20) NOT NULL CONSTRAINT df_prescription_status DEFAULT 'DRAFT',
    diet NVARCHAR(1000) NULL,
    observations NVARCHAR(MAX) NULL,
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_prescription_created_at DEFAULT SYSUTCDATETIME(),
    signed_at DATETIMEOFFSET(7) NULL,
    CONSTRAINT fk_prescription_admission_same_hospital
        FOREIGN KEY (hospital_id, admission_id) REFERENCES dbo.admission (hospital_id, id),
    CONSTRAINT fk_prescription_author_same_hospital
        FOREIGN KEY (hospital_id, author_user_id)
        REFERENCES dbo.hospital_membership (hospital_id, user_id),
    CONSTRAINT ck_prescription_status CHECK (status IN ('DRAFT', 'SIGNED', 'CANCELLED')),
    CONSTRAINT ck_prescription_version CHECK (version_number > 0),
    CONSTRAINT uq_prescription_hospital_id_id UNIQUE (hospital_id, id),
    CONSTRAINT uq_prescription_admission_version UNIQUE (admission_id, version_number)
);

CREATE TABLE dbo.prescription_item (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_prescription_item PRIMARY KEY
        CONSTRAINT df_prescription_item_id DEFAULT NEWSEQUENTIALID(),
    hospital_id UNIQUEIDENTIFIER NOT NULL,
    prescription_id UNIQUEIDENTIFIER NOT NULL,
    section_name NVARCHAR(80) NOT NULL,
    position INT NOT NULL,
    description NVARCHAR(2000) NOT NULL,
    route NVARCHAR(40) NULL,
    frequency NVARCHAR(80) NULL,
    scheduling VARCHAR(20) NULL,
    created_at DATETIMEOFFSET(7) NOT NULL
        CONSTRAINT df_prescription_item_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_prescription_item_prescription_same_hospital
        FOREIGN KEY (hospital_id, prescription_id)
        REFERENCES dbo.prescription (hospital_id, id),
    CONSTRAINT ck_prescription_item_position CHECK (position > 0),
    CONSTRAINT ck_prescription_item_scheduling
        CHECK (scheduling IS NULL OR scheduling IN ('ACM', 'SN', 'FIXED')),
    CONSTRAINT uq_prescription_item_position UNIQUE (prescription_id, position)
);

CREATE TABLE dbo.evolution (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_evolution PRIMARY KEY
        CONSTRAINT df_evolution_id DEFAULT NEWSEQUENTIALID(),
    hospital_id UNIQUEIDENTIFIER NOT NULL,
    admission_id UNIQUEIDENTIFIER NOT NULL,
    author_user_id UNIQUEIDENTIFIER NOT NULL,
    admission_notes NVARCHAR(MAX) NULL,
    evolution_notes NVARCHAR(MAX) NULL,
    physical_exam_json NVARCHAR(MAX) NULL,
    complementary_exams_json NVARCHAR(MAX) NULL,
    conduct NVARCHAR(MAX) NULL,
    status VARCHAR(20) NOT NULL CONSTRAINT df_evolution_status DEFAULT 'DRAFT',
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_evolution_created_at DEFAULT SYSUTCDATETIME(),
    signed_at DATETIMEOFFSET(7) NULL,
    CONSTRAINT fk_evolution_admission_same_hospital
        FOREIGN KEY (hospital_id, admission_id) REFERENCES dbo.admission (hospital_id, id),
    CONSTRAINT fk_evolution_author_same_hospital
        FOREIGN KEY (hospital_id, author_user_id)
        REFERENCES dbo.hospital_membership (hospital_id, user_id),
    CONSTRAINT ck_evolution_physical_exam_json
        CHECK (physical_exam_json IS NULL OR ISJSON(physical_exam_json) = 1),
    CONSTRAINT ck_evolution_complementary_exams_json
        CHECK (complementary_exams_json IS NULL OR ISJSON(complementary_exams_json) = 1),
    CONSTRAINT ck_evolution_status CHECK (status IN ('DRAFT', 'SIGNED', 'CANCELLED'))
);

CREATE INDEX ix_evolution_hospital_admission_created_at
    ON dbo.evolution (hospital_id, admission_id, created_at DESC);

CREATE TABLE dbo.refresh_token (
    id UNIQUEIDENTIFIER NOT NULL
        CONSTRAINT pk_refresh_token PRIMARY KEY
        CONSTRAINT df_refresh_token_id DEFAULT NEWSEQUENTIALID(),
    user_id UNIQUEIDENTIFIER NOT NULL,
    hospital_id UNIQUEIDENTIFIER NULL,
    token_hash BINARY(32) NOT NULL,
    expires_at DATETIMEOFFSET(7) NOT NULL,
    revoked_at DATETIMEOFFSET(7) NULL,
    created_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_refresh_token_created_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES dbo.app_user (id),
    CONSTRAINT fk_refresh_token_hospital FOREIGN KEY (hospital_id) REFERENCES dbo.hospital (id),
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_refresh_token_user_expires_at
    ON dbo.refresh_token (user_id, expires_at);

CREATE TABLE dbo.audit_event (
    id BIGINT IDENTITY(1, 1) NOT NULL CONSTRAINT pk_audit_event PRIMARY KEY,
    hospital_id UNIQUEIDENTIFIER NULL,
    actor_user_id UNIQUEIDENTIFIER NULL,
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NULL,
    entity_id UNIQUEIDENTIFIER NULL,
    source_ip VARCHAR(64) NULL,
    details_json NVARCHAR(MAX) NULL,
    occurred_at DATETIMEOFFSET(7) NOT NULL CONSTRAINT df_audit_event_occurred_at DEFAULT SYSUTCDATETIME(),
    CONSTRAINT fk_audit_event_hospital FOREIGN KEY (hospital_id) REFERENCES dbo.hospital (id),
    CONSTRAINT fk_audit_event_user FOREIGN KEY (actor_user_id) REFERENCES dbo.app_user (id),
    CONSTRAINT ck_audit_event_details_json
        CHECK (details_json IS NULL OR ISJSON(details_json) = 1)
);

CREATE INDEX ix_audit_event_hospital_occurred_at
    ON dbo.audit_event (hospital_id, occurred_at DESC);
