package br.com.inventorymed.clinical;

import java.util.List;

public record RehabilitationPrescriptionResponse(
    StructuredRehabilitation structuredRehabilitation,
    BillingAudit billingAudit
) {
    public record StructuredRehabilitation(
        String summaryLine,
        String prescriptionDetails,
        List<OrderRow> orderRows
    ) {}

    public record OrderRow(
        String description,
        String specialty,
        String frequency,
        String scheduling
    ) {}

    public record BillingAudit(
        List<String> itemsForReview,
        List<String> auditAlerts
    ) {}
}
