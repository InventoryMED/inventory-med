package br.com.inventorymed.security;

import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.HospitalMembership;
import br.com.inventorymed.identity.HospitalMembershipRepository;
import br.com.inventorymed.identity.SystemRole;
import br.com.inventorymed.identity.SystemUserRoleRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionAuthorizationService {

    private final AppUserRepository userRepository;
    private final SystemUserRoleRepository systemRoleRepository;
    private final HospitalMembershipRepository membershipRepository;

    public SessionAuthorizationService(
        AppUserRepository userRepository,
        SystemUserRoleRepository systemRoleRepository,
        HospitalMembershipRepository membershipRepository
    ) {
        this.userRepository = userRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    RefreshedSessionAuthorization refresh(
        InventoryUserPrincipal currentPrincipal,
        UUID selectedHospitalId
    ) {
        AppUser user = userRepository
            .findById(currentPrincipal.userId())
            .filter(AppUser::isActive)
            .orElseThrow(() -> new AccessDeniedException("Usuário inativo ou inexistente"));

        List<String> systemRoles = systemRoleRepository
            .findAllByUserId(user.getId())
            .stream()
            .map(systemRole -> systemRole.getRole().name())
            .toList();
        List<HospitalMembership> memberships = membershipRepository
            .findAllByUserIdAndActiveTrueOrderByHospitalName(user.getId())
            .stream()
            .filter(membership -> membership.getHospital().isActive())
            .toList();

        if (
            memberships.isEmpty() &&
            !systemRoles.contains(SystemRole.ADMIN_SISTEMA.name())
        ) {
            throw new AccessDeniedException("Usuário sem acesso ativo a hospitais");
        }

        HospitalMembership selectedMembership = selectedHospitalId == null
            ? null
            : memberships
                .stream()
                .filter(membership ->
                    membership.getHospital().getId().equals(selectedHospitalId)
                )
                .findFirst()
                .orElse(null);

        InventoryUserPrincipal refreshedPrincipal = new InventoryUserPrincipal(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            null,
            true,
            systemRoles
        );
        return new RefreshedSessionAuthorization(
            refreshedPrincipal,
            selectedMembership == null
                ? null
                : selectedMembership.getHospital().getId(),
            selectedMembership == null ? null : selectedMembership.getRole().name()
        );
    }
}
