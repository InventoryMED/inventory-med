package br.com.inventorymed.auth;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    UserResponse user,
    List<HospitalAccessResponse> hospitals,
    boolean requiresHospitalSelection,
    UUID selectedHospitalId
) {}
