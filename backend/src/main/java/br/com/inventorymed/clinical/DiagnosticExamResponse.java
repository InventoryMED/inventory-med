package br.com.inventorymed.clinical;

import java.util.List;

public record DiagnosticExamResponse(
    StructuredExams structuredExams,
    BillingAudit billingAudit
) {
    public record StructuredExams(
        String summaryLine,
        String requestDetails,
        List<String> duplicateReviewItems
    ) {}

    public record BillingAudit(
        List<String> suppliesEquipmentForReview,
        List<String> auditAlerts
    ) {}
}
