package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class VentilatorySupportService {

    private final VentilatorySupportCatalogService catalog;
    private final VentilatorySupportValidator validator;
    private final ObjectMapper objectMapper;

    public VentilatorySupportService(
        VentilatorySupportCatalogService catalog,
        VentilatorySupportValidator validator,
        ObjectMapper objectMapper
    ) {
        this.catalog = catalog;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public VentilatorySupportCatalog catalog() {
        return catalog.catalog();
    }

    public VentilatorySupportResponse preview(VentilatorySupportRequest request) {
        validator.validate(request);
        List<String> summaries = new ArrayList<>();
        List<String> details = new ArrayList<>();
        List<VentilatorySupportResponse.OrderRow> orderRows = new ArrayList<>();
        List<String> review = new ArrayList<>();
        List<String> alerts = new ArrayList<>();

        for (VentilatorySupportRequest.Item item : request.items()) {
            switch (item.supportType()) {
                case "ROOM_AIR" -> appendRoomAir(summaries, details, orderRows, alerts);
                case "LOW_FLOW" -> appendLowFlow(item, summaries, details, orderRows, review, alerts);
                case "HIGH_FLOW" -> appendHighFlow(item, summaries, details, orderRows, review, alerts);
                case "NIV" -> appendNonInvasive(item, summaries, details, orderRows, review, alerts);
                case "IMV" -> appendInvasive(item, summaries, details, orderRows, review, alerts);
                default -> throw new BusinessValidationException("Tipo de suporte ventilatório inválido");
            }
        }

        return new VentilatorySupportResponse(
            new VentilatorySupportResponse.StructuredVentilatorySupport(
                String.join("; ", summaries) + ".",
                "SUPORTE VENTILATÓRIO E OXIGENOTERAPIA:\n" + String.join("\n", details),
                List.copyOf(orderRows)
            ),
            new VentilatorySupportResponse.BillingAudit(
                List.copyOf(new LinkedHashSet<>(review)),
                List.copyOf(new LinkedHashSet<>(alerts))
            )
        );
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final VentilatorySupportRequest request;
        try {
            request = objectMapper.convertValue(value, VentilatorySupportRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados do suporte ventilatório estão em formato inválido");
        }
        VentilatorySupportResponse response = preview(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", request);
        stored.put("structuredVentilatorySupport", response.structuredVentilatorySupport());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private void appendRoomAir(
        List<String> summaries,
        List<String> details,
        List<VentilatorySupportResponse.OrderRow> orderRows,
        List<String> alerts
    ) {
        summaries.add("MANTER EM AR AMBIENTE");
        details.add("- AR AMBIENTE: SEM NECESSIDADE DE SUPLEMENTAÇÃO DE OXIGÊNIO; MONITORAR SPO₂.");
        orderRows.add(row("MANTER EM AR AMBIENTE E MONITORAR SPO₂", "AA", "CONTÍNUO", "CONTÍNUO"));
        alerts.add("REGISTRAR SPO₂ E REAVALIAR A NECESSIDADE DE SUPORTE CONFORME A EVOLUÇÃO CLÍNICA.");
    }

    private void appendLowFlow(
        VentilatorySupportRequest.Item item,
        List<String> summaries,
        List<String> details,
        List<VentilatorySupportResponse.OrderRow> orderRows,
        List<String> review,
        List<String> alerts
    ) {
        VentilatorySupportRequest.LowFlow value = item.lowFlow();
        String device = label(VentilatorySupportCatalogService.LOW_FLOW_DEVICES, value.device());
        String parameters = "VENTURI_MASK".equals(value.device())
            ? "FIO₂ " + value.fio2Percent() + "%"
            : decimal(value.oxygenFlowLitersMinute()) + " L/MIN";
        String frequency = label(VentilatorySupportCatalogService.LOW_FLOW_FREQUENCIES, item.frequency());
        String summary = device + " — " + parameters + " — " + frequency;
        summaries.add(summary);
        details.add("- OXIGENOTERAPIA DE BAIXO FLUXO: " + device + ".");
        details.add("  * PARÂMETROS: " + parameters + ".");
        details.add("  * USO: " + frequency + ".");
        if (item.frequency().startsWith("PRN_")) {
            details.add("  * SUSPENDER OU DESMAMAR QUANDO O GATILHO DE SATURAÇÃO NÃO ESTIVER PRESENTE, " +
                "CONFORME AVALIAÇÃO CLÍNICA.");
        }
        orderRows.add(row(summary, device, frequency, scheduling(item.scheduling())));
        review.add("01 X " + device + " — CONFERIR DISPENSAÇÃO E USO REGISTRADO.");
        review.add("OXIGÊNIO MEDICINAL — CONFERIR HORAS E FLUXO EFETIVAMENTE REGISTRADOS ANTES DO FATURAMENTO.");
        if (!"VENTURI_MASK".equals(value.device())) {
            review.add("COPO UMIDIFICADOR E ÁGUA DESTILADA — CONFERIR USO E PROTOCOLO INSTITUCIONAL.");
        }
        alerts.add("REGISTRAR SPO₂, DISPOSITIVO, FLUXO OU FIO₂ E TEMPO REAL DE UTILIZAÇÃO.");
    }

    private void appendHighFlow(
        VentilatorySupportRequest.Item item,
        List<String> summaries,
        List<String> details,
        List<VentilatorySupportResponse.OrderRow> orderRows,
        List<String> review,
        List<String> alerts
    ) {
        VentilatorySupportRequest.HighFlow value = item.highFlow();
        String summary = "CNAF " + decimal(value.flowLitersMinute()) + " L/MIN, FIO₂ " +
            value.fio2Percent() + "%, " + decimal(value.temperatureCelsius()) + " °C, CÂNULA " +
            value.interfaceSize();
        summaries.add(summary);
        details.add("- CÂNULA NASAL DE ALTO FLUXO (CNAF):");
        details.add("  * FLUXO: " + decimal(value.flowLitersMinute()) + " L/MIN.");
        details.add("  * FIO₂: " + value.fio2Percent() + "%.");
        details.add("  * TEMPERATURA: " + decimal(value.temperatureCelsius()) + " °C.");
        details.add("  * INTERFACE: CÂNULA NASAL DEDICADA TAMANHO " + value.interfaceSize() + ".");
        details.add("  * USO CONTÍNUO.");
        orderRows.add(row(summary, "CNAF " + value.interfaceSize(), "CONTÍNUO", "CONTÍNUO"));
        review.add("GERADOR DE ALTO FLUXO — CONFERIR DIÁRIA E TEMPO DE USO REGISTRADO.");
        review.add("CIRCUITO, CÂNULA DE ALTO FLUXO E UMIDIFICAÇÃO — CONFERIR DISPENSAÇÃO E USO.");
        review.add("OXIGÊNIO E AR COMPRIMIDO MEDICINAIS — CONFERIR CONSUMO EFETIVAMENTE REGISTRADO.");
        alerts.add("REGISTRAR FLUXO, FIO₂, TEMPERATURA, SPO₂ E HORÁRIO DE INÍCIO, AJUSTE OU SUSPENSÃO.");
    }

    private void appendNonInvasive(
        VentilatorySupportRequest.Item item,
        List<String> summaries,
        List<String> details,
        List<VentilatorySupportResponse.OrderRow> orderRows,
        List<String> review,
        List<String> alerts
    ) {
        VentilatorySupportRequest.NonInvasive value = item.nonInvasive();
        String mode = label(VentilatorySupportCatalogService.NIV_MODES, value.mode());
        String interfaceType = label(VentilatorySupportCatalogService.NIV_INTERFACES, value.interfaceType());
        String parameters = nivParameters(value);
        String frequency = label(VentilatorySupportCatalogService.NIV_FREQUENCIES, item.frequency());
        if (value.sessionHours() != null) frequency += " — " + decimal(value.sessionHours()) + " H POR SESSÃO";
        String summary = "VNI " + mode + " (" + parameters + ") — " + interfaceType + " — " + frequency;
        summaries.add(summary);
        details.add("- VENTILAÇÃO NÃO INVASIVA (VNI):");
        details.add("  * MODO: " + mode + ".");
        details.add("  * PARÂMETROS: " + parameters + ".");
        details.add("  * INTERFACE: " + interfaceType + ".");
        details.add("  * USO: " + frequency + ".");
        orderRows.add(row(summary, interfaceType, frequency, scheduling(item.scheduling())));
        review.add("VENTILADOR PARA VNI — CONFERIR DIÁRIA E TEMPO DE USO REGISTRADO.");
        review.add("CIRCUITO, FILTRO E " + interfaceType + " — CONFERIR DISPENSAÇÃO E USO.");
        review.add("GASES MEDICINAIS — CONFERIR CONSUMO EFETIVAMENTE REGISTRADO.");
        alerts.add("REGISTRAR ADAPTAÇÃO À INTERFACE, VAZAMENTOS, SPO₂ E RESPOSTA CLÍNICA A CADA SESSÃO.");
    }

    private void appendInvasive(
        VentilatorySupportRequest.Item item,
        List<String> summaries,
        List<String> details,
        List<VentilatorySupportResponse.OrderRow> orderRows,
        List<String> review,
        List<String> alerts
    ) {
        VentilatorySupportRequest.Invasive value = item.invasive();
        String airway = label(VentilatorySupportCatalogService.INVASIVE_AIRWAYS, value.airway());
        String mode = label(VentilatorySupportCatalogService.INVASIVE_MODES, value.mode());
        String parameters = invasiveParameters(value);
        String airwayWithDetail = airway + " " + value.airwayDetail().trim();
        String summary = "VMI " + mode + " (" + parameters + ") VIA " + airwayWithDetail;
        summaries.add(summary);
        details.add("- VENTILAÇÃO MECÂNICA INVASIVA (VMI):");
        details.add("  * VIA AÉREA: " + airwayWithDetail + ".");
        details.add("  * MODO: " + mode + ".");
        details.add("  * PARÂMETROS: " + parameters + ".");
        if (value.protectiveGoals() != null && !value.protectiveGoals().isEmpty()) {
            details.add("  * METAS E CUIDADOS: " + value.protectiveGoals().stream()
                .map(goal -> label(VentilatorySupportCatalogService.PROTECTIVE_GOALS, goal))
                .reduce((left, right) -> left + "; " + right)
                .orElse("") + ".");
        }
        details.add("  * USO CONTÍNUO; REGISTRAR AJUSTES E RESPOSTA CLÍNICA.");
        orderRows.add(row(summary, airwayWithDetail, "CONTÍNUO", "CONTÍNUO"));
        review.add("VENTILADOR MECÂNICO — CONFERIR DIÁRIA E TEMPO DE USO REGISTRADO.");
        review.add("CIRCUITO RESPIRATÓRIO E FILTRO HME/HEPA — CONFERIR DISPENSAÇÃO, TROCA E USO.");
        review.add("OXIGÊNIO E AR COMPRIMIDO MEDICINAIS — CONFERIR CONSUMO EFETIVAMENTE REGISTRADO.");
        alerts.add("REGISTRAR PARÂMETROS, PRESSÕES, VOLUMES, GASOMETRIA E ALTERAÇÕES DO VENTILADOR.");
        alerts.add("A EXECUÇÃO E QUALQUER AJUSTE DEVEM SER REALIZADOS POR PROFISSIONAL HABILITADO.");
    }

    private String nivParameters(VentilatorySupportRequest.NonInvasive value) {
        List<String> values = new ArrayList<>();
        if (value.ipapCmH2o() != null) values.add("IPAP " + decimal(value.ipapCmH2o()) + " CMH₂O");
        if (value.epapPeepCmH2o() != null) values.add("EPAP/PEEP " + decimal(value.epapPeepCmH2o()) + " CMH₂O");
        if (value.supportPressureCmH2o() != null) {
            values.add("PSUP " + decimal(value.supportPressureCmH2o()) + " CMH₂O");
        }
        values.add("FIO₂ " + value.fio2Percent() + "%");
        if (value.backupRate() != null) values.add("BACKUP " + value.backupRate() + " IRPM");
        return String.join(", ", values);
    }

    private String invasiveParameters(VentilatorySupportRequest.Invasive value) {
        List<String> values = new ArrayList<>();
        if (value.tidalVolumeMl() != null) values.add("VT " + value.tidalVolumeMl() + " ML");
        if (value.respiratoryRate() != null) values.add("FR " + value.respiratoryRate() + " IRPM");
        values.add("PEEP " + decimal(value.peepCmH2o()) + " CMH₂O");
        values.add("FIO₂ " + value.fio2Percent() + "%");
        if (value.inspiratoryFlowLitersMinute() != null) {
            values.add("FLUXO " + decimal(value.inspiratoryFlowLitersMinute()) + " L/MIN");
        }
        if (value.inspiratoryTimeSeconds() != null) {
            values.add("TI " + decimal(value.inspiratoryTimeSeconds()) + " S");
        }
        if (value.pauseSeconds() != null) values.add("PAUSA " + decimal(value.pauseSeconds()) + " S");
        if (value.inspiratoryPressureCmH2o() != null) {
            values.add("PINSP " + decimal(value.inspiratoryPressureCmH2o()) + " CMH₂O");
        }
        if (value.supportPressureCmH2o() != null) {
            values.add("PSUP " + decimal(value.supportPressureCmH2o()) + " CMH₂O");
        }
        if (value.triggerSensitivity() != null) {
            values.add("SENSIBILIDADE " + decimal(value.triggerSensitivity()));
        }
        return String.join(", ", values);
    }

    private VentilatorySupportResponse.OrderRow row(
        String description,
        String interfaceRoute,
        String frequency,
        String scheduling
    ) {
        return new VentilatorySupportResponse.OrderRow(description, interfaceRoute, frequency, scheduling);
    }

    private String scheduling(String code) {
        return label(VentilatorySupportCatalogService.SCHEDULING, code);
    }

    private String label(List<VentilatorySupportCatalog.Option> options, String code) {
        return catalog.label(options, code);
    }

    private String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?> values)) invalidFormat();
        @SuppressWarnings("unchecked") Map<?, ?> root = (Map<?, ?>) value;
        allowedKeys(root, Set.of("selectedTemplate", "items"));
        Object itemsValue = root.get("items");
        if (!(itemsValue instanceof List<?>)) invalidFormat();
        List<?> items = (List<?>) itemsValue;
        for (Object itemValue : items) {
            if (!(itemValue instanceof Map<?, ?>)) invalidFormat();
            Map<?, ?> item = (Map<?, ?>) itemValue;
            allowedKeys(item, Set.of(
                "id", "supportType", "frequency", "scheduling", "lowFlow", "highFlow", "nonInvasive", "invasive"
            ));
            nested(item.get("lowFlow"), Set.of("device", "oxygenFlowLitersMinute", "fio2Percent"));
            nested(item.get("highFlow"), Set.of("flowLitersMinute", "fio2Percent", "temperatureCelsius", "interfaceSize"));
            nested(item.get("nonInvasive"), Set.of(
                "mode", "ipapCmH2o", "epapPeepCmH2o", "supportPressureCmH2o", "fio2Percent", "backupRate",
                "interfaceType", "sessionHours"
            ));
            nested(item.get("invasive"), Set.of(
                "airway", "airwayDetail", "mode", "tidalVolumeMl", "respiratoryRate", "peepCmH2o",
                "fio2Percent", "inspiratoryFlowLitersMinute", "inspiratoryTimeSeconds", "pauseSeconds",
                "inspiratoryPressureCmH2o", "supportPressureCmH2o", "triggerSensitivity", "protectiveGoals"
            ));
        }
    }

    private void nested(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof Map<?, ?> map)) invalidFormat();
        allowedKeys((Map<?, ?>) value, allowed);
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("O suporte ventilatório contém campos não permitidos");
        }
    }

    private void invalidFormat() {
        throw new BusinessValidationException("Dados do suporte ventilatório estão em formato inválido");
    }
}
