package br.com.inventorymed.clinical;

import java.util.List;

public record AihCatalog(
    List<Option> clinicalContexts,
    List<Option> admissionCharacters,
    List<Option> lateralities,
    List<ProcedureOption> procedures,
    CodeReference imageGuidance
) {
    public record Option(String code, String label) {}

    public record CodeReference(
        String tussCode,
        String cbhpmCode,
        String sigtapCode
    ) {}

    public record ProcedureOption(
        String code,
        String label,
        String tussCode,
        String cbhpmCode,
        String sigtapCode,
        boolean pairedSite,
        String defaultSite
    ) {}
}
