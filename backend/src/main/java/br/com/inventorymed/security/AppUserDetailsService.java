package br.com.inventorymed.security;

import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.SystemUserRoleRepository;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository userRepository;
    private final SystemUserRoleRepository systemRoleRepository;

    public AppUserDetailsService(
        AppUserRepository userRepository,
        SystemUserRoleRepository systemRoleRepository
    ) {
        this.userRepository = userRepository;
        this.systemRoleRepository = systemRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        AppUser user = userRepository
            .findByEmailIgnoreCase(username.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        return new InventoryUserPrincipal(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getPasswordHash(),
            user.isActive(),
            systemRoleRepository
                .findAllByUserId(user.getId())
                .stream()
                .map(systemRole -> systemRole.getRole().name())
                .toList()
        );
    }
}
