package br.com.inventorymed.formtemplates;

import java.util.List;
import java.util.UUID;

public record FormTemplateDefinition(
    UUID templateId,
    String name,
    FormKind kind,
    boolean active,
    UUID versionId,
    int versionNumber,
    String status,
    List<Section> sections
) {
    public record Section(
        UUID id,
        String key,
        String title,
        int displayOrder,
        boolean active,
        List<Field> fields
    ) {}

    public record Field(
        UUID id,
        String key,
        String label,
        FormFieldType type,
        boolean required,
        int displayOrder,
        boolean active,
        String placeholder,
        Integer maxLength,
        List<Option> options
    ) {}

    public record Option(
        UUID id,
        String value,
        String label,
        int displayOrder,
        boolean active
    ) {}
}
