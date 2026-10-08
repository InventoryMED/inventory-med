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

class CriticalCarePrescriptionServiceTest {

    private CriticalCarePrescriptionService service;

    @BeforeEach
    void setUp() {
        CriticalCareCatalogService catalog = new CriticalCareCatalogService();
        service = new CriticalCarePrescriptionService(
            catalog,
            new CriticalCarePrescriptionValidator(catalog),
            new ObjectMapper()
        );
    }

    @Test
    void buildsNumberedCriticalCarePlanAndReviewItems() {
        CriticalCarePrescriptionResponse response = service.preview(new CriticalCarePrescriptionRequest(
            "ICU",
            List.of(new CriticalCarePrescriptionRequest.VasoactiveDrug(
                1, "NOREPINEPHRINE", "16 MG EM SG 5% 250 ML", "64 MCG/ML",
                BigDecimal.TEN, "ML_H", "CVC", "INVASIVE_ARTERIAL",
                "TITULAR PARA MANTER PAM ≥ 65 MMHG", "CONTINUOUS"
            )),
            List.of(
                new CriticalCarePrescriptionRequest.SedationDrug(
                    1, "FENTANYL", "5 AMPOLAS SEM DILUIÇÃO", "CONTINUOUS_BIC",
                    BigDecimal.valueOf(5), "ML_H", "RASS_MINUS_4", "VMI_TOT",
                    "EV_BIC", "CONTINUOUS"
                ),
                new CriticalCarePrescriptionRequest.SedationDrug(
                    2, "ROCURONIUM", "100 MG EM SF 0,9%", "CONTINUOUS_BIC",
                    BigDecimal.valueOf(4), "ML_H", "RASS_MINUS_5", "VMI_TOT",
                    "EV_BIC", "CONTINUOUS"
                )
            ),
            List.of(new CriticalCarePrescriptionRequest.EmergencyMedication(
                1, "SUGAMMADEX", "2 MG/KG EV", "EV",
                "REVERSÃO DE BNM EM EMERGÊNCIA", "NOW"
            ))
        ));

        assertThat(response.structuredCriticalCare().prescriptionDetails())
            .contains("14. DROGAS VASOATIVAS E INOTRÓPICOS:")
            .contains("15. SEDAÇÃO, ANALGESIA CONTÍNUA E BNM:")
            .contains("16. ANTÍDOTOS, REVERSORES E EMERGÊNCIA (PCR):")
            .contains("VMI VIA TUBO OROTRAQUEAL");
        assertThat(response.structuredCriticalCare().orderRows()).hasSize(4);
        assertThat(response.billingAudit().suppliesEquipmentForReview())
            .anyMatch(item -> item.contains("CANAL DE BIC"))
            .anyMatch(item -> item.contains("CARRINHO DE EMERGÊNCIA"));
        assertThat(response.billingAudit().auditAlerts())
            .anyMatch(item -> item.contains("SUGAMADEX"))
            .anyMatch(item -> item.contains("NENHUMA COBRANÇA"));
    }

    @Test
    void rejectsNeuromuscularBlockerWithoutDeepSedationAssociated() {
        CriticalCarePrescriptionRequest request = new CriticalCarePrescriptionRequest(
            "ICU", List.of(),
            List.of(new CriticalCarePrescriptionRequest.SedationDrug(
                1, "ROCURONIUM", "100 MG EM SF 0,9%", "CONTINUOUS_BIC",
                BigDecimal.valueOf(4), "ML_H", "RASS_MINUS_5", "VMI_TOT",
                "EV_BIC", "CONTINUOUS"
            )),
            List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("BNM exige analgesia ou sedação profunda associada");
    }

    @Test
    void rejectsCentralVasopressorOnPeripheralAccess() {
        CriticalCarePrescriptionRequest request = new CriticalCarePrescriptionRequest(
            "ICU",
            List.of(new CriticalCarePrescriptionRequest.VasoactiveDrug(
                1, "NOREPINEPHRINE", "16 MG EM SG 5% 250 ML", "64 MCG/ML",
                BigDecimal.TEN, "ML_H", "TEMPORARY_PERIPHERAL", "FREQUENT_NON_INVASIVE",
                "PAM ≥ 65 MMHG", "CONTINUOUS"
            )),
            List.of(), List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("exige CVC");
    }

    @Test
    void preservesStructuredRequestAndRejectsUnknownFields() {
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "clinicalContext", "ADULT_EMERGENCY",
            "vasoactiveDrugs", List.of(),
            "sedationAnalgesiaBnm", List.of(),
            "emergencyMedications", List.of(Map.of(
                "id", 1,
                "drug", "EPINEPHRINE_BOLUS",
                "doseAdministration", "1 MG EM BOLUS",
                "route", "IO",
                "emergencyIndication", "PCR",
                "scheduling", "EMERGENCY"
            ))
        ));

        assertThat(normalized).containsKeys("request", "structuredCriticalCare", "billingAudit");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "clinicalContext", "ICU", "automaticBilling", true
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Os cuidados críticos contêm campos não permitidos");
    }
}
