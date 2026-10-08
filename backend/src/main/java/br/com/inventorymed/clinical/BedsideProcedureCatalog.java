package br.com.inventorymed.clinical;

import java.util.List;

public record BedsideProcedureCatalog(
    List<Option> recordTypes,
    List<ProcedureOption> procedures,
    List<Option> lateralities,
    List<Option> urgencyOptions,
    List<Option> postProcedureControls,
    List<Template> templates
) {
    public record Option(String code, String label) {}

    public record ProcedureOption(
        String code,
        String label,
        String billingReference,
        boolean pairedSite,
        boolean deviceTraceabilityRequired,
        boolean postProcedureControlRequired,
        String defaultSite,
        String defaultAsepsis,
        String defaultSterileBarrier,
        String defaultAnesthesia,
        String defaultImageGuidance,
        String defaultDevice,
        String defaultCaliber,
        String defaultFixation,
        String defaultSamples,
        String defaultPostProcedureControl,
        String defaultMonitoring
    ) {}

    public record Template(String code, String label, ProcedureItem item) {}

    public record ProcedureItem(
        String procedureCode,
        String anatomicalSite,
        String laterality,
        String asepsisAntisepsis,
        String sterileBarrier,
        String localAnesthesia,
        String imageGuidance,
        String deviceName,
        String deviceCaliber,
        String fixationDressingConnections,
        String samplesLaboratory,
        String postProcedureControl,
        String monitoringAssistance,
        String urgency
    ) {}
}
