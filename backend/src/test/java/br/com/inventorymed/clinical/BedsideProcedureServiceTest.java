package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class BedsideProcedureServiceTest {

    private BedsideProcedureService service;

    @BeforeEach
    void setUp() {
        BedsideProcedureCatalogService catalog = new BedsideProcedureCatalogService();
        service = new BedsideProcedureService(
            catalog,
            new BedsideProcedureValidator(catalog),
            new ObjectMapper()
        );
    }

    @Test
    void buildsNumberedProcedureAndReviewOnlyBillingSection() {
        assertThat(service.catalog().templates())
            .filteredOn(template -> template.code().equals("CVC"))
            .singleElement()
            .extracting(template -> template.item().postProcedureControl())
            .isEqualTo("CHEST_XRAY");
        BedsideProcedureResponse response = service.preview(new BedsideProcedureRequest(
            "CVC",
            List.of(validRequestedItem("CVC", "RIGHT", "CHEST_XRAY"))
        ));

        assertThat(response.structuredProcedures().prescriptionDetails())
            .startsWith("11. PROCEDIMENTOS E INTERVENÇÕES BEIRA-LEITO:")
            .contains("CATETERISMO VENOSO CENTRAL")
            .contains("VEIA JUGULAR INTERNA — DIREITA")
            .contains("NÃO REPRESENTAM EXECUÇÃO DO PROCEDIMENTO");
        assertThat(response.billingAudit().suppliesEquipmentForReview())
            .anyMatch(value -> value.contains("VALIDAR VIGÊNCIA"));
        assertThat(response.billingAudit().auditAlerts())
            .anyMatch(value -> value.contains("NENHUMA COBRANÇA"));
    }

    @Test
    void rejectsPairedSiteWithoutRightOrLeftLaterality() {
        BedsideProcedureRequest.Item item = copy(validRequestedItem("CHEST_DRAINAGE", "RIGHT", "CHEST_XRAY"),
            "NOT_APPLICABLE", "CHEST_XRAY", "REQUESTED", null, "", "", "", "", "");

        assertThatThrownBy(() -> service.preview(new BedsideProcedureRequest("", List.of(item))))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("lateralidade direita ou esquerda");
    }

    @Test
    void performedDeviceProcedureRequiresTraceability() {
        BedsideProcedureRequest.Item item = copy(validRequestedItem("CVC", "RIGHT", "CHEST_XRAY"),
            "RIGHT", "CHEST_XRAY", "PERFORMED", OffsetDateTime.now(), "TÉCNICA SEM INTERCORRÊNCIAS",
            "", "", "", "");

        assertThatThrownBy(() -> service.preview(new BedsideProcedureRequest("", List.of(item))))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("marca do dispositivo");
    }

    @Test
    void formatsPerformedProcedureInClinicalTimeZone() {
        BedsideProcedureRequest.Item item = copy(validRequestedItem("CVC", "RIGHT", "CHEST_XRAY"),
            "RIGHT", "CHEST_XRAY", "PERFORMED", OffsetDateTime.parse("2026-10-08T10:00:00Z"),
            "TÉCNICA SEM INTERCORRÊNCIAS", "MARCA TESTE", "LOTE TESTE", "ANVISA TESTE", "");

        BedsideProcedureResponse response = service.preview(new BedsideProcedureRequest("CVC", List.of(item)));

        assertThat(response.structuredProcedures().prescriptionDetails())
            .contains("DATA/HORA: 08/10/2026 07:00")
            .contains("MARCA: MARCA TESTE")
            .contains("LOTE: LOTE TESTE")
            .contains("REGISTRO ANVISA: ANVISA TESTE");
    }

    @Test
    void invasiveThoracicProcedureRequiresPostProcedureControl() {
        BedsideProcedureRequest.Item item = copy(validRequestedItem("IOT", "NOT_APPLICABLE", "CHEST_XRAY"),
            "NOT_APPLICABLE", "NOT_APPLICABLE", "REQUESTED", null, "", "", "", "", "");

        assertThatThrownBy(() -> service.preview(new BedsideProcedureRequest("", List.of(item))))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("exame de controle");
    }

    @Test
    void storesStructuredResponseAndRejectsUnknownFields() {
        Map<String, Object> item = new ObjectMapper().convertValue(
            validRequestedItem("LUMBAR_PUNCTURE_DIAGNOSTIC", "NOT_APPLICABLE", "NOT_APPLICABLE"),
            new TypeReference<Map<String, Object>>() {}
        );
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "selectedTemplate", "LUMBAR_PUNCTURE_DIAGNOSTIC",
            "items", List.of(item)
        ));

        assertThat(normalized).containsKeys("request", "structuredProcedures", "billingAudit");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "selectedTemplate", "CVC", "items", List.of(item), "automaticBilling", true
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("O registro de procedimentos contém campos não permitidos");
    }

    private BedsideProcedureRequest.Item validRequestedItem(
        String procedure,
        String laterality,
        String postControl
    ) {
        return new BedsideProcedureRequest.Item(
            1, "REQUESTED", procedure, "", "ACESSO VASCULAR PARA TERAPIA ENDOVENOSA",
            "Z45.2", procedure.startsWith("LUMBAR") ? "INTERESPAÇO L3-L4" : "VEIA JUGULAR INTERNA",
            laterality, BedsideProcedureCatalogService.ASEPSIS, BedsideProcedureCatalogService.BARRIER,
            BedsideProcedureCatalogService.ANESTHESIA, "POCUS EM TEMPO REAL", "KIT / DISPOSITIVO",
            "", "7 FR", "", "", "FIXAÇÃO E CURATIVO ESTÉRIL", "SEM AMOSTRAS",
            postControl, "", BedsideProcedureCatalogService.MONITORING, "URGENT", "", "", null
        );
    }

    private BedsideProcedureRequest.Item copy(
        BedsideProcedureRequest.Item source,
        String laterality,
        String postControl,
        String recordType,
        OffsetDateTime performedAt,
        String technique,
        String brand,
        String lot,
        String anvisa,
        String complications
    ) {
        return new BedsideProcedureRequest.Item(
            source.id(), recordType, source.procedureCode(), source.customProcedure(), source.clinicalIndication(),
            source.cid10Reference(), source.anatomicalSite(), laterality, source.asepsisAntisepsis(),
            source.sterileBarrier(), source.localAnesthesia(), source.imageGuidance(), source.deviceName(),
            brand, source.deviceCaliber(), lot, anvisa, source.fixationDressingConnections(),
            source.samplesLaboratory(), postControl, source.postProcedureDetails(), source.monitoringAssistance(),
            source.urgency(), technique, complications, performedAt
        );
    }
}
