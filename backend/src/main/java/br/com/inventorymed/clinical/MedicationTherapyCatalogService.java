package br.com.inventorymed.clinical;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MedicationTherapyCatalogService {

    static final List<MedicationTherapyCatalog.Option> CLINICAL_CONTEXTS = List.of(
        option("ICU", "UTI"),
        option("ADULT_EMERGENCY", "BOX DE EMERGÊNCIA ADULTO"),
        option("WARD_HOSPITAL", "CLÍNICA MÉDICA / HOSPITAL")
    );
    static final List<MedicationTherapyCatalog.MedicationOption> ANTIMICROBIALS = List.of(
        medication("CEFTRIAXONE", "CEFTRIAXONA"),
        medication("MEROPENEM", "MEROPENEM"),
        medication("VANCOMYCIN", "VANCOMICINA"),
        medication("PIPERACILLIN_TAZOBACTAM", "PIPERACILINA + TAZOBACTAM"),
        medication("FLUCONAZOLE", "FLUCONAZOL"),
        medication("ACYCLOVIR", "ACICLOVIR"),
        medication("OTHER", "OUTRO ANTIMICROBIANO")
    );
    static final List<MedicationTherapyCatalog.Option> ANTIMICROBIAL_ROUTES = List.of(
        option("EV", "ENDOVENOSA (EV)"),
        option("VO", "VIA ORAL (VO)"),
        option("IM", "INTRAMUSCULAR (IM)"),
        option("INHALED", "INALATÓRIA")
    );
    static final List<MedicationTherapyCatalog.Option> ANTIMICROBIAL_ADMINISTRATION_MODES = List.of(
        option("BOLUS", "BOLUS"),
        option("RAPID_INFUSION", "INFUSÃO RÁPIDA"),
        option("INTERMITTENT_INFUSION", "INFUSÃO INTERMITENTE"),
        option("ORAL", "ADMINISTRAÇÃO ORAL"),
        option("INTRAMUSCULAR", "ADMINISTRAÇÃO INTRAMUSCULAR"),
        option("INHALED", "ADMINISTRAÇÃO INALATÓRIA")
    );
    static final List<MedicationTherapyCatalog.Option> DILUENTS = List.of(
        option("NONE", "SEM DILUENTE"),
        option("SF_09_100", "SF 0,9% 100 ML"),
        option("SG_5_100", "SG 5% 100 ML"),
        option("OTHER", "OUTRO DILUENTE")
    );
    static final List<MedicationTherapyCatalog.Option> INFUSION_SETS = List.of(
        option("NONE", "SEM EQUIPO"),
        option("MACRODRIP", "EQUIPO MACROGOTAS"),
        option("MICRODRIP", "EQUIPO MICROGOTAS")
    );
    static final List<MedicationTherapyCatalog.Option> ANTIMICROBIAL_SCHEDULING = List.of(
        option("FIXED", "FIXO"),
        option("SINGLE_DOSE", "DOSE ÚNICA"),
        option("ACM", "ACM — A CRITÉRIO MÉDICO"),
        option("LOADING_FIXED", "DOSE DE ATAQUE + FIXO")
    );
    static final List<MedicationTherapyCatalog.Option> CCIH_STATUSES = List.of(
        option("AUTHORIZED", "LIBERADO PELA CCIH"),
        option("OPINION_RECORDED", "PARECER DA CCIH REGISTRADO"),
        option("REQUESTED", "PARECER SOLICITADO / PENDENTE"),
        option("NOT_APPLICABLE", "NÃO SE APLICA PELO PROTOCOLO INSTITUCIONAL")
    );
    static final List<MedicationTherapyCatalog.Option> RENAL_FUNCTION_MEASURES = List.of(
        option("EGFR", "TFG ESTIMADA (ML/MIN/1,73 M²)"),
        option("CREATININE_CLEARANCE", "CLEARANCE DE CREATININA (ML/MIN)")
    );
    static final List<MedicationTherapyCatalog.Option> RENAL_DOSE_ASSESSMENTS = List.of(
        option("ADJUSTED", "DOSE AJUSTADA À FUNÇÃO RENAL"),
        option("NO_ADJUSTMENT_REQUIRED", "REVISADO — SEM AJUSTE NECESSÁRIO")
    );
    static final List<MedicationTherapyCatalog.ProphylaxisOption> PROPHYLAXIS_OPTIONS = List.of(
        prophylaxis("ENOXAPARIN", "ENOXAPARINA", "TEV_FARMACOLOGICA", true, true, false),
        prophylaxis("UFH", "HEPARINA NÃO FRACIONADA (HNF)", "TEV_FARMACOLOGICA", true, false, false),
        prophylaxis("FONDAPARINUX", "FONDAPARINUX", "TEV_FARMACOLOGICA", true, true, false),
        prophylaxis("GRADUATED_COMPRESSION", "MEIAS DE COMPRESSÃO GRADUADA", "TEV_MECANICA", false, false, true),
        prophylaxis("IPC", "COMPRESSOR PNEUMÁTICO INTERMITENTE (CPI)", "TEV_MECANICA", false, false, true),
        prophylaxis("OMEPRAZOLE", "OMEPRAZOL", "LAMG", false, false, false),
        prophylaxis("PANTOPRAZOLE", "PANTOPRAZOL", "LAMG", false, false, false),
        prophylaxis("SUCRALFATE", "SUCRALFATO", "LAMG", false, false, false)
    );
    static final List<MedicationTherapyCatalog.Option> PROPHYLAXIS_ROUTES = List.of(
        option("SC", "SUBCUTÂNEA (SC)"),
        option("EV", "ENDOVENOSA (EV)"),
        option("VO", "VIA ORAL (VO)"),
        option("MECHANICAL", "DISPOSITIVO MECÂNICO")
    );
    static final List<MedicationTherapyCatalog.Option> PROPHYLAXIS_SCHEDULING = List.of(
        option("FIXED", "FIXO"),
        option("ACM", "ACM — A CRITÉRIO MÉDICO"),
        option("SUSPEND", "SUSPENDER")
    );
    static final List<MedicationTherapyCatalog.Option> RECONCILIATION_STATUSES = List.of(
        option("MAINTAINED_HOME", "MANTIDO DO DOMICÍLIO"),
        option("ADJUSTED_HOSPITALIZATION", "AJUSTADO NA INTERNAÇÃO"),
        option("TEMPORARILY_SUSPENDED", "SUSPENSO TEMPORARIAMENTE")
    );
    static final List<MedicationTherapyCatalog.Option> CONTINUOUS_MEDICATION_SCHEDULING = List.of(
        option("FIXED", "FIXO"),
        option("ACM", "ACM — A CRITÉRIO MÉDICO"),
        option("TEMPORARILY_SUSPENDED", "SUSPENSO TEMPORARIAMENTE")
    );
    static final List<MedicationTherapyCatalog.MedicationOption> SYMPTOMATIC_MEDICATIONS = List.of(
        medication("DIPYRONE", "DIPIRONA"),
        medication("PARACETAMOL", "PARACETAMOL"),
        medication("TRAMADOL", "TRAMADOL"),
        medication("MORPHINE", "MORFINA"),
        medication("ONDANSETRON", "ONDANSETRONA"),
        medication("HYOSCINE", "BUSCOPAN"),
        medication("METOCLOPRAMIDE", "METOCLOPRAMIDA"),
        medication("LACTULOSE", "LACTULOSE"),
        medication("SIMETHICONE", "SIMETICONA"),
        medication("OTHER", "OUTRO MEDICAMENTO")
    );
    static final List<MedicationTherapyCatalog.Option> MEDICATION_ROUTES = List.of(
        option("EV", "ENDOVENOSA (EV)"),
        option("VO", "VIA ORAL (VO)"),
        option("IM", "INTRAMUSCULAR (IM)"),
        option("SC", "SUBCUTÂNEA (SC)"),
        option("INHALED", "INALATÓRIA"),
        option("SL", "SUBLINGUAL (SL)"),
        option("RECTAL", "RETAL")
    );
    static final List<MedicationTherapyCatalog.Option> SYMPTOMATIC_SCHEDULING = List.of(
        option("FIXED", "FIXO"),
        option("PRN", "SN — SE NECESSÁRIO"),
        option("ACM", "ACM — A CRITÉRIO MÉDICO"),
        option("PRN_FIXED", "SN / FIXO — ESQUEMA MISTO")
    );

    public MedicationTherapyCatalog catalog() {
        return new MedicationTherapyCatalog(
            CLINICAL_CONTEXTS, ANTIMICROBIALS, ANTIMICROBIAL_ROUTES,
            ANTIMICROBIAL_ADMINISTRATION_MODES, DILUENTS, INFUSION_SETS,
            ANTIMICROBIAL_SCHEDULING, CCIH_STATUSES, RENAL_FUNCTION_MEASURES,
            RENAL_DOSE_ASSESSMENTS, PROPHYLAXIS_OPTIONS, PROPHYLAXIS_ROUTES,
            PROPHYLAXIS_SCHEDULING, RECONCILIATION_STATUSES,
            CONTINUOUS_MEDICATION_SCHEDULING, SYMPTOMATIC_MEDICATIONS,
            MEDICATION_ROUTES, SYMPTOMATIC_SCHEDULING
        );
    }

    public String optionLabel(List<MedicationTherapyCatalog.Option> options, String code) {
        return options.stream().filter(option -> option.code().equals(code)).findFirst()
            .map(MedicationTherapyCatalog.Option::label).orElse(code);
    }

    public MedicationTherapyCatalog.MedicationOption antimicrobial(String code) {
        return ANTIMICROBIALS.stream().filter(item -> item.code().equals(code)).findFirst().orElse(null);
    }

    public MedicationTherapyCatalog.ProphylaxisOption prophylaxis(String code) {
        return PROPHYLAXIS_OPTIONS.stream().filter(item -> item.code().equals(code)).findFirst().orElse(null);
    }

    public MedicationTherapyCatalog.MedicationOption symptomatic(String code) {
        return SYMPTOMATIC_MEDICATIONS.stream().filter(item -> item.code().equals(code)).findFirst().orElse(null);
    }

    private static MedicationTherapyCatalog.Option option(String code, String label) {
        return new MedicationTherapyCatalog.Option(code, label);
    }

    private static MedicationTherapyCatalog.MedicationOption medication(String code, String label) {
        return new MedicationTherapyCatalog.MedicationOption(code, label);
    }

    private static MedicationTherapyCatalog.ProphylaxisOption prophylaxis(
        String code,
        String label,
        String category,
        boolean anticoagulant,
        boolean filledSyringe,
        boolean equipment
    ) {
        return new MedicationTherapyCatalog.ProphylaxisOption(
            code, label, category, anticoagulant, filledSyringe, equipment
        );
    }
}
