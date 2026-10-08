package br.com.inventorymed.clinical;

import java.util.List;

public record IsolationPrecautionCatalog(
    List<Option> precautionTypes,
    List<Option> durationOptions,
    List<Option> schedulingOptions,
    List<Template> templates
) {
    public record Option(String code, String label) {}

    public record Template(
        String code,
        String label,
        List<IsolationPrecautionRequest.Item> items
    ) {}
}
