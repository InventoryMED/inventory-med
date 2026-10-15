package br.com.inventorymed.clinical;

import java.util.List;

public record ReferralResponse(
    ReferralRequest request,
    String specialtyLabel,
    String priorityLabel,
    String deadlineLabel,
    String destinationLabel,
    List<String> attachmentLabels,
    String summaryLine,
    boolean finalizationAllowed,
    List<String> blockingReasons,
    List<String> safetyNotices
) {}
