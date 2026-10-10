package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AihRequest(
    @Size(max = 60) String clinicalContext,
    @Size(max = 100) String patientId,
    @Valid PatientIdentification patient,
    @Size(max = 20) List<@Valid RequestedProcedure> requestedProcedures,
    @Valid ManualData manualData
) {
    public record PatientIdentification(
        @Size(max = 300) String name,
        @Size(max = 30) String cns,
        @Size(max = 300) String motherName,
        @Size(max = 100) String medicalRecordNumber,
        @Size(max = 500) String address,
        @Size(max = 200) String bed,
        @Size(max = 300) String hospital,
        @Size(max = 30) String cnes
    ) {}

    public record RequestedProcedure(
        Integer id,
        @Size(max = 60) String procedureCode,
        @Size(max = 300) String anatomicalSite,
        @Size(max = 30) String laterality,
        @Size(max = 100) String scheduling,
        Boolean imageGuided,
        @Size(max = 300) String imageAttachmentReference,
        @Size(max = 2000) String clinicalJustification
    ) {}

    public record ManualData(
        @Size(max = 4000) String mainSignsSymptoms,
        @Size(max = 4000) String admissionConditions,
        @Size(max = 4000) String examResults,
        @Size(max = 2000) String initialDiagnosis,
        @Size(max = 40) String primaryCid,
        @Size(max = 500) String secondaryCids,
        @Size(max = 30) String requestedProcedureCode,
        @Size(max = 30) String admissionCharacter
    ) {}
}
