package br.com.inventorymed.formtemplates;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class FormTemplateRequests {

    private FormTemplateRequests() {}

    public record Create(
        @NotBlank @Size(max = 160) String name,
        @NotNull FormKind kind
    ) {}

    public record SaveDraft(
        @NotBlank @Size(max = 160) String name,
        @NotEmpty @Size(max = 50) List<@Valid Section> sections
    ) {}

    public record Section(
        @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Za-z0-9_]+") String key,
        @NotBlank @Size(max = 160) String title,
        @Min(0) int displayOrder,
        boolean active,
        @NotEmpty @Size(max = 100) List<@Valid Field> fields
    ) {}

    public record Field(
        @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Za-z0-9_]+") String key,
        @NotBlank @Size(max = 160) String label,
        @NotNull FormFieldType type,
        boolean required,
        @Min(0) int displayOrder,
        boolean active,
        @Size(max = 200) String placeholder,
        @Min(1) @Max(10000) Integer maxLength,
        @Size(max = 200) List<@Valid Option> options
    ) {}

    public record Option(
        @NotBlank @Size(max = 120) String value,
        @NotBlank @Size(max = 160) String label,
        @Min(0) int displayOrder,
        boolean active
    ) {}

    public record Active(boolean active) {}
}
