package br.com.inventorymed.tenancy;

public record ProvisionedTenantCredential(
    String loginName,
    String encryptedPassword
) {}
