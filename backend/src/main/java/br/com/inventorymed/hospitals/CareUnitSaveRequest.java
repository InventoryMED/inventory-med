package br.com.inventorymed.hospitals;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CareUnitSaveRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
    @Min(0) int displayOrder,
    boolean active
) {}
