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
import java.time.Instant;
import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.identity.SystemRole;
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

    @Transactional
    public AdminUserResponse setUserActive(
        UUID userId,
        boolean active,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        AppUser actor = requiredActor(principal);
        AppUser target = userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessValidationException("Usuário não encontrado"));
        if (!active && actor.getId().equals(target.getId())) {
            throw new BusinessValidationException("Você não pode desativar a própria conta");
        }
        if (!active && isLastActiveSystemAdministrator(target)) {
            throw new BusinessValidationException("Mantenha ao menos um administrador geral ativo");
        }
        if (active) target.activate(Instant.now()); else target.deactivate(Instant.now());
        auditService.record(
            "ADMIN_USER_STATUS_CHANGED",
            AuditOutcome.SUCCESS,
            actor,
            null,
            sourceIp,
            userAgent,
            Map.of("targetUserId", userId, "active", active)
        );
        return response(target);
    }

    @Transactional
    public AdminUserResponse setHospitalAccessActive(
        UUID userId,
        UUID hospitalId,
        boolean active,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        AppUser actor = requiredActor(principal);
        var membership = membershipRepository
            .findByUserIdAndHospitalId(userId, hospitalId)
            .orElseThrow(() -> new BusinessValidationException("Perfil hospitalar não encontrado"));
        if (active) membership.activate(); else membership.deactivate();
        auditService.record(
            "ADMIN_HOSPITAL_ACCESS_STATUS_CHANGED",
            AuditOutcome.SUCCESS,
            actor,
            membership.getHospital(),
            sourceIp,
            userAgent,
            Map.of("targetUserId", userId, "active", active, "role", membership.getRole().name())
        );
        return response(membership.getUser());
    }

    private boolean isLastActiveSystemAdministrator(AppUser target) {
        if (!systemRoleRepository.existsByUserIdAndRole(target.getId(), SystemRole.ADMIN_SISTEMA)) {
            return false;
        }
        long activeAdministrators = systemRoleRepository
            .findAllWithUser()
            .stream()
            .filter(role -> role.getRole() == SystemRole.ADMIN_SISTEMA)
            .map(role -> role.getUser())
            .filter(AppUser::isActive)
            .map(AppUser::getId)
            .distinct()
            .count();
        return activeAdministrators <= 1;
    }

    private AdminUserResponse response(AppUser user) {
        List<String> systemRoles = systemRoleRepository
            .findAllByUserId(user.getId())
            .stream()
            .map(role -> role.getRole().name())
            .toList();
        List<UserHospitalAccessResponse> hospitals = membershipRepository
            .findAllWithHospitalAndUser()
            .stream()
            .filter(item -> item.getUser().getId().equals(user.getId()))
            .map(UserHospitalAccessResponse::from)
            .toList();
        return AdminUserResponse.from(user, systemRoles, hospitals);
    }

    private AppUser requiredActor(InventoryUserPrincipal principal) {
        return userRepository
            .findById(principal.userId())
            .filter(AppUser::isActive)
            .orElseThrow(() -> new IllegalStateException("Administrador não encontrado"));
    }
}
