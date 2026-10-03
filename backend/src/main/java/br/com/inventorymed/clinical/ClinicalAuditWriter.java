package br.com.inventorymed.clinical;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClinicalAuditWriter {

    private final ObjectMapper objectMapper;

    public ClinicalAuditWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void success(
        JdbcTemplate jdbc,
        UUID actorId,
        String eventType,
        String entityType,
        UUID entityId,
        String sourceIp,
        Map<String, ?> details
    ) {
        jdbc.update(
            "INSERT INTO dbo.clinical_audit_event " +
            "(actor_user_id, event_type, entity_type, entity_id, outcome, source_ip, details_json) " +
            "VALUES (?, ?, ?, ?, 'SUCCESS', ?, ?)",
            actorId,
            eventType,
            entityType,
            entityId == null ? null : entityId.toString(),
            sourceIp,
            json(details)
        );
    }

    private String json(Map<String, ?> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Falha ao serializar dados de auditoria", exception);
        }
    }
}
