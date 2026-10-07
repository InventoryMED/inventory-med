package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record VentilatorySupportRequest(
    @Size(max = 80) String selectedTemplate,
    @NotEmpty @Size(max = 4) List<@Valid Item> items
) {
    public record Item(
        @NotNull @Positive Integer id,
        @Size(max = 40) String supportType,
        @Size(max = 60) String frequency,
        @Size(max = 20) String scheduling,
        @Valid LowFlow lowFlow,
        @Valid HighFlow highFlow,
        @Valid NonInvasive nonInvasive,
        @Valid Invasive invasive
    ) {}

    public record LowFlow(
        @Size(max = 40) String device,
        @DecimalMin("0.5") @DecimalMax("15.0") BigDecimal oxygenFlowLitersMinute,
        @Min(21) @Max(100) Integer fio2Percent
    ) {}

    public record HighFlow(
        @DecimalMin("30.0") @DecimalMax("60.0") BigDecimal flowLitersMinute,
        @Min(21) @Max(100) Integer fio2Percent,
        @DecimalMin("31.0") @DecimalMax("37.0") BigDecimal temperatureCelsius,
        @Size(max = 10) String interfaceSize
    ) {}

    public record NonInvasive(
        @Size(max = 30) String mode,
        @DecimalMin("1.0") @DecimalMax("40.0") BigDecimal ipapCmH2o,
        @DecimalMin("1.0") @DecimalMax("30.0") BigDecimal epapPeepCmH2o,
        @DecimalMin("1.0") @DecimalMax("40.0") BigDecimal supportPressureCmH2o,
        @Min(21) @Max(100) Integer fio2Percent,
        @Min(1) @Max(60) Integer backupRate,
        @Size(max = 40) String interfaceType,
        @DecimalMin("0.5") @DecimalMax("12.0") BigDecimal sessionHours
    ) {}

    public record Invasive(
        @Size(max = 20) String airway,
        @Size(max = 80) String airwayDetail,
        @Size(max = 20) String mode,
        @Min(50) @Max(1500) Integer tidalVolumeMl,
        @Min(1) @Max(80) Integer respiratoryRate,
        @DecimalMin("0.0") @DecimalMax("30.0") BigDecimal peepCmH2o,
        @Min(21) @Max(100) Integer fio2Percent,
        @DecimalMin("1.0") @DecimalMax("120.0") BigDecimal inspiratoryFlowLitersMinute,
        @DecimalMin("0.1") @DecimalMax("5.0") BigDecimal inspiratoryTimeSeconds,
        @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal pauseSeconds,
        @DecimalMin("1.0") @DecimalMax("50.0") BigDecimal inspiratoryPressureCmH2o,
        @DecimalMin("1.0") @DecimalMax("50.0") BigDecimal supportPressureCmH2o,
        @DecimalMin("0.1") @DecimalMax("20.0") BigDecimal triggerSensitivity,
        @Size(max = 3) List<@Size(max = 60) String> protectiveGoals
    ) {}
}
