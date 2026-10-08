package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class IsolationPrecautionServiceTest {

    private IsolationPrecautionService service;

    @BeforeEach
    void setUp() {
        IsolationPrecautionCatalogService catalog = new IsolationPrecautionCatalogService();
        service = new IsolationPrecautionService(
            catalog,
            new IsolationPrecautionValidator(catalog),
            new ObjectMapper()
        );
    }

    @Test
    void buildsContactPrecautionAndMdrCcIhAlert() {
        IsolationPrecautionResponse response = service.preview(new IsolationPrecautionRequest(
            "CONTACT_KPC_MDR",
            List.of(item(
                1,
                "CONTACT",
                "COLONIZAÇÃO POR KPC / ENTEROBACTÉRIA MULTIRRESISTENTE",
                "ENTIRE_HOSPITALIZATION",
                "CONTINUOUS"
            ))
        ));

        assertThat(response.structuredIsolation().prescriptionDetails())
            .startsWith("1. PRECAUÇÕES E ISOLAMENTO:")
            .contains("AVENTAL / CAPOTE E LUVAS")
            .contains("ESTETOSCÓPIO, ESFIGMOMANÔMETRO E TERMÔMETRO DEDICADOS");
        assertThat(response.structuredIsolation().orderRows()).hasSize(1);
        assertThat(response.billingAudit().auditAlerts())
            .anyMatch(value -> value.contains("CULTURA PRÉVIA OU SWAB"));
        assertThat(response.billingAudit().suppliesEquipmentForReview())
            .anyMatch(value -> value.contains("DIÁRIA DE ISOLAMENTO"));
    }

    @Test
    void buildsAirbornePrecautionWithRespiratoryProtectionAndTransportMeasure() {
        IsolationPrecautionResponse response = service.preview(new IsolationPrecautionRequest(
            "AIRBORNE_TUBERCULOSIS",
            List.of(item(
                1,
                "AIRBORNE",
                "SUSPEITA DE TUBERCULOSE PULMONAR",
                "UNTIL_CCIH_REASSESSMENT",
                "CONTINUOUS"
            ))
        ));

        assertThat(response.structuredIsolation().prescriptionDetails())
            .contains("PFF2 / N95")
            .contains("PORTA FECHADA")
            .contains("MÁSCARA CIRÚRGICA QUANDO O TRANSPORTE FOR INDISPENSÁVEL");
        assertThat(response.billingAudit().auditAlerts())
            .anyMatch(value -> value.contains("CONFERIR CÓDIGO TUSS/SIGTAP"));
    }

    @Test
    void addsSoapAndWaterMeasureForClostridioidesDifficile() {
        IsolationPrecautionResponse response = service.preview(new IsolationPrecautionRequest(
            "CONTACT_CLOSTRIDIOIDES_DIFFICILE",
            List.of(item(
                1,
                "CONTACT",
                "CLOSTRIDIOIDES DIFFICILE CONFIRMADO",
                "UNTIL_CCIH_REASSESSMENT",
                "CONTINUOUS"
            ))
        ));

        assertThat(response.structuredIsolation().prescriptionDetails())
            .contains("ÁGUA E SABONETE");
    }

    @Test
    void rejectsBlankReasonAndUnknownPrecautionType() {
        assertThatThrownBy(() -> service.preview(new IsolationPrecautionRequest(
            "",
            List.of(item(1, "CONTACT", "", "REVIEW_7_DAYS", "FIXED"))
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Informe o motivo, patógeno ou condição clínica da precaução");

        assertThatThrownBy(() -> service.preview(new IsolationPrecautionRequest(
            "",
            List.of(item(1, "UNKNOWN", "TESTE", "REVIEW_7_DAYS", "FIXED"))
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Selecione uma opção válida para tipo de precaução");
    }

    @Test
    void normalizesAndRejectsUnknownDocumentFields() {
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "selectedTemplate", "DROPLET_RESPIRATORY_INFECTION",
            "items", List.of(Map.of(
                "id", 1,
                "precautionType", "DROPLET",
                "reasonPathogen", "SUSPEITA DE INFLUENZA",
                "durationReview", "UNTIL_CCIH_REASSESSMENT",
                "scheduling", "CONTINUOUS"
            ))
        ));

        assertThat(normalized).containsKeys("request", "structuredIsolation", "billingAudit");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "selectedTemplate", "",
            "items", List.of(Map.of(
                "id", 1,
                "precautionType", "DROPLET",
                "reasonPathogen", "SUSPEITA DE INFLUENZA",
                "durationReview", "UNTIL_CCIH_REASSESSMENT",
                "scheduling", "CONTINUOUS",
                "automaticBilling", true
            ))
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("As precauções contêm campos não permitidos");
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
}
