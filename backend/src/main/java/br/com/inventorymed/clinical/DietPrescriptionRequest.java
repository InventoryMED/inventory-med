package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record DietPrescriptionRequest(
    @NotNull DietType type,
    @Valid OralParameters oral,
    @Valid EnteralParameters enteral,
    @Valid ParenteralParameters parenteral,
    @Valid FastingParameters fasting
) {
    public record OralParameters(
        @Size(max = 120) String consistency,
        @Size(max = 8) List<@Size(max = 160) String> restrictions
    ) {}

    public record EnteralParameters(
        @Size(max = 160) String accessRoute,
        @Size(max = 160) String infusionRegimen,
        @Size(max = 200) String formulaType,
        BigDecimal rateMlHour,
        BigDecimal bolusVolumeMl,
        @Size(max = 40) String bolusFrequency,
        BigDecimal tubeFlushMl,
        @Size(max = 40) String flushInterval
    ) {}

    public record ParenteralParameters(
        @Size(max = 160) String accessRoute,
        @Size(max = 160) String preparationType,
        BigDecimal totalVolumeMl,
        BigDecimal rateMlHour,
        BigDecimal totalCaloriesKcalDay,
        BigDecimal proteinGoalGramsKgDay,
        @Size(max = 1000) String gastrointestinalFailureJustification
    ) {}

    public record FastingParameters(
        @Size(max = 200) String reason,
        @Size(max = 120) String reassessment
    ) {}
}
