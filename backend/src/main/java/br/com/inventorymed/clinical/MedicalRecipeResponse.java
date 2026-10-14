package br.com.inventorymed.clinical;

import java.util.List;

public record MedicalRecipeResponse(
    MedicalRecipeRequest request,
    String printTitle,
    List<String> copyLabels,
    boolean finalizationAllowed,
    List<String> allergyConflicts,
    List<String> blockingReasons,
    List<String> safetyNotices
) {}
