package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CriticalCarePrescriptionRequest(
    @Size(max = 50) String clinicalContext,
    @Size(max = 8) List<@Valid VasoactiveDrug> vasoactiveDrugs,
    @Size(max = 8) List<@Valid SedationDrug> sedationAnalgesiaBnm,
    @Size(max = 8) List<@Valid EmergencyMedication> emergencyMedications
) {
    public record VasoactiveDrug(
        @NotNull @Positive Integer id,
        @Size(max = 60) String drug,
        @Size(max = 500) String dilution,
        @Size(max = 160) String finalConcentration,
        @DecimalMin("0.01") @DecimalMax("5000") BigDecimal initialRate,
        @Size(max = 30) String rateUnit,
        @Size(max = 40) String vascularAccess,
        @Size(max = 40) String bloodPressureMonitoring,
        @Size(max = 400) String therapeuticGoal,
        @Size(max = 30) String scheduling
    ) {}

    public record SedationDrug(
        @NotNull @Positive Integer id,
        @Size(max = 60) String drug,
        @Size(max = 500) String preparation,
        @Size(max = 30) String administrationMode,
        @DecimalMin("0.01") @DecimalMax("5000") BigDecimal rateDoseValue,
        @Size(max = 30) String rateDoseUnit,
        @Size(max = 40) String sedationTarget,
        @Size(max = 40) String ventilatoryStatus,
        @Size(max = 20) String route,
        @Size(max = 30) String scheduling
    ) {}

    public record EmergencyMedication(
        @NotNull @Positive Integer id,
        @Size(max = 60) String drug,
        @Size(max = 300) String doseAdministration,
        @Size(max = 20) String route,
        @Size(max = 500) String emergencyIndication,
        @Size(max = 30) String scheduling
    ) {}
}
