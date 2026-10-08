package br.com.inventorymed.clinical;

import java.util.List;

public record MedicationTherapyResponse(
    StructuredMedicationTherapy structuredMedicationTherapy,
    BillingAudit billingAudit
) {
    public record StructuredMedicationTherapy(
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
