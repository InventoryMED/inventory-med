package br.com.inventorymed.clinical;

import java.util.List;

public record PrescriptionStarterCatalog(
    List<ClinicOption> clinics,
    List<TemplateOption> templates
) {
    public record ClinicOption(String code, String label) {}

    public record TemplateOption(
        String code,
        String name,
        String description,
        List<String> clinicCodes
    ) {}
}
