package br.com.inventorymed.bootstrap;

import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.SystemRole;
import br.com.inventorymed.identity.SystemUserRole;
import br.com.inventorymed.identity.SystemUserRoleRepository;
import br.com.inventorymed.security.PasswordPolicy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
    prefix = "inventory.bootstrap",
    name = "enabled",
    havingValue = "true"
)
public class BootstrapDataInitializer implements ApplicationRunner {

    private final BootstrapProperties properties;
    private final AppUserRepository userRepository;
    private final SystemUserRoleRepository systemRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;

    public BootstrapDataInitializer(
        BootstrapProperties properties,
        AppUserRepository userRepository,
        SystemUserRoleRepository systemRoleRepository,
        PasswordEncoder passwordEncoder,
        PasswordPolicy passwordPolicy
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) return;
        createSystemAdministrator();
    }

    private void createSystemAdministrator() {
        if (properties.systemAdminPassword() == null || properties.systemAdminPassword().isBlank()) {
            return;
        }
        passwordPolicy.validate(properties.systemAdminPassword());
        AppUser administrator = userRepository
            .findByEmailIgnoreCase(properties.systemAdminEmail())
            .orElseGet(() ->
                userRepository.save(
                    new AppUser(
                        "ADMINISTRADOR GERAL",
                        properties.systemAdminEmail(),
                        passwordEncoder.encode(properties.systemAdminPassword()),
                        true
                    )
                )
            );
        if (
            !systemRoleRepository.existsByUserIdAndRole(
                administrator.getId(),
                SystemRole.ADMIN_SISTEMA
            )
        ) {
            systemRoleRepository.save(
                new SystemUserRole(administrator, SystemRole.ADMIN_SISTEMA)
            );
        }
    }

}
