package br.com.inventorymed.auth;

import br.com.inventorymed.audit.AuditOutcome;
import br.com.inventorymed.audit.AuthenticationAuditService;
import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.HospitalMembership;
import br.com.inventorymed.identity.HospitalMembershipRepository;
import br.com.inventorymed.identity.SystemRole;
import br.com.inventorymed.security.InventoryUserPrincipal;
import br.com.inventorymed.security.SessionAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "E-mail ou senha inválidos";

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository userRepository;
    private final HospitalMembershipRepository membershipRepository;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final AuthenticationAuditService auditService;

    public AuthService(
        AuthenticationManager authenticationManager,
        AppUserRepository userRepository,
        HospitalMembershipRepository membershipRepository,
        SecurityContextRepository securityContextRepository,
        CsrfTokenRepository csrfTokenRepository,
        AuthenticationAuditService auditService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.auditService = auditService;
    }

    @Transactional
    public LoginResponse login(
        LoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        String email = loginRequest.email().trim().toLowerCase(Locale.ROOT);
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    email,
                    loginRequest.password()
                )
            );
        } catch (AuthenticationException exception) {
            AppUser knownUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
            auditService.record(
                "AUTHENTICATION_LOGIN",
                AuditOutcome.FAILURE,
                knownUser,
                null,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                Map.of("identifier", email, "reason", "INVALID_CREDENTIALS")
            );
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        InventoryUserPrincipal principal = (InventoryUserPrincipal) authentication.getPrincipal();
        AppUser user = activeUser(principal.userId());
        List<HospitalMembership> memberships = activeMemberships(user.getId());

        if (
            memberships.isEmpty() &&
            !principal.hasSystemRole(SystemRole.ADMIN_SISTEMA.name())
        ) {
            auditService.record(
                "AUTHENTICATION_LOGIN",
                AuditOutcome.DENIED,
                user,
                null,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"),
                Map.of("reason", "NO_ACTIVE_HOSPITAL_MEMBERSHIP")
            );
            throw new AccessDeniedException("Usuário sem acesso ativo a hospitais");
        }

        HospitalMembership selectedMembership = memberships.size() == 1
            ? memberships.getFirst()
            : null;
        Authentication sessionAuthentication = sessionAuthentication(principal);
        establishSession(
            sessionAuthentication,
            selectedMembership,
            request,
            response,
            true
        );

        user.recordSuccessfulLogin(Instant.now());
        auditService.record(
            "AUTHENTICATION_LOGIN",
            AuditOutcome.SUCCESS,
            user,
            selectedMembership == null ? null : selectedMembership.getHospital(),
            request.getRemoteAddr(),
            request.getHeader("User-Agent"),
            Map.of("hospitalAutoSelected", selectedMembership != null)
        );

        return new LoginResponse(
            UserResponse.from(user),
            memberships.stream().map(HospitalAccessResponse::from).toList(),
            memberships.size() > 1,
            selectedMembership == null ? null : selectedMembership.getHospital().getId(),
            selectedMembership == null ? null : selectedMembership.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public HospitalSelectionResponse selectHospital(
        InventoryUserPrincipal principal,
        UUID hospitalId,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        HospitalMembership membership = membershipRepository
            .findByUserIdAndHospitalIdAndActiveTrue(principal.userId(), hospitalId)
            .filter(item -> item.getHospital().isActive())
            .orElseThrow(() ->
                new AccessDeniedException("Usuário não possui acesso ao hospital informado")
            );

        Authentication sessionAuthentication = sessionAuthentication(principal);
        establishSession(
            sessionAuthentication,
            membership,
            request,
            response,
            true
        );

        auditService.record(
            "AUTHENTICATION_HOSPITAL_SELECTED",
            AuditOutcome.SUCCESS,
            membership.getUser(),
            membership.getHospital(),
            request.getRemoteAddr(),
            request.getHeader("User-Agent"),
            Map.of("role", membership.getRole().name())
        );
        return new HospitalSelectionResponse(HospitalAccessResponse.from(membership));
    }

    @Transactional(readOnly = true)
    public MeResponse me(InventoryUserPrincipal principal, HttpSession session) {
        AppUser user = activeUser(principal.userId());
        List<HospitalMembership> memberships = activeMemberships(user.getId());
        UUID selectedHospitalId = selectedHospitalId(session);
        String selectedHospitalRole = selectedHospitalRole(session);

        if (
            selectedHospitalId != null &&
            !hasHospitalMembership(memberships, selectedHospitalId)
        ) {
            session.removeAttribute(SessionAttributes.SELECTED_HOSPITAL_ID);
            session.removeAttribute(SessionAttributes.SELECTED_HOSPITAL_ROLE);
            selectedHospitalId = null;
            selectedHospitalRole = null;
        }

        return new MeResponse(
            UserResponse.from(user),
            memberships.stream().map(HospitalAccessResponse::from).toList(),
            selectedHospitalId,
            selectedHospitalRole
        );
    }

    private Authentication sessionAuthentication(InventoryUserPrincipal principal) {
        return UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities()
        );
    }

    private void establishSession(
        Authentication authentication,
        HospitalMembership membership,
        HttpServletRequest request,
        HttpServletResponse response,
        boolean rotateExistingSession
    ) {
        HttpSession existingSession = request.getSession(false);
        if (rotateExistingSession && existingSession != null) {
            request.changeSessionId();
        }
        HttpSession session = request.getSession(true);
        session.removeAttribute(SessionAttributes.SELECTED_HOSPITAL_ID);
        session.removeAttribute(SessionAttributes.SELECTED_HOSPITAL_ROLE);
        if (membership != null) {
            session.setAttribute(
                SessionAttributes.SELECTED_HOSPITAL_ID,
                membership.getHospital().getId()
            );
            session.setAttribute(
                SessionAttributes.SELECTED_HOSPITAL_ROLE,
                membership.getRole().name()
            );
        }

        SecurityContext context = SecurityContextHolder
            .getContextHolderStrategy()
            .createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.getContextHolderStrategy().setContext(context);
        securityContextRepository.saveContext(context, request, response);
        rotateCsrfToken(request, response);
    }

    private void rotateCsrfToken(
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        csrfTokenRepository.saveToken(null, request, response);
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);
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

    private UUID selectedHospitalId(HttpSession session) {
        Object value = session.getAttribute(SessionAttributes.SELECTED_HOSPITAL_ID);
        return value instanceof UUID id ? id : null;
    }

    private boolean hasHospitalMembership(
        List<HospitalMembership> memberships,
        UUID hospitalId
    ) {
        return memberships
            .stream()
            .anyMatch(item -> item.getHospital().getId().equals(hospitalId));
    }

    private String selectedHospitalRole(HttpSession session) {
        Object value = session.getAttribute(SessionAttributes.SELECTED_HOSPITAL_ROLE);
        return value instanceof String role ? role : null;
    }
}
