package br.com.inventorymed.audit;

import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.Hospital;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class AuditService {

    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 512;

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
        String eventType,
        AuditOutcome outcome,
        AppUser actor,
        Hospital hospital,
        String sourceIp,
        String userAgent,
        Map<String, ?> details
    ) {
        repository.save(
            new AuditEvent(
                hospital,
                actor,
                eventType,
                outcome,
                truncate(sourceIp, MAX_IP_LENGTH),
                truncate(userAgent, MAX_USER_AGENT_LENGTH),
                toJson(details)
            )
        );
    }

    private String toJson(Map<String, ?> details) {
        if (details == null || details.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Falha ao serializar evento de auditoria", exception);
        }
    }

    private String truncate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) return value;
        return value.substring(0, maximumLength);
    }
}
