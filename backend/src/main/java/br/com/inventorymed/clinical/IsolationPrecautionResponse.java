package br.com.inventorymed.clinical;

import java.util.List;

public record IsolationPrecautionResponse(
    StructuredIsolation structuredIsolation,
    BillingAudit billingAudit
) {
    public record StructuredIsolation(
        String summaryLine,
        String prescriptionDetails,
        List<OrderRow> orderRows
    ) {}

    public record OrderRow(
        String description,
        String durationReview,
        String scheduling
    ) {}

    public record BillingAudit(
        List<String> suppliesEquipmentForReview,
        List<String> auditAlerts
    ) {}
}
