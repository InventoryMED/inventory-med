package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.hospitals.BedStatus;
import br.com.inventorymed.tenancy.TenantJdbcExecutor;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

@Service
public class MedicalWorkspaceService {

    private final TenantJdbcExecutor tenantJdbc;
    private final ClinicalAuditWriter auditWriter;

    public MedicalWorkspaceService(
        TenantJdbcExecutor tenantJdbc,
        ClinicalAuditWriter auditWriter
    ) {
        this.tenantJdbc = tenantJdbc;
        this.auditWriter = auditWriter;
    }

    public MedicalWorkspaceResponse workspace(UUID hospitalId) {
        return tenantJdbc.read(hospitalId, jdbc -> loadWorkspace(jdbc, hospitalId));
    }

    public MedicalWorkspaceResponse.Admission admit(
        UUID hospitalId,
        UUID actorId,
        ClinicalRequests.AdmitPatient request,
        String sourceIp
    ) {
        try {
            UUID admissionId = tenantJdbc.write(hospitalId, jdbc -> {
                UUID patientId = UUID.randomUUID();
                UUID createdAdmissionId = UUID.randomUUID();
                int reserved = jdbc.update(
                    "UPDATE dbo.bed SET status = 'OCCUPIED', updated_at = SYSUTCDATETIME(), " +
                    "row_version = row_version + 1 WHERE id = ? AND active = 1 AND status = 'AVAILABLE' " +
                    "AND EXISTS (SELECT 1 FROM dbo.room r JOIN dbo.care_unit u ON u.id = r.care_unit_id " +
                    "WHERE r.id = dbo.bed.room_id AND r.active = 1 AND u.active = 1)",
                    request.bedId()
                );
                if (reserved == 0) {
                    throw new BusinessValidationException("O leito não está disponível para admissão");
                }
                jdbc.update(
                    "INSERT INTO dbo.patient " +
                    "(id, full_name, birth_date, sex, weight_kg, diagnosis, comorbidities, allergies, created_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    patientId,
                    normalizeName(request.fullName()),
                    request.birthDate() == null ? null : Date.valueOf(request.birthDate()),
                    request.sex(),
                    request.weightKg(),
                    normalizeNullable(request.diagnosis()),
                    normalizeNullable(request.comorbidities()),
                    normalizeNullable(request.allergies()),
                    actorId
                );
                jdbc.update(
                    "INSERT INTO dbo.admission " +
                    "(id, patient_id, current_bed_id, status, created_by, updated_by) " +
                    "VALUES (?, ?, ?, 'ACTIVE', ?, ?)",
                    createdAdmissionId,
                    patientId,
                    request.bedId(),
                    actorId,
                    actorId
                );
                auditWriter.success(
                    jdbc,
                    actorId,
                    "PATIENT_ADMITTED",
                    "ADMISSION",
                    createdAdmissionId,
                    sourceIp,
                    Map.of("bedId", request.bedId(), "patientId", patientId)
                );
                return createdAdmissionId;
            });
            return requiredAdmission(hospitalId, admissionId);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessValidationException("Não foi possível admitir o paciente neste leito");
        }
    }

    public MedicalWorkspaceResponse.Admission transfer(
        UUID hospitalId,
        UUID actorId,
        UUID admissionId,
        UUID targetBedId,
        String sourceIp
    ) {
        tenantJdbc.write(hospitalId, jdbc -> {
            UUID sourceBedId = requiredActiveBed(jdbc, admissionId);
            if (sourceBedId.equals(targetBedId)) {
                throw new BusinessValidationException("Selecione outro leito para a transferência");
            }
            int reserved = jdbc.update(
                "UPDATE dbo.bed SET status = 'OCCUPIED', updated_at = SYSUTCDATETIME(), " +
                "row_version = row_version + 1 WHERE id = ? AND active = 1 AND status = 'AVAILABLE' " +
                "AND EXISTS (SELECT 1 FROM dbo.room r JOIN dbo.care_unit u ON u.id = r.care_unit_id " +
                "WHERE r.id = dbo.bed.room_id AND r.active = 1 AND u.active = 1)",
                targetBedId
            );
            if (reserved == 0) {
                throw new BusinessValidationException("O leito de destino não está disponível");
            }
            jdbc.update(
                "UPDATE dbo.admission SET current_bed_id = ?, updated_by = ?, " +
                "updated_at = SYSUTCDATETIME(), row_version = row_version + 1 " +
                "WHERE id = ? AND status = 'ACTIVE'",
                targetBedId,
                actorId,
                admissionId
            );
            jdbc.update(
                "UPDATE dbo.bed SET status = 'AVAILABLE', updated_at = SYSUTCDATETIME(), " +
                "row_version = row_version + 1 WHERE id = ?",
                sourceBedId
            );
            auditWriter.success(
                jdbc,
                actorId,
                "PATIENT_TRANSFERRED",
                "ADMISSION",
                admissionId,
                sourceIp,
                Map.of("sourceBedId", sourceBedId, "targetBedId", targetBedId)
            );
            return null;
        });
        return requiredAdmission(hospitalId, admissionId);
    }

    public void discharge(
        UUID hospitalId,
        UUID actorId,
        UUID admissionId,
        String reason,
        String sourceIp
    ) {
        tenantJdbc.write(hospitalId, jdbc -> {
            UUID bedId = requiredActiveBed(jdbc, admissionId);
            int updated = jdbc.update(
                "UPDATE dbo.admission SET status = 'DISCHARGED', discharged_at = SYSUTCDATETIME(), " +
                "discharge_reason = ?, updated_by = ?, updated_at = SYSUTCDATETIME(), " +
                "row_version = row_version + 1 WHERE id = ? AND status = 'ACTIVE'",
                reason,
                actorId,
                admissionId
            );
            if (updated == 0) {
                throw new BusinessValidationException("Internação ativa não encontrada");
            }
            jdbc.update(
                "UPDATE dbo.bed SET status = 'AVAILABLE', updated_at = SYSUTCDATETIME(), " +
                "row_version = row_version + 1 WHERE id = ?",
                bedId
            );
            auditWriter.success(
                jdbc,
                actorId,
                "PATIENT_DISCHARGED",
                "ADMISSION",
                admissionId,
                sourceIp,
                Map.of("bedId", bedId, "reason", reason)
            );
            return null;
        });
    }

    private MedicalWorkspaceResponse loadWorkspace(JdbcTemplate jdbc, UUID hospitalId) {
        Map<UUID, MutableUnit> units = new LinkedHashMap<>();
        Map<UUID, MutableRoom> rooms = new LinkedHashMap<>();
        jdbc.query(
            "SELECT id, name, code FROM dbo.care_unit WHERE active = 1 ORDER BY display_order, name",
            (RowCallbackHandler) row -> units.put(
                row.getObject("id", UUID.class),
                new MutableUnit(
                    row.getObject("id", UUID.class),
                    row.getString("name"),
                    row.getString("code")
                )
            )
        );
        jdbc.query(
            "SELECT id, care_unit_id, name, code, floor_name FROM dbo.room WHERE active = 1 " +
            "ORDER BY display_order, name",
            (RowCallbackHandler) row -> {
                MutableRoom room = new MutableRoom(
                    row.getObject("id", UUID.class),
                    row.getString("name"),
                    row.getString("code"),
                    row.getString("floor_name")
                );
                rooms.put(room.id, room);
                MutableUnit unit = units.get(row.getObject("care_unit_id", UUID.class));
                if (unit != null) unit.rooms.add(room);
            }
        );
        jdbc.query(
            "SELECT b.id AS bed_id, b.room_id, b.code AS bed_code, b.status, " +
            "a.id AS admission_id, a.admitted_at, p.id AS patient_id, p.full_name, " +
            "p.birth_date, p.sex, p.weight_kg, p.diagnosis, p.comorbidities, p.allergies " +
            "FROM dbo.bed b LEFT JOIN dbo.admission a ON a.current_bed_id = b.id AND a.status = 'ACTIVE' " +
            "LEFT JOIN dbo.patient p ON p.id = a.patient_id WHERE b.active = 1 " +
            "ORDER BY b.display_order, b.code",
            (RowCallbackHandler) row -> {
                MutableRoom room = rooms.get(row.getObject("room_id", UUID.class));
                if (room == null) return;
                UUID admissionId = row.getObject("admission_id", UUID.class);
                MedicalWorkspaceResponse.Admission admission = admissionId == null
                    ? null
                    : mapAdmission(row, admissionId);
                room.beds.add(
                    new MedicalWorkspaceResponse.Bed(
                        row.getObject("bed_id", UUID.class),
                        row.getString("bed_code"),
                        BedStatus.valueOf(row.getString("status")),
                        admission
                    )
                );
            }
        );
        return new MedicalWorkspaceResponse(
            hospitalId,
            units.values().stream().map(MutableUnit::response).toList()
        );
    }

    private MedicalWorkspaceResponse.Admission requiredAdmission(UUID hospitalId, UUID admissionId) {
        return tenantJdbc.read(hospitalId, jdbc -> jdbc.query(
            "SELECT a.id AS admission_id, a.admitted_at, p.id AS patient_id, p.full_name, " +
            "p.birth_date, p.sex, p.weight_kg, p.diagnosis, p.comorbidities, p.allergies " +
            "FROM dbo.admission a JOIN dbo.patient p ON p.id = a.patient_id " +
            "WHERE a.id = ? AND a.status = 'ACTIVE'",
            (row, number) -> mapAdmission(row, row.getObject("admission_id", UUID.class)),
            admissionId
        ).stream().findFirst().orElseThrow(() ->
            new BusinessValidationException("Internação ativa não encontrada")
        ));
    }

    private MedicalWorkspaceResponse.Admission mapAdmission(
        java.sql.ResultSet row,
        UUID admissionId
    ) throws java.sql.SQLException {
        Date birthDate = row.getDate("birth_date");
        return new MedicalWorkspaceResponse.Admission(
            admissionId,
            row.getObject("admitted_at", java.time.OffsetDateTime.class).toInstant(),
            new MedicalWorkspaceResponse.Patient(
                row.getObject("patient_id", UUID.class),
                row.getString("full_name"),
                birthDate == null ? null : birthDate.toLocalDate(),
                row.getString("sex"),
                row.getBigDecimal("weight_kg"),
                row.getString("diagnosis"),
                row.getString("comorbidities"),
                row.getString("allergies")
            )
        );
    }

    private UUID requiredActiveBed(JdbcTemplate jdbc, UUID admissionId) {
        List<UUID> beds = jdbc.query(
            "SELECT current_bed_id FROM dbo.admission WHERE id = ? AND status = 'ACTIVE'",
            (row, number) -> row.getObject("current_bed_id", UUID.class),
            admissionId
        );
        if (beds.isEmpty()) {
            throw new BusinessValidationException("Internação ativa não encontrada");
        }
        return beds.getFirst();
    }

    private String normalizeName(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.forLanguageTag("pt-BR"));
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static final class MutableUnit {
        private final UUID id;
        private final String name;
        private final String code;
        private final List<MutableRoom> rooms = new ArrayList<>();

        private MutableUnit(UUID id, String name, String code) {
            this.id = id;
            this.name = name;
            this.code = code;
        }

        private MedicalWorkspaceResponse.CareUnit response() {
            return new MedicalWorkspaceResponse.CareUnit(
                id,
                name,
                code,
                rooms.stream().map(MutableRoom::response).toList()
            );
        }
    }

    private static final class MutableRoom {
        private final UUID id;
        private final String name;
        private final String code;
        private final String floorName;
        private final List<MedicalWorkspaceResponse.Bed> beds = new ArrayList<>();

        private MutableRoom(UUID id, String name, String code, String floorName) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.floorName = floorName;
        }

        private MedicalWorkspaceResponse.Room response() {
            return new MedicalWorkspaceResponse.Room(id, name, code, floorName, List.copyOf(beds));
        }
    }
}
