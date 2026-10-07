package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RehabilitationPrescriptionRequest(
    @Size(max = 80) String selectedTemplate,
    @NotEmpty @Size(max = 12) List<@Valid Item> items
) {
    public record Item(
        @NotNull @Positive Integer id,
        @Size(max = 40) String specialty,
        @Size(max = 80) String procedure,
        @Size(max = 40) String frequency,
        @Size(max = 20) String scheduling,
        @Size(max = 500) String clinicalJustification
    ) {}
}
