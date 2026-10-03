package br.com.inventorymed.administration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record HospitalCreateRequest(
    @NotBlank @Size(max = 180) String name,
    @NotBlank
    @Size(min = 2, max = 40)
    @Pattern(regexp = "[A-Za-zÀ-ÿ0-9 ._-]+")
    String shortName,
    @NotBlank @Size(max = 120) String city
) {}
