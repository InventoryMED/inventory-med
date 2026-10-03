package br.com.inventorymed.formtemplates;

import br.com.inventorymed.audit.AuditOutcome;
import br.com.inventorymed.audit.AuditService;
import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.identity.AppUser;
import br.com.inventorymed.identity.AppUserRepository;
import br.com.inventorymed.identity.Hospital;
import br.com.inventorymed.identity.HospitalRepository;
import br.com.inventorymed.security.InventoryUserPrincipal;
import br.com.inventorymed.tenancy.TenantJdbcExecutor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

@Service
public class FormTemplateService {

    private final TenantJdbcExecutor tenantJdbc;
    private final HospitalRepository hospitalRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    public FormTemplateService(
        TenantJdbcExecutor tenantJdbc,
        HospitalRepository hospitalRepository,
        AppUserRepository userRepository,
        AuditService auditService
    ) {
        this.tenantJdbc = tenantJdbc;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public List<FormTemplateDefinition> list(UUID hospitalId) {
        requiredHospital(hospitalId);
        return tenantJdbc.read(hospitalId, jdbc -> listDefinitions(jdbc, null, false));
    }

    public List<FormTemplateDefinition> published(UUID hospitalId, FormKind kind) {
        requiredHospital(hospitalId);
        return tenantJdbc.read(hospitalId, jdbc -> listDefinitions(jdbc, kind, true));
    }

    public FormTemplateDefinition create(
        UUID hospitalId,
        FormTemplateRequests.Create request,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        UUID templateId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        tenantJdbc.write(hospitalId, jdbc -> {
            Integer duplicate = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.form_template WHERE kind = ? AND UPPER(name) = ?",
                Integer.class,
                request.kind().name(),
                normalizeText(request.name())
            );
            if (duplicate != null && duplicate > 0) {
                throw new BusinessValidationException("Já existe um modelo com este nome e tipo");
            }
            jdbc.update(
                "INSERT INTO dbo.form_template (id, kind, name, created_by) VALUES (?, ?, ?, ?)",
                templateId,
                request.kind().name(),
                normalizeText(request.name()),
                principal.userId()
            );
            jdbc.update(
                "INSERT INTO dbo.form_template_version " +
                "(id, template_id, version_number, status, created_by) VALUES (?, ?, 1, 'DRAFT', ?)",
                versionId,
                templateId,
                principal.userId()
            );
            return null;
        });
        audit("ADMIN_FORM_TEMPLATE_CREATED", hospital, principal, sourceIp, userAgent, templateId);
        return requiredDefinition(hospitalId, templateId);
    }

    public FormTemplateDefinition saveDraft(
        UUID hospitalId,
        UUID templateId,
        FormTemplateRequests.SaveDraft request,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        validateDefinition(request);
        Hospital hospital = requiredHospital(hospitalId);
        tenantJdbc.write(hospitalId, jdbc -> {
            UUID versionId = requiredDraftVersion(jdbc, templateId);
            jdbc.update(
                "UPDATE dbo.form_template SET name = ?, updated_at = SYSUTCDATETIME(), " +
                "row_version = row_version + 1 WHERE id = ?",
                normalizeText(request.name()),
                templateId
            );
            replaceSections(jdbc, versionId, request.sections());
            return null;
        });
        audit("ADMIN_FORM_TEMPLATE_DRAFT_SAVED", hospital, principal, sourceIp, userAgent, templateId);
        return requiredDefinition(hospitalId, templateId);
    }

    public FormTemplateDefinition publish(
        UUID hospitalId,
        UUID templateId,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        tenantJdbc.write(hospitalId, jdbc -> {
            UUID draftId = requiredDraftVersion(jdbc, templateId);
            Integer activeFields = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.form_field f " +
                "JOIN dbo.form_section s ON s.id = f.section_id " +
                "WHERE s.version_id = ? AND s.active = 1 AND f.active = 1",
                Integer.class,
                draftId
            );
            if (activeFields == null || activeFields == 0) {
                throw new BusinessValidationException("O modelo precisa ter ao menos um campo ativo");
            }
            jdbc.update(
                "UPDATE dbo.form_template_version SET status = 'RETIRED' " +
                "WHERE template_id = ? AND status = 'PUBLISHED'",
                templateId
            );
            jdbc.update(
                "UPDATE dbo.form_template_version SET status = 'PUBLISHED', published_by = ?, " +
                "published_at = SYSUTCDATETIME() WHERE id = ?",
                principal.userId(),
                draftId
            );
            return null;
        });
        audit("ADMIN_FORM_TEMPLATE_PUBLISHED", hospital, principal, sourceIp, userAgent, templateId);
        return requiredDefinition(hospitalId, templateId);
    }

    public FormTemplateDefinition newDraft(
        UUID hospitalId,
        UUID templateId,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        tenantJdbc.write(hospitalId, jdbc -> {
            Integer existingDraft = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.form_template_version WHERE template_id = ? AND status = 'DRAFT'",
                Integer.class,
                templateId
            );
            if (existingDraft != null && existingDraft > 0) {
                throw new BusinessValidationException("Este modelo já possui um rascunho em edição");
            }
            FormTemplateDefinition published = definitionByStatus(jdbc, templateId, "PUBLISHED");
            int nextVersion = published.versionNumber() + 1;
            UUID versionId = UUID.randomUUID();
            jdbc.update(
                "INSERT INTO dbo.form_template_version " +
                "(id, template_id, version_number, status, created_by) VALUES (?, ?, ?, 'DRAFT', ?)",
                versionId,
                templateId,
                nextVersion,
                principal.userId()
            );
            List<FormTemplateRequests.Section> sections = published.sections().stream().map(section ->
                new FormTemplateRequests.Section(
                    section.key(),
                    section.title(),
                    section.displayOrder(),
                    section.active(),
                    section.fields().stream().map(field ->
                        new FormTemplateRequests.Field(
                            field.key(),
                            field.label(),
                            field.type(),
                            field.required(),
                            field.displayOrder(),
                            field.active(),
                            field.placeholder(),
                            field.maxLength(),
                            field.options().stream().map(option ->
                                new FormTemplateRequests.Option(
                                    option.value(),
                                    option.label(),
                                    option.displayOrder(),
                                    option.active()
                                )
                            ).toList()
                        )
                    ).toList()
                )
            ).toList();
            replaceSections(jdbc, versionId, sections);
            return null;
        });
        audit("ADMIN_FORM_TEMPLATE_NEW_VERSION", hospital, principal, sourceIp, userAgent, templateId);
        return requiredDefinition(hospitalId, templateId);
    }

    public FormTemplateDefinition setActive(
        UUID hospitalId,
        UUID templateId,
        boolean active,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent
    ) {
        Hospital hospital = requiredHospital(hospitalId);
        tenantJdbc.write(hospitalId, jdbc -> {
            int updated = jdbc.update(
                "UPDATE dbo.form_template SET active = ?, updated_at = SYSUTCDATETIME(), " +
                "row_version = row_version + 1 WHERE id = ?",
                active,
                templateId
            );
            if (updated == 0) throw new BusinessValidationException("Modelo não encontrado");
            return null;
        });
        audit("ADMIN_FORM_TEMPLATE_STATUS_CHANGED", hospital, principal, sourceIp, userAgent, templateId);
        return requiredDefinition(hospitalId, templateId);
    }

    private List<FormTemplateDefinition> listDefinitions(
        JdbcTemplate jdbc,
        FormKind kind,
        boolean publishedOnly
    ) {
        String sql = "SELECT t.id FROM dbo.form_template t WHERE 1 = 1" +
            (kind == null ? "" : " AND t.kind = ?") +
            (publishedOnly ? " AND t.active = 1 AND EXISTS (SELECT 1 FROM dbo.form_template_version v WHERE v.template_id = t.id AND v.status = 'PUBLISHED')" : "") +
            " ORDER BY t.kind, t.name";
        List<UUID> ids = kind == null
            ? jdbc.query(sql, (result, row) -> result.getObject("id", UUID.class))
            : jdbc.query(sql, (result, row) -> result.getObject("id", UUID.class), kind.name());
        return ids.stream().map(id ->
            publishedOnly
                ? definitionByStatus(jdbc, id, "PUBLISHED")
                : preferredDefinition(jdbc, id)
        ).toList();
    }

    private FormTemplateDefinition requiredDefinition(UUID hospitalId, UUID templateId) {
        return tenantJdbc.read(hospitalId, jdbc -> preferredDefinition(jdbc, templateId));
    }

    private FormTemplateDefinition preferredDefinition(JdbcTemplate jdbc, UUID templateId) {
        List<String> statuses = jdbc.query(
            "SELECT status FROM dbo.form_template_version WHERE template_id = ? " +
            "ORDER BY CASE status WHEN 'DRAFT' THEN 0 WHEN 'PUBLISHED' THEN 1 ELSE 2 END, version_number DESC",
            (result, row) -> result.getString("status"),
            templateId
        );
        if (statuses.isEmpty()) throw new BusinessValidationException("Modelo não encontrado");
        return definitionByStatus(jdbc, templateId, statuses.getFirst());
    }

    private FormTemplateDefinition definitionByStatus(
        JdbcTemplate jdbc,
        UUID templateId,
        String status
    ) {
        List<FormTemplateDefinition> headers = jdbc.query(
            "SELECT TOP 1 t.id, t.name, t.kind, t.active, v.id AS version_id, " +
            "v.version_number, v.status FROM dbo.form_template t " +
            "JOIN dbo.form_template_version v ON v.template_id = t.id " +
            "WHERE t.id = ? AND v.status = ? ORDER BY v.version_number DESC",
            (result, row) -> new FormTemplateDefinition(
                result.getObject("id", UUID.class),
                result.getString("name"),
                FormKind.valueOf(result.getString("kind")),
                result.getBoolean("active"),
                result.getObject("version_id", UUID.class),
                result.getInt("version_number"),
                result.getString("status"),
                List.of()
            ),
            templateId,
            status
        );
        if (headers.isEmpty()) throw new BusinessValidationException("Versão do modelo não encontrada");
        FormTemplateDefinition header = headers.getFirst();
        return new FormTemplateDefinition(
            header.templateId(),
            header.name(),
            header.kind(),
            header.active(),
            header.versionId(),
            header.versionNumber(),
            header.status(),
            loadSections(jdbc, header.versionId())
        );
    }

    private List<FormTemplateDefinition.Section> loadSections(JdbcTemplate jdbc, UUID versionId) {
        Map<UUID, MutableSection> sections = new LinkedHashMap<>();
        Map<UUID, MutableField> fields = new LinkedHashMap<>();
        jdbc.query(
            "SELECT id, section_key, title, display_order, active FROM dbo.form_section " +
            "WHERE version_id = ? ORDER BY display_order, title",
            (RowCallbackHandler) result -> sections.put(
                result.getObject("id", UUID.class),
                new MutableSection(
                    result.getObject("id", UUID.class),
                    result.getString("section_key"),
                    result.getString("title"),
                    result.getInt("display_order"),
                    result.getBoolean("active")
                )
            ),
            versionId
        );
        jdbc.query(
            "SELECT f.id, f.section_id, f.field_key, f.label, f.field_type, f.required, " +
            "f.display_order, f.active, f.placeholder, f.max_length FROM dbo.form_field f " +
            "JOIN dbo.form_section s ON s.id = f.section_id WHERE s.version_id = ? " +
            "ORDER BY f.display_order, f.label",
            (RowCallbackHandler) result -> {
                MutableField field = new MutableField(
                    result.getObject("id", UUID.class),
                    result.getString("field_key"),
                    result.getString("label"),
                    FormFieldType.valueOf(result.getString("field_type")),
                    result.getBoolean("required"),
                    result.getInt("display_order"),
                    result.getBoolean("active"),
                    result.getString("placeholder"),
                    result.getObject("max_length", Integer.class)
                );
                fields.put(field.id, field);
                MutableSection section = sections.get(result.getObject("section_id", UUID.class));
                if (section != null) section.fields.add(field);
            },
            versionId
        );
        jdbc.query(
            "SELECT o.id, o.field_id, o.option_value, o.option_label, o.display_order, o.active " +
            "FROM dbo.form_field_option o JOIN dbo.form_field f ON f.id = o.field_id " +
            "JOIN dbo.form_section s ON s.id = f.section_id WHERE s.version_id = ? " +
            "ORDER BY o.display_order, o.option_label",
            (RowCallbackHandler) result -> {
                MutableField field = fields.get(result.getObject("field_id", UUID.class));
                if (field != null) field.options.add(
                    new FormTemplateDefinition.Option(
                        result.getObject("id", UUID.class),
                        result.getString("option_value"),
                        result.getString("option_label"),
                        result.getInt("display_order"),
                        result.getBoolean("active")
                    )
                );
            },
            versionId
        );
        return sections.values().stream().map(MutableSection::response).toList();
    }

    private void replaceSections(
        JdbcTemplate jdbc,
        UUID versionId,
        List<FormTemplateRequests.Section> sections
    ) {
        jdbc.update(
            "DELETE o FROM dbo.form_field_option o JOIN dbo.form_field f ON f.id = o.field_id " +
            "JOIN dbo.form_section s ON s.id = f.section_id WHERE s.version_id = ?",
            versionId
        );
        jdbc.update(
            "DELETE f FROM dbo.form_field f JOIN dbo.form_section s ON s.id = f.section_id " +
            "WHERE s.version_id = ?",
            versionId
        );
        jdbc.update("DELETE FROM dbo.form_section WHERE version_id = ?", versionId);

        for (FormTemplateRequests.Section section : sections) {
            UUID sectionId = UUID.randomUUID();
            jdbc.update(
                "INSERT INTO dbo.form_section " +
                "(id, version_id, section_key, title, display_order, active) VALUES (?, ?, ?, ?, ?, ?)",
                sectionId,
                versionId,
                normalizeKey(section.key()),
                normalizeText(section.title()),
                section.displayOrder(),
                section.active()
            );
            for (FormTemplateRequests.Field field : section.fields()) {
                UUID fieldId = UUID.randomUUID();
                jdbc.update(
                    "INSERT INTO dbo.form_field " +
                    "(id, section_id, field_key, label, field_type, required, display_order, active, placeholder, max_length) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    fieldId,
                    sectionId,
                    normalizeKey(field.key()),
                    normalizeText(field.label()),
                    field.type().name(),
                    field.required(),
                    field.displayOrder(),
                    field.active(),
                    normalizeNullable(field.placeholder()),
                    field.maxLength()
                );
                if (field.options() != null) {
                    for (FormTemplateRequests.Option option : field.options()) {
                        jdbc.update(
                            "INSERT INTO dbo.form_field_option " +
                            "(id, field_id, option_value, option_label, display_order, active) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                            UUID.randomUUID(),
                            fieldId,
                            option.value().trim(),
                            normalizeText(option.label()),
                            option.displayOrder(),
                            option.active()
                        );
                    }
                }
            }
        }
    }

    private UUID requiredDraftVersion(JdbcTemplate jdbc, UUID templateId) {
        List<UUID> versions = jdbc.query(
            "SELECT id FROM dbo.form_template_version WHERE template_id = ? AND status = 'DRAFT'",
            (result, row) -> result.getObject("id", UUID.class),
            templateId
        );
        if (versions.isEmpty()) {
            throw new BusinessValidationException("Crie uma nova versão antes de editar este modelo");
        }
        return versions.getFirst();
    }

    private void validateDefinition(FormTemplateRequests.SaveDraft request) {
        Set<String> sectionKeys = new HashSet<>();
        for (FormTemplateRequests.Section section : request.sections()) {
            if (!sectionKeys.add(normalizeKey(section.key()))) {
                throw new BusinessValidationException("As chaves das seções não podem se repetir");
            }
            Set<String> fieldKeys = new HashSet<>();
            for (FormTemplateRequests.Field field : section.fields()) {
                if (!fieldKeys.add(normalizeKey(field.key()))) {
                    throw new BusinessValidationException("As chaves dos campos não podem se repetir na mesma seção");
                }
                boolean selection = field.type() == FormFieldType.SINGLE_SELECT ||
                    field.type() == FormFieldType.MULTI_SELECT;
                if (selection && (field.options() == null || field.options().isEmpty())) {
                    throw new BusinessValidationException("Campos de seleção precisam ter opções");
                }
                if (!selection && field.options() != null && !field.options().isEmpty()) {
                    throw new BusinessValidationException("Somente campos de seleção podem ter opções");
                }
            }
        }
    }

    private Hospital requiredHospital(UUID hospitalId) {
        return hospitalRepository
            .findById(hospitalId)
            .filter(Hospital::isActive)
            .orElseThrow(() -> new BusinessValidationException("Hospital inexistente ou inativo"));
    }

    private void audit(
        String eventType,
        Hospital hospital,
        InventoryUserPrincipal principal,
        String sourceIp,
        String userAgent,
        UUID templateId
    ) {
        AppUser actor = userRepository
            .findById(principal.userId())
            .orElseThrow(() -> new BusinessValidationException("Administrador não encontrado"));
        auditService.record(
            eventType,
            AuditOutcome.SUCCESS,
            actor,
            hospital,
            sourceIp,
            userAgent,
            Map.of("templateId", templateId)
        );
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.forLanguageTag("pt-BR"));
    }

    private String normalizeKey(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static final class MutableSection {
        private final UUID id;
        private final String key;
        private final String title;
        private final int displayOrder;
        private final boolean active;
        private final List<MutableField> fields = new ArrayList<>();

        private MutableSection(UUID id, String key, String title, int displayOrder, boolean active) {
            this.id = id;
            this.key = key;
            this.title = title;
            this.displayOrder = displayOrder;
            this.active = active;
        }

        private FormTemplateDefinition.Section response() {
            return new FormTemplateDefinition.Section(
                id,
                key,
                title,
                displayOrder,
                active,
                fields.stream().map(MutableField::response).toList()
            );
        }
    }

    private static final class MutableField {
        private final UUID id;
        private final String key;
        private final String label;
        private final FormFieldType type;
        private final boolean required;
        private final int displayOrder;
        private final boolean active;
        private final String placeholder;
        private final Integer maxLength;
        private final List<FormTemplateDefinition.Option> options = new ArrayList<>();

        private MutableField(
            UUID id,
            String key,
            String label,
            FormFieldType type,
            boolean required,
            int displayOrder,
            boolean active,
            String placeholder,
            Integer maxLength
        ) {
            this.id = id;
            this.key = key;
            this.label = label;
            this.type = type;
            this.required = required;
            this.displayOrder = displayOrder;
            this.active = active;
            this.placeholder = placeholder;
            this.maxLength = maxLength;
        }

        private FormTemplateDefinition.Field response() {
            return new FormTemplateDefinition.Field(
                id,
                key,
                label,
                type,
                required,
                displayOrder,
                active,
                placeholder,
                maxLength,
                List.copyOf(options)
            );
        }
    }
}
