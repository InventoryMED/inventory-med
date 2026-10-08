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

public record TherapeuticSupportRequest(
    @Size(max = 50) String clinicalContext,
    @Size(max = 8) List<@Valid HydrationSolution> hydrationSolutions,
    @Valid GlucoseControl glucoseControl,
    @Size(max = 8) List<@Valid BloodProduct> bloodProducts
) {
    public record HydrationSolution(
        @NotNull @Positive Integer id,
        @Size(max = 60) String baseSolution,
        @Size(max = 6) List<@Size(max = 60) String> additives,
        @Size(max = 20) String route,
        @Size(max = 30) String frequency,
        @Size(max = 30) String infusionMode,
        @DecimalMin("0.1") @DecimalMax("2000") BigDecimal rateValue,
        @Size(max = 20) String rateUnit,
        @Size(max = 20) String scheduling
    ) {}

    public record GlucoseControl(
        @Size(max = 40) String frequency,
        Boolean hypoglycemiaProtocolActive,
        Boolean correctionScaleActive,
        @Size(max = 30) String insulinType,
        Boolean continuousPump
    ) {}

    public record BloodProduct(
        @NotNull @Positive Integer id,
        @Size(max = 60) String product,
        @Size(max = 4) List<@Size(max = 50) String> modifications,
        @Min(1) @Max(1000) Integer quantity,
        @Size(max = 20) String quantityUnit,
        @Size(max = 30) String route,
        @Min(15) @Max(240) Integer infusionMinutes,
        @Size(max = 3) List<@Size(max = 60) String> preMedications,
        @Size(max = 20) String scheduling
    ) {}
}
