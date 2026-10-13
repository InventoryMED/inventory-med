package br.com.inventorymed.clinical;

import java.util.List;

public record DiagnosticExamCatalog(
    List<Option> clinicalContexts,
    List<QuickPack> quickPacks,
    List<ExamOption> bloodExams,
    List<ExamOption> urineExams,
    List<ExamOption> tomographyRegions,
    List<Option> contrastOptions,
    List<Option> tomographyJustifications,
    List<XrayProfile> xrayProfiles,
    List<Option> lateralities,
    List<UltrasoundOption> ultrasoundExams,
    List<Option> ecgPriorities,
    List<Option> ecgJustifications
) {
    public record Option(String code, String label) {}

    public record ExamOption(
        String code,
        String label,
        String tussCode,
        String sigtapCode,
        boolean highCost,
        List<String> supplies
    ) {}

    public record QuickPack(
        String code,
        String label,
        List<String> bloodExamCodes,
        List<String> urineExamCodes,
        String tomographyRegion,
        String xrayRegion,
        String ultrasoundCode,
        boolean ecg
    ) {}

    public record XrayProfile(
        String code,
        String label,
        String tussCode,
        String sigtapCode,
        boolean pairedSite,
        List<Option> incidences,
        List<String> defaultIncidences
    ) {}

    public record UltrasoundOption(
        String code,
        String label,
        String tussCode,
        String sigtapCode,
        boolean pairedSite,
        List<Option> justifications
    ) {}
}
