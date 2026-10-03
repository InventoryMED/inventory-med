package br.com.inventorymed.clinical;

import br.com.inventorymed.formtemplates.FormKind;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public final class ClinicalRequests {

    private ClinicalRequests() {}

    public record AdmitPatient(
        @NotNull UUID bedId,
        @NotBlank @Size(max = 180) String fullName,
        LocalDate birthDate,
        @NotBlank @Pattern(regexp = "FEMININO|MASCULINO|OUTRO|NAO_INFORMADO") String sex,
        @DecimalMin("0.10") @DecimalMax("500.00") BigDecimal weightKg,
        @Size(max = 1000) String diagnosis,
        @Size(max = 2000) String comorbidities,
        @Size(max = 2000) String allergies
    ) {}

    public record Transfer(@NotNull UUID targetBedId) {}

    public record Discharge(
        @NotBlank @Pattern(regexp = "OBITO|TRANSFERENCIA|ALTA_MELHORA") String reason
    ) {}

    public record SaveDocument(
        @NotNull UUID templateVersionId,
        @NotNull FormKind kind,
        @NotNull @Size(max = 500) Map<String, Object> values,
        boolean finalizeDocument
    ) {}
}
