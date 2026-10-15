package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ReferralServiceTest {

    private ReferralService service;

    @BeforeEach
    void setUp() {
        service = new ReferralService(null, new ObjectMapper());
    }

    @Test
    void normalizesReferralAndKeepsOptionalAttachmentsEmpty() {
        ReferralResponse response = service.previewWithoutAdmission(request(
            "PULMONOLOGY",
            null,
            "MEDIUM",
            "UP_TO_15_DAYS",
            List.of(),
            true
        ));

        assertThat(response.finalizationAllowed()).isTrue();
        assertThat(response.specialtyLabel()).isEqualTo("PNEUMOLOGIA");
        assertThat(response.attachmentLabels()).isEmpty();
        assertThat(response.summaryLine()).contains("ATÉ 15 DIAS");
    }

    @Test
    void requiresFreeDescriptionForOtherSpecialty() {
        assertThatThrownBy(() -> service.previewWithoutAdmission(request(
            "OTHER",
            " ",
            "ELECTIVE",
            "REGULATION_DISCRETION",
            List.of(),
            true
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("especialidade");
    }

    @Test
    void blocksFinalizationWithoutPhysicianAttestation() {
        ReferralResponse response = service.previewWithoutAdmission(request(
            "CARDIOLOGY",
            "ARRITMOLOGIA",
            "URGENT_HIGH",
            "UP_TO_7_DAYS",
            List.of("DISCHARGE_SUMMARY"),
            false
        ));

        assertThat(response.finalizationAllowed()).isFalse();
        assertThat(response.blockingReasons()).containsExactly(
            "Confirme a revisão e a responsabilidade médica antes de finalizar"
        );
    }

    @Test
    void rejectsUnknownCatalogValues() {
        assertThatThrownBy(() -> service.previewWithoutAdmission(request(
            "PULMONOLOGY",
            null,
            "IMMEDIATE",
            "UP_TO_7_DAYS",
            List.of(),
            true
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("Prioridade inválida");
    }

    @Test
    void rejectsUnknownDocumentFieldsBeforePersistence() {
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(
            Map.of(
                "specialtyCode", "PULMONOLOGY",
                "specialtyDetail", "",
                "priority", "MEDIUM",
                "deadline", "UP_TO_15_DAYS",
                "clinicalJustification", "REAVALIAÇÃO APÓS ALTA HOSPITALAR",
                "destination", "PUBLIC_REGULATION",
                "attachments", List.of(),
                "physicianAttestation", true,
                "hospitalId", "outro-hospital"
            ),
            true
        ))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("formato inválido");
    }

    private ReferralRequest request(
        String specialty,
        String detail,
        String priority,
        String deadline,
        List<String> attachments,
        boolean attestation
    ) {
        return new ReferralRequest(
            specialty,
            detail,
            priority,
            deadline,
            "PACIENTE NECESSITA DE ACOMPANHAMENTO ESPECIALIZADO APÓS A INTERNAÇÃO.",
            "PUBLIC_REGULATION",
            attachments,
            attestation
        );
    }
}
