package br.com.inventorymed.administration;

import br.com.inventorymed.audit.AuditOutcome;
import br.com.inventorymed.audit.AuditService;
import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.HospitalMembershipRepository;
import br.com.inventorymed.identity.SystemUserRoleRepository;
import br.com.inventorymed.security.InventoryUserPrincipal;
import br.com.inventorymed.security.PasswordPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdministrationService {

    private final AppUserRepository userRepository;
    private final HospitalMembershipRepository membershipRepository;
    private final SystemUserRoleRepository systemRoleRepository;
    private final UserRegistryService registryService;
    private final PasswordPolicy passwordPolicy;
    private final AuditService auditService;

    public UserAdministrationService(
        AppUserRepository userRepository,
        HospitalMembershipRepository membershipRepository,
        SystemUserRoleRepository systemRoleRepository,
        UserRegistryService registryService,
        PasswordPolicy passwordPolicy,
        AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.registryService = registryService;
        this.passwordPolicy = passwordPolicy;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        Map<UUID, List<String>> systemRoles = new HashMap<>();
        systemRoleRepository
            .findAllWithUser()
            .forEach(role ->
                systemRoles
                    .computeIfAbsent(role.getUser().getId(), ignored -> new ArrayList<>())
                    .add(role.getRole().name())
            );
        Map<UUID, List<UserHospitalAccessResponse>> hospitalAccesses = new HashMap<>();
        membershipRepository
            .findAllWithHospitalAndUser()
            .forEach(membership ->
                hospitalAccesses
                    .computeIfAbsent(
                        membership.getUser().getId(),
                        ignored -> new ArrayList<>()
                    )
                    .add(UserHospitalAccessResponse.from(membership))
            );

        return userRepository
            .findAllByOrderByFullName()
            .stream()
            .map(user ->
                AdminUserResponse.from(
                    user,
                    systemRoles.getOrDefault(user.getId(), List.of()),
                    hospitalAccesses.getOrDefault(user.getId(), List.of())
                )
            )
            .toList();
    }

    public AdminUserResponse create(
        UserCreateRequest request,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        AppUser actor = requiredActor(principal);
        AdminUserResponse created;
        try {
            passwordPolicy.validate(request.initialPassword());
            created = registryService.create(request);
        } catch (RuntimeException exception) {
            auditService.record(
                "ADMIN_USER_CREATED",
                AuditOutcome.FAILURE,
                actor,
                null,
                sourceIp,
                userAgent,
                Map.of("reason", exception.getClass().getSimpleName())
            );
            throw exception;
        }
        auditService.record(
            "ADMIN_USER_CREATED",
            AuditOutcome.SUCCESS,
            actor,
            null,
            sourceIp,
            userAgent,
            Map.of(
                "createdUserId",
                created.id(),
                "systemRoles",
                created.systemRoles(),
                "hospitalCount",
                created.hospitals().size()
            )
        );
        return created;
    }

    private AppUser requiredActor(InventoryUserPrincipal principal) {
        return userRepository
            .findById(principal.userId())
            .filter(AppUser::isActive)
            .orElseThrow(() -> new IllegalStateException("Administrador não encontrado"));
    }
}
