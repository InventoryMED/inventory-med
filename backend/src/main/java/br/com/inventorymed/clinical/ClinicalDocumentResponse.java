package br.com.inventorymed.clinical;

import br.com.inventorymed.formtemplates.FormKind;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ClinicalDocumentResponse(
    UUID id,
    UUID admissionId,
    FormKind kind,
    UUID templateVersionId,
    String status,
    int versionNumber,
    UUID authorUserId,
    Map<String, Object> values,
    Instant createdAt,
    Instant finalizedAt
) {}
