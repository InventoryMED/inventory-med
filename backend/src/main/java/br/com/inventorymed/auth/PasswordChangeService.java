package br.com.inventorymed.auth;

import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.security.PasswordPolicy;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class PasswordChangeService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    PasswordChangeService(
        AppUserRepository userRepository,
        PasswordEncoder passwordEncoder,
        PasswordPolicy passwordPolicy
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    AppUser change(UUID userId, ChangePasswordRequest request) {
        AppUser user = userRepository
            .findById(userId)
            .filter(AppUser::isActive)
            .orElseThrow(() -> new AccessDeniedException("Usuário inativo ou inexistente"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessValidationException("A senha atual está incorreta");
        }
        passwordPolicy.validate(request.newPassword());
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessValidationException(
                "A nova senha deve ser diferente da senha atual"
            );
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()), Instant.now());
        userRepository.flush();
        return user;
    }
}
