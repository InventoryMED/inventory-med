package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MedicalRecipeServiceTest {

    private MedicalRecipeService service;

    @BeforeEach
    void setUp() {
        service = new MedicalRecipeService(null, new ObjectMapper());
    }

    @Test
    void allowsCommonRecipeWithOptionalRecommendations() {
        MedicalRecipeResponse response = service.previewWithRecordedAllergies(
            "SEM ALERGIAS",
            request("COMMON", "LOSARTANA 50 MG", null, true)
        );

        assertThat(response.finalizationAllowed()).isTrue();
        assertThat(response.copyLabels()).containsExactly("VIA DO PACIENTE");
        assertThat(response.request().items().getFirst().recommendations()).isNull();
    }

    @Test
    void formatsSpecialControlAsTwoCopies() {
        MedicalRecipeResponse response = service.previewWithRecordedAllergies(
            null,
            request("SPECIAL_CONTROL", "MEDICAMENTO CONTROLADO", "USAR APÓS ALIMENTAÇÃO", true)
        );

        assertThat(response.finalizationAllowed()).isTrue();
        assertThat(response.copyLabels()).containsExactly(
            "1ª VIA — RETIDA NA FARMÁCIA",
            "2ª VIA — PACIENTE"
        );
    }

    @Test
    void blocksNotificationBUntilOfficialIntegrationsExist() {
        MedicalRecipeResponse response = service.previewWithRecordedAllergies(
            null,
            request("NOTIFICATION_B", "MEDICAMENTO", null, true)
        );

        assertThat(response.finalizationAllowed()).isFalse();
        assertThat(response.blockingReasons()).anyMatch(reason -> reason.contains("SNCR"));
        assertThat(response.printTitle()).contains("NÃO VÁLIDO");
    }

    @Test
    void blocksTextualMatchWithRecordedAllergy() {
        MedicalRecipeResponse response = service.previewWithRecordedAllergies(
            "DIPIRONA; PENICILINA",
            request("COMMON", "DIPIRONA SÓDICA 500 MG", null, true)
        );

        assertThat(response.finalizationAllowed()).isFalse();
        assertThat(response.allergyConflicts()).containsExactly("DIPIRONA SÓDICA 500 MG ↔ DIPIRONA");
    }

    @Test
    void rejectsUnknownDocumentFieldsBeforePersistence() {
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(
            null,
            null,
            Map.of(
                "recipeType", "COMMON",
                "items", List.of(),
                "physicianAttestation", true,
                "unsafeHtml", "<script>"
            ),
            true
        ))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("formato inválido");
    }

    private MedicalRecipeRequest request(
        String type,
        String medication,
        String recommendations,
        boolean attestation
    ) {
        return new MedicalRecipeRequest(
            type,
            List.of(new MedicalRecipeRequest.Item(
                "ORAL",
                medication,
                "30 COMPRIMIDOS",
                "TOMAR 1 COMPRIMIDO DE 12/12H",
                recommendations
            )),
            null,
            null,
            null,
            attestation
        );
    }
}
