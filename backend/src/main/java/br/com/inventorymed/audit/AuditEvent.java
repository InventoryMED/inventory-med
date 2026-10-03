package br.com.inventorymed.audit;

import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.Hospital;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id")
    private Hospital hospital;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUser actor;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditOutcome outcome;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "details_json", columnDefinition = "nvarchar(max)")
    private String detailsJson;

    @Column(name = "occurred_at", nullable = false, insertable = false, updatable = false)
    private Instant occurredAt;

    protected AuditEvent() {}

    public AuditEvent(
        Hospital hospital,
        AppUser actor,
        String eventType,
        AuditOutcome outcome,
        String sourceIp,
        String userAgent,
        String detailsJson
    ) {
        this.hospital = hospital;
        this.actor = actor;
        this.eventType = eventType;
        this.outcome = outcome;
        this.sourceIp = sourceIp;
        this.userAgent = userAgent;
        this.detailsJson = detailsJson;
    }
}
