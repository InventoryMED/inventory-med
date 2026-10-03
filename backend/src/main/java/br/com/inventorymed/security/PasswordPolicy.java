package br.com.inventorymed.security;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,128}$"
    );

    public void validate(String password) {
        if (password == null || !STRONG_PASSWORD.matcher(password).matches()) {
            throw new BusinessValidationException(
                "A senha deve ter entre 12 e 128 caracteres e conter maiúscula, minúscula, número e símbolo"
            );
        }
    }
}
