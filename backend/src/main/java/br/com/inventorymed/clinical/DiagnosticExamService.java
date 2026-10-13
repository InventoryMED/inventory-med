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
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class DiagnosticExamService {

    private static final TypeReference<Map<String, Object>> VALUE_MAP = new TypeReference<>() {};
    private static final Set<String> ROOT_KEYS = Set.of(
        "clinicalContext", "bloodExams", "urineExams", "laboratoryClinicalJustification",
        "tomography", "xray", "ultrasound", "electrocardiogram"
    );
    private static final Set<String> TOMOGRAPHY_KEYS = Set.of(
        "region", "contrast", "clinicalJustification", "previousCreatinineMgDl"
    );
    private static final Set<String> XRAY_KEYS = Set.of(
        "region", "laterality", "incidences", "clinicalJustification"
    );
    private static final Set<String> ULTRASOUND_KEYS = Set.of(
        "type", "laterality", "clinicalJustification"
    );
    private static final Set<String> ECG_KEYS = Set.of("type", "priority", "clinicalJustification");
    private static final String REASSESSMENT = "REAVALIAÇÃO CLÍNICA AGUDA PÓS-CONDUTA";

    private final DiagnosticExamCatalogService catalog;
    private final DiagnosticExamValidator validator;
    private final TenantJdbcExecutor tenantJdbc;
    private final ObjectMapper objectMapper;

    public DiagnosticExamService(
        DiagnosticExamCatalogService catalog,
        DiagnosticExamValidator validator,
        TenantJdbcExecutor tenantJdbc,
        ObjectMapper objectMapper
    ) {
        this.catalog = catalog;
        this.validator = validator;
        this.tenantJdbc = tenantJdbc;
        this.objectMapper = objectMapper;
    }

    public DiagnosticExamCatalog catalog() {
        return catalog.catalog();
    }

    public DiagnosticExamResponse preview(
        UUID hospitalId,
        UUID admissionId,
        DiagnosticExamRequest request
    ) {
        requireActiveAdmission(hospitalId, admissionId);
        DiagnosticExamRequest normalized = normalizeRequest(request);
        return build(normalized, recentExamKeys(hospitalId, admissionId));
    }

    public Map<String, Object> normalizeForClinicalDocument(
        UUID hospitalId,
        UUID admissionId,
        Object value
    ) {
        validateDocumentShape(value);
        final DiagnosticExamRequest request;
        try {
            request = objectMapper.convertValue(value, DiagnosticExamRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados da solicitação de exames estão em formato inválido");
        }
        DiagnosticExamRequest normalized = normalizeRequest(request);
        DiagnosticExamResponse response = build(normalized, recentExamKeys(hospitalId, admissionId));
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", normalized);
        stored.put("structuredExams", response.structuredExams());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private DiagnosticExamRequest normalizeRequest(DiagnosticExamRequest request) {
        validator.validate(request);
        LinkedHashSet<String> urine = new LinkedHashSet<>(safe(request.urineExams()));
        if (urine.contains("URINE_CULTURE")) urine.add("EAS");
        return new DiagnosticExamRequest(
            request.clinicalContext(),
            List.copyOf(safe(request.bloodExams())),
            List.copyOf(urine),
            trim(request.laboratoryClinicalJustification()),
            request.tomography(),
            request.xray(),
            request.ultrasound(),
            request.electrocardiogram()
        );
    }

    private DiagnosticExamResponse build(DiagnosticExamRequest request, Set<String> recent) {
        List<String> details = new ArrayList<>();
        List<String> summary = new ArrayList<>();
        List<String> supplies = new ArrayList<>();
        List<String> alerts = new ArrayList<>();
        List<String> duplicates = new ArrayList<>();
        String context = catalog.optionLabel(catalog.catalog().clinicalContexts(), request.clinicalContext());
        details.add("CONTEXTO CLÍNICO: " + context + ".");

        appendLaboratory(request, recent, details, summary, supplies, alerts, duplicates);
        appendTomography(request.tomography(), recent, details, summary, supplies, alerts, duplicates);
        appendXray(request.xray(), recent, details, summary, alerts, duplicates);
        appendUltrasound(request.ultrasound(), recent, details, summary, alerts, duplicates);
        appendEcg(request.electrocardiogram(), recent, details, summary, supplies, alerts, duplicates);

        alerts.add("CÓDIGOS TUSS E SIGTAP SÃO REFERÊNCIAS DE CATÁLOGO E DEVEM SER VALIDADOS NA COMPETÊNCIA VIGENTE.");
        alerts.add("INSUMOS SÃO SUGESTÕES PARA CONFERÊNCIA; NENHUMA COBRANÇA É LANÇADA AUTOMATICAMENTE.");
        alerts.add("A INDICAÇÃO CLÍNICA INFORMADA DEVE SER CONFERIDA E ASSINADA PELO PROFISSIONAL SOLICITANTE.");

        return new DiagnosticExamResponse(
            new DiagnosticExamResponse.StructuredExams(
                "EXAMES SOLICITADOS: " + String.join(" + ", summary) + ".",
                "SOLICITAÇÃO DE EXAMES DIAGNÓSTICOS:\n" + String.join("\n", details),
                List.copyOf(new LinkedHashSet<>(duplicates))
            ),
            new DiagnosticExamResponse.BillingAudit(
                List.copyOf(new LinkedHashSet<>(supplies)),
                List.copyOf(new LinkedHashSet<>(alerts))
            )
        );
    }

    private void appendLaboratory(
        DiagnosticExamRequest request,
        Set<String> recent,
        List<String> details,
        List<String> summary,
        List<String> supplies,
        List<String> alerts,
        List<String> duplicates
    ) {
        if (request.bloodExams().isEmpty() && request.urineExams().isEmpty()) return;
        details.add("- LABORATÓRIO CLÍNICO (SANGUE E URINA):");
        if (!request.bloodExams().isEmpty()) {
            summary.add("PAINEL DE SANGUE");
            supplies.add("01 X TUSS 40309010 — TAXA DE COLETA DE MATERIAL VENOSO — CONFERIR COLETA EFETIVA.");
        }
        for (String code : request.bloodExams()) {
            DiagnosticExamCatalog.ExamOption exam = catalog.bloodExam(code);
            appendExam("BLOOD:" + code, exam, request.laboratoryClinicalJustification(), recent,
                details, supplies, alerts, duplicates);
        }
        if (!request.urineExams().isEmpty()) summary.add("URINA");
        for (String code : request.urineExams()) {
            DiagnosticExamCatalog.ExamOption exam = catalog.urineExam(code);
            appendExam("URINE:" + code, exam, null, recent, details, supplies, alerts, duplicates);
            if ("URINE_CULTURE".equals(code)) {
                details.add("  * ANTIBIOGRAMA / TSA VINCULADO À UROCULTURA.");
            }
        }
    }

    private void appendExam(
        String key,
        DiagnosticExamCatalog.ExamOption exam,
        String justification,
        Set<String> recent,
        List<String> details,
        List<String> supplies,
        List<String> alerts,
        List<String> duplicates
    ) {
        String suffix = duplicateSuffix(key, exam.label(), recent, duplicates);
        details.add("  * " + exam.label() + codeText(exam.tussCode(), exam.sigtapCode()) + suffix + ".");
        if (exam.highCost()) details.add("    - INDICAÇÃO: " + justification.trim() + ".");
        exam.supplies().forEach(item -> supplies.add(
            (Character.isDigit(item.charAt(0)) ? "" : "01 X ") + item + " — CONFERIR USO EFETIVO."
        ));
        missingCodeAlert(exam.label(), exam.tussCode(), exam.sigtapCode(), alerts);
    }

    private void appendTomography(
        DiagnosticExamRequest.Tomography value,
        Set<String> recent,
        List<String> details,
        List<String> summary,
        List<String> supplies,
        List<String> alerts,
        List<String> duplicates
    ) {
        if (value == null) return;
        DiagnosticExamCatalog.ExamOption region = catalog.tomographyRegion(value.region());
        String contrast = catalog.optionLabel(catalog.catalog().contrastOptions(), value.contrast());
        String key = "CT:" + value.region() + ":" + value.contrast();
        String suffix = duplicateSuffix(key, "TC " + region.label(), recent, duplicates);
        summary.add("TC " + region.label() + " " + contrast);
        details.add("- TOMOGRAFIA COMPUTADORIZADA DE " + region.label() + " — " + contrast +
            codeText(region.tussCode(), region.sigtapCode()) + suffix + ".");
        details.add("  * INDICAÇÃO: " + value.clinicalJustification().trim() + ".");
        if (!"WITHOUT_CONTRAST".equals(value.contrast())) {
            details.add("  * CREATININA PRÉVIA INFORMADA: " + value.previousCreatinineMgDl().trim() + " MG/DL.");
            supplies.add("01 X FRASCO DE CONTRASTE IODADO NÃO IÔNICO — CONFERIR VOLUME E USO EFETIVO.");
            supplies.add("01 X KIT INJETOR / EQUIPO DE ALTA PRESSÃO — CONFERIR USO EFETIVO.");
            alerts.add("TC COM CONTRASTE: CONFERIR FUNÇÃO RENAL, ALERGIA, ACESSO VENOSO E PROTOCOLO INSTITUCIONAL.");
        }
        missingCodeAlert("TC " + region.label(), region.tussCode(), region.sigtapCode(), alerts);
    }

    private void appendXray(
        DiagnosticExamRequest.Xray value,
        Set<String> recent,
        List<String> details,
        List<String> summary,
        List<String> alerts,
        List<String> duplicates
    ) {
        if (value == null) return;
        DiagnosticExamCatalog.XrayProfile profile = catalog.xrayProfile(value.region());
        String lateral = catalog.optionLabel(catalog.catalog().lateralities(), value.laterality());
        String incidences = value.incidences().stream()
            .map(code -> catalog.optionLabel(profile.incidences(), code))
            .collect(java.util.stream.Collectors.joining(" + "));
        String key = "XRAY:" + value.region() + ":" + value.laterality() + ":" +
            String.join(",", value.incidences());
        String suffix = duplicateSuffix(key, "RX " + profile.label(), recent, duplicates);
        String site = profile.label() + (profile.pairedSite() ? " " + lateral : "");
        summary.add("RX " + site + " (" + incidences + ")");
        details.add("- RAIO-X DE " + site + " — INCIDÊNCIAS: " + incidences +
            codeText(profile.tussCode(), profile.sigtapCode()) + suffix + ".");
        details.add("  * INDICAÇÃO: " + value.clinicalJustification().trim() + ".");
        missingCodeAlert("RX " + site, profile.tussCode(), profile.sigtapCode(), alerts);
    }

    private void appendUltrasound(
        DiagnosticExamRequest.Ultrasound value,
        Set<String> recent,
        List<String> details,
        List<String> summary,
        List<String> alerts,
        List<String> duplicates
    ) {
        if (value == null) return;
        DiagnosticExamCatalog.UltrasoundOption exam = catalog.ultrasound(value.type());
        String lateral = catalog.optionLabel(catalog.catalog().lateralities(), value.laterality());
        String site = exam.label() + (exam.pairedSite() ? " — " + lateral : "");
        String key = "USG:" + value.type() + ":" + value.laterality();
        String suffix = duplicateSuffix(key, exam.label(), recent, duplicates);
        summary.add(site);
        details.add("- " + site + codeText(exam.tussCode(), exam.sigtapCode()) + suffix + ".");
        details.add("  * INDICAÇÃO: " + value.clinicalJustification().trim() + ".");
        missingCodeAlert(exam.label(), exam.tussCode(), exam.sigtapCode(), alerts);
    }

    private void appendEcg(
        DiagnosticExamRequest.Electrocardiogram value,
        Set<String> recent,
        List<String> details,
        List<String> summary,
        List<String> supplies,
        List<String> alerts,
        List<String> duplicates
    ) {
        if (value == null) return;
        String priority = catalog.optionLabel(catalog.catalog().ecgPriorities(), value.priority());
        String suffix = duplicateSuffix("ECG:12_LEADS:" + value.priority(), "ECG 12 DERIVAÇÕES", recent, duplicates);
        summary.add("ECG 12 DERIVAÇÕES");
        details.add("- ELETROCARDIOGRAMA — ECG 12 DERIVAÇÕES (TUSS 40101010 / SIGTAP 0211020036)" + suffix + ".");
        details.add("  * PRIORIDADE: " + priority + ".");
        details.add("  * INDICAÇÃO: " + value.clinicalJustification().trim() + ".");
        supplies.add("10 X ELETRODOS DESCARTÁVEIS PARA ECG — CONFERIR USO EFETIVO.");
    }

    private String duplicateSuffix(
        String key,
        String label,
        Set<String> recent,
        List<String> duplicates
    ) {
        if (!recent.contains(key)) return "";
        duplicates.add(label + " — SOLICITAÇÃO REPETIDA EM INTERVALO INFERIOR A 12 HORAS.");
        return " — " + REASSESSMENT;
    }

    private String codeText(String tuss, String sigtap) {
        List<String> codes = new ArrayList<>();
        if (notBlank(tuss)) codes.add("TUSS: " + tuss);
        if (notBlank(sigtap)) codes.add("SIGTAP: " + sigtap);
        return codes.isEmpty() ? "" : " (" + String.join(" / ", codes) + ")";
    }

    private void missingCodeAlert(
        String label,
        String tuss,
        String sigtap,
        List<String> alerts
    ) {
        if (!notBlank(tuss) || !notBlank(sigtap)) {
            alerts.add(label + ": REFERÊNCIA TUSS/SIGTAP INCOMPLETA — VALIDAR NO CATÁLOGO VIGENTE.");
        }
    }

    private Set<String> recentExamKeys(UUID hospitalId, UUID admissionId) {
        return tenantJdbc.read(hospitalId, jdbc -> {
            List<String> documents = jdbc.queryForList(
                "SELECT d.content_json FROM dbo.clinical_document d " +
                "INNER JOIN dbo.admission previous_admission ON previous_admission.id = d.admission_id " +
                "INNER JOIN dbo.admission current_admission ON current_admission.id = ? " +
                "WHERE previous_admission.patient_id = current_admission.patient_id " +
                "AND d.kind = 'EXAM' AND d.status = 'FINALIZED' " +
                "AND d.finalized_at >= DATEADD(HOUR, -12, SYSUTCDATETIME())",
                String.class,
                admissionId
            );
            LinkedHashSet<String> keys = new LinkedHashSet<>();
            documents.forEach(document -> keys.addAll(examKeys(document)));
            return Set.copyOf(keys);
        });
    }

    private Set<String> examKeys(String contentJson) {
        try {
            Map<String, Object> values = objectMapper.readValue(contentJson, VALUE_MAP);
            Object field = values.get("EXAMS.REQUEST");
            if (!(field instanceof Map<?, ?> stored)) return Set.of();
            Object requestValue = stored.get("request");
            if (!(requestValue instanceof Map<?, ?> request)) return Set.of();
            LinkedHashSet<String> keys = new LinkedHashSet<>();
            addListKeys(keys, "BLOOD:", request.get("bloodExams"));
            addListKeys(keys, "URINE:", request.get("urineExams"));
            addTomographyKey(keys, request.get("tomography"));
            addXrayKey(keys, request.get("xray"));
            addUltrasoundKey(keys, request.get("ultrasound"));
            addEcgKey(keys, request.get("electrocardiogram"));
            return Set.copyOf(keys);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Documento de exames armazenado é inválido", exception);
        }
    }

    private void addListKeys(Set<String> keys, String prefix, Object value) {
        if (!(value instanceof Collection<?> items)) return;
        items.stream().filter(String.class::isInstance).map(String.class::cast)
            .forEach(item -> keys.add(prefix + item));
    }

    private void addTomographyKey(Set<String> keys, Object value) {
        if (!(value instanceof Map<?, ?> item)) return;
        keys.add("CT:" + item.get("region") + ":" + item.get("contrast"));
    }

    private void addXrayKey(Set<String> keys, Object value) {
        if (!(value instanceof Map<?, ?> item)) return;
        Object incidences = item.get("incidences");
        String incidenceKey = incidences instanceof Collection<?> values
            ? values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))
            : "";
        keys.add("XRAY:" + item.get("region") + ":" + item.get("laterality") + ":" + incidenceKey);
    }

    private void addUltrasoundKey(Set<String> keys, Object value) {
        if (!(value instanceof Map<?, ?> item)) return;
        keys.add("USG:" + item.get("type") + ":" + item.get("laterality"));
    }

    private void addEcgKey(Set<String> keys, Object value) {
        if (!(value instanceof Map<?, ?> item)) return;
        keys.add("ECG:12_LEADS:" + item.get("priority"));
    }

    private void requireActiveAdmission(UUID hospitalId, UUID admissionId) {
        tenantJdbc.read(hospitalId, jdbc -> {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM dbo.admission WHERE id = ? AND status = 'ACTIVE'",
                Integer.class,
                admissionId
            );
            if (count == null || count == 0) {
                throw new BusinessValidationException("Internação ativa não encontrada");
            }
            return null;
        });
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        Map<?, ?> root = (Map<?, ?>) value;
        allowedKeys(root, ROOT_KEYS);
        nested(root.get("tomography"), TOMOGRAPHY_KEYS);
        nested(root.get("xray"), XRAY_KEYS);
        nested(root.get("ultrasound"), ULTRASOUND_KEYS);
        nested(root.get("electrocardiogram"), ECG_KEYS);
    }

    private void nested(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        Map<?, ?> map = (Map<?, ?>) value;
        allowedKeys(map, allowed);
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("O registro de exames contém campos não permitidos");
        }
    }

    private void invalidFormat() {
        throw new BusinessValidationException("Dados da solicitação de exames estão em formato inválido");
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
