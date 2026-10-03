package br.com.inventorymed.hospitals;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RoomSaveRequest(
    @NotNull UUID careUnitId,
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
    @Size(max = 80) String floorName,
    @Min(0) int displayOrder,
    boolean active
) {}
