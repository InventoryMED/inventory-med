package br.com.inventorymed.clinical;

import java.util.List;

public record MedicalRecipeCatalog(
    List<Option> recipeTypes,
    List<Option> routes,
    boolean medicationCatalogIntegrated,
    boolean sncrIntegrated,
    boolean qualifiedDigitalSignatureIntegrated
) {
    public record Option(String code, String label, boolean enabled) {}
}
