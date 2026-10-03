package br.com.inventorymed.administration;

import br.com.inventorymed.audit.AuditOutcome;
import br.com.inventorymed.audit.AuditService;
import br.com.inventorymed.common.ProvisioningException;
import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.Hospital;
import br.com.inventorymed.identity.HospitalRepository;
import br.com.inventorymed.security.InventoryUserPrincipal;
import br.com.inventorymed.tenancy.ProvisionedTenantCredential;
import br.com.inventorymed.tenancy.TenantDatabaseProvisioner;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HospitalAdministrationService {

    private final HospitalRepository hospitalRepository;
    private final AppUserRepository userRepository;
    private final HospitalRegistryService registryService;
    private final TenantDatabaseProvisioner databaseProvisioner;
    private final AuditService auditService;

    public HospitalAdministrationService(
        HospitalRepository hospitalRepository,
        AppUserRepository userRepository,
        HospitalRegistryService registryService,
        TenantDatabaseProvisioner databaseProvisioner,
        AuditService auditService
    ) {
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.registryService = registryService;
        this.databaseProvisioner = databaseProvisioner;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<HospitalSummaryResponse> list() {
        return hospitalRepository
            .findAllByOrderByName()
            .stream()
            .map(HospitalSummaryResponse::from)
            .toList();
    }

    public HospitalSummaryResponse create(
        HospitalCreateRequest request,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        AppUser actor = requiredActor(principal);
        Hospital pending;
        try {
            pending = registryService.startProvisioning(request);
        } catch (RuntimeException exception) {
            auditAdministrativeFailure(
                "ADMIN_HOSPITAL_CREATED",
                actor,
                sourceIp,
                userAgent,
                exception
            );
            throw exception;
        }
        return provisionPending(
            pending,
            actor,
            sourceIp,
            userAgent,
            "ADMIN_HOSPITAL_CREATED"
        );
    }

    public HospitalSummaryResponse retryProvisioning(
        UUID hospitalId,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        AppUser actor = requiredActor(principal);
        Hospital pending;
        try {
            pending = registryService.restartProvisioning(hospitalId);
        } catch (RuntimeException exception) {
            auditAdministrativeFailure(
                "ADMIN_HOSPITAL_PROVISIONING_RETRIED",
                actor,
                sourceIp,
                userAgent,
                exception
            );
            throw exception;
        }
        return provisionPending(
            pending,
            actor,
            sourceIp,
            userAgent,
            "ADMIN_HOSPITAL_PROVISIONING_RETRIED"
        );
    }

    private HospitalSummaryResponse provisionPending(
        Hospital pending,
        AppUser actor,
        String sourceIp,
        String userAgent,
        String eventType
    ) {
        try {
            ProvisionedTenantCredential credential = databaseProvisioner.provision(pending);
            Hospital active = registryService.completeProvisioning(pending.getId(), credential);
            auditService.record(
                eventType,
                AuditOutcome.SUCCESS,
                actor,
                active,
                sourceIp,
                userAgent,
                Map.of("hospitalId", active.getId(), "status", active.getStatus().name())
            );
            return HospitalSummaryResponse.from(active);
        } catch (RuntimeException exception) {
            registryService.failProvisioning(
                pending.getId(),
                exception.getClass().getSimpleName()
            );
            auditService.record(
                eventType,
                AuditOutcome.FAILURE,
                actor,
                pending,
                sourceIp,
                userAgent,
                Map.of("hospitalId", pending.getId(), "reason", "PROVISIONING_FAILED")
            );
            if (exception instanceof ProvisioningException provisioningException) {
                throw provisioningException;
            }
            throw new ProvisioningException(
                "Não foi possível concluir a criação do hospital",
                exception
            );
        }
    }

    private AppUser requiredActor(InventoryUserPrincipal principal) {
        return userRepository
            .findById(principal.userId())
            .filter(AppUser::isActive)
            .orElseThrow(() -> new IllegalStateException("Administrador não encontrado"));
    }

    private void auditAdministrativeFailure(
        String eventType,
        AppUser actor,
        String sourceIp,
        String userAgent,
        RuntimeException exception
    ) {
        auditService.record(
            eventType,
            AuditOutcome.FAILURE,
            actor,
            null,
            sourceIp,
            userAgent,
            Map.of("reason", exception.getClass().getSimpleName())
        );
    }
}
