package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MonitoringPrescriptionServiceTest {

    private MonitoringPrescriptionService service;

    @BeforeEach
    void setUp() {
        service = new MonitoringPrescriptionService(new ObjectMapper());
    }

    @Test
    void buildsWardMonitoringWithProtocolsAndReviewEstimate() {
        MonitoringPrescriptionResponse response = service.preview(new MonitoringPrescriptionRequest(
            new MonitoringPrescriptionRequest.VitalSigns("6/6H", "EVA", "", "MORSE"),
            new MonitoringPrescriptionRequest.GlucoseMonitoring("6/6H", true, true, "REGULAR"),
            new MonitoringPrescriptionRequest.FluidBalanceOutputs(
                "SEM INDICAÇÃO", "DIURESE ESPONTÂNEA", List.of(), List.of()
            ),
            null
        ));

        assertThat(response.structuredMonitoring().prescriptionDetails())
            .contains("AFERIR PA, FC, FR, SPO₂ E TEMPERATURA")
            .contains("SE DXT < 70 MG/DL")
            .contains("331–380 MG/DL: 8 UI")
            .contains("> 380 MG/DL: 10 UI");
        assertThat(response.billingAudit().itemsForReview())
            .anyMatch(item -> item.contains("4 FITAS REAGENTES E 4 LANCETAS"));
        assertThat(response.billingAudit().auditAlerts())
            .anyMatch(item -> item.contains("COMUNICAR A EQUIPE MÉDICA"));
    }

    @Test
    void buildsIntensiveMonitoringWithoutAutomaticBillingClaims() {
        MonitoringPrescriptionResponse response = service.preview(new MonitoringPrescriptionRequest(
            new MonitoringPrescriptionRequest.VitalSigns(
                "CONTÍNUO / MONITORIZAÇÃO MULTIPARAMÉTRICA EM UTI", "CPOT", "RASS", ""
            ),
            new MonitoringPrescriptionRequest.GlucoseMonitoring("4/4H", true, false, ""),
            new MonitoringPrescriptionRequest.FluidBalanceOutputs(
                "BALANÇO HÍDRICO RIGOROSO 1/1H",
                "SVD COM URÔMETRO",
                List.of("DRENO TORÁCICO"),
                List.of("PESO DIÁRIO")
            ),
            new MonitoringPrescriptionRequest.InvasiveMonitoring(
                List.of("PRESSÃO ARTERIAL INVASIVA (PAI)"),
                List.of("PRESSÃO INTRACRANIANA (PIC)")
            )
        ));

        assertThat(response.structuredMonitoring().summaryLine())
            .contains("MONITORIZAÇÃO MULTIPARAMÉTRICA")
            .contains("BALANÇO HÍDRICO RIGOROSO")
            .contains("PRESSÃO ARTERIAL INVASIVA");
        assertThat(response.billingAudit().itemsForReview())
            .anyMatch(item -> item.contains("URÔMETRO"))
            .anyMatch(item -> item.contains("TRANSDUTORES"))
            .allMatch(item -> item.contains("CONFERIR") || item.contains("ESTIMATIVA"));
    }

    @Test
    void requiresInsulinTypeForSlidingScale() {
        MonitoringPrescriptionRequest request = new MonitoringPrescriptionRequest(
            null,
            new MonitoringPrescriptionRequest.GlucoseMonitoring("6/6H", false, true, ""),
            null,
            null
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("Selecione uma opção válida para tipo de insulina da escala");
    }

    @Test
    void normalizesAndRejectsUnknownDocumentFields() {
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "vitalSigns", Map.of(
                "frequency", "8/8H",
                "painScale", "EVA",
                "consciousnessSedationScale", "",
                "fallRiskScale", ""
            )
        ));

        assertThat(normalized).containsKeys("request", "structuredMonitoring", "billingAudit");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "vitalSigns", Map.of("frequency", "8/8H"),
            "automaticBilling", true
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("A monitorização contém campos não permitidos");
    }
}
