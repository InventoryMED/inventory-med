package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record MedicationTherapyRequest(
    @Size(max = 50) String clinicalContext,
    @Valid RenalFunction renalFunction,
    @Valid BleedingRisk bleedingRisk,
    @Size(max = 12) List<@Valid Antimicrobial> antimicrobials,
    @Size(max = 12) List<@Valid HospitalProphylaxis> prophylaxes,
    @Size(max = 20) List<@Valid ContinuousMedication> continuousMedications,
    @Size(max = 20) List<@Valid SymptomaticMedication> analgesiaSymptomatics
) {
    public record RenalFunction(
        @Size(max = 40) String measure,
        @DecimalMin("1") @DecimalMax("200") BigDecimal valueMlMin
    ) {}

    public record BleedingRisk(
        @Min(0) @Max(2000000) Integer plateletCount,
        Boolean activeBleeding
    ) {}

    public record Antimicrobial(
        @NotNull @Positive Integer id,
        @Size(max = 60) String drug,
        @Size(max = 160) String customDrug,
        @Size(max = 500) String dosePreparation,
        @Size(max = 20) String route,
        @Size(max = 40) String administrationMode,
        @Size(max = 30) String diluent,
        @Size(max = 30) String infusionSet,
        @Size(max = 80) String frequency,
        @Size(max = 30) String scheduling,
        @Size(max = 300) String loadingDose,
        @Size(max = 500) String conditionalTrigger,
        @Min(1) @Max(999) Integer treatmentDay,
        @Size(max = 500) String infectionFocus,
        @Size(max = 40) String ccihStatus,
        @Size(max = 500) String ccihOpinion,
        @Size(max = 40) String renalDoseAssessment
    ) {}

    public record HospitalProphylaxis(
        @NotNull @Positive Integer id,
        @Size(max = 60) String intervention,
        @Size(max = 500) String dosePreparation,
        @Size(max = 20) String route,
        @Size(max = 80) String frequency,
        @Size(max = 30) String scheduling,
        @Size(max = 500) String conditionalTrigger,
        @Size(max = 500) String suspensionReason
    ) {}

    public record ContinuousMedication(
        @NotNull @Positive Integer id,
        @Size(max = 200) String medication,
        @Size(max = 500) String dosePreparation,
        @Size(max = 20) String route,
        @Size(max = 80) String frequency,
        @Size(max = 40) String reconciliationStatus,
        @Size(max = 40) String scheduling,
        @Size(max = 500) String conditionalTrigger,
        @Size(max = 500) String suspensionReason
    ) {}

    public record SymptomaticMedication(
        @NotNull @Positive Integer id,
        @Size(max = 60) String drug,
        @Size(max = 160) String customDrug,
        @Size(max = 500) String dosePreparation,
        @Size(max = 20) String route,
        @Size(max = 80) String frequency,
        @Size(max = 30) String scheduling,
        @Size(max = 500) String trigger,
        @Size(max = 80) String minimumInterval
    ) {}
}
