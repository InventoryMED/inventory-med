package br.com.inventorymed.clinical;

import java.util.List;

public record MedicationTherapyCatalog(
    List<Option> clinicalContexts,
    List<MedicationOption> antimicrobials,
    List<Option> antimicrobialRoutes,
    List<Option> antimicrobialAdministrationModes,
    List<Option> diluents,
    List<Option> infusionSets,
    List<Option> antimicrobialScheduling,
    List<Option> ccihStatuses,
    List<Option> renalFunctionMeasures,
    List<Option> renalDoseAssessments,
    List<ProphylaxisOption> prophylaxisOptions,
    List<Option> prophylaxisRoutes,
    List<Option> prophylaxisScheduling,
    List<Option> reconciliationStatuses,
    List<Option> continuousMedicationScheduling,
    List<MedicationOption> symptomaticMedications,
    List<Option> medicationRoutes,
    List<Option> symptomaticScheduling
) {
    public record Option(String code, String label) {}

    public record MedicationOption(String code, String label) {}

    public record ProphylaxisOption(
        String code,
        String label,
        String category,
        boolean anticoagulant,
        boolean filledSyringe,
        boolean equipment
    ) {}
}
