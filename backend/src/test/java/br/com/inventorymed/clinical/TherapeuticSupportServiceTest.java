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

class TherapeuticSupportServiceTest {

    private TherapeuticSupportService service;

    @BeforeEach
    void setUp() {
        TherapeuticSupportCatalogService catalog = new TherapeuticSupportCatalogService();
        service = new TherapeuticSupportService(
            catalog,
            new TherapeuticSupportValidator(catalog),
            new ObjectMapper()
        );
    }

    @Test
    void buildsUnifiedHydrationGlucoseAndTransfusionPlan() {
        TherapeuticSupportResponse response = service.preview(new TherapeuticSupportRequest(
            "ICU",
            List.of(new TherapeuticSupportRequest.HydrationSolution(
                1,
                "GLYCOSALINE_1000",
                List.of("KCL191_10", "NACL20_10"),
                "EV",
                "EVERY_24_HOURS",
                "PUMP",
                BigDecimal.valueOf(42),
                "ML_H",
                "FIXED"
            )),
            new TherapeuticSupportRequest.GlucoseControl(
                "EVERY_4_HOURS", true, true, "REGULAR", false
            ),
            List.of(new TherapeuticSupportRequest.BloodProduct(
                1,
                "RBC",
                List.of("LEUKOREDUCED"),
                1,
                "UNIT",
                "DEDICATED_ACCESS",
                180,
                List.of("DIPYRONE_1G_EV"),
                "URGENT"
            ))
        ));

        assertThat(response.structuredTherapeuticSupport().prescriptionDetails())
            .contains("7. HIDRATAÇÃO E SOLUÇÕES:")
            .contains("8. CONTROLE GLICÊMICO E INSULINOTERAPIA:")
            .contains("10. HEMODERIVADOS E TRANSFUSÕES:")
            .contains("BOMBA DE INFUSÃO A 42 ML/H")
            .contains("DXT 321 A 380 MG/DL: 08 UI SC")
            .contains("TEMPO TOTAL MÁXIMO DE 4 HORAS POR BOLSA");
        assertThat(response.structuredTherapeuticSupport().orderRows()).hasSize(3);
        assertThat(response.billingAudit().auditAlerts())
            .anyMatch(item -> item.contains("KCL CONCENTRADO"))
            .anyMatch(item -> item.contains("PROVA CRUZADA"));
    }

    @Test
    void requiresBothSafetyProtocolsForEveryDxtOrder() {
        TherapeuticSupportRequest request = new TherapeuticSupportRequest(
            "WARD_UPA",
            List.of(),
            new TherapeuticSupportRequest.GlucoseControl(
                "EVERY_6_HOURS", true, false, "REGULAR", false
            ),
            List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("Toda ordem de DXT");
    }

    @Test
    void requiresHourlyDxtAndRegularInsulinForContinuousPump() {
        TherapeuticSupportRequest request = new TherapeuticSupportRequest(
            "ICU",
            List.of(),
            new TherapeuticSupportRequest.GlucoseControl(
                "EVERY_2_HOURS", true, true, "LISPRO", true
            ),
            List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("A bomba contínua exige DXT 1/1H e insulina regular");
    }

    @Test
    void rejectsIncompatibleInfusionRateUnit() {
        TherapeuticSupportRequest request = new TherapeuticSupportRequest(
            "ICU",
            List.of(new TherapeuticSupportRequest.HydrationSolution(
                1, "SF09_500", List.of(), "EV", "EVERY_12_HOURS", "PUMP",
                BigDecimal.valueOf(40), "DROPS_MIN", "FIXED"
            )),
            null,
            List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("unidade de vazão compatível");
    }

    @Test
    void distinguishesAlbuminFromBloodComponents() {
        TherapeuticSupportResponse response = service.preview(new TherapeuticSupportRequest(
            "WARD_UPA",
            List.of(),
            null,
            List.of(new TherapeuticSupportRequest.BloodProduct(
                1, "ALBUMIN20_50", List.of(), 100, "ML", "EV", 120, List.of(), "FIXED"
            ))
        ));

        assertThat(response.structuredTherapeuticSupport().prescriptionDetails())
            .contains("HEMODERIVADO 1")
            .doesNotContain("EQUIPO DE TRANSFUSÃO COM FILTRO");
        assertThat(response.billingAudit().auditAlerts())
            .noneMatch(item -> item.contains("PROVA CRUZADA"));
    }

    @Test
    void limitsBloodComponentsToTwentyUnitsOrBags() {
        TherapeuticSupportRequest request = new TherapeuticSupportRequest(
            "ICU",
            List.of(),
            null,
            List.of(new TherapeuticSupportRequest.BloodProduct(
                1, "RBC", List.of(), 21, "UNIT", "DEDICATED_ACCESS", 120, List.of(), "URGENT"
            ))
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("quantidade máxima é 20");
    }

    @Test
    void normalizesAndRejectsUnknownDocumentFields() {
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "clinicalContext", "ICU",
            "hydrationSolutions", List.of(),
            "glucoseControl", Map.of(
                "frequency", "EVERY_4_HOURS",
                "hypoglycemiaProtocolActive", true,
                "correctionScaleActive", true,
                "insulinType", "REGULAR",
                "continuousPump", false
            ),
            "bloodProducts", List.of()
        ));

        assertThat(normalized).containsKeys("request", "structuredTherapeuticSupport", "billingAudit");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "clinicalContext", "ICU",
            "automaticBilling", true
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("O suporte terapêutico contém campos não permitidos");
    }
}
