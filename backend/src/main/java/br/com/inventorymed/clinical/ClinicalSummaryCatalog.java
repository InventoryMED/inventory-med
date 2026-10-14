package br.com.inventorymed.clinical;

import java.util.List;

public record ClinicalSummaryCatalog(
    List<Option> outcomeTypes,
    List<Option> dischargeConditions,
    List<Option> transferReasons,
    List<Option> devices,
    List<Option> necropsyOptions,
    List<Option> orientationSuggestions
) {
    public record Option(String code, String label, String exitReferenceCode) {}
}
