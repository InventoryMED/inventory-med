package br.com.inventorymed.auth;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SelectHospitalRequest(@NotNull UUID hospitalId) {}
