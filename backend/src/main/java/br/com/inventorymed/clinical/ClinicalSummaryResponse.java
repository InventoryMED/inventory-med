package br.com.inventorymed.clinical;

import java.util.List;

public record ClinicalSummaryResponse(
    ClinicalSummaryRequest request,
    OutcomeReference outcomeReference,
    String reportText,
    List<String> auditAlerts
) {
    public record OutcomeReference(
        String dischargeReason,
        String sigtapExitReferenceCode,
        String sigtapExitReferenceLabel,
        boolean closesAdmission
    ) {}
}
