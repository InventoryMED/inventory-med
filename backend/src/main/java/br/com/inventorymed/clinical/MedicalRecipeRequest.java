package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MedicalRecipeRequest(
    @NotBlank @Pattern(regexp = "COMMON|SPECIAL_CONTROL|NOTIFICATION_B") String recipeType,
    @NotEmpty @Size(max = 30) List<@Valid Item> items,
    @Size(max = 180) String buyerName,
    @Size(max = 40) String buyerIdentityDocument,
    @Size(max = 300) String buyerAddress,
    boolean physicianAttestation
) {
    public record Item(
        @NotBlank @Pattern(
            regexp = "ORAL|INHALATION|TOPICAL|INJECTABLE_SC|OCULAR|NASAL|RECTAL"
        ) String route,
        @NotBlank @Size(max = 180) String medication,
        @NotBlank @Size(max = 80) String quantity,
        @NotBlank @Size(max = 500) String dosage,
        @Size(max = 1000) String recommendations
    ) {}
}
