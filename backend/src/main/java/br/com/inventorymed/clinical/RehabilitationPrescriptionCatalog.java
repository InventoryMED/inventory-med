package br.com.inventorymed.clinical;

import java.util.List;

public record RehabilitationPrescriptionCatalog(
    List<Option> specialties,
    List<Option> respiratoryProcedures,
    List<Option> motorProcedures,
    List<Option> speechTherapyProcedures,
    List<Option> occupationalTherapyProcedures,
    List<Option> respiratoryFrequencies,
    List<Option> motorFrequencies,
    List<Option> speechTherapyFrequencies,
    List<Option> occupationalTherapyFrequencies,
    List<Option> schedulingOptions,
    List<Template> templates
) {
    public record Option(String code, String label) {}

    public record Template(
        String code,
        String label,
        RehabilitationPrescriptionRequest.Item item
    ) {}
}
