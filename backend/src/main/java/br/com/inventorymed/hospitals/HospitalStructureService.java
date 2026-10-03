package br.com.inventorymed.hospitals;

import br.com.inventorymed.audit.AuditOutcome;
import br.com.inventorymed.audit.AuditService;
import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.Hospital;
import br.com.inventorymed.identity.HospitalRepository;
import br.com.inventorymed.security.InventoryUserPrincipal;
import br.com.inventorymed.tenancy.TenantJdbcExecutor;
import java.time.Instant;
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
public class HospitalStructureService {

    private final TenantJdbcExecutor tenantJdbc;
    private final HospitalRepository hospitalRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    public HospitalStructureService(
        TenantJdbcExecutor tenantJdbc,
        HospitalRepository hospitalRepository,
        AppUserRepository userRepository,
        AuditService auditService
    ) {
        this.tenantJdbc = tenantJdbc;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public HospitalStructureResponse structure(UUID hospitalId) {
        requiredHospital(hospitalId);
        return tenantJdbc.read(hospitalId, this::loadStructure);
    }

    public HospitalStructureResponse.CareUnitResponse createCareUnit(
        UUID hospitalId,
        CareUnitSaveRequest request,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        UUID id = UUID.randomUUID();
        executeWrite(hospitalId, () ->
            tenantJdbc.write(hospitalId, jdbc -> {
                jdbc.update(
                    "INSERT INTO dbo.care_unit " +
                    "(id, name, code, display_order, active) VALUES (?, ?, ?, ?, ?)",
                    id,
                    normalizeText(request.name()),
                    normalizeCode(request.code()),
                    request.displayOrder(),
                    request.active()
                );
                return null;
            })
        );
        audit("ADMIN_CARE_UNIT_CREATED", hospital, principal, sourceIp, userAgent, id);
        return findCareUnit(hospitalId, id);
    }

    public HospitalStructureResponse.CareUnitResponse updateCareUnit(
        UUID hospitalId,
        UUID careUnitId,
        CareUnitSaveRequest request,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        executeWrite(hospitalId, () -> tenantJdbc.write(hospitalId, jdbc -> {
            int updated = jdbc.update(
                "UPDATE dbo.care_unit SET name = ?, code = ?, display_order = ?, active = ?, " +
                "updated_at = SYSUTCDATETIME(), row_version = row_version + 1 WHERE id = ?",
                normalizeText(request.name()),
                normalizeCode(request.code()),
                request.displayOrder(),
                request.active(),
                careUnitId
            );
            requireUpdated(updated, "Unidade de internação não encontrada");
            return null;
        }));
        audit("ADMIN_CARE_UNIT_UPDATED", hospital, principal, sourceIp, userAgent, careUnitId);
        return findCareUnit(hospitalId, careUnitId);
    }

    public HospitalStructureResponse.RoomResponse createRoom(
        UUID hospitalId,
        RoomSaveRequest request,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        UUID id = UUID.randomUUID();
        executeWrite(hospitalId, () -> tenantJdbc.write(hospitalId, jdbc -> {
            requireCareUnitExists(jdbc, request.careUnitId());
            jdbc.update(
                "INSERT INTO dbo.room " +
                "(id, care_unit_id, name, code, floor_name, display_order, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                id,
                request.careUnitId(),
                normalizeText(request.name()),
                normalizeCode(request.code()),
                normalizeNullable(request.floorName()),
                request.displayOrder(),
                request.active()
            );
            return null;
        }));
        audit("ADMIN_ROOM_CREATED", hospital, principal, sourceIp, userAgent, id);
        return findRoom(hospitalId, id);
    }

    public HospitalStructureResponse.RoomResponse updateRoom(
        UUID hospitalId,
        UUID roomId,
        RoomSaveRequest request,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        executeWrite(hospitalId, () -> tenantJdbc.write(hospitalId, jdbc -> {
            requireCareUnitExists(jdbc, request.careUnitId());
            int updated = jdbc.update(
                "UPDATE dbo.room SET care_unit_id = ?, name = ?, code = ?, floor_name = ?, " +
                "display_order = ?, active = ?, updated_at = SYSUTCDATETIME(), " +
                "row_version = row_version + 1 WHERE id = ?",
                request.careUnitId(),
                normalizeText(request.name()),
                normalizeCode(request.code()),
                normalizeNullable(request.floorName()),
                request.displayOrder(),
                request.active(),
                roomId
            );
            requireUpdated(updated, "Quarto não encontrado");
            return null;
        }));
        audit("ADMIN_ROOM_UPDATED", hospital, principal, sourceIp, userAgent, roomId);
        return findRoom(hospitalId, roomId);
    }

    public HospitalStructureResponse.BedResponse createBed(
        UUID hospitalId,
        BedSaveRequest request,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        requireAdministrativeStatus(request.status());
        UUID id = UUID.randomUUID();
        executeWrite(hospitalId, () -> tenantJdbc.write(hospitalId, jdbc -> {
            requireRoomExists(jdbc, request.roomId());
            jdbc.update(
                "INSERT INTO dbo.bed (id, room_id, code, status, display_order, active) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                id,
                request.roomId(),
                normalizeCode(request.code()),
                request.status().name(),
                request.displayOrder(),
                request.active()
            );
            return null;
        }));
        audit("ADMIN_BED_CREATED", hospital, principal, sourceIp, userAgent, id);
        return findBed(hospitalId, id);
    }

    public HospitalStructureResponse.BedResponse updateBed(
        UUID hospitalId,
        UUID bedId,
        BedSaveRequest request,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        requireAdministrativeStatus(request.status());
        executeWrite(hospitalId, () -> tenantJdbc.write(hospitalId, jdbc -> {
            requireRoomExists(jdbc, request.roomId());
            List<String> statuses = jdbc.query(
                "SELECT status FROM dbo.bed WHERE id = ?",
                (result, row) -> result.getString("status"),
                bedId
            );
            if (statuses.isEmpty()) throw new BusinessValidationException("Leito não encontrado");
            String currentStatus = statuses.getFirst();
            if (BedStatus.OCCUPIED.name().equals(currentStatus)) {
                throw new BusinessValidationException("Leito ocupado não pode ser alterado administrativamente");
            }
            int updated = jdbc.update(
                "UPDATE dbo.bed SET room_id = ?, code = ?, status = ?, display_order = ?, " +
                "active = ?, updated_at = SYSUTCDATETIME(), row_version = row_version + 1 WHERE id = ?",
                request.roomId(),
                normalizeCode(request.code()),
                request.status().name(),
                request.displayOrder(),
                request.active(),
                bedId
            );
            requireUpdated(updated, "Leito não encontrado");
            return null;
        }));
        audit("ADMIN_BED_UPDATED", hospital, principal, sourceIp, userAgent, bedId);
        return findBed(hospitalId, bedId);
    }

    private HospitalStructureResponse loadStructure(JdbcTemplate jdbc) {
        Map<UUID, MutableCareUnit> units = new LinkedHashMap<>();
        jdbc.query(
            "SELECT id, name, code, display_order, active FROM dbo.care_unit " +
            "ORDER BY display_order, name",
            (RowCallbackHandler) result -> units.put(
                result.getObject("id", UUID.class),
                new MutableCareUnit(
                    result.getObject("id", UUID.class),
                    result.getString("name"),
                    result.getString("code"),
                    result.getInt("display_order"),
                    result.getBoolean("active")
                )
            )
        );
        Map<UUID, MutableRoom> rooms = new LinkedHashMap<>();
        jdbc.query(
            "SELECT id, care_unit_id, name, code, floor_name, display_order, active " +
            "FROM dbo.room ORDER BY display_order, name",
            (RowCallbackHandler) result -> {
                MutableRoom room = new MutableRoom(
                    result.getObject("id", UUID.class),
                    result.getObject("care_unit_id", UUID.class),
                    result.getString("name"),
                    result.getString("code"),
                    result.getString("floor_name"),
                    result.getInt("display_order"),
                    result.getBoolean("active")
                );
                rooms.put(room.id, room);
                MutableCareUnit unit = units.get(room.careUnitId);
                if (unit != null) unit.rooms.add(room);
            }
        );
        jdbc.query(
            "SELECT id, room_id, code, status, display_order, active FROM dbo.bed " +
            "ORDER BY display_order, code",
            (RowCallbackHandler) result -> {
                MutableRoom room = rooms.get(result.getObject("room_id", UUID.class));
                if (room != null) {
                    room.beds.add(
                        new HospitalStructureResponse.BedResponse(
                            result.getObject("id", UUID.class),
                            result.getObject("room_id", UUID.class),
                            result.getString("code"),
                            BedStatus.valueOf(result.getString("status")),
                            result.getInt("display_order"),
                            result.getBoolean("active")
                        )
                    );
                }
            }
        );
        return new HospitalStructureResponse(
            units.values().stream().map(MutableCareUnit::response).toList()
        );
    }

    private HospitalStructureResponse.CareUnitResponse findCareUnit(UUID hospitalId, UUID id) {
        return structure(hospitalId)
            .careUnits()
            .stream()
            .filter(unit -> unit.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new BusinessValidationException("Unidade de internação não encontrada"));
    }

    private HospitalStructureResponse.RoomResponse findRoom(UUID hospitalId, UUID id) {
        return structure(hospitalId)
            .careUnits()
            .stream()
            .flatMap(unit -> unit.rooms().stream())
            .filter(room -> room.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new BusinessValidationException("Quarto não encontrado"));
    }

    private HospitalStructureResponse.BedResponse findBed(UUID hospitalId, UUID id) {
        return structure(hospitalId)
            .careUnits()
            .stream()
            .flatMap(unit -> unit.rooms().stream())
            .flatMap(room -> room.beds().stream())
            .filter(bed -> bed.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new BusinessValidationException("Leito não encontrado"));
    }

    private void requireCareUnitExists(JdbcTemplate jdbc, UUID id) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.care_unit WHERE id = ?",
            Integer.class,
            id
        );
        if (count == null || count == 0) {
            throw new BusinessValidationException("Unidade de internação não encontrada");
        }
    }

    private void requireRoomExists(JdbcTemplate jdbc, UUID id) {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.room WHERE id = ?",
            Integer.class,
            id
        );
        if (count == null || count == 0) {
            throw new BusinessValidationException("Quarto não encontrado");
        }
    }

    private void requireUpdated(int count, String message) {
        if (count == 0) throw new BusinessValidationException(message);
    }

    private void requireAdministrativeStatus(BedStatus status) {
        if (status == BedStatus.OCCUPIED) {
            throw new BusinessValidationException("O estado ocupado é definido somente por uma admissão");
        }
    }

    private void executeWrite(UUID hospitalId, Runnable operation) {
        try {
            operation.run();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessValidationException("Código duplicado ou vínculo inválido neste hospital");
        }
    }

    private Hospital requiredHospital(UUID hospitalId) {
        return hospitalRepository
            .findById(hospitalId)
            .filter(Hospital::isActive)
            .orElseThrow(() -> new BusinessValidationException("Hospital inexistente ou inativo"));
    }

    private void audit(
        String eventType,
        Hospital hospital,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent,
        UUID entityId
    ) {
        AppUser actor = userRepository
            .findById(principal.userId())
            .orElseThrow(() -> new BusinessValidationException("Administrador não encontrado"));
        auditService.record(
            eventType,
            AuditOutcome.SUCCESS,
            actor,
            hospital,
            sourceIp,
            userAgent,
            Map.of("entityId", entityId)
        );
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.forLanguageTag("pt-BR"));
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : normalizeText(value);
    }

    private static final class MutableCareUnit {
        private final UUID id;
        private final String name;
        private final String code;
        private final int displayOrder;
        private final boolean active;
        private final List<MutableRoom> rooms = new ArrayList<>();

        private MutableCareUnit(UUID id, String name, String code, int displayOrder, boolean active) {
            this.id = id;
            this.name = name;
            this.code = code;
            this.displayOrder = displayOrder;
            this.active = active;
        }

        private HospitalStructureResponse.CareUnitResponse response() {
            return new HospitalStructureResponse.CareUnitResponse(
                id,
                name,
                code,
                displayOrder,
                active,
                rooms.stream().map(MutableRoom::response).toList()
            );
        }
    }

    private static final class MutableRoom {
        private final UUID id;
        private final UUID careUnitId;
        private final String name;
        private final String code;
        private final String floorName;
        private final int displayOrder;
        private final boolean active;
        private final List<HospitalStructureResponse.BedResponse> beds = new ArrayList<>();

        private MutableRoom(
            UUID id,
            UUID careUnitId,
            String name,
            String code,
            String floorName,
            int displayOrder,
            boolean active
        ) {
            this.id = id;
            this.careUnitId = careUnitId;
            this.name = name;
            this.code = code;
            this.floorName = floorName;
            this.displayOrder = displayOrder;
            this.active = active;
        }

        private HospitalStructureResponse.RoomResponse response() {
            return new HospitalStructureResponse.RoomResponse(
                id,
                careUnitId,
                name,
                code,
                floorName,
                displayOrder,
                active,
                List.copyOf(beds)
            );
        }
    }
}
