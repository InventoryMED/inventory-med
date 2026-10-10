package br.com.inventorymed.clinical;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AihCatalogService {

    public static final String IMAGE_GUIDANCE_TUSS = "40901262";

    private static final List<AihCatalog.Option> ADMISSION_CHARACTERS = List.of(
        option("URGENCY", "URGÊNCIA"),
        option("ELECTIVE", "ELETIVA")
    );

    private final BedsideProcedureCatalogService procedureCatalog;

    public AihCatalogService(BedsideProcedureCatalogService procedureCatalog) {
        this.procedureCatalog = procedureCatalog;
    }

    public AihCatalog catalog() {
        return new AihCatalog(
            BedsideProcedureCatalogService.CLINICAL_CONTEXTS.stream()
                .map(item -> option(item.code(), item.label()))
                .toList(),
            ADMISSION_CHARACTERS,
            BedsideProcedureCatalogService.LATERALITIES.stream()
                .map(item -> option(item.code(), item.label()))
                .toList(),
            BedsideProcedureCatalogService.PROCEDURES.stream()
                .filter(item -> !"OTHER".equals(item.code()))
                .map(this::aihProcedure)
                .toList(),
            new AihCatalog.CodeReference(IMAGE_GUIDANCE_TUSS, null, null)
        );
    }

    public AihCatalog.ProcedureOption procedure(String code) {
        if (code == null) return null;
        return catalog().procedures().stream()
            .filter(item -> item.code().equals(code))
            .findFirst()
            .orElse(null);
    }

    public String contextLabel(String code) {
        return procedureCatalog.optionLabel(BedsideProcedureCatalogService.CLINICAL_CONTEXTS, code);
    }

    public String admissionCharacterLabel(String code) {
        return ADMISSION_CHARACTERS.stream()
            .filter(item -> item.code().equals(code))
            .map(AihCatalog.Option::label)
            .findFirst()
            .orElse(code);
    }

    public String lateralityLabel(String code) {
        return procedureCatalog.optionLabel(BedsideProcedureCatalogService.LATERALITIES, code);
    }

    private AihCatalog.ProcedureOption aihProcedure(
        BedsideProcedureCatalog.ProcedureOption procedure
    ) {
        String tussCode = procedure.billingReference().startsWith("TUSS ")
            ? procedure.billingReference().substring(5)
            : null;
        return new AihCatalog.ProcedureOption(
            procedure.code(),
            procedure.label(),
            tussCode,
            null,
            null,
            procedure.pairedSite(),
            procedure.defaultSite()
        );
    }

    private static AihCatalog.Option option(String code, String label) {
        return new AihCatalog.Option(code, label);
    }
}
