package br.com.inventorymed.clinical;

import br.com.inventorymed.hospitals.BedStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MedicalWorkspaceResponse(
    UUID hospitalId,
    List<CareUnit> careUnits
) {
    public record CareUnit(UUID id, String name, String code, List<Room> rooms) {}

    public record Room(
        UUID id,
        String name,
        String code,
        String floorName,
        List<Bed> beds
    ) {}

    public record Bed(
        UUID id,
        String code,
        BedStatus status,
        Admission admission
    ) {}

    public record Admission(
        UUID id,
        Instant admittedAt,
        Patient patient
    ) {}

    public record Patient(
        UUID id,
        String fullName,
        LocalDate birthDate,
        String sex,
        BigDecimal weightKg,
        String diagnosis,
        String comorbidities,
        String allergies
    ) {}
}
