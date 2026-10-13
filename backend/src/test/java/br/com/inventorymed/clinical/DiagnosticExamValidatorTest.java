package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DiagnosticExamValidatorTest {

    private DiagnosticExamCatalogService catalog;
    private DiagnosticExamValidator validator;

    @BeforeEach
    void setUp() {
        catalog = new DiagnosticExamCatalogService();
        validator = new DiagnosticExamValidator(catalog);
    }

    @Test
    void catalogExposesQuickPacksAndDynamicXrayProfiles() {
        assertThat(catalog.catalog().quickPacks())
            .extracting(DiagnosticExamCatalog.QuickPack::code)
            .containsExactly("ROUTINE_ICU", "SEPSIS", "CHEST_PAIN", "ACUTE_ABDOMEN");
        assertThat(catalog.xrayProfile("ELBOW").defaultIncidences())
            .containsExactly("AP", "PROFILE");
    }

    @Test
    void highCostLaboratoryExamRequiresExplicitClinicalJustification() {
        DiagnosticExamRequest request = request(
            List.of("TROPONIN"),
            "",
            null,
            null,
            null,
            null
        );

        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("justificativa clínica");
    }

    @Test
    void contrastedTomographyRequiresPreviousCreatinine() {
        DiagnosticExamRequest request = request(
            List.of("HEMOGRAM"),
            "",
            new DiagnosticExamRequest.Tomography(
                "CHEST", "WITH_CONTRAST", "SUSPEITA DE TEP", ""
            ),
            null,
            null,
            null
        );

        assertThatThrownBy(() -> validator.validate(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("creatinina prévia");
    }

    @Test
    void acceptsCompleteMultimodalRequest() {
        DiagnosticExamRequest request = request(
            List.of("HEMOGRAM", "TROPONIN"),
            "DOR TORÁCICA",
            new DiagnosticExamRequest.Tomography(
                "CHEST", "WITH_CONTRAST", "SUSPEITA DE TEP", "1,1"
            ),
            new DiagnosticExamRequest.Xray(
                "ELBOW", "RIGHT", List.of("AP", "PROFILE"), "TRAUMA"
            ),
            new DiagnosticExamRequest.Ultrasound(
                "VENOUS_DOPPLER_LOWER_LIMB", "LEFT", "SUSPEITA DE TVP"
            ),
            new DiagnosticExamRequest.Electrocardiogram(
                "ECG_12_LEADS", "IMMEDIATE", "DOR TORÁCICA"
            )
        );

        validator.validate(request);
    }

    private DiagnosticExamRequest request(
        List<String> blood,
        String laboratoryJustification,
        DiagnosticExamRequest.Tomography tomography,
        DiagnosticExamRequest.Xray xray,
        DiagnosticExamRequest.Ultrasound ultrasound,
        DiagnosticExamRequest.Electrocardiogram ecg
    ) {
        return new DiagnosticExamRequest(
            "ADULT_ICU",
            blood,
            List.of(),
            laboratoryJustification,
            tomography,
            xray,
            ultrasound,
            ecg
        );
    }
}
