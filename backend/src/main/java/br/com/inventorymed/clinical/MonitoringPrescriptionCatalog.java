package br.com.inventorymed.clinical;

import java.util.List;

public record MonitoringPrescriptionCatalog(
    List<String> vitalSignsFrequencies,
    List<String> painScales,
    List<String> consciousnessSedationScales,
    List<String> fallRiskScales,
    List<String> glucoseMonitoringFrequencies,
    List<String> insulinTypes,
    List<String> fluidBalanceOptions,
    List<String> urineOutputOptions,
    List<String> drainsTubes,
    List<String> otherMeasurements,
    List<String> hemodynamicMonitoring,
    List<String> neurologicalMonitoring
) {}
