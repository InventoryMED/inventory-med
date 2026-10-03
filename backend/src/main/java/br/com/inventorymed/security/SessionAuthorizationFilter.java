package br.com.inventorymed.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionAuthorizationFilter extends OncePerRequestFilter {

    private final SessionAuthorizationService authorizationService;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityErrorWriter errorWriter;

    public SessionAuthorizationFilter(
        SessionAuthorizationService authorizationService,
        SecurityContextRepository securityContextRepository,
        SecurityErrorWriter errorWriter
    ) {
        this.authorizationService = authorizationService;
        this.securityContextRepository = securityContextRepository;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder
            .getContextHolderStrategy()
            .getContext()
            .getAuthentication();

        if (
            authentication == null ||
            !authentication.isAuthenticated() ||
            !(authentication.getPrincipal() instanceof InventoryUserPrincipal principal)
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        UUID selectedHospitalId = selectedHospitalId(session);

        try {
            RefreshedSessionAuthorization refreshed = authorizationService.refresh(
                principal,
                selectedHospitalId
            );
            synchronizeHospitalScope(session, refreshed);
            UsernamePasswordAuthenticationToken refreshedAuthentication =
                UsernamePasswordAuthenticationToken.authenticated(
                    refreshed.principal(),
                    null,
                    refreshed.principal().getAuthorities()
                );
            refreshedAuthentication.setDetails(authentication.getDetails());

            SecurityContext context = SecurityContextHolder
                .getContextHolderStrategy()
                .createEmptyContext();
            context.setAuthentication(refreshedAuthentication);
            SecurityContextHolder.getContextHolderStrategy().setContext(context);
            securityContextRepository.saveContext(context, request, response);
            filterChain.doFilter(request, response);
        } catch (AccessDeniedException exception) {
            SecurityContextHolder.getContextHolderStrategy().clearContext();
            if (session != null) {
                session.invalidate();
            }
            errorWriter.write(
                request,
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                "SESSION_REVOKED",
                "A autorização desta sessão não é mais válida"
            );
        }
    }

    private UUID selectedHospitalId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionAttributes.SELECTED_HOSPITAL_ID);
        return value instanceof UUID id ? id : null;
    }

    private void synchronizeHospitalScope(
        HttpSession session,
        RefreshedSessionAuthorization refreshed
    ) {
        if (session == null) {
            return;
        }
        if (refreshed.selectedHospitalId() == null) {
            session.removeAttribute(SessionAttributes.SELECTED_HOSPITAL_ID);
            session.removeAttribute(SessionAttributes.SELECTED_HOSPITAL_ROLE);
            return;
        }
        session.setAttribute(
            SessionAttributes.SELECTED_HOSPITAL_ID,
            refreshed.selectedHospitalId()
        );
        session.setAttribute(
            SessionAttributes.SELECTED_HOSPITAL_ROLE,
            refreshed.selectedHospitalRole()
        );
    }
}
