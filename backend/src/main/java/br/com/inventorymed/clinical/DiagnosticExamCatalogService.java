package br.com.inventorymed.clinical;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticExamCatalogService {

    private static final List<DiagnosticExamCatalog.Option> CLINICAL_CONTEXTS = List.of(
        option("ADULT_ICU", "UTI ADULTO"),
        option("EMERGENCY", "URGÊNCIA / EMERGÊNCIA"),
        option("MEDICAL_WARD", "CLÍNICA MÉDICA / ENFERMARIA"),
        option("PEDIATRICS", "PEDIATRIA"),
        option("OPERATING_ROOM", "BLOCO CIRÚRGICO / SALA R")
    );

    private static final List<DiagnosticExamCatalog.ExamOption> BLOOD_EXAMS = List.of(
        exam("HEMOGRAM", "HEMOGRAMA COMPLETO COM PLAQUETAS", "40304361", "0202020382", false,
            "TUBO VACUTAINER EDTA 4 ML (ROXO)"),
        exam("UREA", "UREIA", null, null, false, "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("CREATININE", "CREATININA SÉRICA", "40301630", "0202010310", false,
            "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("SODIUM", "SÓDIO (NA)", null, null, false, "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("POTASSIUM", "POTÁSSIO (K)", null, null, false, "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("MAGNESIUM", "MAGNÉSIO", null, null, false, "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("IONIC_CALCIUM", "CÁLCIO IÔNICO", null, null, false, "SERINGA HEPARINIZADA"),
        exam("CRP", "PROTEÍNA C REATIVA (PCR QUANTITATIVA)", "40302319", "0202030205", false,
            "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("ESR", "VSH", null, null, false, "TUBO PARA VSH"),
        exam("COAGULATION", "COAGULOGRAMA (TAP / INR / TTPA)", null, null, false,
            "TUBO VACUTAINER CITRATO (AZUL)"),
        exam("TROPONIN", "TROPONINA I / T QUANTITATIVA", "40302580", "0202030906", true,
            "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("CK_MB", "CK-MB", null, null, false,
            "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("PROCALCITONIN", "PROCALCITONINA (PCT)", null, null, true,
            "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("LACTATE", "LACTATO", null, null, false,
            "SERINGA HEPARINIZADA / TUBO VALIDADO PARA LACTATO"),
        exam("ARTERIAL_BLOOD_GAS", "GASOMETRIA ARTERIAL COM ELETRÓLITOS E LACTATO", null, null, false,
            "SERINGA HEPARINIZADA PARA GASOMETRIA"),
        exam("VENOUS_BLOOD_GAS", "GASOMETRIA VENOSA", null, null, false,
            "SERINGA HEPARINIZADA PARA GASOMETRIA"),
        exam("BLOOD_CULTURE_SITE_1", "HEMOCULTURA AERÓBIA + ANAERÓBIA (PAR 1 — SÍTIO 1)",
            "40310159", null, false, "02 X FRASCOS DE HEMOCULTURA"),
        exam("BLOOD_CULTURE_SITE_2", "HEMOCULTURA AERÓBIA + ANAERÓBIA (PAR 2 — SÍTIO 2)",
            "40310159", null, false, "02 X FRASCOS DE HEMOCULTURA"),
        exam("AST", "TGO (AST)", null, null, false, "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("ALT", "TGP (ALT)", null, null, false, "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("BILIRUBIN", "BILIRRUBINAS TOTAIS E FRAÇÕES", null, null, false,
            "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("AMYLASE", "AMILASE", null, null, false, "TUBO VACUTAINER GEL SEPARADOR (AMARELO)"),
        exam("LIPASE", "LIPASE", null, null, false, "TUBO VACUTAINER GEL SEPARADOR (AMARELO)")
    );

    private static final List<DiagnosticExamCatalog.ExamOption> URINE_EXAMS = List.of(
        exam("EAS", "URINA TIPO 1 (EAS)", "40309029", "0202050022", false,
            "FRASCO COLETOR ESTÉRIL DE URINA", "TUBO CÔNICO"),
        exam("URINE_CULTURE", "UROCULTURA COM ANTIBIOGRAMA / TSA", "40310400", "0202050162", false,
            "FRASCO COLETOR ESTÉRIL DE URINA"),
        exam("CREATININE_CLEARANCE", "CLEARANCE DE CREATININA (URINA 24 HORAS)", null, null, false,
            "FRASCO COLETOR PARA URINA DE 24 HORAS"),
        exam("RBC_DYSMORPHISM", "PESQUISA DE DISMORFISMO ERITROCITÁRIO", null, null, false,
            "FRASCO COLETOR ESTÉRIL DE URINA")
    );

    private static final List<DiagnosticExamCatalog.ExamOption> TOMOGRAPHY_REGIONS = List.of(
        exam("HEAD", "CRÂNIO", null, null, true),
        exam("CHEST", "TÓRAX", "40805018", "0206010079", true),
        exam("TOTAL_ABDOMEN", "ABDOME TOTAL", null, null, true),
        exam("UPPER_ABDOMEN", "ABDOME SUPERIOR", null, null, true),
        exam("PELVIS", "PELVE", null, null, true),
        exam("CERVICAL_SPINE", "COLUNA CERVICAL", null, null, true),
        exam("LUMBAR_SPINE", "COLUNA LOMBAR", null, null, true),
        exam("ANGIOGRAPHY", "ANGIOTOMOGRAFIA (TEP / AORTA)", null, null, true)
    );

    private static final List<DiagnosticExamCatalog.Option> CONTRAST_OPTIONS = List.of(
        option("WITHOUT_CONTRAST", "SEM CONTRASTE"),
        option("WITH_CONTRAST", "COM CONTRASTE"),
        option("WITH_AND_WITHOUT_CONTRAST", "COM E SEM CONTRASTE")
    );

    private static final List<DiagnosticExamCatalog.Option> TOMOGRAPHY_JUSTIFICATIONS = List.of(
        option("SUSPECTED_TEP", "SUSPEITA DE TEP / INSTABILIDADE HEMODINÂMICA"),
        option("ABSCESS_APPENDICITIS", "ABSCESSO / APENDICITE AGUDA"),
        option("ACUTE_STROKE", "AVC AGUDO < 4,5 H"),
        option("TRAUMA", "TRAUMA / LESÃO AGUDA"),
        option("OTHER", "OUTRA JUSTIFICATIVA CLÍNICA")
    );

    private static final List<DiagnosticExamCatalog.XrayProfile> XRAY_PROFILES = List.of(
        xray("CHEST", "TÓRAX", null, null, false,
            List.of("AP", "PA"), "AP", "PA", "PROFILE", "PORTABLE"),
        xray("ELBOW", "COTOVELO", "40802051", "0204040051", true,
            List.of("AP", "PROFILE"), "AP", "PROFILE", "OBLIQUE"),
        xray("SHOULDER", "OMBRO", null, null, true,
            List.of("TRUE_AP", "SCAPULAR_Y"), "TRUE_AP", "SCAPULAR_Y", "AXILLARY"),
        xray("KNEE", "JOELHO", null, null, true,
            List.of("AP", "PROFILE"), "AP", "PROFILE", "WEIGHT_BEARING", "PATELLA_AXIAL"),
        xray("ANKLE_FOOT", "TORNOZELO / PÉ", null, null, true,
            List.of("AP", "PROFILE", "OBLIQUE"), "AP", "PROFILE", "OBLIQUE"),
        xray("LUMBAR_SPINE", "COLUNA LOMBAR", null, null, false,
            List.of("AP", "PROFILE"), "AP", "PROFILE", "DYNAMIC"),
        xray("CERVICAL_SPINE", "COLUNA CERVICAL", null, null, false,
            List.of("AP", "PROFILE"), "AP", "PROFILE", "DYNAMIC"),
        xray("ABDOMEN", "ABDOME", null, null, false,
            List.of("UPRIGHT_AP", "SUPINE_AP"), "UPRIGHT_AP", "SUPINE_AP")
    );

    private static final List<DiagnosticExamCatalog.Option> LATERALITIES = List.of(
        option("RIGHT", "DIREITO"),
        option("LEFT", "ESQUERDO"),
        option("BILATERAL", "BILATERAL"),
        option("NOT_APPLICABLE", "NÃO SE APLICA")
    );

    private static final List<DiagnosticExamCatalog.UltrasoundOption> ULTRASOUND_EXAMS = List.of(
        ultrasound("TOTAL_ABDOMEN", "USG ABDOME TOTAL", null, null, false,
            "DOR EM QDR", "SUSPEITA DE COLECISTITE", "ASCITE"),
        ultrasound("UPPER_ABDOMEN", "USG ABDOME SUPERIOR", null, null, false,
            "DOR ABDOMINAL", "ALTERAÇÃO HEPATOBILIAR"),
        ultrasound("URINARY_TRACT", "USG APARELHO URINÁRIO / VIAS URINÁRIAS", null, null, false,
            "CÓLICA NEFRÉTICA", "HIDRONEFROSE"),
        ultrasound("VENOUS_DOPPLER_LOWER_LIMB", "USG DOPPLER VENOSO DE MEMBROS INFERIORES",
            "40901386", "0205010032", true, "SUSPEITA DE TVP", "ASSIMETRIA DE PANTURRILHAS"),
        ultrasound("POCUS_FAST", "USG BEIRA-LEITO (POCUS / FAST / E-FAST)", null, null, false,
            "TRAUMA", "CHOQUE A ESCLARECER")
    );

    private static final List<DiagnosticExamCatalog.Option> ECG_PRIORITIES = List.of(
        option("IMMEDIATE", "IMEDIATO / AGORA"),
        option("ROUTINE_06", "ROTINA 06H"),
        option("SERIAL_6H", "SERIADO 6/6H")
    );

    private static final List<DiagnosticExamCatalog.Option> ECG_JUSTIFICATIONS = List.of(
        option("CHEST_PAIN", "DOR TORÁCICA"),
        option("ARRHYTHMIA", "ARRITMIA"),
        option("PREOPERATIVE", "PRÉ-OPERATÓRIO"),
        option("SEPSIS", "PROTOCOLO DE SEPSE"),
        option("ELECTROLYTE_DISORDER", "DISTÚRBIO ELETROLÍTICO"),
        option("OTHER", "OUTRA JUSTIFICATIVA CLÍNICA")
    );

    private static final List<DiagnosticExamCatalog.QuickPack> QUICK_PACKS = List.of(
        pack("ROUTINE_ICU", "+ PAINEL ROTINA / ADMISSÃO UTI",
            List.of("HEMOGRAM", "UREA", "CREATININE", "SODIUM", "POTASSIUM", "CRP", "COAGULATION", "ARTERIAL_BLOOD_GAS"),
            List.of(), null, null, null, false),
        pack("SEPSIS", "+ PAINEL SEPSE / INFECÇÃO",
            List.of("BLOOD_CULTURE_SITE_1", "BLOOD_CULTURE_SITE_2", "LACTATE", "PROCALCITONIN", "HEMOGRAM", "CRP", "ARTERIAL_BLOOD_GAS"),
            List.of(), null, null, null, false),
        pack("CHEST_PAIN", "+ PAINEL DOR TORÁCICA / IAM",
            List.of("TROPONIN", "CK_MB"), List.of(), null, "CHEST", null, true),
        pack("ACUTE_ABDOMEN", "+ PAINEL ABDOME AGUDO",
            List.of("HEMOGRAM", "AMYLASE", "LIPASE", "CRP", "AST", "ALT"),
            List.of(), null, null, "TOTAL_ABDOMEN", false)
    );

    public DiagnosticExamCatalog catalog() {
        return new DiagnosticExamCatalog(
            CLINICAL_CONTEXTS, QUICK_PACKS, BLOOD_EXAMS, URINE_EXAMS, TOMOGRAPHY_REGIONS,
            CONTRAST_OPTIONS, TOMOGRAPHY_JUSTIFICATIONS, XRAY_PROFILES, LATERALITIES,
            ULTRASOUND_EXAMS, ECG_PRIORITIES, ECG_JUSTIFICATIONS
        );
    }

    public DiagnosticExamCatalog.ExamOption bloodExam(String code) {
        return find(BLOOD_EXAMS, code);
    }

    public DiagnosticExamCatalog.ExamOption urineExam(String code) {
        return find(URINE_EXAMS, code);
    }

    public DiagnosticExamCatalog.ExamOption tomographyRegion(String code) {
        return find(TOMOGRAPHY_REGIONS, code);
    }

    public DiagnosticExamCatalog.XrayProfile xrayProfile(String code) {
        return XRAY_PROFILES.stream().filter(item -> item.code().equals(code)).findFirst().orElse(null);
    }

    public DiagnosticExamCatalog.UltrasoundOption ultrasound(String code) {
        return ULTRASOUND_EXAMS.stream().filter(item -> item.code().equals(code)).findFirst().orElse(null);
    }

    public String optionLabel(List<DiagnosticExamCatalog.Option> options, String code) {
        return options.stream().filter(item -> item.code().equals(code))
            .map(DiagnosticExamCatalog.Option::label).findFirst().orElse(code);
    }

    private DiagnosticExamCatalog.ExamOption find(
        List<DiagnosticExamCatalog.ExamOption> options,
        String code
    ) {
        return options.stream().filter(item -> item.code().equals(code)).findFirst().orElse(null);
    }

    private static DiagnosticExamCatalog.ExamOption exam(
        String code,
        String label,
        String tuss,
        String sigtap,
        boolean highCost,
        String... supplies
    ) {
        return new DiagnosticExamCatalog.ExamOption(
            code, label, tuss, sigtap, highCost, List.of(supplies)
        );
    }

    private static DiagnosticExamCatalog.XrayProfile xray(
        String code,
        String label,
        String tuss,
        String sigtap,
        boolean paired,
        List<String> defaults,
        String... incidenceCodes
    ) {
        List<DiagnosticExamCatalog.Option> incidences = java.util.Arrays.stream(incidenceCodes)
            .map(DiagnosticExamCatalogService::incidence)
            .toList();
        return new DiagnosticExamCatalog.XrayProfile(
            code, label, tuss, sigtap, paired, incidences, defaults
        );
    }

    private static DiagnosticExamCatalog.Option incidence(String code) {
        return option(code, switch (code) {
            case "AP" -> "AP";
            case "PA" -> "PA";
            case "PROFILE" -> "PERFIL";
            case "PORTABLE" -> "LEITO (PORTÁTIL)";
            case "OBLIQUE" -> "OBLÍQUA";
            case "TRUE_AP" -> "AP VERDADEIRO";
            case "SCAPULAR_Y" -> "PERFIL DE ESCÁPULA (Y)";
            case "AXILLARY" -> "AXILAR";
            case "WEIGHT_BEARING" -> "CARGA (ORTOSTÁTICA)";
            case "PATELLA_AXIAL" -> "AXIAL DE PATELA";
            case "DYNAMIC" -> "DINÂMICAS (FLEXÃO / EXTENSÃO)";
            case "UPRIGHT_AP" -> "AP ORTOSTÁTICO";
            case "SUPINE_AP" -> "AP DECÚBITO";
            default -> code;
        });
    }

    private static DiagnosticExamCatalog.UltrasoundOption ultrasound(
        String code,
        String label,
        String tuss,
        String sigtap,
        boolean paired,
        String... justifications
    ) {
        List<DiagnosticExamCatalog.Option> values = java.util.stream.IntStream
            .range(0, justifications.length)
            .mapToObj(index -> option("J" + (index + 1), justifications[index]))
            .toList();
        return new DiagnosticExamCatalog.UltrasoundOption(
            code, label, tuss, sigtap, paired, values
        );
    }

    private static DiagnosticExamCatalog.QuickPack pack(
        String code,
        String label,
        List<String> blood,
        List<String> urine,
        String tomography,
        String xray,
        String ultrasound,
        boolean ecg
    ) {
        return new DiagnosticExamCatalog.QuickPack(
            code, label, blood, urine, tomography, xray, ultrasound, ecg
        );
    }

    private static DiagnosticExamCatalog.Option option(String code, String label) {
        return new DiagnosticExamCatalog.Option(code, label);
    }
}
