package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public record ClinicalSummaryRequest(
    @NotBlank
    @Pattern(regexp = "ALTA_HOSPITALAR|TRANSFERENCIA_HOSPITALAR|OBITO")
    String outcomeType,
    @Size(max = 40) String dischargeCondition,
    @NotBlank @Size(max = 10_000) String clinicalSummary,
    @Size(max = 20) String primaryCid,
    @Size(max = 30) List<@Valid DischargeMedication> dischargeMedications,
    @Size(max = 20) List<@Size(max = 1000) String> dischargeOrientations,
    @Valid TransferDetails transfer,
    @Valid DeathDetails death,
    boolean physicianAttestation
) {
    public record DischargeMedication(
        @Size(max = 300) String medication,
        @Size(max = 500) String dosage,
        @Size(max = 200) String duration
    ) {}

    public record TransferDetails(
        @Size(max = 60) String reason,
        @Size(max = 300) String destinationHospital,
        @Pattern(regexp = "[0-9]*") @Size(max = 20) String destinationCnes,
        @Size(max = 20) List<@Size(max = 60) String> devices
    ) {}

    public record DeathDetails(
        OffsetDateTime occurredAt,
        @Size(max = 500) String directCause,
        @Size(max = 500) String antecedentCause,
        @Size(max = 500) String underlyingCause,
        @Size(max = 30) String necropsy
    ) {}
}
