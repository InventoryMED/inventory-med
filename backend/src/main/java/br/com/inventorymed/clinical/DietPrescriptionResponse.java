package br.com.inventorymed.clinical;

import java.util.List;

public record DietPrescriptionResponse(
    StructuredDiet structuredDiet,
    BillingAudit billingAudit
) {
    public record StructuredDiet(
        DietType type,
        String summaryLine,
        String prescriptionDetails
    ) {}

    public record BillingAudit(
        List<String> itemsForReview,
        List<String> auditAlerts
    ) {}
}
