package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record IsolationPrecautionRequest(
    @Size(max = 80) String selectedTemplate,
    @NotEmpty @Size(max = 8) List<@Valid Item> items
) {
    public record Item(
        @NotNull @Positive Integer id,
        @Size(max = 40) String precautionType,
        @Size(max = 500) String reasonPathogen,
        @Size(max = 40) String durationReview,
        @Size(max = 20) String scheduling
    ) {}
}
