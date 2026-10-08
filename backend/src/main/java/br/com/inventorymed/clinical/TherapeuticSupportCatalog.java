package br.com.inventorymed.clinical;

import java.util.List;

public record TherapeuticSupportCatalog(
    List<Option> clinicalContexts,
    List<Option> baseSolutions,
    List<Option> electrolyteAdditives,
    List<Option> hydrationFrequencies,
    List<Option> infusionModes,
    List<Option> rateUnits,
    List<Option> schedulingOptions,
    List<Option> glucoseFrequencies,
    List<Option> insulinTypes,
    List<ProductOption> bloodProducts,
    List<Option> bloodProductModifications,
    List<Option> transfusionRoutes,
    List<Option> quantityUnits,
    List<Option> preMedications
) {
    public record Option(String code, String label) {}

    public record ProductOption(String code, String label, String category) {}
}
