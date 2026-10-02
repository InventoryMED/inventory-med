package br.com.inventorymed.auth;

public record HospitalSelectionResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    HospitalAccessResponse hospital
) {}
