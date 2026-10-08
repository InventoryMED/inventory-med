package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public record BedsideProcedureRequest(
    @Size(max = 60) String selectedTemplate,
    @NotNull @Size(min = 1, max = 10) List<@Valid Item> items
) {
    public record Item(
        @NotNull @Positive Integer id,
        @Size(max = 20) String recordType,
        @Size(max = 60) String procedureCode,
        @Size(max = 200) String customProcedure,
        @Size(max = 1000) String clinicalIndication,
        @Size(max = 40) String cid10Reference,
        @Size(max = 300) String anatomicalSite,
        @Size(max = 30) String laterality,
        @Size(max = 1000) String asepsisAntisepsis,
        @Size(max = 1000) String sterileBarrier,
        @Size(max = 1000) String localAnesthesia,
        @Size(max = 1000) String imageGuidance,
        @Size(max = 300) String deviceName,
        @Size(max = 200) String deviceBrand,
        @Size(max = 100) String deviceCaliber,
        @Size(max = 160) String deviceLot,
        @Size(max = 160) String anvisaRegistration,
        @Size(max = 1000) String fixationDressingConnections,
        @Size(max = 1000) String samplesLaboratory,
        @Size(max = 60) String postProcedureControl,
        @Size(max = 1000) String postProcedureDetails,
        @Size(max = 1000) String monitoringAssistance,
        @Size(max = 30) String urgency,
        @Size(max = 3000) String techniqueOutcome,
        @Size(max = 1000) String complications,
        OffsetDateTime performedAt
    ) {}
}
