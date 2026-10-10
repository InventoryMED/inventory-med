package br.com.inventorymed.clinical;

import java.util.List;

public record AihResponse(
    StructuredAih structuredAih,
    BillingAudit billingAudit
) {
    public record StructuredAih(
        AihRequest.PatientIdentification patient,
        AihRequest.ManualData manualData,
        String clinicalContext,
        String admissionCharacter,
        List<ResolvedProcedure> requestedProcedures,
        String reportText
    ) {}

    public record ResolvedProcedure(
        Integer id,
        String name,
        String anatomicalSite,
        String laterality,
        String scheduling,
        boolean imageGuided,
        String imageAttachmentReference,
        String clinicalJustification,
        String tussCode,
        String cbhpmCode,
        String sigtapCode,
        String imageGuidanceTussCode
    ) {}

    public record BillingAudit(List<String> alerts) {}
}
