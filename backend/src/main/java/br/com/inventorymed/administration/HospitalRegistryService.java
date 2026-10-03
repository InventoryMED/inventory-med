package br.com.inventorymed.administration;

import br.com.inventorymed.common.ResourceConflictException;
import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.identity.Hospital;
import br.com.inventorymed.identity.HospitalDatabaseSecret;
import br.com.inventorymed.identity.HospitalDatabaseSecretRepository;
import br.com.inventorymed.identity.HospitalRepository;
import br.com.inventorymed.identity.HospitalStatus;
import br.com.inventorymed.tenancy.ProvisionedTenantCredential;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class HospitalRegistryService {

    private static final int MAX_PROVISIONING_ERROR_LENGTH = 1000;

    private final HospitalRepository hospitalRepository;
    private final HospitalDatabaseSecretRepository secretRepository;

    HospitalRegistryService(
        HospitalRepository hospitalRepository,
        HospitalDatabaseSecretRepository secretRepository
    ) {
        this.hospitalRepository = hospitalRepository;
        this.secretRepository = secretRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Hospital startProvisioning(HospitalCreateRequest request) {
        String name = normalize(request.name());
        if (hospitalRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceConflictException("Já existe um hospital com este nome");
        }

        String technicalCode = nextTechnicalCode();
        String databaseName = "inventory_med_hospital_" + technicalCode.toLowerCase(Locale.ROOT);
        return hospitalRepository.saveAndFlush(
            new Hospital(
                name,
                normalize(request.shortName()),
                normalize(request.city()),
                databaseName,
                technicalCode,
                HospitalStatus.PROVISIONING
            )
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Hospital completeProvisioning(
        UUID hospitalId,
        ProvisionedTenantCredential credential
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        secretRepository.save(
            new HospitalDatabaseSecret(
                hospital,
                credential.loginName(),
                credential.encryptedPassword()
            )
        );
        hospital.markProvisioned(Instant.now());
        return hospital;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    Hospital restartProvisioning(UUID hospitalId) {
        Hospital hospital = requiredHospital(hospitalId);
        if (hospital.getStatus() != HospitalStatus.PROVISIONING_FAILED) {
            throw new BusinessValidationException(
                "Somente hospitais com falha de provisionamento podem ser reprocessados"
            );
        }
        if (secretRepository.existsById(hospitalId)) {
            throw new ResourceConflictException(
                "O hospital já possui uma credencial de banco registrada"
            );
        }
        hospital.restartProvisioning(Instant.now());
        return hospital;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void failProvisioning(UUID hospitalId, String reason) {
        Hospital hospital = requiredHospital(hospitalId);
        hospital.markProvisioningFailed(
            truncate(reason, MAX_PROVISIONING_ERROR_LENGTH),
            Instant.now()
        );
    }

    private Hospital requiredHospital(UUID hospitalId) {
        return hospitalRepository
            .findById(hospitalId)
            .orElseThrow(() -> new IllegalStateException("Hospital em provisionamento não encontrado"));
    }

    private String nextTechnicalCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "H" +
                UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 16)
                    .toUpperCase(Locale.ROOT);
            if (!hospitalRepository.existsByTechnicalCode(candidate)) return candidate;
        }
        throw new IllegalStateException("Não foi possível gerar o identificador do hospital");
    }

    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.forLanguageTag("pt-BR"));
    }

    private String truncate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) return value;
        return value.substring(0, maximumLength);
    }
}
