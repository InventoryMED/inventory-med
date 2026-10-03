package br.com.inventorymed.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory.bootstrap")
public record BootstrapProperties(
    boolean enabled,
    String systemAdminEmail,
    String systemAdminPassword
) {}
