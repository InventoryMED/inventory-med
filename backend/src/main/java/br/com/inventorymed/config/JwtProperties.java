package br.com.inventorymed.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inventory.security.jwt")
public record JwtProperties(String issuer, String secret, Duration accessTokenTtl) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer não pode ser vazio");
        }
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret deve possuir ao menos 32 caracteres");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalArgumentException("JWT access token TTL deve ser positivo");
        }
    }
}
