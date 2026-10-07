package br.com.inventorymed.clinical;

import java.util.List;

public record MonitoringPrescriptionResponse(
    StructuredMonitoring structuredMonitoring,
    BillingAudit billingAudit
) {
    public record StructuredMonitoring(String summaryLine, String prescriptionDetails) {}

    public record BillingAudit(
        List<String> itemsForReview,
        List<String> auditAlerts
    ) {}
}
