package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.formtemplates.FormFieldType;
import br.com.inventorymed.formtemplates.FormKind;
import br.com.inventorymed.formtemplates.FormTemplateDefinition;
import br.com.inventorymed.formtemplates.FormTemplateService;
import br.com.inventorymed.tenancy.TenantJdbcExecutor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClinicalDocumentService {

    private static final TypeReference<Map<String, Object>> VALUE_MAP = new TypeReference<>() {};

    private final TenantJdbcExecutor tenantJdbc;
    private final FormTemplateService templateService;
    private final ClinicalAuditWriter auditWriter;
    private final DietPrescriptionService dietPrescriptionService;
    private final NursingCarePrescriptionService nursingCarePrescriptionService;
    private final MonitoringPrescriptionService monitoringPrescriptionService;
    private final VentilatorySupportService ventilatorySupportService;
    private final RehabilitationPrescriptionService rehabilitationPrescriptionService;
    private final IsolationPrecautionService isolationPrecautionService;
    private final TherapeuticSupportService therapeuticSupportService;
    private final CriticalCarePrescriptionService criticalCarePrescriptionService;
    private final MedicationTherapyService medicationTherapyService;
    private final BedsideProcedureService bedsideProcedureService;
    private final AihService aihService;
    private final ObjectMapper objectMapper;

    public ClinicalDocumentService(
        TenantJdbcExecutor tenantJdbc,
        FormTemplateService templateService,
        ClinicalAuditWriter auditWriter,
        DietPrescriptionService dietPrescriptionService,
        NursingCarePrescriptionService nursingCarePrescriptionService,
        MonitoringPrescriptionService monitoringPrescriptionService,
        VentilatorySupportService ventilatorySupportService,
        RehabilitationPrescriptionService rehabilitationPrescriptionService,
        IsolationPrecautionService isolationPrecautionService,
        TherapeuticSupportService therapeuticSupportService,
        CriticalCarePrescriptionService criticalCarePrescriptionService,
        MedicationTherapyService medicationTherapyService,
        BedsideProcedureService bedsideProcedureService,
        AihService aihService,
        ObjectMapper objectMapper
    ) {
        this.tenantJdbc = tenantJdbc;
        this.templateService = templateService;
        this.auditWriter = auditWriter;
        this.dietPrescriptionService = dietPrescriptionService;
        this.nursingCarePrescriptionService = nursingCarePrescriptionService;
        this.monitoringPrescriptionService = monitoringPrescriptionService;
        this.ventilatorySupportService = ventilatorySupportService;
        this.rehabilitationPrescriptionService = rehabilitationPrescriptionService;
        this.isolationPrecautionService = isolationPrecautionService;
        this.therapeuticSupportService = therapeuticSupportService;
        this.criticalCarePrescriptionService = criticalCarePrescriptionService;
        this.medicationTherapyService = medicationTherapyService;
        this.bedsideProcedureService = bedsideProcedureService;
        this.aihService = aihService;
        this.objectMapper = objectMapper;
    }

    public List<FormTemplateDefinition> publishedTemplates(UUID hospitalId, FormKind kind) {
        return templateService.published(hospitalId, kind);
    }

    public List<ClinicalDocumentResponse> documents(UUID hospitalId, UUID admissionId) {
        return tenantJdbc.read(hospitalId, jdbc -> jdbc.query(
            "SELECT id, admission_id, kind, template_version_id, status, version_number, " +
            "author_user_id, content_json, created_at, finalized_at FROM dbo.clinical_document " +
            "WHERE admission_id = ? ORDER BY created_at DESC",
            (row, number) -> new ClinicalDocumentResponse(
                row.getObject("id", UUID.class),
                row.getObject("admission_id", UUID.class),
                FormKind.valueOf(row.getString("kind")),
                row.getObject("template_version_id", UUID.class),
                row.getString("status"),
                row.getInt("version_number"),
                row.getObject("author_user_id", UUID.class),
                readValues(row.getString("content_json")),
                row.getObject("created_at", OffsetDateTime.class).toInstant(),
                row.getObject("finalized_at", OffsetDateTime.class) == null
                    ? null
                    : row.getObject("finalized_at", OffsetDateTime.class).toInstant()
            ),
            admissionId
        ));
    }

    public ClinicalDocumentResponse create(
        UUID hospitalId,
        UUID actorId,
        UUID admissionId,
        ClinicalRequests.SaveDocument request,
        String sourceIp
    ) {
        FormTemplateDefinition template = requiredPublishedTemplate(
            hospitalId,
            request.templateVersionId(),
            request.kind()
        );
        Map<String, Object> normalizedValues = validateValues(template, request.values());
        UUID documentId = UUID.randomUUID();
        tenantJdbc.write(hospitalId, jdbc -> {
            Integer activeAdmission = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.admission WHERE id = ? AND status = 'ACTIVE'",
                Integer.class,
                admissionId
            );
            if (activeAdmission == null || activeAdmission == 0) {
                throw new BusinessValidationException("Internação ativa não encontrada");
            }
            jdbc.update(
                "INSERT INTO dbo.clinical_document " +
                "(id, admission_id, kind, template_version_id, status, version_number, " +
                "author_user_id, content_json, template_snapshot_json, finalized_at) " +
                "VALUES (?, ?, ?, ?, ?, 1, ?, ?, ?, " +
                (request.finalizeDocument() ? "SYSUTCDATETIME()" : "NULL") + ")",
                documentId,
                admissionId,
                request.kind().name(),
                request.templateVersionId(),
                request.finalizeDocument() ? "FINALIZED" : "DRAFT",
                actorId,
                json(normalizedValues),
                json(template)
            );
            auditWriter.success(
                jdbc,
                actorId,
                request.finalizeDocument() ? "CLINICAL_DOCUMENT_FINALIZED" : "CLINICAL_DOCUMENT_DRAFT_CREATED",
                "CLINICAL_DOCUMENT",
                documentId,
                sourceIp,
                Map.of("admissionId", admissionId, "kind", request.kind().name())
            );
            return null;
        });
        return requiredDocument(hospitalId, documentId);
    }

    public ClinicalDocumentResponse updateDraft(
        UUID hospitalId,
        UUID actorId,
        UUID documentId,
        ClinicalRequests.SaveDocument request,
        String sourceIp
    ) {
        FormTemplateDefinition template = requiredPublishedTemplate(
            hospitalId,
            request.templateVersionId(),
            request.kind()
        );
        Map<String, Object> normalizedValues = validateValues(template, request.values());
        tenantJdbc.write(hospitalId, jdbc -> {
            int updated = jdbc.update(
                "UPDATE dbo.clinical_document SET content_json = ?, template_snapshot_json = ?, " +
                "status = ?, finalized_at = " +
                (request.finalizeDocument() ? "SYSUTCDATETIME()" : "NULL") +
                ", row_version = row_version + 1 WHERE id = ? AND status = 'DRAFT' " +
                "AND author_user_id = ? AND template_version_id = ? AND kind = ?",
                json(normalizedValues),
                json(template),
                request.finalizeDocument() ? "FINALIZED" : "DRAFT",
                documentId,
                actorId,
                request.templateVersionId(),
                request.kind().name()
            );
            if (updated == 0) {
                throw new BusinessValidationException(
                    "Somente o autor pode alterar um documento ainda em rascunho"
                );
            }
            auditWriter.success(
                jdbc,
                actorId,
                request.finalizeDocument() ? "CLINICAL_DOCUMENT_FINALIZED" : "CLINICAL_DOCUMENT_DRAFT_UPDATED",
                "CLINICAL_DOCUMENT",
                documentId,
                sourceIp,
                Map.of("kind", request.kind().name())
            );
            return null;
        });
        return requiredDocument(hospitalId, documentId);
    }

    private FormTemplateDefinition requiredPublishedTemplate(
        UUID hospitalId,
        UUID versionId,
        FormKind kind
    ) {
        return templateService
            .published(hospitalId, kind)
            .stream()
            .filter(template -> template.versionId().equals(versionId))
            .findFirst()
            .orElseThrow(() ->
                new BusinessValidationException("Modelo publicado e ativo não encontrado")
            );
    }

    private Map<String, Object> validateValues(
        FormTemplateDefinition template,
        Map<String, Object> values
    ) {
        Map<String, FormTemplateDefinition.Field> allowed = new LinkedHashMap<>();
        for (FormTemplateDefinition.Section section : template.sections()) {
            if (!section.active()) continue;
            for (FormTemplateDefinition.Field field : section.fields()) {
                if (field.active()) allowed.put(section.key() + "." + field.key(), field);
            }
        }
        Set<String> unknown = new HashSet<>(values.keySet());
        unknown.removeAll(allowed.keySet());
        if (!unknown.isEmpty()) {
            throw new BusinessValidationException("A requisição contém campos não permitidos");
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        allowed.forEach((key, field) -> {
            Object value = values.get(key);
            if (field.required() && empty(value)) {
                throw new BusinessValidationException("Preencha o campo obrigatório: " + field.label());
            }
            if (!empty(value)) {
                normalized.put(key, normalizedValue(field, value));
            }
        });
        requireContinuousInfusionRates(normalized, "SEDACAO.MEDICAMENTOS", false);
        requireContinuousInfusionRates(normalized, "CARDIOVASCULAR.DROGAS_VASOATIVAS", true);
        return normalized;
    }

    private Object normalizedValue(FormTemplateDefinition.Field field, Object value) {
        if (field.type() == FormFieldType.DIET_PLAN) {
            return dietPrescriptionService.normalizeForClinicalDocument(value);
        }
        if (field.type() == FormFieldType.NURSING_CARE_PLAN) {
            return nursingCarePrescriptionService.normalizeForClinicalDocument(value);
        }
        if (field.type() == FormFieldType.MONITORING_PLAN) {
            return monitoringPrescriptionService.normalizeForClinicalDocument(value);
        }
        if (field.type() == FormFieldType.VENTILATORY_SUPPORT_PLAN) {
            return ventilatorySupportService.normalizeForClinicalDocument(value);
        }
        if (field.type() == FormFieldType.REHABILITATION_PLAN) {
            return rehabilitationPrescriptionService.normalizeForClinicalDocument(value);
        }
        if (field.type() == FormFieldType.ISOLATION_PRECAUTIONS_PLAN) {
            return isolationPrecautionService.normalizeForClinicalDocument(value);
        }
        if (field.type() == FormFieldType.THERAPEUTIC_SUPPORT_PLAN) {
            return therapeuticSupportService.normalizeForClinicalDocument(value);
        }
        if (field.type() == FormFieldType.CRITICAL_CARE_PLAN) {
            return criticalCarePrescriptionService.normalizeForClinicalDocument(value);
        }
        if (field.type() == FormFieldType.MEDICATION_THERAPY_PLAN) {
            return medicationTherapyService.normalizeForClinicalDocument(value);
        }
        if (field.type() == FormFieldType.PROCEDURE_PLAN) {
            return bedsideProcedureService.normalizeForClinicalDocument(value);
        }
        if (field.type() == FormFieldType.AIH_PLAN) {
            return aihService.normalizeForClinicalDocument(value);
        }
        validateType(field, value);
        return value;
    }

    private void requireContinuousInfusionRates(
        Map<String, Object> values,
        String fieldPath,
        boolean allowsNoMedicationStatement
    ) {
        Object value = values.get(fieldPath);
        if (!(value instanceof List<?> rows)) return;

        for (Object rowValue : rows) {
            if (!(rowValue instanceof Map<?, ?> row)) continue;
            String description = row.get("description") instanceof String text ? text.trim() : "";
            if (allowsNoMedicationStatement && description.equals("SEM USO DE DROGAS VASOATIVAS")) {
                continue;
            }
            String frequency = row.get("frequency") instanceof String text ? text.trim() : "";
            if (description.isBlank() || frequency.isBlank()) {
                throw new BusinessValidationException(
                    "Informe o medicamento e a vazão em ML/H para cada infusão selecionada"
                );
            }
        }
    }

    private void validateType(FormTemplateDefinition.Field field, Object value) {
        FormFieldType type = field.type();
        if (type == FormFieldType.INTEGER || type == FormFieldType.DECIMAL) {
            if (!(value instanceof Number)) invalid(field);
            return;
        }
        if (type == FormFieldType.BOOLEAN) {
            if (!(value instanceof Boolean)) invalid(field);
            return;
        }
        if (type == FormFieldType.MULTI_SELECT) {
            if (!(value instanceof List<?> selected)) invalid(field);
            @SuppressWarnings("unchecked")
            List<?> selectedValues = (List<?>) value;
            Set<String> allowedOptions = activeOptions(field);
            if (selectedValues.stream().anyMatch(item ->
                !(item instanceof String text) || !allowedOptions.contains(text)
            )) invalid(field);
            return;
        }
        if (type == FormFieldType.SINGLE_SELECT) {
            if (!(value instanceof String selected) || !activeOptions(field).contains(selected)) {
                invalid(field);
            }
            return;
        }
        if (type == FormFieldType.MEDICATION_LINE) {
            validateStructuredRows(field, value, 100, Set.of(
                "description", "route", "frequency", "scheduling"
            ));
            return;
        }
        if (type == FormFieldType.CLINICAL_TABLE) {
            validateStructuredRows(field, value, 200, null);
            return;
        }
        if (!(value instanceof String text)) invalid(field);
        if (value instanceof String text && field.maxLength() != null && text.length() > field.maxLength()) {
            throw new BusinessValidationException("O campo excede o limite permitido: " + field.label());
        }
    }

    private Set<String> activeOptions(FormTemplateDefinition.Field field) {
        Set<String> options = new HashSet<>();
        field.options().stream().filter(FormTemplateDefinition.Option::active).forEach(option ->
            options.add(option.value())
        );
        return options;
    }

    private void validateStructuredRows(
        FormTemplateDefinition.Field field,
        Object value,
        int maximumRows,
        Set<String> allowedKeys
    ) {
        if (!(value instanceof List<?> rows) || rows.size() > maximumRows) invalid(field);
        @SuppressWarnings("unchecked")
        List<?> rows = (List<?>) value;
        for (Object rowValue : rows) {
            if (!(rowValue instanceof Map<?, ?> row) || row.size() > 30) invalid(field);
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) rowValue).entrySet()) {
                if (!(entry.getKey() instanceof String key)) invalid(field);
                if (allowedKeys != null && !allowedKeys.contains(entry.getKey())) invalid(field);
                Object cell = entry.getValue();
                if (
                    cell != null &&
                    !(cell instanceof String) &&
                    !(cell instanceof Number) &&
                    !(cell instanceof Boolean)
                ) invalid(field);
                if (cell instanceof String text && text.length() > 2000) invalid(field);
            }
        }
    }

    private boolean empty(Object value) {
        return value == null ||
            (value instanceof String text && text.isBlank()) ||
            (value instanceof List<?> list && list.isEmpty()) ||
            (value instanceof Map<?, ?> map && map.isEmpty());
    }

    private void invalid(FormTemplateDefinition.Field field) {
        throw new BusinessValidationException("Valor inválido para o campo: " + field.label());
    }

    private ClinicalDocumentResponse requiredDocument(UUID hospitalId, UUID documentId) {
        return tenantJdbc.read(hospitalId, jdbc -> jdbc.query(
            "SELECT id, admission_id, kind, template_version_id, status, version_number, " +
            "author_user_id, content_json, created_at, finalized_at FROM dbo.clinical_document WHERE id = ?",
            (row, number) -> new ClinicalDocumentResponse(
                row.getObject("id", UUID.class),
                row.getObject("admission_id", UUID.class),
                FormKind.valueOf(row.getString("kind")),
                row.getObject("template_version_id", UUID.class),
                row.getString("status"),
                row.getInt("version_number"),
                row.getObject("author_user_id", UUID.class),
                readValues(row.getString("content_json")),
                row.getObject("created_at", OffsetDateTime.class).toInstant(),
                row.getObject("finalized_at", OffsetDateTime.class) == null
                    ? null
                    : row.getObject("finalized_at", OffsetDateTime.class).toInstant()
            ),
            documentId
        ).stream().findFirst().orElseThrow(() ->
            new BusinessValidationException("Documento clínico não encontrado")
        ));
    }

    private String json(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            if (json.length() > 100_000) {
                throw new BusinessValidationException("O documento excede o tamanho permitido");
            }
            return json;
        } catch (JacksonException exception) {
            throw new IllegalStateException("Falha ao serializar documento clínico", exception);
        }
    }

    private Map<String, Object> readValues(String json) {
        try {
            return objectMapper.readValue(json, VALUE_MAP);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Documento clínico armazenado é inválido", exception);
        }
    }
}
