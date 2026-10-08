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
public class TherapeuticSupportService {

    private static final Map<String, Integer> HYDRATION_ADMINISTRATIONS_PER_DAY = Map.of(
        "EVERY_24_HOURS", 1,
        "EVERY_12_HOURS", 2,
        "EVERY_8_HOURS", 3,
        "EVERY_6_HOURS", 4
    );
    private static final Map<String, Integer> GLUCOSE_CHECKS_PER_DAY = Map.of(
        "EVERY_1_HOUR", 24,
        "EVERY_2_HOURS", 12,
        "EVERY_4_HOURS", 6,
        "EVERY_6_HOURS", 4,
        "BEFORE_MEALS", 3
    );

    private final TherapeuticSupportCatalogService catalog;
    private final TherapeuticSupportValidator validator;
    private final ObjectMapper objectMapper;

    public TherapeuticSupportService(
        TherapeuticSupportCatalogService catalog,
        TherapeuticSupportValidator validator,
        ObjectMapper objectMapper
    ) {
        this.catalog = catalog;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public TherapeuticSupportCatalog catalog() {
        return catalog.catalog();
    }

    public TherapeuticSupportResponse preview(TherapeuticSupportRequest request) {
        validator.validate(request);
        List<String> sections = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        List<TherapeuticSupportResponse.OrderRow> orderRows = new ArrayList<>();
        List<String> supplies = new ArrayList<>();
        List<String> alerts = new ArrayList<>();

        appendHydration(request.hydrationSolutions(), sections, summaries, orderRows, supplies, alerts);
        appendGlucose(request.glucoseControl(), sections, summaries, orderRows, supplies, alerts);
        appendBloodProducts(request.bloodProducts(), sections, summaries, orderRows, supplies, alerts);
        alerts.add("VALIDAR INDICAÇÃO, DOSES, COMPATIBILIDADES E EXECUÇÃO COM OS PROTOCOLOS INSTITUCIONAIS.");

        return new TherapeuticSupportResponse(
            new TherapeuticSupportResponse.StructuredTherapeuticSupport(
                String.join(" + ", summaries) + ".",
                String.join("\n\n", sections),
                List.copyOf(orderRows)
            ),
            new TherapeuticSupportResponse.BillingAudit(
                List.copyOf(new LinkedHashSet<>(supplies)),
                List.copyOf(new LinkedHashSet<>(alerts))
            )
        );
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final TherapeuticSupportRequest request;
        try {
            request = objectMapper.convertValue(value, TherapeuticSupportRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados do suporte terapêutico estão em formato inválido");
        }
        TherapeuticSupportResponse response = preview(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", request);
        stored.put("structuredTherapeuticSupport", response.structuredTherapeuticSupport());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private void appendHydration(
        List<TherapeuticSupportRequest.HydrationSolution> values,
        List<String> sections,
        List<String> summaries,
        List<TherapeuticSupportResponse.OrderRow> orderRows,
        List<String> supplies,
        List<String> alerts
    ) {
        if (safe(values).isEmpty()) return;
        List<String> details = new ArrayList<>();
        for (TherapeuticSupportRequest.HydrationSolution value : values) {
            String base = label(TherapeuticSupportCatalogService.BASE_SOLUTIONS, value.baseSolution());
            String frequency = label(TherapeuticSupportCatalogService.HYDRATION_FREQUENCIES, value.frequency());
            String scheduling = label(TherapeuticSupportCatalogService.SCHEDULING_OPTIONS, value.scheduling());
            String administration = hydrationAdministration(value);
            details.add("- SOLUÇÃO " + value.id() + ":");
            details.add("  * " + base + ".");
            for (String additive : safe(value.additives())) {
                details.add("  * ADITIVO: " + label(TherapeuticSupportCatalogService.ELECTROLYTE_ADDITIVES, additive) + ".");
            }
            details.add("  * ADMINISTRAR VIA ENDOVENOSA — " + administration + ".");
            details.add("  * FREQUÊNCIA: " + frequency + ". APRAZAMENTO: " + scheduling + ".");
            summaries.add(base + " — " + administration);
            orderRows.add(new TherapeuticSupportResponse.OrderRow(
                "HIDRATAÇÃO / SOLUÇÕES",
                base + additivesSummary(value.additives()),
                "EV",
                frequency,
                scheduling
            ));
            addHydrationReview(value, base, supplies, alerts);
        }
        sections.add("7. HIDRATAÇÃO E SOLUÇÕES:\n" + String.join("\n", details));
    }

    private void appendGlucose(
        TherapeuticSupportRequest.GlucoseControl value,
        List<String> sections,
        List<String> summaries,
        List<TherapeuticSupportResponse.OrderRow> orderRows,
        List<String> supplies,
        List<String> alerts
    ) {
        if (value == null || !notBlank(value.frequency())) return;
        String frequency = label(TherapeuticSupportCatalogService.GLUCOSE_FREQUENCIES, value.frequency());
        String insulin = label(TherapeuticSupportCatalogService.INSULIN_TYPES, value.insulinType());
        List<String> details = new ArrayList<>();
        details.add("- MONITORIZAÇÃO GLICÊMICA:");
        details.add("  * AFERIR GLICEMIA CAPILAR (DXT) " + frequency + ".");
        details.add("- PROTOCOLO DE HIPOGLICEMIA (DXT < 70 MG/DL):");
        details.add("  * ADMINISTRAR GLICOSE 50% 40 ML (20 G) EV E REAVALIAR DXT EM 15 A 30 MINUTOS, CONFORME PROTOCOLO INSTITUCIONAL.");
        details.add("  * REGISTRAR A RESPOSTA E COMUNICAR A EQUIPE MÉDICA.");
        details.add("- ESCALA DE CORREÇÃO COM " + insulin + " SC:");
        details.add("  * DXT < 180 MG/DL: SEM AÇÃO.");
        details.add("  * DXT 180 A 220 MG/DL: 02 UI SC.");
        details.add("  * DXT 221 A 270 MG/DL: 04 UI SC.");
        details.add("  * DXT 271 A 320 MG/DL: 06 UI SC.");
        details.add("  * DXT 321 A 380 MG/DL: 08 UI SC.");
        details.add("  * DXT > 380 MG/DL: 10 UI SC E COMUNICAR A EQUIPE MÉDICA IMEDIATAMENTE.");
        if (Boolean.TRUE.equals(value.continuousPump())) {
            details.add("- BOMBA DE INSULINA CONTÍNUA:");
            details.add("  * INSULINA REGULAR 100 UI EM SF 0,9% 100 ML (1 UI/ML), EM BOMBA DE INFUSÃO.");
            details.add("  * TITULAÇÃO DA VAZÃO EXCLUSIVAMENTE CONFORME PROTOCOLO INSTITUCIONAL E DXT 1/1H.");
        }
        sections.add("8. CONTROLE GLICÊMICO E INSULINOTERAPIA:\n" + String.join("\n", details));
        summaries.add("DXT " + frequency + " COM PROTOCOLO E ESCALA SC" +
            (Boolean.TRUE.equals(value.continuousPump()) ? " + BIC" : ""));
        orderRows.add(new TherapeuticSupportResponse.OrderRow(
            "CONTROLE GLICÊMICO",
            "DXT + PROTOCOLO DE HIPOGLICEMIA + ESCALA DE " + insulin,
            "SC / EV CONFORME CONDUTA",
            frequency,
            "FIXO"
        ));
        Integer checks = GLUCOSE_CHECKS_PER_DAY.get(value.frequency());
        if (checks == null) {
            supplies.add("FITAS REAGENTES E LANCETAS — CONFERIR QUANTIDADE EFETIVAMENTE UTILIZADA.");
        } else {
            supplies.add("ESTIMATIVA EM 24H: " + checks + " FITAS REAGENTES E " + checks +
                " LANCETAS — CONFERIR EXECUÇÃO REGISTRADA.");
        }
        supplies.add("INSULINA, SERINGAS E AGULHAS — CONFERIR SOMENTE O CONSUMO ADMINISTRADO E REGISTRADO.");
        supplies.add("GLICOSE 50% — CONFERIR CONSUMO SOMENTE SE O PROTOCOLO FOR ACIONADO.");
        if (Boolean.TRUE.equals(value.continuousPump())) {
            supplies.add("SF 0,9% 100 ML, INSULINA REGULAR E EQUIPO PARA BOMBA — CONFERIR PREPARO E USO REGISTRADOS.");
            supplies.add("BOMBA DE INFUSÃO — CONFERIR DISPONIBILIDADE, PERÍODO DE USO E REGRA CONTRATUAL.");
        }
        alerts.add("INSULINA É MEDICAMENTO DE ALTA VIGILÂNCIA: EXIGIR DUPLA CHECAGEM DE PREPARO, DOSE E ADMINISTRAÇÃO.");
        alerts.add("DXT > 380 MG/DL: COMUNICAR IMEDIATAMENTE E AVALIAR CAD / EHH CONFORME PROTOCOLO CLÍNICO.");
    }

    private void appendBloodProducts(
        List<TherapeuticSupportRequest.BloodProduct> values,
        List<String> sections,
        List<String> summaries,
        List<TherapeuticSupportResponse.OrderRow> orderRows,
        List<String> supplies,
        List<String> alerts
    ) {
        if (safe(values).isEmpty()) return;
        List<String> details = new ArrayList<>();
        for (TherapeuticSupportRequest.BloodProduct value : values) {
            boolean plasmaDerivative = catalog.isPlasmaDerivative(value.product());
            String product = catalog.productLabel(value.product());
            String modifications = labels(TherapeuticSupportCatalogService.BLOOD_MODIFICATIONS,
                value.modifications());
            String description = product + (modifications.isBlank() ? "" : " — " + modifications);
            String quantity = value.quantity() + " " +
                label(TherapeuticSupportCatalogService.QUANTITY_UNITS, value.quantityUnit());
            String route = label(TherapeuticSupportCatalogService.TRANSFUSION_ROUTES, value.route());
            String scheduling = label(TherapeuticSupportCatalogService.SCHEDULING_OPTIONS, value.scheduling());
            details.add("- " + (plasmaDerivative ? "HEMODERIVADO " : "HEMOCOMPONENTE ") + value.id() + ":");
            details.add("  * " + description + " — " + quantity + ".");
            details.add("  * ADMINISTRAR POR " + route + " EM " + value.infusionMinutes() + " MINUTOS.");
            for (String medication : safe(value.preMedications())) {
                details.add("  * PRÉ-MEDICAÇÃO, SE CLINICAMENTE INDICADA: " +
                    label(TherapeuticSupportCatalogService.PRE_MEDICATIONS, medication) + ".");
            }
            details.add("  * APRAZAMENTO: " + scheduling + ".");
            if (!plasmaDerivative) {
                details.add("  * USAR EQUIPO DE TRANSFUSÃO COM FILTRO; TEMPO TOTAL MÁXIMO DE 4 HORAS POR BOLSA.");
                details.add("  * CONFERIR IDENTIFICAÇÃO, BOLSA, VALIDADE, INSPEÇÃO VISUAL E TESTES PRÉ-TRANSFUSIONAIS APLICÁVEIS.");
                details.add("  * VERIFICAR SSVV ANTES, AOS 15 MINUTOS E AO TÉRMINO; ACOMPANHAR À BEIRA-LEITO NOS PRIMEIROS 10 MINUTOS.");
            }
            summaries.add(quantity + " DE " + description + " EM " + value.infusionMinutes() + " MIN");
            orderRows.add(new TherapeuticSupportResponse.OrderRow(
                plasmaDerivative ? "HEMODERIVADO" : "TRANSFUSÃO",
                description + " — " + quantity,
                route,
                "CORRER EM " + value.infusionMinutes() + " MIN",
                scheduling
            ));
            addBloodProductReview(value, description, quantity, plasmaDerivative, supplies, alerts);
        }
        sections.add("10. HEMODERIVADOS E TRANSFUSÕES:\n" + String.join("\n", details));
    }

    private String hydrationAdministration(TherapeuticSupportRequest.HydrationSolution value) {
        return switch (value.infusionMode()) {
            case "PUMP" -> "BOMBA DE INFUSÃO A " + decimal(value.rateValue()) + " ML/H";
            case "MACRODRIP" -> "EQUIPO MACROGOTAS A " + decimal(value.rateValue()) + " GOTAS/MIN";
            case "RAPID_30_MIN" -> "EXPANSÃO RÁPIDA, CORRER EM 30 MINUTOS";
            case "RAPID_60_MIN" -> "EXPANSÃO RÁPIDA, CORRER EM 60 MINUTOS";
            default -> throw new BusinessValidationException("Modalidade de infusão não reconhecida");
        };
    }

    private void addHydrationReview(
        TherapeuticSupportRequest.HydrationSolution value,
        String base,
        List<String> supplies,
        List<String> alerts
    ) {
        Integer daily = HYDRATION_ADMINISTRATIONS_PER_DAY.get(value.frequency());
        supplies.add((daily == null ? "CONFERIR QUANTIDADE UTILIZADA DE " : "ESTIMATIVA EM 24H: " + daily + " X ") +
            base + " — VALIDAR ADMINISTRAÇÃO REGISTRADA.");
        for (String additive : safe(value.additives())) {
            supplies.add(label(TherapeuticSupportCatalogService.ELECTROLYTE_ADDITIVES, additive) +
                " — CONFERIR QUANTIDADE EFETIVAMENTE PREPARADA E ADMINISTRADA.");
        }
        supplies.add(("PUMP".equals(value.infusionMode()) ? "EQUIPO PARA BOMBA DE INFUSÃO" : "EQUIPO MACROGOTAS") +
            " — CONFERIR USO REGISTRADO.");
        if ("PUMP".equals(value.infusionMode())) {
            supplies.add("BOMBA DE INFUSÃO — CONFERIR DISPONIBILIDADE, PERÍODO DE USO E REGRA CONTRATUAL.");
        }
        if (safe(value.additives()).stream().anyMatch(code -> code.startsWith("KCL"))) {
            alerts.add("KCL CONCENTRADO É MEDICAMENTO DE ALTA VIGILÂNCIA: EXIGIR DUPLA CHECAGEM, DILUIÇÃO, VAZÃO E AVALIAÇÃO CLÍNICA / LABORATORIAL.");
        }
        if (!safe(value.additives()).isEmpty()) {
            alerts.add("ELETRÓLITOS CONCENTRADOS EXIGEM DUPLA CHECAGEM E VALIDAÇÃO DE COMPATIBILIDADE DA SOLUÇÃO.");
        }
    }

    private void addBloodProductReview(
        TherapeuticSupportRequest.BloodProduct value,
        String description,
        String quantity,
        boolean plasmaDerivative,
        List<String> supplies,
        List<String> alerts
    ) {
        supplies.add(quantity + " DE " + description + " — CONFERIR DISPENSAÇÃO E ADMINISTRAÇÃO REGISTRADAS.");
        for (String medication : safe(value.preMedications())) {
            supplies.add(label(TherapeuticSupportCatalogService.PRE_MEDICATIONS, medication) +
                " — CONFERIR SOMENTE SE ADMINISTRADA E REGISTRADA.");
        }
        if (plasmaDerivative) return;
        supplies.add("EQUIPO DE TRANSFUSÃO COM FILTRO PARA COÁGULOS E AGREGADOS — CONFERIR USO REGISTRADO.");
        supplies.add("TESTES IMUNO-HEMATOLÓGICOS APLICÁVEIS AO COMPONENTE — CONFERIR EXECUÇÃO E REGRA CONTRATUAL.");
        if ("RBC".equals(value.product())) {
            alerts.add("CONCENTRADO DE HEMÁCIAS: CONFIRMAR COMPATIBILIDADE ABO/RH, PAI E PROVA CRUZADA ANTES DA LIBERAÇÃO.");
        } else {
            alerts.add("CONFIRMAR TESTES PRÉ-TRANSFUSIONAIS E COMPATIBILIDADE APLICÁVEIS AO HEMOCOMPONENTE SELECIONADO.");
        }
        alerts.add("A INFUSÃO DO HEMOCOMPONENTE NÃO PODE ULTRAPASSAR 4 HORAS; REGISTRAR INÍCIO, TÉRMINO E EVENTUAIS REAÇÕES.");
    }

    private String additivesSummary(List<String> additives) {
        String values = labels(TherapeuticSupportCatalogService.ELECTROLYTE_ADDITIVES, additives);
        return values.isBlank() ? "" : " + " + values;
    }

    private String labels(List<TherapeuticSupportCatalog.Option> options, List<String> codes) {
        return String.join(" + ", safe(codes).stream().map(code -> label(options, code)).toList());
    }

    private String label(List<TherapeuticSupportCatalog.Option> options, String code) {
        return catalog.label(options, code);
    }

    private String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        Map<?, ?> root = (Map<?, ?>) value;
        allowedKeys(root, Set.of("clinicalContext", "hydrationSolutions", "glucoseControl", "bloodProducts"));
        nestedList(root.get("hydrationSolutions"), Set.of(
            "id", "baseSolution", "additives", "route", "frequency", "infusionMode",
            "rateValue", "rateUnit", "scheduling"
        ));
        nested(root.get("glucoseControl"), Set.of(
            "frequency", "hypoglycemiaProtocolActive", "correctionScaleActive", "insulinType",
            "continuousPump"
        ));
        nestedList(root.get("bloodProducts"), Set.of(
            "id", "product", "modifications", "quantity", "quantityUnit", "route",
            "infusionMinutes", "preMedications", "scheduling"
        ));
    }

    private void nested(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        Map<?, ?> map = (Map<?, ?>) value;
        allowedKeys(map, allowed);
    }

    private void nestedList(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof List<?>)) invalidFormat();
        List<?> list = (List<?>) value;
        for (Object item : list) nested(item, allowed);
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("O suporte terapêutico contém campos não permitidos");
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void invalidFormat() {
        throw new BusinessValidationException("Dados do suporte terapêutico estão em formato inválido");
    }
}
