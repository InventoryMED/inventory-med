package br.com.inventorymed.clinical;

import br.com.inventorymed.identity.HospitalRole;
import br.com.inventorymed.security.InventoryUserPrincipal;
import br.com.inventorymed.security.SessionAttributes;
import jakarta.servlet.http.HttpSession;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class HospitalSessionContext {

    public Scope require(HttpSession session, HospitalRole... allowedRoles) {
        if (session == null) {
            throw new AccessDeniedException("Selecione um hospital para continuar");
        }
        Object hospitalValue = session.getAttribute(SessionAttributes.SELECTED_HOSPITAL_ID);
        Object roleValue = session.getAttribute(SessionAttributes.SELECTED_HOSPITAL_ROLE);
        if (!(hospitalValue instanceof UUID hospitalId) || !(roleValue instanceof String roleName)) {
            throw new AccessDeniedException("Selecione um hospital para continuar");
        }

        HospitalRole role;
        try {
            role = HospitalRole.valueOf(roleName);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException("Perfil hospitalar inválido");
        }

        if (allowedRoles.length > 0 && !EnumSet.of(allowedRoles[0], allowedRoles).contains(role)) {
            throw new AccessDeniedException("Seu perfil não permite esta operação");
        }
        return new Scope(hospitalId, role);
    }

    public Actor requireActor(
        InventoryUserPrincipal principal,
        HttpSession session,
        HospitalRole... allowedRoles
    ) {
        if (principal == null) {
            throw new AccessDeniedException("Autenticação necessária");
        }
        Scope scope = require(session, allowedRoles);
        return new Actor(principal.userId(), scope.hospitalId(), scope.role());
    }

    public record Scope(UUID hospitalId, HospitalRole role) {}

    public record Actor(UUID userId, UUID hospitalId, HospitalRole role) {}
}
