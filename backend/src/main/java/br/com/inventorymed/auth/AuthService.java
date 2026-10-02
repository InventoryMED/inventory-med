package br.com.inventorymed.auth;

import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.HospitalMembership;
import br.com.inventorymed.identity.HospitalMembershipRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos";

    private final AppUserRepository userRepository;
    private final HospitalMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final String dummyPasswordHash;

    public AuthService(
        AppUserRepository userRepository,
        HospitalMembershipRepository membershipRepository,
        PasswordEncoder passwordEncoder,
        TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        AppUser user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        List<HospitalMembership> memberships = activeMemberships(user.getId());
        if (memberships.isEmpty()) {
            throw new AccessDeniedException("Usuário sem acesso ativo a hospitais");
        }

        HospitalMembership selectedMembership = memberships.size() == 1 ? memberships.getFirst() : null;
        TokenService.IssuedToken token = tokenService.issueAccessToken(user, selectedMembership);

        return new LoginResponse(
            token.value(),
            "Bearer",
            token.expiresInSeconds(),
            UserResponse.from(user),
            memberships.stream().map(HospitalAccessResponse::from).toList(),
            selectedMembership == null,
            token.hospitalId()
        );
    }

    @Transactional(readOnly = true)
    public HospitalSelectionResponse selectHospital(UUID userId, UUID hospitalId) {
        AppUser user = activeUser(userId);
        HospitalMembership membership = membershipRepository
            .findByUserIdAndHospitalIdAndActiveTrue(userId, hospitalId)
            .filter(item -> item.getHospital().isActive())
            .orElseThrow(() ->
                new AccessDeniedException("Usuário não possui acesso ao hospital informado")
            );

        TokenService.IssuedToken token = tokenService.issueAccessToken(user, membership);
        return new HospitalSelectionResponse(
            token.value(),
            "Bearer",
            token.expiresInSeconds(),
            HospitalAccessResponse.from(membership)
        );
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId, UUID selectedHospitalId) {
        AppUser user = activeUser(userId);
        return new MeResponse(
            UserResponse.from(user),
            activeMemberships(userId).stream().map(HospitalAccessResponse::from).toList(),
            selectedHospitalId
        );
    }

    private AppUser activeUser(UUID userId) {
        return userRepository
            .findById(userId)
            .filter(AppUser::isActive)
            .orElseThrow(() -> new AccessDeniedException("Usuário inativo ou inexistente"));
    }

    private List<HospitalMembership> activeMemberships(UUID userId) {
        return membershipRepository
            .findAllByUserIdAndActiveTrueOrderByHospitalName(userId)
            .stream()
            .filter(membership -> membership.getHospital().isActive())
            .toList();
    }
}
