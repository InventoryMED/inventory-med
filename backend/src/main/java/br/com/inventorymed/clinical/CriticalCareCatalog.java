package br.com.inventorymed.clinical;

import java.util.List;

public record CriticalCareCatalog(
    List<Option> clinicalContexts,
    List<VasoactiveOption> vasoactiveDrugs,
    List<SedationOption> sedationDrugs,
    List<EmergencyOption> emergencyDrugs,
    List<Option> vascularAccesses,
    List<Option> bloodPressureMonitoring,
    List<Option> vasoactiveRateUnits,
    List<Option> administrationModes,
    List<Option> sedationRateUnits,
    List<Option> sedationTargets,
    List<Option> ventilatoryStatuses,
    List<Option> sedationRoutes,
    List<Option> emergencyRoutes,
    List<Option> emergencyScheduling
) {
    public record Option(String code, String label) {}

    public record VasoactiveOption(
        String code,
        String label,
        boolean centralAccessRequired,
        String specialTubing
    ) {}

    public record SedationOption(
        String code,
        String label,
        String category,
        List<String> allowedModes
    ) {}

    public record EmergencyOption(String code, String label, boolean highCost) {}
}
