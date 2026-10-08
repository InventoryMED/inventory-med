package br.com.inventorymed.clinical;

import java.util.List;

public record TherapeuticSupportResponse(
    StructuredTherapeuticSupport structuredTherapeuticSupport,
    BillingAudit billingAudit
) {
    public record StructuredTherapeuticSupport(
        String summaryLine,
        String prescriptionDetails,
        List<OrderRow> orderRows
    ) {}

    public record OrderRow(
        String section,
        String description,
        String route,
        String frequency,
        String scheduling
    ) {}

    public record BillingAudit(
        List<String> suppliesEquipmentForReview,
        List<String> auditAlerts
    ) {}
}
