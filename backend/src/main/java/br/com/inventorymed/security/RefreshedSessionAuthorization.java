package br.com.inventorymed.security;

import java.util.UUID;

record RefreshedSessionAuthorization(
    InventoryUserPrincipal principal,
    UUID selectedHospitalId,
    String selectedHospitalRole
) {}
