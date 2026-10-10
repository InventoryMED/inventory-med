package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AihServiceTest {

    private AihService service;

    @BeforeEach
    void setUp() {
        AihCatalogService catalog = new AihCatalogService(new BedsideProcedureCatalogService());
        service = new AihService(catalog, new ObjectMapper());
    }

    @Test
    void acceptsBlankOptionalAihAndProducesPrintableReport() {
        AihResponse response = service.preview(new AihRequest(null, null, null, null, null));

        assertThat(response.structuredAih().reportText())
            .contains("LAUDO PARA SOLICITAÇÃO")
            .contains("PACIENTE: ")
            .contains("CID PRINCIPAL: ");
        assertThat(response.structuredAih().requestedProcedures()).isEmpty();
        assertThat(response.billingAudit().alerts())
            .anyMatch(value -> value.contains("CAMPOS EM BRANCO SÃO PERMITIDOS"));
    }

    @Test
    void resolvesCodesFromMasterCatalogAndAddsImageGuidanceCode() {
        AihResponse response = service.preview(new AihRequest(
            "ADULT_ICU",
            "PATIENT-1",
            patient(),
            List.of(procedure("CVC", "RIGHT", true, "ANEXO-PEP-42")),
            manual()
        ));

        assertThat(response.structuredAih().requestedProcedures())
            .singleElement()
            .satisfies(procedure -> {
                assertThat(procedure.tussCode()).isEqualTo("31401606");
                assertThat(procedure.imageGuidanceTussCode()).isEqualTo("40901262");
                assertThat(procedure.name()).contains("CATETERISMO VENOSO CENTRAL");
            });
        assertThat(response.structuredAih().reportText())
            .contains("TUSS 31401606")
            .contains("GUIAGEM TUSS 40901262");
    }

    @Test
    void rejectsPairedProcedureWithoutRightOrLeftLaterality() {
        AihRequest request = new AihRequest(
            "ADULT_ICU",
            "PATIENT-1",
            patient(),
            List.of(procedure("CVC", "NOT_APPLICABLE", false, "")),
            manual()
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("lateralidade direita ou esquerda");
    }

    @Test
    void rejectsImageGuidanceWithoutPepAttachmentReference() {
        AihRequest request = new AihRequest(
            "ADULT_ICU",
            "PATIENT-1",
            patient(),
            List.of(procedure("IOT", "NOT_APPLICABLE", true, "")),
            manual()
        );

        assertThatThrownBy(() -> service.preview(request))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("referência do anexo no PEP");
    }

    @Test
    void storesStructuredAihAndRejectsUnknownFields() {
        Map<String, Object> normalized = service.normalizeForClinicalDocument(Map.of(
            "clinicalContext", "",
            "patientId", "PATIENT-1",
            "patient", Map.of("name", "PACIENTE TESTE"),
            "requestedProcedures", List.of(),
            "manualData", Map.of("primaryCid", "")
        ));

        assertThat(normalized).containsKeys("request", "structuredAih", "billingAudit");
        assertThatThrownBy(() -> service.normalizeForClinicalDocument(Map.of(
            "clinicalContext", "",
            "patientId", "PATIENT-1",
            "patient", Map.of("name", "PACIENTE TESTE", "globalHospitalId", "INVALID"),
            "requestedProcedures", List.of(),
            "manualData", Map.of()
        )))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessage("O registro da AIH contém campos não permitidos");
    }

    private AihRequest.PatientIdentification patient() {
        return new AihRequest.PatientIdentification(
            "PACIENTE TESTE",
            "",
            "",
            "PRONTUARIO-1",
            "",
            "LEITO 01",
            "HOSPITAL TESTE",
            ""
        );
    }

    private AihRequest.ManualData manual() {
        return new AihRequest.ManualData(
            "FEBRE E DISPNEIA",
            "NECESSIDADE DE SUPORTE HOSPITALAR",
            "",
            "CHOQUE SÉPTICO",
            "A41.9",
            "",
            "0303010192",
            "URGENCY"
        );
    }

    private AihRequest.RequestedProcedure procedure(
        String code,
        String laterality,
        boolean imageGuided,
        String attachment
    ) {
        return new AihRequest.RequestedProcedure(
            1,
            code,
            "VEIA JUGULAR INTERNA",
            laterality,
            "IMEDIATO",
            imageGuided,
            attachment,
            "NECESSIDADE DE ACESSO PARA TERAPIA ENDOVENOSA"
        );
    }
}
