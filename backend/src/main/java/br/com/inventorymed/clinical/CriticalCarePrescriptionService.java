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
public class CriticalCarePrescriptionService {

    private final CriticalCareCatalogService catalog;
    private final CriticalCarePrescriptionValidator validator;
    private final ObjectMapper objectMapper;

    public CriticalCarePrescriptionService(
        CriticalCareCatalogService catalog,
        CriticalCarePrescriptionValidator validator,
        ObjectMapper objectMapper
    ) {
        this.catalog = catalog;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public CriticalCareCatalog catalog() {
        return catalog.catalog();
    }

    public CriticalCarePrescriptionResponse preview(CriticalCarePrescriptionRequest request) {
        validator.validate(request);
        List<String> sections = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        List<CriticalCarePrescriptionResponse.OrderRow> rows = new ArrayList<>();
        List<String> supplies = new ArrayList<>();
        List<String> alerts = new ArrayList<>();

        appendVasoactives(request.vasoactiveDrugs(), sections, summaries, rows, supplies, alerts);
        appendSedation(request.sedationAnalgesiaBnm(), sections, summaries, rows, supplies, alerts);
        appendEmergency(request.emergencyMedications(), sections, summaries, rows, supplies, alerts);
        alerts.add("CONFIRMAR DOSES, DILUIÇÕES, COMPATIBILIDADES E METAS CONFORME PROTOCOLO INSTITUCIONAL E CONDIÇÃO DO PACIENTE.");
        alerts.add("OS ITENS DE INSUMOS E EQUIPAMENTOS SÃO PARA CONFERÊNCIA; NENHUMA COBRANÇA É GERADA AUTOMATICAMENTE.");

        return new CriticalCarePrescriptionResponse(
            new CriticalCarePrescriptionResponse.StructuredCriticalCare(
                String.join(" + ", summaries) + ".",
                String.join("\n\n", sections),
                List.copyOf(rows)
            ),
            new CriticalCarePrescriptionResponse.BillingAudit(
                List.copyOf(new LinkedHashSet<>(supplies)),
                List.copyOf(new LinkedHashSet<>(alerts))
            )
        );
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final CriticalCarePrescriptionRequest request;
        try {
            request = objectMapper.convertValue(value, CriticalCarePrescriptionRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados de cuidados críticos estão em formato inválido");
        }
        CriticalCarePrescriptionResponse response = preview(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", request);
        stored.put("structuredCriticalCare", response.structuredCriticalCare());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private void appendVasoactives(
        List<CriticalCarePrescriptionRequest.VasoactiveDrug> values,
        List<String> sections,
        List<String> summaries,
        List<CriticalCarePrescriptionResponse.OrderRow> rows,
        List<String> supplies,
        List<String> alerts
    ) {
        if (safe(values).isEmpty()) return;
        List<String> details = new ArrayList<>();
        for (CriticalCarePrescriptionRequest.VasoactiveDrug value : values) {
            CriticalCareCatalog.VasoactiveOption drug = catalog.vasoactive(value.drug());
            String access = label(CriticalCareCatalogService.VASCULAR_ACCESSES, value.vascularAccess());
            String monitoring = label(CriticalCareCatalogService.BLOOD_PRESSURE_MONITORING,
                value.bloodPressureMonitoring());
            String rate = decimal(value.initialRate()) + " " +
                label(CriticalCareCatalogService.VASOACTIVE_RATE_UNITS, value.rateUnit());
            details.add("- " + drug.label() + ":");
            details.add("  * " + value.dilution().trim() + ". CONCENTRAÇÃO FINAL: " +
                value.finalConcentration().trim() + ".");
            details.add("  * ADMINISTRAR POR " + access + " EM BIC DEDICADA — VAZÃO INICIAL: " + rate + ".");
            details.add("  * MONITORIZAÇÃO: " + monitoring + ". META: " + value.therapeuticGoal().trim() + ".");
            summaries.add(drug.label() + " A " + rate + " EM BIC");
            rows.add(new CriticalCarePrescriptionResponse.OrderRow(
                "DROGAS VASOATIVAS / INOTRÓPICOS", drug.label() + " — " + value.dilution().trim() +
                    " — META: " + value.therapeuticGoal().trim(), access, rate, "CONTÍNUO"
            ));
            supplies.add("01 CANAL DE BIC DEDICADO PARA " + drug.label() + " — CONFERIR PERÍODO DE USO REGISTRADO.");
            supplies.add("01 EQUIPO PARENTERAL DEDICADO PARA BIC — CONFERIR USO REGISTRADO.");
            if ("CVC".equals(value.vascularAccess())) {
                supplies.add("MANUTENÇÃO / CURATIVO DE CVC — CONFERIR EXECUÇÃO E REGRA CONTRATUAL.");
            }
            if ("INVASIVE_ARTERIAL".equals(value.bloodPressureMonitoring())) {
                supplies.add("SISTEMA DE MONITORIZAÇÃO DE PAI — CONFERIR INSTALAÇÃO E PERÍODO DE USO.");
            }
            if ("PHOTOPROTECTIVE".equals(drug.specialTubing())) {
                supplies.add("EQUIPO E PROTEÇÃO FOTOSSENSÍVEL / OPACA PARA " + drug.label() + " — CONFERIR USO.");
                alerts.add(drug.label() + " EXIGE PROTEÇÃO DA SOLUÇÃO E DO SISTEMA CONTRA A LUZ.");
            }
            if ("NON_ADSORPTIVE".equals(drug.specialTubing())) {
                supplies.add("EQUIPO NÃO ADSORVENTE COMPATÍVEL COM " + drug.label() + " — CONFERIR USO.");
                alerts.add(drug.label() + " EXIGE SISTEMA DE ADMINISTRAÇÃO NÃO ADSORVENTE COMPATÍVEL.");
            }
        }
        alerts.add("DROGAS VASOATIVAS SÃO DE ALTA VIGILÂNCIA: EXIGIR DUPLA CHECAGEM DE DILUIÇÃO, CONCENTRAÇÃO, CANAL E VAZÃO.");
        sections.add("14. DROGAS VASOATIVAS E INOTRÓPICOS:\n" + String.join("\n", details));
    }

    private void appendSedation(
        List<CriticalCarePrescriptionRequest.SedationDrug> values,
        List<String> sections,
        List<String> summaries,
        List<CriticalCarePrescriptionResponse.OrderRow> rows,
        List<String> supplies,
        List<String> alerts
    ) {
        if (safe(values).isEmpty()) return;
        List<String> details = new ArrayList<>();
        for (CriticalCarePrescriptionRequest.SedationDrug value : values) {
            CriticalCareCatalog.SedationOption drug = catalog.sedation(value.drug());
            String mode = label(CriticalCareCatalogService.ADMINISTRATION_MODES, value.administrationMode());
            String dose = decimal(value.rateDoseValue()) + " " +
                label(CriticalCareCatalogService.SEDATION_RATE_UNITS, value.rateDoseUnit());
            String target = label(CriticalCareCatalogService.SEDATION_TARGETS, value.sedationTarget());
            String ventilation = label(CriticalCareCatalogService.VENTILATORY_STATUSES, value.ventilatoryStatus());
            String route = label(CriticalCareCatalogService.SEDATION_ROUTES, value.route());
            details.add("- " + drug.label() + ":");
            details.add("  * " + value.preparation().trim() + ".");
            details.add("  * " + mode + " — " + route + " — VAZÃO / DOSE: " + dose + ".");
            details.add("  * " + ventilation + ". META DE MONITORIZAÇÃO: " + target + ".");
            summaries.add(drug.label() + " " + dose + " — " + target);
            rows.add(new CriticalCarePrescriptionResponse.OrderRow(
                "SEDAÇÃO / ANALGESIA / BNM", drug.label() + " — " + value.preparation().trim() +
                    " — " + target, route, dose, "CONTINUOUS".equals(value.scheduling()) ? "CONTÍNUO" : "DOSE ÚNICA"
            ));
            if ("CONTINUOUS_BIC".equals(value.administrationMode())) {
                supplies.add("01 CANAL DE BIC DEDICADO PARA " + drug.label() + " — CONFERIR PERÍODO DE USO REGISTRADO.");
                supplies.add("01 EQUIPO PARENTERAL DEDICADO PARA BIC — CONFERIR USO REGISTRADO.");
            }
            if (value.drug().startsWith("PROPOFOL")) {
                alerts.add("PROPOFOL: IDENTIFICAR DATA E HORA DE ABERTURA E TROCAR FRASCO / EQUIPO EM ATÉ 12 HORAS, CONFORME PROTOCOLO INSTITUCIONAL.");
            }
        }
        alerts.add("SEDAÇÃO E BNM EXIGEM VMI CONFIRMADA, MONITORIZAÇÃO CONTÍNUA E REGISTRO SERIADO DE RASS OU BIS.");
        sections.add("15. SEDAÇÃO, ANALGESIA CONTÍNUA E BNM:\n" + String.join("\n", details));
    }

    private void appendEmergency(
        List<CriticalCarePrescriptionRequest.EmergencyMedication> values,
        List<String> sections,
        List<String> summaries,
        List<CriticalCarePrescriptionResponse.OrderRow> rows,
        List<String> supplies,
        List<String> alerts
    ) {
        if (safe(values).isEmpty()) return;
        List<String> details = new ArrayList<>();
        for (CriticalCarePrescriptionRequest.EmergencyMedication value : values) {
            CriticalCareCatalog.EmergencyOption drug = catalog.emergency(value.drug());
            String route = label(CriticalCareCatalogService.EMERGENCY_ROUTES, value.route());
            String scheduling = label(CriticalCareCatalogService.EMERGENCY_SCHEDULING, value.scheduling());
            details.add("- " + drug.label() + ": " + value.doseAdministration().trim() + " — " + route +
                " — " + scheduling + ".");
            details.add("  * INDICAÇÃO: " + value.emergencyIndication().trim() + ".");
            summaries.add(drug.label() + " — " + scheduling);
            rows.add(new CriticalCarePrescriptionResponse.OrderRow(
                "ANTÍDOTOS / EMERGÊNCIA", drug.label() + " — " + value.doseAdministration().trim() +
                    " — INDICAÇÃO: " + value.emergencyIndication().trim(), route, "—", scheduling
            ));
            supplies.add(drug.label() + " — CONFERIR DISPENSAÇÃO, ADMINISTRAÇÃO E REPOSIÇÃO DO CARRINHO DE EMERGÊNCIA.");
            if (drug.highCost()) {
                alerts.add(drug.label() + ": MOTIVO DA REVERSÃO REGISTRADO; SUBMETER À CONFERÊNCIA ASSISTENCIAL E CONTRATUAL.");
            }
        }
        alerts.add("APÓS USO EM EMERGÊNCIA, CONFERIR E REABASTECER AS GAVETAS DO CARRINHO DE PARADA / SALA VERMELHA.");
        sections.add("16. ANTÍDOTOS, REVERSORES E EMERGÊNCIA (PCR):\n" + String.join("\n", details));
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        Map<?, ?> root = (Map<?, ?>) value;
        allowedKeys(root, Set.of("clinicalContext", "vasoactiveDrugs", "sedationAnalgesiaBnm", "emergencyMedications"));
        nestedList(root.get("vasoactiveDrugs"), Set.of(
            "id", "drug", "dilution", "finalConcentration", "initialRate", "rateUnit",
            "vascularAccess", "bloodPressureMonitoring", "therapeuticGoal", "scheduling"
        ));
        nestedList(root.get("sedationAnalgesiaBnm"), Set.of(
            "id", "drug", "preparation", "administrationMode", "rateDoseValue", "rateDoseUnit",
            "sedationTarget", "ventilatoryStatus", "route", "scheduling"
        ));
        nestedList(root.get("emergencyMedications"), Set.of(
            "id", "drug", "doseAdministration", "route", "emergencyIndication", "scheduling"
        ));
    }

    private void nestedList(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof List<?>)) invalidFormat();
        List<?> list = (List<?>) value;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?>)) invalidFormat();
            Map<?, ?> map = (Map<?, ?>) item;
            allowedKeys(map, allowed);
        }
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("Os cuidados críticos contêm campos não permitidos");
        }
    }

    private String label(List<CriticalCareCatalog.Option> options, String code) {
        return catalog.optionLabel(options, code);
    }

    private String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void invalidFormat() {
        throw new BusinessValidationException("Dados de cuidados críticos estão em formato inválido");
    }
}
