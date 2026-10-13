SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;

ALTER TABLE dbo.patient ADD
    cns VARCHAR(20) NULL,
    mother_name NVARCHAR(180) NULL;
GO

ALTER TABLE dbo.patient ADD CONSTRAINT ck_patient_cns
    CHECK (cns IS NULL OR (LEN(cns) BETWEEN 1 AND 20 AND cns NOT LIKE '%[^0-9]%'));
GO

DROP INDEX uq_admission_active_bed ON dbo.admission;
ALTER TABLE dbo.admission ALTER COLUMN current_bed_id UNIQUEIDENTIFIER NULL;
CREATE UNIQUE INDEX uq_admission_active_bed
    ON dbo.admission (current_bed_id)
    WHERE status = 'ACTIVE' AND current_bed_id IS NOT NULL;

ALTER TABLE dbo.admission ADD
    provisional_care_unit_id UNIQUEIDENTIFIER NULL,
    admission_origin NVARCHAR(200) NULL,
    allocated_at DATETIMEOFFSET(7) NULL;
GO

ALTER TABLE dbo.admission ADD CONSTRAINT fk_admission_provisional_care_unit
    FOREIGN KEY (provisional_care_unit_id) REFERENCES dbo.care_unit (id);

ALTER TABLE dbo.admission DROP CONSTRAINT ck_admission_discharge_reason;
ALTER TABLE dbo.admission ADD CONSTRAINT ck_admission_discharge_reason CHECK (
    discharge_reason IS NULL OR discharge_reason IN (
        'OBITO', 'TRANSFERENCIA', 'ALTA_MELHORA', 'CANCELAMENTO'
    )
);

ALTER TABLE dbo.admission ADD CONSTRAINT ck_admission_active_location CHECK (
    status <> 'ACTIVE' OR current_bed_id IS NOT NULL OR provisional_care_unit_id IS NOT NULL
);

CREATE INDEX ix_admission_provisional_active
    ON dbo.admission (provisional_care_unit_id, admitted_at)
    WHERE status = 'ACTIVE' AND current_bed_id IS NULL;
