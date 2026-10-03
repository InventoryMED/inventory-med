package br.com.inventorymed.auth;

import java.util.List;
import java.util.UUID;

public record MeResponse(
    UserResponse user,
    List<HospitalAccessResponse> hospitals,
    UUID selectedHospitalId,
    String selectedHospitalRole
) {}
