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

class DietPrescriptionServiceTest {

    private DietPrescriptionService service;

    @BeforeEach
    void setUp() {
        service = new DietPrescriptionService(new ObjectMapper());
    }

    @Test
    void buildsOralDietWithMultipleRestrictions() {
        DietPrescriptionResponse response = service.preview(new DietPrescriptionRequest(
            DietType.ORAL,
            new DietPrescriptionRequest.OralParameters(
                "BRANDA",
                List.of("HIPOSSÓDICA (HAS / CARDIOLOGIA)", "HIPOGLÍDICA / DIABÉTICA (DM)")
            ),
            null,
            null,
            null
        ));

        assertThat(response.structuredDiet().summaryLine())
            .contains("DIETA ORAL BRANDA")
            .contains("HIPOSSÓDICA")
            .contains("HIPOGLÍDICA");
        assertThat(response.billingAudit().itemsForReview())
            .containsExactly("DIETA HOSPITALAR ESPECIALIZADA — CONFERIR COBERTURA E CONTRATO");
    }

    @Test
    void requiresRateForContinuousEnteralDiet() {
        DietPrescriptionRequest request = new DietPrescriptionRequest(
            DietType.ENTERAL,
            null,
            new DietPrescriptionRequest.EnteralParameters(
                "SONDA NASOENTÉRICA (SNE)",
                "CONTÍNUO EM BOMBA DE INFUSÃO",
                "POLIMÉRICA HIPERCALÓRICA / HIPERPROTEICA",
                null,
                null,
                null,
                new BigDecimal("50"),
                "4/4H"
            ),
            null,
            null
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Informe uma vazão válida em ML/H");
    }

    @Test
    void requiresClinicalJustificationForParenteralNutrition() {
        DietPrescriptionRequest request = new DietPrescriptionRequest(
            DietType.PARENTERAL,
            null,
            null,
            new DietPrescriptionRequest.ParenteralParameters(
                "ACESSO VENOSO CENTRAL (CVC)",
                "INDUSTRIALIZADA PRONTA (3 EM 1)",
                new BigDecimal("1500"),
                new BigDecimal("62.5"),
                null,
                null,
                ""
            ),
            null
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Informe a justificativa clínica para a nutrição parenteral");
    }

    @Test
    void normalizesDietBeforeStoringClinicalDocument() {
        Map<String, Object> stored = service.normalizeForClinicalDocument(Map.of(
            "type", "JEJUM",
            "fasting", Map.of(
                "reason", "EXAME DIAGNÓSTICO",
                "reassessment", "24 HORAS"
            )
        ));

        assertThat(stored).containsKeys("request", "structuredDiet", "billingAudit");
        @SuppressWarnings("unchecked")
        Map<String, Object> structured = (Map<String, Object>) new ObjectMapper().convertValue(
            stored.get("structuredDiet"),
            Map.class
        );
        assertThat(structured.get("summaryLine").toString()).contains("JEJUM");
    }

    @Test
    void rejectsUnknownFieldsInClinicalDocument() {
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "type", "ORAL",
            "oral", Map.of("consistency", "BRANDA", "restrictions", List.of()),
            "script", "CONTEÚDO NÃO PERMITIDO"
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("A dieta contém campos não permitidos");
    }
}
