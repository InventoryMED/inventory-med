package br.com.inventorymed.clinical;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CriticalCareCatalogService {

    static final List<CriticalCareCatalog.Option> CLINICAL_CONTEXTS = List.of(
        option("ICU", "UTI"),
        option("ADULT_EMERGENCY", "BOX DE EMERGÊNCIA ADULTO"),
        option("WARD_HOSPITAL", "CLÍNICA MÉDICA / HOSPITAL")
    );
    static final List<CriticalCareCatalog.VasoactiveOption> VASOACTIVE_DRUGS = List.of(
        vasoactive("NOREPINEPHRINE", "NORADRENALINA", true, "STANDARD"),
        vasoactive("VASOPRESSIN", "VASOPRESSINA", true, "STANDARD"),
        vasoactive("DOBUTAMINE", "DOBUTAMINA", false, "STANDARD"),
        vasoactive("EPINEPHRINE_CONTINUOUS", "ADRENALINA — INFUSÃO CONTÍNUA", true, "STANDARD"),
        vasoactive("MILRINONE", "MILRINONA", false, "STANDARD"),
        vasoactive("NITROGLYCERIN", "NITROGLICERINA (TRIDIL)", false, "NON_ADSORPTIVE"),
        vasoactive("NITROPRUSSIDE", "NITROPRUSSIATO DE SÓDIO (NIPRIDE)", false, "PHOTOPROTECTIVE")
    );
    static final List<CriticalCareCatalog.SedationOption> SEDATION_DRUGS = List.of(
        sedation("FENTANYL", "FENTANIL", "ANALGESIC", List.of("CONTINUOUS_BIC")),
        sedation("PROPOFOL_1", "PROPOFOL 1%", "SEDATIVE", List.of("CONTINUOUS_BIC")),
        sedation("PROPOFOL_2", "PROPOFOL 2%", "SEDATIVE", List.of("CONTINUOUS_BIC")),
        sedation("MIDAZOLAM", "MIDAZOLAM (DORMONID)", "SEDATIVE", List.of("CONTINUOUS_BIC")),
        sedation("DEXMEDETOMIDINE", "DEXMEDETOMIDINA (PRECEDEX)", "SEDATIVE", List.of("CONTINUOUS_BIC")),
        sedation("KETAMINE", "KETAMINA (CETAMINA)", "SEDATIVE", List.of("CONTINUOUS_BIC")),
        sedation("ROCURONIUM", "ROCURÔNIO", "NEUROMUSCULAR_BLOCKER", List.of("CONTINUOUS_BIC", "SINGLE_DOSE")),
        sedation("CISATRACURIUM", "CISATRACÚRIO", "NEUROMUSCULAR_BLOCKER", List.of("CONTINUOUS_BIC")),
        sedation("SUCCINYLCHOLINE", "SUXAMETÔNIO (SUCCINILCOLINA)", "NEUROMUSCULAR_BLOCKER", List.of("SINGLE_DOSE"))
    );
    static final List<CriticalCareCatalog.EmergencyOption> EMERGENCY_DRUGS = List.of(
        emergency("SUGAMMADEX", "SUGAMADEX", true),
        emergency("NALOXONE", "NALOXONA", false),
        emergency("FLUMAZENIL", "FLUMAZENIL", false),
        emergency("PROTAMINE", "PROTAMINA", false),
        emergency("CALCIUM_GLUCONATE", "GLUCONATO DE CÁLCIO", false),
        emergency("CALCIUM_CHLORIDE", "CLORETO DE CÁLCIO", false),
        emergency("ATROPINE", "ATROPINA", false),
        emergency("AMIODARONE_BOLUS", "AMIODARONA — BOLUS", false),
        emergency("EPINEPHRINE_BOLUS", "ADRENALINA — BOLUS", false),
        emergency("SODIUM_BICARBONATE_84", "BICARBONATO DE SÓDIO 8,4%", false)
    );
    static final List<CriticalCareCatalog.Option> VASCULAR_ACCESSES = List.of(
        option("CVC", "CVC — ACESSO VENOSO CENTRAL"),
        option("TEMPORARY_PERIPHERAL", "ACESSO PERIFÉRICO TEMPORÁRIO")
    );
    static final List<CriticalCareCatalog.Option> BLOOD_PRESSURE_MONITORING = List.of(
        option("INVASIVE_ARTERIAL", "PRESSÃO ARTERIAL INVASIVA (PAI)"),
        option("FREQUENT_NON_INVASIVE", "PRESSÃO NÃO INVASIVA EM INTERVALOS FREQUENTES")
    );
    static final List<CriticalCareCatalog.Option> VASOACTIVE_RATE_UNITS = List.of(
        option("ML_H", "ML/H"), option("MCG_KG_MIN", "MCG/KG/MIN"), option("MCG_MIN", "MCG/MIN")
    );
    static final List<CriticalCareCatalog.Option> ADMINISTRATION_MODES = List.of(
        option("CONTINUOUS_BIC", "INFUSÃO CONTÍNUA EM BIC"),
        option("SINGLE_DOSE", "DOSE ÚNICA")
    );
    static final List<CriticalCareCatalog.Option> SEDATION_RATE_UNITS = List.of(
        option("ML_H", "ML/H"), option("MCG_KG_H", "MCG/KG/H"),
        option("MCG_KG_MIN", "MCG/KG/MIN"), option("MG_KG_H", "MG/KG/H"),
        option("MG_KG_MIN", "MG/KG/MIN"), option("MG_KG", "MG/KG")
    );
    static final List<CriticalCareCatalog.Option> SEDATION_TARGETS = List.of(
        option("RASS_MINUS_1", "RASS -1 — SEDAÇÃO LEVE"),
        option("RASS_MINUS_2", "RASS -2 — SEDAÇÃO LEVE"),
        option("RASS_MINUS_4", "RASS -4 — SEDAÇÃO PROFUNDA"),
        option("RASS_MINUS_5", "RASS -5 — NÃO DESPERTÁVEL"),
        option("BIS_40_60", "BIS 40–60 — SEDAÇÃO PROFUNDA")
    );
    static final List<CriticalCareCatalog.Option> VENTILATORY_STATUSES = List.of(
        option("VMI_TOT", "VMI VIA TUBO OROTRAQUEAL (TOT)"),
        option("VMI_TQT", "VMI VIA TRAQUEOSTOMIA (TQT)")
    );
    static final List<CriticalCareCatalog.Option> SEDATION_ROUTES = List.of(
        option("EV_BIC", "EV — BOMBA DE INFUSÃO"), option("EV", "ENDOVENOSA (EV)")
    );
    static final List<CriticalCareCatalog.Option> EMERGENCY_ROUTES = List.of(
        option("EV", "ENDOVENOSA (EV)"), option("IO", "INTRAÓSSEA (IO)"), option("CVC", "CVC")
    );
    static final List<CriticalCareCatalog.Option> EMERGENCY_SCHEDULING = List.of(
        option("NOW", "AGORA"), option("EMERGENCY", "EMERGÊNCIA"), option("PRN", "SE NECESSÁRIO (SN)")
    );

    public CriticalCareCatalog catalog() {
        return new CriticalCareCatalog(
            CLINICAL_CONTEXTS, VASOACTIVE_DRUGS, SEDATION_DRUGS, EMERGENCY_DRUGS,
            VASCULAR_ACCESSES, BLOOD_PRESSURE_MONITORING, VASOACTIVE_RATE_UNITS,
            ADMINISTRATION_MODES, SEDATION_RATE_UNITS, SEDATION_TARGETS,
            VENTILATORY_STATUSES, SEDATION_ROUTES, EMERGENCY_ROUTES, EMERGENCY_SCHEDULING
        );
    }

    public String optionLabel(List<CriticalCareCatalog.Option> options, String code) {
        return options.stream().filter(option -> option.code().equals(code)).findFirst()
            .map(CriticalCareCatalog.Option::label).orElse(code);
    }

    public CriticalCareCatalog.VasoactiveOption vasoactive(String code) {
        return VASOACTIVE_DRUGS.stream().filter(option -> option.code().equals(code)).findFirst().orElse(null);
    }

    public CriticalCareCatalog.SedationOption sedation(String code) {
        return SEDATION_DRUGS.stream().filter(option -> option.code().equals(code)).findFirst().orElse(null);
    }

    public CriticalCareCatalog.EmergencyOption emergency(String code) {
        return EMERGENCY_DRUGS.stream().filter(option -> option.code().equals(code)).findFirst().orElse(null);
    }

    private static CriticalCareCatalog.Option option(String code, String label) {
        return new CriticalCareCatalog.Option(code, label);
    }

    private static CriticalCareCatalog.VasoactiveOption vasoactive(
        String code, String label, boolean centralAccessRequired, String specialTubing
    ) {
        return new CriticalCareCatalog.VasoactiveOption(code, label, centralAccessRequired, specialTubing);
    }

    private static CriticalCareCatalog.SedationOption sedation(
        String code, String label, String category, List<String> allowedModes
    ) {
        return new CriticalCareCatalog.SedationOption(code, label, category, allowedModes);
    }

    private static CriticalCareCatalog.EmergencyOption emergency(String code, String label, boolean highCost) {
        return new CriticalCareCatalog.EmergencyOption(code, label, highCost);
    }
}
