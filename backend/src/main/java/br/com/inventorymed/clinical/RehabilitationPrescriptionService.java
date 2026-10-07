package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class RehabilitationPrescriptionService {

    private final RehabilitationPrescriptionCatalogService catalog;
    private final RehabilitationPrescriptionValidator validator;
    private final ObjectMapper objectMapper;

    public RehabilitationPrescriptionService(
        RehabilitationPrescriptionCatalogService catalog,
        RehabilitationPrescriptionValidator validator,
        ObjectMapper objectMapper
    ) {
        this.catalog = catalog;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public RehabilitationPrescriptionCatalog catalog() {
        return catalog.catalog();
    }

    public RehabilitationPrescriptionResponse preview(RehabilitationPrescriptionRequest request) {
        validator.validate(request);
        List<String> summaries = new ArrayList<>();
        List<String> details = new ArrayList<>();
        List<RehabilitationPrescriptionResponse.OrderRow> orderRows = new ArrayList<>();
        List<String> reviewItems = new ArrayList<>();
        List<String> alerts = new ArrayList<>();

        for (RehabilitationPrescriptionRequest.Item item : request.items()) {
            appendItem(item, summaries, details, orderRows, reviewItems, alerts);
        }

        return new RehabilitationPrescriptionResponse(
            new RehabilitationPrescriptionResponse.StructuredRehabilitation(
                String.join("; ", summaries) + ".",
                "REABILITAÇÃO MULTIDISCIPLINAR:\n" + String.join("\n", details),
                List.copyOf(orderRows)
            ),
            new RehabilitationPrescriptionResponse.BillingAudit(
                List.copyOf(new LinkedHashSet<>(reviewItems)),
                List.copyOf(new LinkedHashSet<>(alerts))
            )
        );
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final RehabilitationPrescriptionRequest request;
        try {
            request = objectMapper.convertValue(value, RehabilitationPrescriptionRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados da reabilitação estão em formato inválido");
        }
        RehabilitationPrescriptionResponse response = preview(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", request);
        stored.put("structuredRehabilitation", response.structuredRehabilitation());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private void appendItem(
        RehabilitationPrescriptionRequest.Item item,
        List<String> summaries,
        List<String> details,
        List<RehabilitationPrescriptionResponse.OrderRow> orderRows,
        List<String> reviewItems,
        List<String> alerts
    ) {
        String specialty = label(RehabilitationPrescriptionCatalogService.SPECIALTIES, item.specialty());
        String procedure = label(catalog.procedures(item.specialty()), item.procedure());
        String frequency = label(catalog.frequencies(item.specialty()), item.frequency());
        String scheduling = "PRN".equals(item.scheduling()) ? "SN" : "FIXO";

        summaries.add(specialty + ": " + procedure + " — " + frequency);
        details.add("- " + specialty + ": " + procedure + ".");
        details.add("  * FREQUÊNCIA: " + frequency + ".");
        if (notBlank(item.clinicalJustification())) {
            details.add("  * JUSTIFICATIVA CLÍNICA: " + item.clinicalJustification().trim() + ".");
        }
        orderRows.add(new RehabilitationPrescriptionResponse.OrderRow(
            procedure, specialty, frequency, scheduling
        ));
        addAuditReview(item, specialty, procedure, frequency, reviewItems, alerts);
    }

    private void addAuditReview(
        RehabilitationPrescriptionRequest.Item item,
        String specialty,
        String procedure,
        String frequency,
        List<String> reviewItems,
        List<String> alerts
    ) {
        reviewItems.add(specialty + " — " + procedure + " — " + frequency +
            " — CONFERIR CÓDIGO TUSS/SIGTAP, EXECUÇÃO E REGISTRO DA SESSÃO ANTES DO FATURAMENTO.");
        if (Set.of("EVERY_12H", "EVERY_8H").contains(item.frequency())) {
            int sessions = "EVERY_8H".equals(item.frequency()) ? 3 : 2;
            alerts.add(
                "CONFERIR " + sessions + " SESSÕES DIÁRIAS DE " + specialty +
                ", JUSTIFICATIVA CLÍNICA E HORÁRIOS INDIVIDUAIS PARA EVITAR GLOSA."
            );
        }
        if (
            "SPEECH_THERAPY".equals(item.specialty()) &&
            Set.of("SWALLOWING_ASSESSMENT", "SWALLOWING_TRAINING").contains(item.procedure())
        ) {
            alerts.add(
                "VINCULAR AVALIAÇÃO DE DEGLUTIÇÃO À CONDIÇÃO CLÍNICA E À DEFINIÇÃO SEGURA DA VIA ALIMENTAR."
            );
        }
        alerts.add("REGISTRAR DATA, HORÁRIO, PROFISSIONAL, CONDUTA EXECUTADA E RESPOSTA DO PACIENTE.");
    }

    private String label(List<RehabilitationPrescriptionCatalog.Option> options, String code) {
        return catalog.label(options, code);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        Map<?, ?> root = (Map<?, ?>) value;
        allowedKeys(root, Set.of("selectedTemplate", "items"));
        Object itemsValue = root.get("items");
        if (!(itemsValue instanceof List<?>)) invalidFormat();
        List<?> items = (List<?>) itemsValue;
        for (Object itemValue : items) {
            if (!(itemValue instanceof Map<?, ?>)) invalidFormat();
            allowedKeys(
                (Map<?, ?>) itemValue,
                Set.of("id", "specialty", "procedure", "frequency", "scheduling", "clinicalJustification")
            );
        }
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("A reabilitação contém campos não permitidos");
        }
    }

    private void invalidFormat() {
        throw new BusinessValidationException("Dados da reabilitação estão em formato inválido");
    }
}
