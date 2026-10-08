package br.com.inventorymed.clinical;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BedsideProcedureCatalogService {

    static final String ASEPSIS = "CLOREXIDINA DEGERMANTE 2% E CLOREXIDINA ALCOÓLICA 0,5%";
    static final String BARRIER = "BARREIRA ESTÉRIL MÁXIMA: GORRO, MÁSCARA, AVENTAL, LUVAS E CAMPO AMPLO";
    static final String ANESTHESIA = "LIDOCAÍNA 2% SEM VASOCONSTRITOR, CONFORME AVALIAÇÃO CLÍNICA";
    static final String MONITORING = "MONITORIZAÇÃO CONTÍNUA COM ECG, SPO2 E PANI; ASSISTÊNCIA DE ENFERMAGEM";
    static final String CHEST_XRAY = "RADIOGRAFIA DE TÓRAX NO LEITO — REFERÊNCIA INFORMADA TUSS 40805018";

    static final List<BedsideProcedureCatalog.Option> RECORD_TYPES = List.of(
        option("REQUESTED", "SOLICITADO / PLANEJADO"),
        option("PERFORMED", "REALIZADO")
    );
    static final List<BedsideProcedureCatalog.Option> LATERALITIES = List.of(
        option("RIGHT", "DIREITA"),
        option("LEFT", "ESQUERDA"),
        option("MIDLINE", "LINHA MÉDIA"),
        option("NOT_APPLICABLE", "NÃO SE APLICA")
    );
    static final List<BedsideProcedureCatalog.Option> URGENCY_OPTIONS = List.of(
        option("NOW", "AGORA"),
        option("IMMEDIATE", "IMEDIATO"),
        option("URGENT", "URGENTE"),
        option("EMERGENCY", "EMERGENCIAL")
    );
    static final List<BedsideProcedureCatalog.Option> POST_CONTROLS = List.of(
        option("CHEST_XRAY", CHEST_XRAY),
        option("ULTRASOUND", "ULTRASSONOGRAFIA / POCUS DE CONTROLE"),
        option("OTHER_IMAGE", "OUTRO EXAME DE IMAGEM DE CONTROLE"),
        option("CLINICAL_JUSTIFICATION", "NÃO REALIZADO — JUSTIFICATIVA CLÍNICA OBRIGATÓRIA"),
        option("NOT_APPLICABLE", "NÃO SE APLICA")
    );
    static final List<BedsideProcedureCatalog.ProcedureOption> PROCEDURES = List.of(
        procedure("CVC", "CATETERISMO VENOSO CENTRAL POR PUNÇÃO", "TUSS 31401606", true, true, true,
            "VEIA JUGULAR INTERNA", "POCUS EM TEMPO REAL, SE DISPONÍVEL", "KIT CVC DUPLO LÚMEN",
            "7 FR X 20 CM", "MONONYLON 2-0 E FILME TRANSPARENTE COM CLOREXIDINA",
            "SEM AMOSTRAS", "CHEST_XRAY"),
        procedure("IOT", "INTUBAÇÃO OROTRAQUEAL DE EMERGÊNCIA", "TUSS 31403021", false, true, true,
            "VIA OROTRAQUEAL", "LARINGOSCOPIA / VIDEOLARINGOSCOPIA", "CÂNULA OROTRAQUEAL COM CUFF",
            "7,5", "FIXADOR DE TUBO, FILTRO HME E CIRCUITO FECHADO", "SEM AMOSTRAS", "CHEST_XRAY"),
        procedure("TRACHEOSTOMY_PERCUTANEOUS", "TRAQUEOSTOMIA PERCUTÂNEA BEIRA-LEITO", "TUSS 31401304", false, true, true,
            "TRAQUEIA — LINHA MÉDIA", "BRONCOSCOPIA / POCUS CONFORME DISPONIBILIDADE", "KIT DE TRAQUEOSTOMIA PERCUTÂNEA",
            "CÂNULA 8,0 COM CUFF", "FIXAÇÃO DA CÂNULA E CURATIVO ESTÉRIL", "SEM AMOSTRAS", "CHEST_XRAY"),
        procedure("TRACHEOSTOMY_SURGICAL", "TRAQUEOSTOMIA CIRÚRGICA", "TUSS 31401304", false, true, true,
            "TRAQUEIA — LINHA MÉDIA", "CONFORME TÉCNICA CIRÚRGICA", "CÂNULA DE TRAQUEOSTOMIA COM CUFF",
            "8,0", "FIXAÇÃO DA CÂNULA E CURATIVO ESTÉRIL", "SEM AMOSTRAS", "CHEST_XRAY"),
        procedure("CHEST_DRAINAGE", "DRENAGEM TUBULAR FECHADA DE TÓRAX", "TUSS 31401100", true, true, true,
            "5º ESPAÇO INTERCOSTAL — LINHA AXILAR MÉDIA", "POCUS PARA DEMARCAÇÃO, SE DISPONÍVEL",
            "DRENO TORÁCICO RADIOPACO E FRASCO COM SELO D'ÁGUA", "32 FR / FRASCO 2.000 ML",
            "MONONYLON E GAZE VASELINADA", "SEM AMOSTRAS", "CHEST_XRAY"),
        procedure("THORACENTESIS", "TORACOCENTESE DIAGNÓSTICA / ALIVIADORA", "TUSS 31401401", true, true, false,
            "HEMITÓRAX", "POCUS PARA DEMARCAÇÃO", "CATETER PARA TORACOCENTESE", "CONFORME KIT",
            "CURATIVO ESTÉRIL OCLUSIVO", "CITOLOGIA, BIOQUÍMICA, GRAM, CULTURA E ANTIBIOGRAMA", "NOT_APPLICABLE"),
        procedure("PARACENTESIS_DIAGNOSTIC", "PARACENTESE ABDOMINAL DIAGNÓSTICA", "TUSS 31401703", true, true, false,
            "FOSSA ILÍACA / QUADRANTE INFERIOR", "POCUS PARA DEMARCAÇÃO", "CATETER / JELCO", "14–16 G",
            "CURATIVO ESTÉRIL OCLUSIVO", "CONTAGEM CELULAR, ALBUMINA, PROTEÍNAS, GRAM E CULTURA", "NOT_APPLICABLE"),
        procedure("PARACENTESIS_RELIEF", "PARACENTESE ABDOMINAL ALIVIADORA", "TUSS 31401703", true, true, false,
            "FOSSA ILÍACA / QUADRANTE INFERIOR", "POCUS PARA DEMARCAÇÃO", "CATETER, EQUIPO E BOLSA COLETORA", "14–16 G",
            "CURATIVO ESTÉRIL OCLUSIVO", "AMOSTRAS CONFORME INDICAÇÃO; AVALIAR ALBUMINA 20% SE > 5 L", "NOT_APPLICABLE"),
        procedure("LUMBAR_PUNCTURE_DIAGNOSTIC", "PUNÇÃO LOMBAR DIAGNÓSTICA", "TUSS 31401509", false, true, false,
            "INTERESPAÇO L3-L4 OU L4-L5", "NÃO SE APLICA", "AGULHA DE RAQUIA E MANÔMETRO DE LCR", "20–22 G",
            "CURATIVO ESTÉRIL OCLUSIVO", "CITOLOGIA, BIOQUÍMICA, MICROBIOLOGIA E PAINEL VIRAL", "NOT_APPLICABLE"),
        procedure("LUMBAR_PUNCTURE_THERAPEUTIC", "PUNÇÃO LOMBAR TERAPÊUTICA", "TUSS 31401509", false, true, false,
            "INTERESPAÇO L3-L4 OU L4-L5", "NÃO SE APLICA", "AGULHA DE RAQUIA E MANÔMETRO DE LCR", "20–22 G",
            "CURATIVO ESTÉRIL OCLUSIVO", "AMOSTRAS CONFORME INDICAÇÃO", "NOT_APPLICABLE"),
        procedure("OTHER", "OUTRO PROCEDIMENTO BEIRA-LEITO", "SEM REFERÊNCIA FINANCEIRA PRÉ-DEFINIDA", false, false, false,
            "", "CONFORME TÉCNICA DO PROCEDIMENTO", "", "", "CURATIVO E FIXAÇÃO CONFORME INDICAÇÃO", "CONFORME INDICAÇÃO", "NOT_APPLICABLE")
    );

    public BedsideProcedureCatalog catalog() {
        return new BedsideProcedureCatalog(
            RECORD_TYPES, PROCEDURES, LATERALITIES, URGENCY_OPTIONS, POST_CONTROLS,
            PROCEDURES.stream().filter(item -> !"OTHER".equals(item.code())).map(item ->
                new BedsideProcedureCatalog.Template(item.code(), item.label(), new BedsideProcedureCatalog.ProcedureItem(
                    item.code(), item.defaultSite(), defaultLaterality(item),
                    item.defaultAsepsis(), item.defaultSterileBarrier(), item.defaultAnesthesia(),
                    item.defaultImageGuidance(), item.defaultDevice(), item.defaultCaliber(),
                    item.defaultFixation(), item.defaultSamples(), item.defaultPostProcedureControl(),
                    item.defaultMonitoring(), "URGENT"
                ))
            ).toList()
        );
    }

    public BedsideProcedureCatalog.ProcedureOption procedure(String code) {
        return PROCEDURES.stream().filter(item -> item.code().equals(code)).findFirst().orElse(null);
    }

    public String optionLabel(List<BedsideProcedureCatalog.Option> values, String code) {
        return values.stream().filter(item -> item.code().equals(code)).findFirst()
            .map(BedsideProcedureCatalog.Option::label).orElse(code);
    }

    private String defaultLaterality(BedsideProcedureCatalog.ProcedureOption item) {
        if (item.code().startsWith("PARACENTESIS")) return "LEFT";
        if (item.code().startsWith("LUMBAR_PUNCTURE")) return "MIDLINE";
        return item.pairedSite() ? "RIGHT" : "NOT_APPLICABLE";
    }

    private static BedsideProcedureCatalog.Option option(String code, String label) {
        return new BedsideProcedureCatalog.Option(code, label);
    }

    private static BedsideProcedureCatalog.ProcedureOption procedure(
        String code, String label, String billingReference, boolean pairedSite,
        boolean traceability, boolean postControl, String site, String guidance,
        String device, String caliber, String fixation, String samples, String control
    ) {
        return new BedsideProcedureCatalog.ProcedureOption(
            code, label, billingReference, pairedSite, traceability, postControl, site,
            ASEPSIS, BARRIER, ANESTHESIA, guidance, device, caliber, fixation, samples,
            control, MONITORING
        );
    }
}
