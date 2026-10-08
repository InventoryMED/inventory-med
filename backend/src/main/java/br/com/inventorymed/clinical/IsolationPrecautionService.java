package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class IsolationPrecautionService {

    private static final Set<String> MDR_MARKERS = Set.of(
        "KPC", "MRSA", "VRE", "ACINETOBACTER", "PSEUDOMONAS MDR", "MDR", "MULTIRRESISTENTE"
    );

    private final IsolationPrecautionCatalogService catalog;
    private final IsolationPrecautionValidator validator;
    private final ObjectMapper objectMapper;

    public IsolationPrecautionService(
        IsolationPrecautionCatalogService catalog,
        IsolationPrecautionValidator validator,
        ObjectMapper objectMapper
    ) {
        this.catalog = catalog;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public IsolationPrecautionCatalog catalog() {
        return catalog.catalog();
    }

    public IsolationPrecautionResponse preview(IsolationPrecautionRequest request) {
        validator.validate(request);
        List<String> summaries = new ArrayList<>();
        List<String> details = new ArrayList<>();
        List<IsolationPrecautionResponse.OrderRow> orderRows = new ArrayList<>();
        List<String> supplies = new ArrayList<>();
        List<String> alerts = new ArrayList<>();

        for (IsolationPrecautionRequest.Item item : request.items()) {
            appendItem(item, summaries, details, orderRows, supplies, alerts);
        }
        alerts.add(
            "VALIDAR INDICAÇÃO, DATA DE INÍCIO, DURAÇÃO E CRITÉRIO DE SUSPENSÃO COM A CCIH."
        );

        return new IsolationPrecautionResponse(
            new IsolationPrecautionResponse.StructuredIsolation(
                String.join("; ", summaries) + ".",
                "1. PRECAUÇÕES E ISOLAMENTO:\n" + String.join("\n", details),
                List.copyOf(orderRows)
            ),
            new IsolationPrecautionResponse.BillingAudit(
                List.copyOf(new LinkedHashSet<>(supplies)),
                List.copyOf(new LinkedHashSet<>(alerts))
            )
        );
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final IsolationPrecautionRequest request;
        try {
            request = objectMapper.convertValue(value, IsolationPrecautionRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados das precauções estão em formato inválido");
        }
        IsolationPrecautionResponse response = preview(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", request);
        stored.put("structuredIsolation", response.structuredIsolation());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private void appendItem(
        IsolationPrecautionRequest.Item item,
        List<String> summaries,
        List<String> details,
        List<IsolationPrecautionResponse.OrderRow> orderRows,
        List<String> supplies,
        List<String> alerts
    ) {
        String type = catalog.label(
            IsolationPrecautionCatalogService.PRECAUTION_TYPES, item.precautionType()
        );
        String duration = catalog.label(
            IsolationPrecautionCatalogService.DURATIONS, item.durationReview()
        );
        String scheduling = catalog.label(
            IsolationPrecautionCatalogService.SCHEDULING, item.scheduling()
        );
        String reason = item.reasonPathogen().trim();

        summaries.add(type + " — " + reason);
        details.add("- " + type + ":");
        details.add("  * INDICAÇÃO / MOTIVO: " + reason + ".");
        measures(item.precautionType(), reason).forEach(measure -> details.add("  * " + measure));
        details.add("  * DURAÇÃO / REAVALIAÇÃO: " + duration + ".");
        details.add("  * APRAZAMENTO: " + scheduling + ".");

        orderRows.add(new IsolationPrecautionResponse.OrderRow(
            type + " — " + reason, duration, scheduling
        ));
        addAuditReview(item, reason, supplies, alerts);
    }

    private List<String> measures(String precautionType, String reason) {
        List<String> measures = new ArrayList<>();
        switch (precautionType) {
            case "STANDARD" -> {
                measures.add("HIGIENIZAÇÃO DAS MÃOS ANTES E APÓS O CONTATO COM O PACIENTE.");
                measures.add("USAR LUVAS, AVENTAL, MÁSCARA E PROTEÇÃO OCULAR CONFORME O RISCO DE EXPOSIÇÃO.");
                measures.add("DESCARTAR PERFUROCORTANTES EM RECIPIENTE APROPRIADO.");
            }
            case "CONTACT" -> {
                measures.add("USAR AVENTAL / CAPOTE E LUVAS DE PROCEDIMENTO AO ENTRAR NO AMBIENTE ASSISTENCIAL.");
                measures.add("PRIORIZAR QUARTO PRIVATIVO OU COORTE DEFINIDA PELA CCIH.");
                measures.add("MANTER ESTETOSCÓPIO, ESFIGMOMANÔMETRO E TERMÔMETRO DEDICADOS AO LEITO.");
                measures.add("HIGIENIZAR AS MÃOS ANTES E APÓS A RETIRADA DOS EPIS.");
                if (normalized(reason).contains("CLOSTRIDIOIDES DIFFICILE")) {
                    measures.add("PRIORIZAR HIGIENIZAÇÃO DAS MÃOS COM ÁGUA E SABONETE APÓS O CUIDADO.");
                }
            }
            case "DROPLET" -> {
                measures.add("PROFISSIONAL DEVE USAR MÁSCARA CIRÚRGICA AO ENTRAR NO QUARTO.");
                measures.add("PACIENTE DEVE USAR MÁSCARA CIRÚRGICA DURANTE TRANSPORTE INTERNO.");
                measures.add("PRIORIZAR QUARTO PRIVATIVO OU COORTE; MANTER DISTÂNCIA MÍNIMA DE 1 METRO ENTRE LEITOS.");
            }
            case "AIRBORNE" -> {
                measures.add("PROFISSIONAL DEVE COLOCAR RESPIRADOR PFF2 / N95 COM AJUSTE FACIAL ANTES DE ENTRAR.");
                measures.add("MANTER O PACIENTE EM QUARTO PRIVATIVO E A PORTA FECHADA.");
                measures.add("USAR AMBIENTE COM PRESSÃO NEGATIVA QUANDO INDICADO E DISPONÍVEL, CONFORME PROTOCOLO INSTITUCIONAL.");
                measures.add("PACIENTE DEVE USAR MÁSCARA CIRÚRGICA QUANDO O TRANSPORTE FOR INDISPENSÁVEL.");
            }
            case "PROTECTIVE_REVERSE" -> {
                measures.add("EXIGIR HIGIENIZAÇÃO RIGOROSA DAS MÃOS E MÁSCARA CIRÚRGICA DE QUEM ENTRAR.");
                measures.add("RESTRINGIR VISITANTES E NÃO PERMITIR PLANTAS, FLORES OU ALIMENTOS CRUS.");
                measures.add("PRIORIZAR QUARTO PRIVATIVO E AMBIENTE PROTETOR COM PRESSÃO POSITIVA QUANDO INDICADO E DISPONÍVEL.");
            }
            default -> throw new BusinessValidationException("Tipo de precaução não reconhecido");
        }
        if (!"STANDARD".equals(precautionType)) {
            measures.add("MANTER TAMBÉM AS PRECAUÇÕES PADRÃO.");
        }
        return measures;
    }

    private void addAuditReview(
        IsolationPrecautionRequest.Item item,
        String reason,
        List<String> supplies,
        List<String> alerts
    ) {
        switch (item.precautionType()) {
            case "STANDARD" -> supplies.add(
                "EPIS CONFORME RISCO DE EXPOSIÇÃO — CONFERIR CONSUMO REAL E PROTOCOLO INSTITUCIONAL."
            );
            case "CONTACT" -> {
                supplies.add("AVENTAL / CAPOTE E LUVAS DE PROCEDIMENTO — CONFERIR CONSUMO REAL.");
                supplies.add("EQUIPAMENTOS ASSISTENCIAIS DEDICADOS AO LEITO.");
                supplies.add("DIÁRIA DE ISOLAMENTO / QUARTO PRIVATIVO — CONFERIR REGRA CONTRATUAL E REGISTRO ASSISTENCIAL.");
            }
            case "DROPLET" -> supplies.add(
                "MÁSCARAS CIRÚRGICAS PARA EQUIPE E TRANSPORTE DO PACIENTE — CONFERIR CONSUMO REAL."
            );
            case "AIRBORNE" -> {
                supplies.add("RESPIRADORES PFF2 / N95 E MÁSCARA CIRÚRGICA PARA TRANSPORTE — CONFERIR CONSUMO REAL.");
                supplies.add("DIÁRIA DE ISOLAMENTO / QUARTO PRIVATIVO — CONFERIR REGRA CONTRATUAL E REGISTRO ASSISTENCIAL.");
            }
            case "PROTECTIVE_REVERSE" -> supplies.add(
                "QUARTO PRIVATIVO / AMBIENTE PROTETOR E MÁSCARAS CIRÚRGICAS — CONFERIR REGRA CONTRATUAL."
            );
            default -> throw new BusinessValidationException("Tipo de precaução não reconhecido");
        }
        if ("CONTACT".equals(item.precautionType()) && isMdr(reason)) {
            alerts.add(
                "GERME MULTIRRESISTENTE: CONFIRMAR CULTURA PRÉVIA OU SWAB DE VIGILÂNCIA E COMUNICAR A CCIH."
            );
        }
        if (Set.of("CONTACT", "AIRBORNE").contains(item.precautionType())) {
            alerts.add(
                "ANTES DO FATURAMENTO, CONFERIR CÓDIGO TUSS/SIGTAP, DIÁRIA, CONSUMO DE EPIS E EVIDÊNCIA ASSISTENCIAL."
            );
        }
    }

    private boolean isMdr(String reason) {
        String normalizedReason = normalized(reason);
        return MDR_MARKERS.stream().anyMatch(normalizedReason::contains);
    }

    private String normalized(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toUpperCase(Locale.ROOT);
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
            Map<?, ?> item = (Map<?, ?>) itemValue;
            allowedKeys(
                item,
                Set.of("id", "precautionType", "reasonPathogen", "durationReview", "scheduling")
            );
        }
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("As precauções contêm campos não permitidos");
        }
    }

    private void invalidFormat() {
        throw new BusinessValidationException("Dados das precauções estão em formato inválido");
    }
}
