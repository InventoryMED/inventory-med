package br.com.inventorymed.clinical;

import java.util.List;

public record BedsideProcedureResponse(
    StructuredProcedures structuredProcedures,
    BillingAudit billingAudit
) {
    public record StructuredProcedures(String summaryLine, String prescriptionDetails) {}

    public record BillingAudit(
        List<String> suppliesEquipmentForReview,
        List<String> auditAlerts
    ) {}
}
