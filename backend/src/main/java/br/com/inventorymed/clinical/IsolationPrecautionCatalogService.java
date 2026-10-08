package br.com.inventorymed.clinical;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class IsolationPrecautionCatalogService {

    static final List<IsolationPrecautionCatalog.Option> PRECAUTION_TYPES = List.of(
        option("STANDARD", "PRECAUÇÃO PADRÃO"),
        option("CONTACT", "PRECAUÇÃO DE CONTATO"),
        option("DROPLET", "PRECAUÇÃO POR GOTÍCULAS"),
        option("AIRBORNE", "PRECAUÇÃO POR AEROSSÓIS"),
        option("PROTECTIVE_REVERSE", "PRECAUÇÃO PROTETORA / ISOLAMENTO REVERSO")
    );

    static final List<IsolationPrecautionCatalog.Option> DURATIONS = List.of(
        option("UNTIL_CULTURE_RESULT", "ATÉ RESULTADO DE CULTURA"),
        option("ENTIRE_HOSPITALIZATION", "DURANTE TODA A INTERNAÇÃO"),
        option("REVIEW_7_DAYS", "REAVALIAR EM 7 DIAS"),
        option("UNTIL_CCIH_REASSESSMENT", "ATÉ REAVALIAÇÃO PELA CCIH")
    );

    static final List<IsolationPrecautionCatalog.Option> SCHEDULING = List.of(
        option("FIXED", "FIXO"),
        option("CONTINUOUS", "CONTÍNUO")
    );

    public IsolationPrecautionCatalog catalog() {
        return new IsolationPrecautionCatalog(
            PRECAUTION_TYPES,
            DURATIONS,
            SCHEDULING,
            templates()
        );
    }

    public String label(List<IsolationPrecautionCatalog.Option> options, String code) {
        return options.stream()
            .filter(option -> option.code().equals(code))
            .findFirst()
            .map(IsolationPrecautionCatalog.Option::label)
            .orElse(code);
    }

    public boolean contains(List<IsolationPrecautionCatalog.Option> options, String code) {
        return options.stream().anyMatch(option -> option.code().equals(code));
    }

    public boolean templateExists(String code) {
        return templates().stream().anyMatch(template -> template.code().equals(code));
    }

    private List<IsolationPrecautionCatalog.Template> templates() {
        return List.of(
            template(
                "STANDARD_ALL_PATIENTS",
                "PRECAUÇÃO PADRÃO — TODOS OS PACIENTES",
                item(1, "STANDARD", "APLICÁVEL A TODOS OS PACIENTES INTERNADOS",
                    "ENTIRE_HOSPITALIZATION", "CONTINUOUS")
            ),
            template(
                "CONTACT_KPC_MDR",
                "PRECAUÇÃO DE CONTATO — KPC / ENTEROBACTÉRIAS MDR",
                item(1, "CONTACT",
                    "INFECÇÃO OU COLONIZAÇÃO POR KPC / ENTEROBACTÉRIA MULTIRRESISTENTE",
                    "ENTIRE_HOSPITALIZATION", "CONTINUOUS")
            ),
            template(
                "CONTACT_CLOSTRIDIOIDES_DIFFICILE",
                "PRECAUÇÃO DE CONTATO — CLOSTRIDIOIDES DIFFICILE",
                item(1, "CONTACT", "SUSPEITA OU CONFIRMAÇÃO DE CLOSTRIDIOIDES DIFFICILE",
                    "UNTIL_CCIH_REASSESSMENT", "CONTINUOUS")
            ),
            template(
                "DROPLET_RESPIRATORY_INFECTION",
                "PRECAUÇÃO POR GOTÍCULAS — INFECÇÃO RESPIRATÓRIA",
                item(1, "DROPLET",
                    "SUSPEITA OU CONFIRMAÇÃO DE INFECÇÃO TRANSMITIDA POR GOTÍCULAS",
                    "UNTIL_CCIH_REASSESSMENT", "CONTINUOUS")
            ),
            template(
                "AIRBORNE_TUBERCULOSIS",
                "PRECAUÇÃO POR AEROSSÓIS — TUBERCULOSE PULMONAR / LARÍNGEA",
                item(1, "AIRBORNE", "SUSPEITA OU CONFIRMAÇÃO DE TUBERCULOSE PULMONAR / LARÍNGEA",
                    "UNTIL_CCIH_REASSESSMENT", "CONTINUOUS")
            ),
            template(
                "AIRBORNE_AEROSOL_GENERATING_PROCEDURE",
                "PRECAUÇÃO POR AEROSSÓIS — PROCEDIMENTO GERADOR DE AEROSSOL",
                item(1, "AIRBORNE",
                    "PROCEDIMENTO GERADOR DE AEROSSOL EM PACIENTE COM INFECÇÃO RESPIRATÓRIA SUSPEITA OU CONFIRMADA",
                    "UNTIL_CCIH_REASSESSMENT", "CONTINUOUS")
            ),
            new IsolationPrecautionCatalog.Template(
                "AIRBORNE_CONTACT_VARICELLA",
                "AEROSSÓIS + CONTATO — VARICELA",
                List.of(
                    item(1, "AIRBORNE", "SUSPEITA OU CONFIRMAÇÃO DE VARICELA",
                        "UNTIL_CCIH_REASSESSMENT", "CONTINUOUS"),
                    item(2, "CONTACT", "SUSPEITA OU CONFIRMAÇÃO DE VARICELA",
                        "UNTIL_CCIH_REASSESSMENT", "CONTINUOUS")
                )
            ),
            template(
                "PROTECTIVE_SEVERE_NEUTROPENIA",
                "ISOLAMENTO PROTETOR — NEUTROPENIA / IMUNOSSUPRESSÃO GRAVE",
                item(1, "PROTECTIVE_REVERSE",
                    "NEUTROPENIA GRAVE (NEUTRÓFILOS < 500/MM³) OU IMUNOSSUPRESSÃO GRAVE",
                    "UNTIL_CCIH_REASSESSMENT", "CONTINUOUS")
            )
        );
    }

    private IsolationPrecautionCatalog.Template template(
        String code,
        String label,
        IsolationPrecautionRequest.Item item
    ) {
        return new IsolationPrecautionCatalog.Template(code, label, List.of(item));
    }

    private IsolationPrecautionRequest.Item item(
        int id,
        String precautionType,
        String reasonPathogen,
        String durationReview,
        String scheduling
    ) {
        return new IsolationPrecautionRequest.Item(
            id, precautionType, reasonPathogen, durationReview, scheduling
        );
    }

    private static IsolationPrecautionCatalog.Option option(String code, String label) {
        return new IsolationPrecautionCatalog.Option(code, label);
    }
}
