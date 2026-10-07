package br.com.inventorymed.clinical;

import java.util.List;

public record VentilatorySupportResponse(
    StructuredVentilatorySupport structuredVentilatorySupport,
    BillingAudit billingAudit
) {
    public record StructuredVentilatorySupport(
        String summaryLine,
        String prescriptionDetails,
        List<OrderRow> orderRows
    ) {}

    public record OrderRow(
        String description,
        String interfaceRoute,
        String frequency,
        String scheduling
    ) {}

    public record BillingAudit(
        List<String> itemsForReview,
        List<String> auditAlerts
    ) {}
}
