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

class MedicationTherapyServiceTest {

    private MedicationTherapyService service;

    @BeforeEach
    void setUp() {
        MedicationTherapyCatalogService catalog = new MedicationTherapyCatalogService();
        service = new MedicationTherapyService(
            catalog,
            new MedicationTherapyValidator(catalog),
            new ObjectMapper()
        );
    }

    @Test
    void buildsFourNumberedModulesAndBillingReview() {
        MedicationTherapyResponse response = service.preview(new MedicationTherapyRequest(
            "ICU",
            new MedicationTherapyRequest.RenalFunction("CREATININE_CLEARANCE", BigDecimal.valueOf(72)),
            new MedicationTherapyRequest.BleedingRisk(180_000, false),
            List.of(new MedicationTherapyRequest.Antimicrobial(
                1, "CEFTRIAXONE", "", "1 G + 100 ML DE SF 0,9%", "EV", "RAPID_INFUSION",
                "SF_09_100", "MACRODRIP", "12/12H", "FIXED", "", "", 2,
                "PNEUMONIA", "AUTHORIZED", "PARECER CCIH 123", "NO_ADJUSTMENT_REQUIRED"
            )),
            List.of(
                new MedicationTherapyRequest.HospitalProphylaxis(
                    1, "ENOXAPARIN", "40 MG", "SC", "24/24H", "FIXED", "", ""
                ),
                new MedicationTherapyRequest.HospitalProphylaxis(
                    2, "IPC", "MANTER ENQUANTO RESTRITO AO LEITO", "MECHANICAL", "CONTÍNUO",
                    "FIXED", "", ""
                )
            ),
            List.of(new MedicationTherapyRequest.ContinuousMedication(
                1, "LOSARTANA", "50 MG", "VO", "12/12H", "MAINTAINED_HOME",
                "FIXED", "", ""
            )),
            List.of(new MedicationTherapyRequest.SymptomaticMedication(
                1, "DIPYRONE", "", "1 G", "EV", "", "PRN",
                "SE DOR OU FEBRE > 37,8 °C", "6/6H"
            ))
        ));

        assertThat(response.structuredMedicationTherapy().prescriptionDetails())
            .contains("11. ANTIMICROBIANOS E ANTIBIOTICOTERAPIA:")
            .contains("12. PROFILAXIAS HOSPITALARES (TEV / LAMG):")
            .contains("13. MEDICAMENTOS DE USO CONTÍNUO E ROTINA:")
            .contains("14. ANALGESIA, ANTI-INFLAMATÓRIOS E SINTOMÁTICOS:")
            .contains("D2 — CEFTRIAXONA")
            .contains("INTERVALO MÍNIMO: 6/6H");
        assertThat(response.structuredMedicationTherapy().orderRows()).hasSize(5);
        assertThat(response.billingAudit().suppliesEquipmentForReview())
            .anyMatch(item -> item.contains("SERINGA"))
            .anyMatch(item -> item.contains("SERINGA PREENCHIDA"))
            .anyMatch(item -> item.contains("DIÁRIA DO APARELHO"));
        assertThat(response.billingAudit().auditAlerts())
            .anyMatch(item -> item.contains("NENHUMA COBRANÇA"));
    }

    @Test
    void requiresRenalAssessmentForAntimicrobial() {
        MedicationTherapyRequest request = new MedicationTherapyRequest(
            "WARD_HOSPITAL", null, null,
            List.of(new MedicationTherapyRequest.Antimicrobial(
                1, "VANCOMYCIN", "", "1 G", "EV", "INTERMITTENT_INFUSION",
                "SF_09_100", "MACRODRIP", "12/12H", "FIXED", "", "", 1,
                "FOCO A ESCLARECER", "REQUESTED", "SOLICITADO", "ADJUSTED"
            )),
            List.of(), List.of(), List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("função renal");
    }

    @Test
    void blocksActiveAnticoagulantProphylaxisWithLowPlatelets() {
        MedicationTherapyRequest request = new MedicationTherapyRequest(
            "ICU", null, new MedicationTherapyRequest.BleedingRisk(42_000, false),
            List.of(),
            List.of(new MedicationTherapyRequest.HospitalProphylaxis(
                1, "UFH", "5000 UI", "SC", "12/12H", "FIXED", "", ""
            )),
            List.of(), List.of()
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("bloqueada");
    }

    @Test
    void requiresTriggerAndMinimumIntervalForPrnMedication() {
        MedicationTherapyRequest request = new MedicationTherapyRequest(
            "WARD_HOSPITAL", null, null, List.of(), List.of(), List.of(),
            List.of(new MedicationTherapyRequest.SymptomaticMedication(
                1, "ONDANSETRON", "", "4 MG", "EV", "", "PRN", "", ""
            ))
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("gatilho de disparo");
    }

    @Test
    void preservesStructuredRequestAndRejectsUnknownFields() {
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "clinicalContext", "WARD_HOSPITAL",
            "antimicrobials", List.of(),
            "prophylaxes", List.of(),
            "continuousMedications", List.of(Map.of(
                "id", 1,
                "medication", "LEVOTIROXINA",
                "dosePreparation", "50 MCG",
                "route", "VO",
                "frequency", "1X/DIA EM JEJUM",
                "reconciliationStatus", "MAINTAINED_HOME",
                "scheduling", "FIXED",
                "conditionalTrigger", "",
                "suspensionReason", ""
            )),
            "analgesiaSymptomatics", List.of()
        ));

        assertThat(normalized).containsKeys("request", "structuredMedicationTherapy", "billingAudit");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "clinicalContext", "ICU", "automaticBilling", true
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("A terapia medicamentosa contém campos não permitidos");
    }
}
