package br.com.inventorymed.auth;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
    UserResponse user,
    List<HospitalAccessResponse> hospitals,
    boolean requiresHospitalSelection,
    UUID selectedHospitalId,
    String selectedHospitalRole
) {}
