package br.com.inventorymed.tenancy;

import br.com.inventorymed.common.ProvisioningException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class CredentialCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int REQUIRED_KEY_LENGTH = 32;

    private final TenantProvisioningProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialCipher(TenantProvisioningProperties properties) {
        this.properties = properties;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] key = encryptionKey();
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(key, "AES"),
                new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new ProvisioningException(
                "Não foi possível proteger a credencial do banco hospitalar",
                exception
            );
        }
    }

    private byte[] encryptionKey() {
        String configuredKey = properties.credentialEncryptionKey();
        if (configuredKey == null || configuredKey.isBlank()) {
            throw new ProvisioningException(
                "A chave de proteção dos bancos hospitalares não foi configurada"
            );
        }
        byte[] decoded = Base64.getDecoder().decode(configuredKey);
        if (decoded.length != REQUIRED_KEY_LENGTH) {
            throw new ProvisioningException(
                "A chave de proteção dos bancos hospitalares deve possuir 256 bits"
            );
        }
        return decoded;
    }
}
