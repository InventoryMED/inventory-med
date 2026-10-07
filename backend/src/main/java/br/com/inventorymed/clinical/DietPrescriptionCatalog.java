package br.com.inventorymed.clinical;

import java.util.List;

public record DietPrescriptionCatalog(
    List<String> oralConsistencies,
    List<String> oralRestrictions,
    List<String> enteralAccessRoutes,
    List<String> enteralInfusionRegimens,
    List<String> enteralFormulaTypes,
    List<String> parenteralAccessRoutes,
    List<String> parenteralPreparationTypes,
    List<String> fastingReasons
) {}
