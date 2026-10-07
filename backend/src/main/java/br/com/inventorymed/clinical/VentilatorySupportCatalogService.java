package br.com.inventorymed.clinical;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VentilatorySupportCatalogService {

    static final List<VentilatorySupportCatalog.Option> SUPPORT_TYPES = List.of(
        option("ROOM_AIR", "AR AMBIENTE"),
        option("LOW_FLOW", "OXIGENOTERAPIA DE BAIXO FLUXO"),
        option("HIGH_FLOW", "CÂNULA NASAL DE ALTO FLUXO (CNAF)"),
        option("NIV", "VENTILAÇÃO NÃO INVASIVA (VNI)"),
        option("IMV", "VENTILAÇÃO MECÂNICA INVASIVA (VMI)")
    );
    static final List<VentilatorySupportCatalog.Option> LOW_FLOW_DEVICES = List.of(
        option("NASAL_CANNULA", "CATETER NASAL DE O₂"),
        option("VENTURI_MASK", "MÁSCARA DE VENTURI"),
        option("NON_REBREATHER_MASK", "MÁSCARA COM RESERVATÓRIO / NÃO REINALANTE"),
        option("SIMPLE_FACE_MASK", "MÁSCARA FACIAL SIMPLES")
    );
    static final List<VentilatorySupportCatalog.Option> LOW_FLOW_FREQUENCIES = List.of(
        option("CONTINUOUS", "CONTÍNUO"),
        option("PRN_SPO2_92", "SE NECESSÁRIO SE SPO₂ < 92%"),
        option("PRN_SPO2_88_COPD", "SE NECESSÁRIO SE SPO₂ < 88% EM DPOC"),
        option("INTERMITTENT", "INTERMITENTE")
    );
    static final List<VentilatorySupportCatalog.Option> HIGH_FLOW_SIZES = List.of(
        option("P", "P"), option("M", "M"), option("G", "G")
    );
    static final List<VentilatorySupportCatalog.Option> NIV_MODES = List.of(
        option("CPAP", "CPAP"), option("BIPAP", "BIPAP (BILEVEL)"), option("PSV_PEEP", "PSV + PEEP")
    );
    static final List<VentilatorySupportCatalog.Option> NIV_INTERFACES = List.of(
        option("NASAL_MASK", "MÁSCARA NASAL"),
        option("ORONASAL_MASK", "MÁSCARA ORONASAL / FACIAL TOTAL"),
        option("TOTAL_FACE", "INTERFACE TOTAL FACE")
    );
    static final List<VentilatorySupportCatalog.Option> NIV_FREQUENCIES = List.of(
        option("CONTINUOUS", "CONTÍNUO"),
        option("EVERY_12H", "12/12H"),
        option("NIGHT", "NOTURNO"),
        option("PRN", "SE NECESSÁRIO")
    );
    static final List<VentilatorySupportCatalog.Option> INVASIVE_AIRWAYS = List.of(
        option("TOT", "TUBO OROTRAQUEAL (TOT)"), option("TQT", "CÂNULA DE TRAQUEOSTOMIA (TQT)")
    );
    static final List<VentilatorySupportCatalog.Option> INVASIVE_MODES = List.of(
        option("VCV", "VCV — VOLUME CONTROLADO"),
        option("PCV", "PCV — PRESSÃO CONTROLADA"),
        option("PSV", "PSV — PRESSÃO DE SUPORTE")
    );
    static final List<VentilatorySupportCatalog.Option> SCHEDULING = List.of(
        option("FIXED", "FIXO"), option("CONTINUOUS", "CONTÍNUO"), option("PRN", "SN")
    );
    static final List<VentilatorySupportCatalog.Option> PROTECTIVE_GOALS = List.of(
        option("PROTECTIVE_VT_6", "VENTILAÇÃO PROTETORA — VT 6 ML/KG DE PESO PREDITO"),
        option("PEAK_LT_30", "MANTER PRESSÃO DE PICO < 30 CMH₂O"),
        option("DRIVING_LT_15", "MANTER DRIVING PRESSURE < 15 CMH₂O")
    );

    public VentilatorySupportCatalog catalog() {
        return new VentilatorySupportCatalog(
            SUPPORT_TYPES,
            LOW_FLOW_DEVICES,
            LOW_FLOW_FREQUENCIES,
            HIGH_FLOW_SIZES,
            NIV_MODES,
            NIV_INTERFACES,
            NIV_FREQUENCIES,
            INVASIVE_AIRWAYS,
            INVASIVE_MODES,
            SCHEDULING,
            PROTECTIVE_GOALS,
            templates()
        );
    }

    public String label(List<VentilatorySupportCatalog.Option> options, String code) {
        return options.stream()
            .filter(option -> option.code().equals(code))
            .findFirst()
            .map(VentilatorySupportCatalog.Option::label)
            .orElse(code);
    }

    public boolean contains(List<VentilatorySupportCatalog.Option> options, String code) {
        return options.stream().anyMatch(option -> option.code().equals(code));
    }

    public boolean templateExists(String code) {
        return templates().stream().anyMatch(template -> template.code().equals(code));
    }

    private List<VentilatorySupportCatalog.Template> templates() {
        return List.of(
            new VentilatorySupportCatalog.Template(
                "ROOM_AIR",
                "AR AMBIENTE",
                item(1, "ROOM_AIR", "CONTINUOUS", "CONTINUOUS", null, null, null, null)
            ),
            new VentilatorySupportCatalog.Template(
                "NASAL_CANNULA_2L_PRN_92",
                "CATETER NASAL 2 L/MIN — SN SE SPO₂ < 92%",
                item(
                    1, "LOW_FLOW", "PRN_SPO2_92", "PRN",
                    new VentilatorySupportRequest.LowFlow("NASAL_CANNULA", new BigDecimal("2"), null),
                    null, null, null
                )
            ),
            new VentilatorySupportCatalog.Template(
                "HIGH_FLOW_TO_CONFIGURE",
                "CNAF — PREENCHER PARÂMETROS",
                item(
                    1, "HIGH_FLOW", "CONTINUOUS", "CONTINUOUS", null,
                    new VentilatorySupportRequest.HighFlow(null, null, null, "M"), null, null
                )
            ),
            new VentilatorySupportCatalog.Template(
                "NIV_BIPAP_TO_CONFIGURE",
                "VNI BIPAP — PREENCHER PARÂMETROS",
                item(
                    1, "NIV", "CONTINUOUS", "CONTINUOUS", null, null,
                    new VentilatorySupportRequest.NonInvasive(
                        "BIPAP", null, null, null, null, null, "ORONASAL_MASK", null
                    ),
                    null
                )
            ),
            new VentilatorySupportCatalog.Template(
                "IMV_VCV_PROTECTIVE_TO_CONFIGURE",
                "VMI VCV PROTETORA — PREENCHER PARÂMETROS",
                item(
                    1, "IMV", "CONTINUOUS", "CONTINUOUS", null, null, null,
                    new VentilatorySupportRequest.Invasive(
                        "TOT", "", "VCV", null, null, null, null, null, null, null, null, null, null,
                        List.of("PROTECTIVE_VT_6", "PEAK_LT_30", "DRIVING_LT_15")
                    )
                )
            )
        );
    }

    private static VentilatorySupportRequest.Item item(
        int id,
        String supportType,
        String frequency,
        String scheduling,
        VentilatorySupportRequest.LowFlow lowFlow,
        VentilatorySupportRequest.HighFlow highFlow,
        VentilatorySupportRequest.NonInvasive nonInvasive,
        VentilatorySupportRequest.Invasive invasive
    ) {
        return new VentilatorySupportRequest.Item(
            id, supportType, frequency, scheduling, lowFlow, highFlow, nonInvasive, invasive
        );
    }

    private static VentilatorySupportCatalog.Option option(String code, String label) {
        return new VentilatorySupportCatalog.Option(code, label);
    }
}
