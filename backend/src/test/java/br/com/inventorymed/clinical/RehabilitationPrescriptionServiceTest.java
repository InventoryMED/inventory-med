package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class RehabilitationPrescriptionServiceTest {

    private RehabilitationPrescriptionService service;

    @BeforeEach
    void setUp() {
        RehabilitationPrescriptionCatalogService catalog =
            new RehabilitationPrescriptionCatalogService();
        service = new RehabilitationPrescriptionService(
            catalog,
            new RehabilitationPrescriptionValidator(catalog),
            new ObjectMapper()
        );
    }

    @Test
    void buildsRespiratoryAndMotorRehabilitationWithSessionReview() {
        RehabilitationPrescriptionResponse response = service.preview(
            new RehabilitationPrescriptionRequest(
                "",
                List.of(
                    item(
                        1, "RESPIRATORY_PHYSIOTHERAPY", "BRONCHIAL_HYGIENE",
                        "EVERY_12H", "FIXED", "RETENÇÃO DE SECREÇÃO E SUPORTE VENTILATÓRIO"
                    ),
                    item(
                        2, "MOTOR_PHYSIOTHERAPY", "PASSIVE_MOBILIZATION",
                        "DAILY", "FIXED", ""
                    )
                )
            )
        );

        assertThat(response.structuredRehabilitation().summaryLine())
            .contains("FISIOTERAPIA RESPIRATÓRIA")
            .contains("12/12H")
            .contains("MOBILIZAÇÃO PASSIVA");
        assertThat(response.structuredRehabilitation().orderRows()).hasSize(2);
        assertThat(response.billingAudit().itemsForReview())
            .allMatch(value -> value.contains("CONFERIR CÓDIGO TUSS/SIGTAP"));
        assertThat(response.billingAudit().auditAlerts())
            .anyMatch(value -> value.contains("2 SESSÕES DIÁRIAS"));
    }

    @Test
    void buildsSpeechTherapySafetyAlertForSwallowingAssessment() {
        RehabilitationPrescriptionResponse response = service.preview(
            new RehabilitationPrescriptionRequest(
                "SPEECH_SWALLOWING_ASSESSMENT",
                List.of(item(
                    1, "SPEECH_THERAPY", "SWALLOWING_ASSESSMENT",
                    "SINGLE_ASSESSMENT", "FIXED", ""
                ))
            )
        );

        assertThat(response.structuredRehabilitation().prescriptionDetails())
            .contains("AVALIAÇÃO CLÍNICA DA DEGLUTIÇÃO");
        assertThat(response.billingAudit().auditAlerts())
            .anyMatch(value -> value.contains("VIA ALIMENTAR"));
    }

    @Test
    void requiresClinicalJustificationForTwoOrThreeDailyPhysiotherapySessions() {
        RehabilitationPrescriptionRequest request = new RehabilitationPrescriptionRequest(
            "",
            List.of(item(
                1, "MOTOR_PHYSIOTHERAPY", "SITTING_TRAINING",
                "EVERY_8H", "FIXED", ""
            ))
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Informe a justificativa clínica para duas ou três sessões diárias de fisioterapia");
    }

    @Test
    void rejectsProcedureFromAnotherSpecialtyAndIncompatibleScheduling() {
        assertThatThrownBy(() -> service.preview(new RehabilitationPrescriptionRequest(
            "",
            List.of(item(
                1, "SPEECH_THERAPY", "PASSIVE_MOBILIZATION",
                "DAILY", "FIXED", ""
            ))
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Selecione uma opção válida para conduta");

        assertThatThrownBy(() -> service.preview(new RehabilitationPrescriptionRequest(
            "",
            List.of(item(
                1, "OCCUPATIONAL_THERAPY", "COGNITIVE_SENSORY_REHABILITATION",
                "AS_NEEDED", "FIXED", ""
            ))
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("O aprazamento não corresponde à frequência de reabilitação selecionada");
    }

    @Test
    void normalizesAndRejectsUnknownDocumentFields() {
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "selectedTemplate", "MOTOR_PASSIVE_DAILY",
            "items", List.of(Map.of(
                "id", 1,
                "specialty", "MOTOR_PHYSIOTHERAPY",
                "procedure", "PASSIVE_MOBILIZATION",
                "frequency", "DAILY",
                "scheduling", "FIXED",
                "clinicalJustification", ""
            ))
        ));

        assertThat(normalized).containsKeys("request", "structuredRehabilitation", "billingAudit");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "selectedTemplate", "",
            "items", List.of(Map.of(
                "id", 1,
                "specialty", "MOTOR_PHYSIOTHERAPY",
                "procedure", "PASSIVE_MOBILIZATION",
                "frequency", "DAILY",
                "scheduling", "FIXED",
                "clinicalJustification", "",
                "automaticBilling", true
            ))
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("A reabilitação contém campos não permitidos");
    }

    private RehabilitationPrescriptionRequest.Item item(
        int id,
        String specialty,
        String procedure,
        String frequency,
        String scheduling,
        String clinicalJustification
    ) {
        return new RehabilitationPrescriptionRequest.Item(
            id, specialty, procedure, frequency, scheduling, clinicalJustification
        );
    }
}
