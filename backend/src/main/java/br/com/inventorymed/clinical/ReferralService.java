package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.tenancy.TenantJdbcExecutor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ReferralService {

    private static final Set<String> ROOT_KEYS = Set.of(
        "specialtyCode", "specialtyDetail", "priority", "deadline",
        "clinicalJustification", "destination", "attachments", "physicianAttestation"
    );

    private final TenantJdbcExecutor tenantJdbc;
    private final ObjectMapper objectMapper;

    public ReferralService(TenantJdbcExecutor tenantJdbc, ObjectMapper objectMapper) {
        this.tenantJdbc = tenantJdbc;
        this.objectMapper = objectMapper;
    }

    public ReferralCatalog catalog() {
        return new ReferralCatalog(
            List.of(
                option("CARDIOLOGY", "CARDIOLOGIA"),
                option("ORTHOPEDICS_TRAUMATOLOGY", "ORTOPEDIA E TRAUMATOLOGIA"),
                option("NEPHROLOGY", "NEFROLOGIA"),
                option("NEUROLOGY", "NEUROLOGIA"),
                option("GENERAL_SURGERY", "CIRURGIA GERAL"),
                option("ENDOCRINOLOGY", "ENDOCRINOLOGIA"),
                option("GASTROENTEROLOGY", "GASTROENTEROLOGIA"),
                option("PULMONOLOGY", "PNEUMOLOGIA"),
                option("INFECTIOUS_DISEASE", "INFECTOLOGIA"),
                option("DERMATOLOGY", "DERMATOLOGIA"),
                option("UROLOGY", "UROLOGIA"),
                option("VASCULAR_ANGIOLOGY", "VASCULAR / ANGIOLOGIA"),
                option("MOTOR_RESPIRATORY_PHYSIOTHERAPY", "FISIOTERAPIA MOTORA / RESPIRATÓRIA"),
                option("NUTRITION", "NUTRIÇÃO"),
                option("PSYCHOLOGY", "PSICOLOGIA"),
                option("OTHER", "OUTRA ESPECIALIDADE / ÁREA")
            ),
            List.of(
                option("URGENT_HIGH", "URGENTE / ALTA PRIORIDADE"),
                option("MEDIUM", "MÉDIA PRIORIDADE"),
                option("ELECTIVE", "ELETIVO / ROTINA")
            ),
            List.of(
                option("UP_TO_7_DAYS", "ATÉ 7 DIAS"),
                option("UP_TO_15_DAYS", "ATÉ 15 DIAS"),
                option("UP_TO_30_DAYS", "ATÉ 30 DIAS"),
                option("REGULATION_DISCRETION", "A CRITÉRIO DA REGULAÇÃO")
            ),
            List.of(
                option("PUBLIC_REGULATION", "REDE PÚBLICA / REGULAÇÃO SUS"),
                option("HOSPITAL_SPECIALTY_CLINIC", "AMBULATÓRIO DE ESPECIALIDADES DO HOSPITAL"),
                option("INSURANCE_PRIVATE", "REDE CREDENCIADA / CONVÊNIO / PARTICULAR")
            ),
            List.of(
                option("DISCHARGE_SUMMARY", "SUMÁRIO / RELATÓRIO DE ALTA HOSPITALAR"),
                option("IMAGING", "EXAMES DE IMAGEM / LAUDOS"),
                option("LABORATORY", "EXAMES DE LABORATÓRIO"),
                option("PENDING_PATHOLOGY", "BIÓPSIA / LAUDO HISTOPATOLÓGICO PENDENTE")
            ),
            false
        );
    }

    public ReferralResponse preview(
        UUID hospitalId,
        UUID admissionId,
        ReferralRequest request
    ) {
        requireActiveAdmission(hospitalId, admissionId);
        return build(normalize(request));
    }

    ReferralResponse previewWithoutAdmission(ReferralRequest request) {
        return build(normalize(request));
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value, boolean finalizing) {
        validateDocumentShape(value);
        final ReferralRequest request;
        try {
            request = objectMapper.convertValue(value, ReferralRequest.class);
        } catch (IllegalArgumentException exception) {
            throw invalidFormat();
        }
        ReferralResponse response = build(normalize(request));
        if (finalizing && !response.finalizationAllowed()) {
            throw new BusinessValidationException(response.blockingReasons().getFirst());
        }
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", response.request());
        stored.put("specialtyLabel", response.specialtyLabel());
        stored.put("priorityLabel", response.priorityLabel());
        stored.put("deadlineLabel", response.deadlineLabel());
        stored.put("destinationLabel", response.destinationLabel());
        stored.put("attachmentLabels", response.attachmentLabels());
        stored.put("summaryLine", response.summaryLine());
        stored.put("finalizationAllowed", response.finalizationAllowed());
        stored.put("blockingReasons", response.blockingReasons());
        stored.put("safetyNotices", response.safetyNotices());
        return stored;
    }

    private ReferralRequest normalize(ReferralRequest request) {
        String specialtyCode = trim(request.specialtyCode());
        String specialtyDetail = nullable(request.specialtyDetail());
        String priority = trim(request.priority());
        String deadline = trim(request.deadline());
        String justification = trim(request.clinicalJustification());
        String destination = trim(request.destination());
        List<String> attachments = request.attachments() == null
            ? List.of()
            : new ArrayList<>(new LinkedHashSet<>(request.attachments().stream()
                .map(this::trim)
                .filter(item -> !item.isBlank())
                .toList()));

        requireCatalogOption(catalog().specialties(), specialtyCode, "Especialidade inválida");
        requireCatalogOption(catalog().priorities(), priority, "Prioridade inválida");
        requireCatalogOption(catalog().deadlines(), deadline, "Prazo de atendimento inválido");
        requireCatalogOption(catalog().destinations(), destination, "Destino inválido");
        for (String attachment : attachments) {
            requireCatalogOption(catalog().attachments(), attachment, "Documento anexo inválido");
        }
        if ("OTHER".equals(specialtyCode) && specialtyDetail == null) {
            throw new BusinessValidationException("Informe a especialidade ou área de destino");
        }
        if (justification.isBlank()) {
            throw new BusinessValidationException("Informe a justificativa clínica do encaminhamento");
        }
        if (specialtyCode.length() > 80 || (specialtyDetail != null && specialtyDetail.length() > 180) ||
            justification.length() > 5000 || attachments.size() > 4) {
            throw new BusinessValidationException("Um dos campos do encaminhamento excede o limite permitido");
        }
        return new ReferralRequest(
            specialtyCode,
            specialtyDetail,
            priority,
            deadline,
            justification,
            destination,
            List.copyOf(attachments),
            request.physicianAttestation()
        );
    }

    private ReferralResponse build(ReferralRequest request) {
        List<String> blocking = new ArrayList<>();
        if (!request.physicianAttestation()) {
            blocking.add("Confirme a revisão e a responsabilidade médica antes de finalizar");
        }

        String specialty = label(catalog().specialties(), request.specialtyCode());
        if (request.specialtyDetail() != null) {
            specialty = "OTHER".equals(request.specialtyCode())
                ? request.specialtyDetail()
                : specialty + " — " + request.specialtyDetail();
        }
        String priority = label(catalog().priorities(), request.priority());
        String deadline = label(catalog().deadlines(), request.deadline());
        String destination = label(catalog().destinations(), request.destination());
        List<String> attachmentLabels = request.attachments().stream()
            .map(code -> label(catalog().attachments(), code))
            .toList();
        String summary = "GUIA DE ENCAMINHAMENTO — " + specialty +
            " • " + priority + " • " + deadline;
        return new ReferralResponse(
            request,
            specialty,
            priority,
            deadline,
            destination,
            attachmentLabels,
            summary,
            blocking.isEmpty(),
            List.copyOf(blocking),
            List.of(
                "O PRAZO INFORMADO É UMA RECOMENDAÇÃO CLÍNICA E PODE SER REAVALIADO PELA REGULAÇÃO.",
                "A EMISSÃO DESTA GUIA NÃO REGISTRA AUTOMATICAMENTE A SOLICITAÇÃO NO SISREG, CROSS, CONVÊNIO OU OUTRO SISTEMA EXTERNO."
            )
        );
    }

    private void requireActiveAdmission(UUID hospitalId, UUID admissionId) {
        Integer count = tenantJdbc.read(hospitalId, jdbc -> jdbc.queryForObject(
            "SELECT COUNT(*) FROM dbo.admission WHERE id = ? AND status = 'ACTIVE'",
            Integer.class,
            admissionId
        ));
        if (count == null || count == 0) {
            throw new BusinessValidationException("Internação ativa não encontrada");
        }
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?> root)) throw invalidFormat();
        if (root.keySet().stream().anyMatch(key ->
            !(key instanceof String) || !ROOT_KEYS.contains(key)
        )) throw invalidFormat();
        Object attachments = root.get("attachments");
        if (attachments != null && (!(attachments instanceof Collection<?> values) ||
            values.size() > 4 || values.stream().anyMatch(item -> !(item instanceof String)))) {
            throw invalidFormat();
        }
    }

    private void requireCatalogOption(
        List<ReferralCatalog.Option> options,
        String code,
        String message
    ) {
        if (options.stream().noneMatch(option -> option.code().equals(code))) {
            throw new BusinessValidationException(message);
        }
    }

    private String label(List<ReferralCatalog.Option> options, String code) {
        return options.stream()
            .filter(option -> option.code().equals(code))
            .map(ReferralCatalog.Option::label)
            .findFirst()
            .orElseThrow(() -> new BusinessValidationException("Opção de encaminhamento inválida"));
    }

    private ReferralCatalog.Option option(String code, String label) {
        return new ReferralCatalog.Option(code, label);
    }

    private BusinessValidationException invalidFormat() {
        return new BusinessValidationException("Dados do encaminhamento estão em formato inválido");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(String value) {
        String normalized = trim(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
