package br.com.inventorymed.tenancy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory.tenancy.provisioning")
public record TenantProvisioningProperties(
    boolean enabled,
    String serverJdbcUrl,
    String username,
    String password,
    String credentialEncryptionKey
) {}
