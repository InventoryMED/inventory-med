package br.com.inventorymed.clinical;

import java.util.List;

public record NursingCarePrescriptionResponse(
    StructuredCare structuredCare,
    BillingAudit billingAudit
) {
    public record StructuredCare(String summaryLine, String prescriptionDetails) {}

    public record BillingAudit(
        List<String> itemsForReview,
        List<String> qualitySafetyIndicators
    ) {}
}
