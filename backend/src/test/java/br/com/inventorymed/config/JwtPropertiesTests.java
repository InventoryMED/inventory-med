package br.com.inventorymed.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTests {

    @Test
    void acceptsAValidSecretAndTtl() {
        assertDoesNotThrow(() ->
            new JwtProperties(
                "inventory-med-api",
                "a-secure-secret-with-more-than-32-characters",
                Duration.ofMinutes(15)
            )
        );
    }

    @Test
    void rejectsShortSecrets() {
        assertThrows(IllegalArgumentException.class, () ->
            new JwtProperties("inventory-med-api", "short", Duration.ofMinutes(15))
        );
    }
}
