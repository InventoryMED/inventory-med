package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ClinicalSummaryServiceTest {

    private ClinicalSummaryService service;

    @BeforeEach
    void setUp() {
        ClinicalSummaryCatalogService catalog = new ClinicalSummaryCatalogService();
        service = new ClinicalSummaryService(
            catalog,
            new ClinicalSummaryValidator(),
            new ObjectMapper()
        );
    }

    @Test
    void buildsHospitalDischargeWithPrescriptionAndExitReference() {
        ClinicalSummaryResponse response = service.preview(new ClinicalSummaryRequest(
            "ALTA_HOSPITALAR",
            "MELHORADO",
            "Paciente evoluiu com melhora clínica e laboratorial.",
            "J18.9",
            List.of(new ClinicalSummaryRequest.DischargeMedication(
                "AMOXICILINA 500 MG", "01 COMPRIMIDO DE 8/8H", "5 DIAS"
            )),
            List.of("RETORNAR À UBS EM 7 DIAS"),
            null,
            null,
            true
        ));

        assertThat(response.outcomeReference().dischargeReason()).isEqualTo("ALTA_MELHORA");
        assertThat(response.outcomeReference().sigtapExitReferenceCode()).isEqualTo("12");
        assertThat(response.reportText())
            .contains("PRESCRIÇÃO DE ALTA")
            .contains("AMOXICILINA 500 MG")
            .contains("RETORNAR À UBS EM 7 DIAS");
    }

    @Test
    void hospitalTransferRequiresDestinationAndAllowsDevices() {
        ClinicalSummaryRequest request = new ClinicalSummaryRequest(
            "TRANSFERENCIA_HOSPITALAR",
            null,
            "Paciente necessita continuidade em unidade de maior complexidade.",
            null,
            List.of(),
            List.of(),
            new ClinicalSummaryRequest.TransferDetails(
                "VAGA_UTI", "HOSPITAL DE REFERÊNCIA", "1234567", List.of("CVC", "SVD")
            ),
            null,
            true
        );

        assertThat(service.preview(request).reportText())
            .contains("HOSPITAL DE REFERÊNCIA")
            .contains("CATETER VENOSO CENTRAL")
            .contains("SONDA VESICAL");
    }

    @Test
    void deathRequiresTimestampDirectCauseAndUnderlyingCause() {
        ClinicalSummaryRequest request = new ClinicalSummaryRequest(
            "OBITO",
            null,
            "Paciente apresentou deterioração clínica refratária.",
            null,
            List.of(),
            List.of(),
            null,
            new ClinicalSummaryRequest.DeathDetails(
                OffsetDateTime.now().minusMinutes(10), "CHOQUE REFRATÁRIO", null, "SEPSE", "NAO"
            ),
            true
        );

        assertThat(service.preview(request).outcomeReference().sigtapExitReferenceCode())
            .isEqualTo("61");
    }

    @Test
    void refusesFinalizationWithoutPhysicianAttestation() {
        ClinicalSummaryRequest request = new ClinicalSummaryRequest(
            "ALTA_HOSPITALAR",
            "CURADO",
            "Paciente curado.",
            null,
            List.of(),
            List.of(),
            null,
            null,
            false
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("autoria médica");
    }

    @Test
    void storesNormalizedSummaryAndRejectsUnknownFields() {
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "outcomeType", "ALTA_HOSPITALAR",
            "dischargeCondition", "CURADO",
            "clinicalSummary", "Paciente curado e orientado.",
            "dischargeMedications", List.of(),
            "dischargeOrientations", List.of(),
            "physicianAttestation", true
        ));

        assertThat(normalized).containsKeys("request", "outcomeReference", "reportText", "auditAlerts");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "outcomeType", "ALTA_HOSPITALAR",
            "clinicalSummary", "Paciente curado.",
            "physicianAttestation", true,
            "unsafeHtml", "<script>"
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("formato inválido");
    }
}
