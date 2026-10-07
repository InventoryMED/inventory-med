package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MonitoringPrescriptionRequest(
    @Valid VitalSigns vitalSigns,
    @Valid GlucoseMonitoring glucoseMonitoring,
    @Valid FluidBalanceOutputs fluidBalanceOutputs,
    @Valid InvasiveMonitoring invasiveMonitoring
) {
    public record VitalSigns(
        @Size(max = 100) String frequency,
        @Size(max = 100) String painScale,
        @Size(max = 100) String consciousnessSedationScale,
        @Size(max = 100) String fallRiskScale
    ) {}

    public record GlucoseMonitoring(
        @Size(max = 120) String frequency,
        Boolean hypoglycemiaProtocol,
        Boolean slidingScale,
        @Size(max = 80) String insulinType
    ) {}

    public record FluidBalanceOutputs(
        @Size(max = 100) String fluidBalance,
        @Size(max = 120) String urineOutput,
        @Size(max = 5) List<@Size(max = 160) String> drainsTubes,
        @Size(max = 2) List<@Size(max = 160) String> otherMeasurements
    ) {}

    public record InvasiveMonitoring(
        @Size(max = 3) List<@Size(max = 160) String> hemodynamic,
        @Size(max = 2) List<@Size(max = 160) String> neurological
    ) {}
}
