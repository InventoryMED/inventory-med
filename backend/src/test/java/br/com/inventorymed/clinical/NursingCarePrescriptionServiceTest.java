package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class NursingCarePrescriptionServiceTest {

    private NursingCarePrescriptionService service;

    @BeforeEach
    void setUp() {
        service = new NursingCarePrescriptionService(new ObjectMapper());
    }

    @Test
    void buildsCriticalCarePrescriptionAndProducesReviewItems() {
        NursingCarePrescriptionCatalog catalog = service.catalog();
        NursingCarePrescriptionResponse response = service.preview(new NursingCarePrescriptionRequest(
            new NursingCarePrescriptionRequest.Positioning(
                catalog.headPositions().getFirst(),
                catalog.repositioningFrequencies().getFirst(),
                List.of(catalog.pressureProtection().get(1))
            ),
            new NursingCarePrescriptionRequest.HygieneSkin(
                catalog.baths().getFirst(),
                catalog.oralHygiene().getFirst(),
                List.of(catalog.skinCare().getFirst())
            ),
            new NursingCarePrescriptionRequest.DressingsDrains(
                catalog.catheterDressings().getFirst(),
                "",
                catalog.complexWoundCoverages().get(1),
                catalog.dressingChangeFrequencies().getFirst(),
                List.of()
            ),
            new NursingCarePrescriptionRequest.Procedures(
                catalog.airwaySuction().getFirst(),
                List.of(catalog.deviceCare().get(1)),
                catalog.fluidBalance().getFirst()
            )
        ));

        assertThat(response.structuredCare().summaryLine())
            .contains("CUIDADOS DE ENFERMAGEM")
            .contains("30° - 45°")
            .contains("CLOREXIDINA");
        assertThat(response.structuredCare().prescriptionDetails()).contains("- CURATIVO DE CATETER:");
        assertThat(response.billingAudit().itemsForReview())
            .anyMatch(item -> item.contains("COLCHÃO PNEUMÁTICO"))
            .anyMatch(item -> item.contains("COBERTURA ESPECIAL"));
        assertThat(response.billingAudit().qualitySafetyIndicators())
            .anyMatch(item -> item.contains("PROTOCOLO INSTITUCIONAL"));
    }

    @Test
    void requiresFrequencyWhenComplexWoundCoverageIsSelected() {
        NursingCarePrescriptionCatalog catalog = service.catalog();
        NursingCarePrescriptionRequest request = new NursingCarePrescriptionRequest(
            null,
            null,
            new NursingCarePrescriptionRequest.DressingsDrains(
                "", "", catalog.complexWoundCoverages().getFirst(), "", List.of()
            ),
            null
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Informe a cobertura e a frequência de troca da ferida complexa");
    }

    @Test
    void rejectsEmptyCarePlan() {
        NursingCarePrescriptionRequest request = new NursingCarePrescriptionRequest(
            new NursingCarePrescriptionRequest.Positioning("", "", List.of()),
            null,
            null,
            null
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Selecione ao menos um cuidado de enfermagem");
    }

    @Test
    void normalizesAndRejectsUnknownDocumentFields() {
        String headPosition = service.catalog().headPositions().getFirst();
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "positioning", Map.of(
                "headPosition", headPosition,
                "repositioningFrequency", "",
                "pressureProtection", List.of()
            )
        ));

        assertThat(normalized).containsKeys("request", "structuredCare", "billingAudit");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "positioning", Map.of("headPosition", headPosition),
            "automaticBilling", true
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Os cuidados de enfermagem contêm campos não permitidos");
    }
}
