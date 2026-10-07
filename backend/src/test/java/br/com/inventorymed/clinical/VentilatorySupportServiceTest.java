package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class VentilatorySupportServiceTest {

    private VentilatorySupportService service;

    @BeforeEach
    void setUp() {
        VentilatorySupportCatalogService catalog = new VentilatorySupportCatalogService();
        service = new VentilatorySupportService(
            catalog,
            new VentilatorySupportValidator(catalog),
            new ObjectMapper()
        );
    }

    @Test
    void buildsLowFlowOxygenWithPrnTriggerAndReviewItems() {
        VentilatorySupportResponse response = service.preview(new VentilatorySupportRequest(
            "NASAL_CANNULA_2L_PRN_92",
            List.of(item(
                "LOW_FLOW", "PRN_SPO2_92", "PRN",
                new VentilatorySupportRequest.LowFlow("NASAL_CANNULA", new BigDecimal("2"), null),
                null, null, null
            ))
        ));

        assertThat(response.structuredVentilatorySupport().summaryLine())
            .contains("CATETER NASAL DE O₂")
            .contains("2 L/MIN")
            .contains("SPO₂ < 92%");
        assertThat(response.structuredVentilatorySupport().orderRows()).singleElement()
            .satisfies(row -> {
                assertThat(row.frequency()).contains("SPO₂ < 92%");
                assertThat(row.scheduling()).isEqualTo("SN");
            });
        assertThat(response.billingAudit().itemsForReview())
            .allMatch(value -> value.contains("CONFERIR"));
    }

    @Test
    void buildsHighFlowAndNonInvasiveSupportWithoutAutomaticBilling() {
        VentilatorySupportRequest.HighFlow highFlow = new VentilatorySupportRequest.HighFlow(
            new BigDecimal("45"), 50, new BigDecimal("34"), "M"
        );
        VentilatorySupportRequest.NonInvasive nonInvasive = new VentilatorySupportRequest.NonInvasive(
            "BIPAP", new BigDecimal("14"), new BigDecimal("8"), null, 40, 14,
            "ORONASAL_MASK", new BigDecimal("2")
        );

        VentilatorySupportResponse response = service.preview(new VentilatorySupportRequest(
            "",
            List.of(
                item("HIGH_FLOW", "CONTINUOUS", "CONTINUOUS", null, highFlow, null, null),
                new VentilatorySupportRequest.Item(
                    2, "NIV", "EVERY_12H", "FIXED", null, null, nonInvasive, null
                )
            )
        ));

        assertThat(response.structuredVentilatorySupport().prescriptionDetails())
            .contains("CÂNULA NASAL DE ALTO FLUXO")
            .contains("IPAP 14 CMH₂O")
            .contains("2 H POR SESSÃO");
        assertThat(response.billingAudit().itemsForReview())
            .anyMatch(value -> value.contains("GERADOR DE ALTO FLUXO"))
            .anyMatch(value -> value.contains("VENTILADOR PARA VNI"))
            .allMatch(value -> value.contains("CONFERIR"));
    }

    @Test
    void buildsProtectiveInvasiveVentilation() {
        VentilatorySupportRequest.Invasive invasive = new VentilatorySupportRequest.Invasive(
            "TOT", "Nº 8.0", "VCV", 420, 18, new BigDecimal("8"), 40,
            new BigDecimal("60"), null, null, null, null, null,
            List.of("PROTECTIVE_VT_6", "PEAK_LT_30", "DRIVING_LT_15")
        );

        VentilatorySupportResponse response = service.preview(new VentilatorySupportRequest(
            "IMV_VCV_PROTECTIVE_TO_CONFIGURE",
            List.of(item("IMV", "CONTINUOUS", "CONTINUOUS", null, null, null, invasive))
        ));

        assertThat(response.structuredVentilatorySupport().summaryLine())
            .contains("VMI VCV")
            .contains("VT 420 ML")
            .contains("TUBO OROTRAQUEAL (TOT) Nº 8.0");
        assertThat(response.structuredVentilatorySupport().prescriptionDetails())
            .contains("VT 6 ML/KG")
            .contains("DRIVING PRESSURE < 15 CMH₂O");
        assertThat(response.billingAudit().auditAlerts())
            .anyMatch(value -> value.contains("PROFISSIONAL HABILITADO"));
    }

    @Test
    void rejectsIncompatibleBipapPressuresAndScheduling() {
        VentilatorySupportRequest.NonInvasive invalidPressure = new VentilatorySupportRequest.NonInvasive(
            "BIPAP", new BigDecimal("6"), new BigDecimal("8"), null, 40, 14,
            "ORONASAL_MASK", null
        );
        VentilatorySupportRequest request = new VentilatorySupportRequest(
            "",
            List.of(item("NIV", "CONTINUOUS", "CONTINUOUS", null, null, invalidPressure, null))
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("No BiPAP, a IPAP deve ser maior que a EPAP");

        VentilatorySupportRequest.LowFlow lowFlow = new VentilatorySupportRequest.LowFlow(
            "NASAL_CANNULA", new BigDecimal("2"), null
        );
        assertThatThrownBy(() -> service.preview(new VentilatorySupportRequest(
            "", List.of(item("LOW_FLOW", "PRN_SPO2_92", "FIXED", lowFlow, null, null, null))
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("O aprazamento não corresponde à frequência selecionada");
    }

    @Test
    void normalizesAndRejectsUnknownDocumentFields() {
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "selectedTemplate", "ROOM_AIR",
            "items", List.of(Map.of(
                "id", 1,
                "supportType", "ROOM_AIR",
                "frequency", "CONTINUOUS",
                "scheduling", "CONTINUOUS"
            ))
        ));

        assertThat(normalized).containsKeys("request", "structuredVentilatorySupport", "billingAudit");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "selectedTemplate", "ROOM_AIR",
            "items", List.of(Map.of(
                "id", 1,
                "supportType", "ROOM_AIR",
                "frequency", "CONTINUOUS",
                "scheduling", "CONTINUOUS",
                "automaticBilling", true
            ))
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("O suporte ventilatório contém campos não permitidos");
    }

    private VentilatorySupportRequest.Item item(
        String supportType,
        String frequency,
        String scheduling,
        VentilatorySupportRequest.LowFlow lowFlow,
        VentilatorySupportRequest.HighFlow highFlow,
        VentilatorySupportRequest.NonInvasive nonInvasive,
        VentilatorySupportRequest.Invasive invasive
    ) {
        return new VentilatorySupportRequest.Item(
            1, supportType, frequency, scheduling, lowFlow, highFlow, nonInvasive, invasive
        );
    }
}
