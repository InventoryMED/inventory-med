package br.com.inventorymed.administration;

import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.common.ResourceConflictException;
import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.Hospital;
import br.com.inventorymed.identity.HospitalMembership;
import br.com.inventorymed.identity.HospitalMembershipRepository;
import br.com.inventorymed.identity.HospitalRepository;
import br.com.inventorymed.identity.SystemRole;
import br.com.inventorymed.identity.SystemUserRole;
import br.com.inventorymed.identity.SystemUserRoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserRegistryService {

    private final AppUserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalMembershipRepository membershipRepository;
    private final SystemUserRoleRepository systemRoleRepository;
    private final PasswordEncoder passwordEncoder;

    UserRegistryService(
        AppUserRepository userRepository,
        HospitalRepository hospitalRepository,
        HospitalMembershipRepository membershipRepository,
        SystemUserRoleRepository systemRoleRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.membershipRepository = membershipRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    AdminUserResponse create(UserCreateRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResourceConflictException("Já existe um usuário com este e-mail");
        }

        Set<SystemRole> systemRoles = request.systemRoles() == null
            ? Set.of()
            : Set.copyOf(request.systemRoles());
        List<UserHospitalAssignmentRequest> assignments = request.hospitalAssignments() == null
            ? List.of()
            : List.copyOf(request.hospitalAssignments());
        requireAtLeastOneAccess(systemRoles, assignments);
        requireUniqueHospitals(assignments);

        try {
            AppUser user = userRepository.saveAndFlush(
                new AppUser(
                    normalizeName(request.fullName()),
                    email,
                    passwordEncoder.encode(request.initialPassword()),
                    true
                )
            );
            systemRoles.forEach(role ->
                systemRoleRepository.save(new SystemUserRole(user, role))
            );

            List<UserHospitalAccessResponse> createdMemberships = assignments
                .stream()
                .map(assignment -> createMembership(user, assignment))
                .map(UserHospitalAccessResponse::from)
                .toList();

            return AdminUserResponse.from(
                user,
                systemRoles.stream().map(Enum::name).sorted().toList(),
                createdMemberships
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResourceConflictException("Não foi possível criar o usuário informado");
        }
    }

    private HospitalMembership createMembership(
        AppUser user,
        UserHospitalAssignmentRequest assignment
    ) {
        Hospital hospital = hospitalRepository
            .findById(assignment.hospitalId())
            .filter(Hospital::isActive)
            .orElseThrow(() ->
                new BusinessValidationException("Hospital informado não está ativo")
            );
        return membershipRepository.save(
            new HospitalMembership(hospital, user, assignment.role())
        );
    }

    private void requireAtLeastOneAccess(
        Set<SystemRole> systemRoles,
        List<UserHospitalAssignmentRequest> assignments
    ) {
        if (systemRoles.isEmpty() && assignments.isEmpty()) {
            throw new BusinessValidationException(
                "Informe um perfil geral ou ao menos um vínculo hospitalar"
            );
        }
    }

    private void requireUniqueHospitals(
        List<UserHospitalAssignmentRequest> assignments
    ) {
        Set<UUID> hospitals = new HashSet<>();
        boolean unique = assignments
            .stream()
            .map(UserHospitalAssignmentRequest::hospitalId)
            .allMatch(hospitals::add);
        if (!unique) {
            throw new BusinessValidationException(
                "O mesmo hospital não pode ser vinculado duas vezes"
            );
        }
    }

    private String normalizeName(String value) {
        return value
            .trim()
            .replaceAll("\\s+", " ")
            .toUpperCase(Locale.forLanguageTag("pt-BR"));
    }
}
