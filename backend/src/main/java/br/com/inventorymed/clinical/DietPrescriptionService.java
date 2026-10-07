package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class DietPrescriptionService {

    private static final Pattern FIRST_NUMBER = Pattern.compile("(\\d+)");

    private static final List<String> ORAL_CONSISTENCIES = List.of(
        "LIVRE / GERAL", "BRANDA", "PASTOSA", "LÍQUIDA COMPLETA", "LÍQUIDA RESTRITA", "ZERO"
    );
    private static final List<String> ORAL_RESTRICTIONS = List.of(
        "HIPOSSÓDICA (HAS / CARDIOLOGIA)",
        "HIPOGLÍDICA / DIABÉTICA (DM)",
        "HIPOPROTEICA / PARA INSUFICIÊNCIA RENAL (SEM DIÁLISE)",
        "HIPERPROTEICA / PARA DOENTE CRÍTICO OU DIÁLISE",
        "HIPOLIPÍDICA / HEPÁTICA",
        "LAXATIVA / RICA EM FIBRAS",
        "OBSTIPANTE / SEM RESÍDUOS",
        "SEM LACTOSE / SEM GLÚTEN"
    );
    private static final List<String> ENTERAL_ACCESS_ROUTES = List.of(
        "SONDA NASOENTÉRICA (SNE)", "SONDA NASOGÁSTRICA (SNG)",
        "GASTROSTOMIA (GTT)", "JEJUNOSTOMIA"
    );
    private static final List<String> ENTERAL_REGIMENS = List.of(
        "CONTÍNUO EM BOMBA DE INFUSÃO", "INTERMITENTE / BOLUS"
    );
    private static final List<String> ENTERAL_FORMULAS = List.of(
        "POLIMÉRICA NORMOCALÓRICA NORMOPROTEICA",
        "POLIMÉRICA HIPERCALÓRICA / HIPERPROTEICA",
        "OLIGOMÉRICA / SEMIELEMENTAR",
        "ESPECÍFICA RENAL",
        "ESPECÍFICA DIABÉTICA"
    );
    private static final List<String> PARENTERAL_ACCESS_ROUTES = List.of(
        "ACESSO VENOSO CENTRAL (CVC)", "ACESSO VENOSO PERIFÉRICO"
    );
    private static final List<String> PARENTERAL_PREPARATIONS = List.of(
        "INDUSTRIALIZADA PRONTA (3 EM 1)", "MANIPULADA INDIVIDUALIZADA"
    );
    private static final List<String> FASTING_REASONS = List.of(
        "PRÉ-OPERATÓRIO / PROCEDIMENTO",
        "EXAME DIAGNÓSTICO",
        "INSTABILIDADE HEMODINÂMICA",
        "ÍLEO PARALÍTICO / OBSTRUÇÃO INTESTINAL",
        "SANGRAMENTO DIGESTIVO AGUDO"
    );

    private final ObjectMapper objectMapper;

    public DietPrescriptionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DietPrescriptionCatalog catalog() {
        return new DietPrescriptionCatalog(
            ORAL_CONSISTENCIES,
            ORAL_RESTRICTIONS,
            ENTERAL_ACCESS_ROUTES,
            ENTERAL_REGIMENS,
            ENTERAL_FORMULAS,
            PARENTERAL_ACCESS_ROUTES,
            PARENTERAL_PREPARATIONS,
            FASTING_REASONS
        );
    }

    public DietPrescriptionResponse preview(DietPrescriptionRequest request) {
        validate(request);
        return switch (request.type()) {
            case ORAL -> oral(request.oral());
            case ENTERAL -> enteral(request.enteral());
            case PARENTERAL -> parenteral(request.parenteral());
            case JEJUM -> fasting(request.fasting());
        };
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final DietPrescriptionRequest request;
        try {
            request = objectMapper.convertValue(value, DietPrescriptionRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados da dieta estão em formato inválido");
        }
        DietPrescriptionResponse response = preview(request);
        DietPrescriptionRequest activeRequest = activeParametersOnly(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", activeRequest);
        stored.put("structuredDiet", response.structuredDiet());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new BusinessValidationException("Dados da dieta estão em formato inválido");
        }
        allowedKeys(map, Set.of("type", "oral", "enteral", "parenteral", "fasting"));
        nestedKeys(map.get("oral"), Set.of("consistency", "restrictions"));
        nestedKeys(map.get("enteral"), Set.of(
            "accessRoute", "infusionRegimen", "formulaType", "rateMlHour",
            "bolusVolumeMl", "bolusFrequency", "tubeFlushMl", "flushInterval"
        ));
        nestedKeys(map.get("parenteral"), Set.of(
            "accessRoute", "preparationType", "totalVolumeMl", "rateMlHour",
            "totalCaloriesKcalDay", "proteinGoalGramsKgDay",
            "gastrointestinalFailureJustification"
        ));
        nestedKeys(map.get("fasting"), Set.of("reason", "reassessment"));
    }

    private void nestedKeys(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof Map<?, ?> map)) {
            throw new BusinessValidationException("Dados da dieta estão em formato inválido");
        }
        allowedKeys(map, allowed);
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("A dieta contém campos não permitidos");
        }
    }

    private DietPrescriptionRequest activeParametersOnly(DietPrescriptionRequest request) {
        return switch (request.type()) {
            case ORAL -> new DietPrescriptionRequest(DietType.ORAL, request.oral(), null, null, null);
            case ENTERAL -> new DietPrescriptionRequest(DietType.ENTERAL, null, request.enteral(), null, null);
            case PARENTERAL -> new DietPrescriptionRequest(DietType.PARENTERAL, null, null, request.parenteral(), null);
            case JEJUM -> new DietPrescriptionRequest(DietType.JEJUM, null, null, null, request.fasting());
        };
    }

    private DietPrescriptionResponse oral(DietPrescriptionRequest.OralParameters oral) {
        String restrictions = oral.restrictions() == null || oral.restrictions().isEmpty()
            ? ""
            : ", " + String.join(" E ", oral.restrictions());
        String summary = "DIETA ORAL " + oral.consistency() + restrictions + ".";
        String details = "1. DIETA ORAL:\n- CONSISTÊNCIA: " + oral.consistency() + ".";
        if (!restrictions.isBlank()) {
            details += "\n- MODIFICADORES NUTRICIONAIS: " + String.join("; ", oral.restrictions()) + ".";
        }
        details += "\n- ORIENTAÇÃO: ESTIMULAR E REGISTRAR A ACEITAÇÃO ORAL CONFORME A ROTINA ASSISTENCIAL.";

        List<String> items = oral.restrictions() == null || oral.restrictions().isEmpty()
            ? List.of()
            : List.of("DIETA HOSPITALAR ESPECIALIZADA — CONFERIR COBERTURA E CONTRATO");
        List<String> alerts = oral.restrictions() == null || oral.restrictions().isEmpty()
            ? List.of()
            : List.of("CONFIRMAR O REGISTRO DA DIETA ESPECIAL E A ADEQUAÇÃO DO CARDÁPIO.");
        return response(DietType.ORAL, summary, details, items, alerts);
    }

    private DietPrescriptionResponse enteral(DietPrescriptionRequest.EnteralParameters enteral) {
        boolean continuous = "CONTÍNUO EM BOMBA DE INFUSÃO".equals(enteral.infusionRegimen());
        String administration = continuous
            ? "CONTÍNUA EM BOMBA DE INFUSÃO A " + decimal(enteral.rateMlHour()) + " ML/H"
            : "INTERMITENTE / BOLUS DE " + decimal(enteral.bolusVolumeMl()) + " ML " + enteral.bolusFrequency();
        String hydration = enteral.tubeFlushMl() == null
            ? ""
            : " + ÁGUA " + decimal(enteral.tubeFlushMl()) + " ML VIA SONDA " + enteral.flushInterval();
        String summary = "DIETA ENTERAL VIA " + enteral.accessRoute() + " (" +
            enteral.formulaType() + ") — " + administration + hydration + ".";
        String details = "1. DIETA ENTERAL VIA " + enteral.accessRoute() + ":\n" +
            "- FORMULAÇÃO: " + enteral.formulaType() + ".\n" +
            "- ADMINISTRAÇÃO: " + administration + ".";
        if (enteral.tubeFlushMl() != null) {
            details += "\n- LAVAGEM DA SONDA: " + decimal(enteral.tubeFlushMl()) +
                " ML DE ÁGUA VIA SONDA " + enteral.flushInterval() +
                " E APÓS MEDICAÇÕES, CONFORME PROTOCOLO INSTITUCIONAL.";
        }

        List<String> items = new ArrayList<>();
        items.add("EQUIPO E FRASCO/BOLSA DE NUTRIÇÃO ENTERAL — CONFERIR LANÇAMENTO");
        if (continuous) items.add("BOMBA DE INFUSÃO — CONFERIR TAXA DIÁRIA E DISPONIBILIDADE");
        List<String> alerts = List.of(
            "CONFIRMAR POSICIONAMENTO, FIXAÇÃO E PERMEABILIDADE DA SONDA ANTES DO INÍCIO.",
            "REGISTRAR VOLUME ADMINISTRADO E EVENTUAIS INTERRUPÇÕES."
        );
        return response(DietType.ENTERAL, summary, details, items, alerts);
    }

    private DietPrescriptionResponse parenteral(DietPrescriptionRequest.ParenteralParameters parenteral) {
        String summary = "NUTRIÇÃO PARENTERAL VIA " + parenteral.accessRoute() + " — " +
            parenteral.preparationType() + ", " + decimal(parenteral.totalVolumeMl()) +
            " ML/24H A " + decimal(parenteral.rateMlHour()) + " ML/H EM BOMBA EXCLUSIVA.";
        String details = "1. NUTRIÇÃO PARENTERAL:\n" +
            "- VIA: " + parenteral.accessRoute() + ".\n" +
            "- TIPO: " + parenteral.preparationType() + ".\n" +
            "- VOLUME TOTAL: " + decimal(parenteral.totalVolumeMl()) + " ML/24H.\n" +
            "- INFUSÃO: " + decimal(parenteral.rateMlHour()) + " ML/H EM BOMBA DE INFUSÃO DEDICADA.";
        if (parenteral.totalCaloriesKcalDay() != null) {
            details += "\n- META CALÓRICA INFORMADA: " + decimal(parenteral.totalCaloriesKcalDay()) + " KCAL/DIA.";
        }
        if (parenteral.proteinGoalGramsKgDay() != null) {
            details += "\n- META PROTEICA INFORMADA: " + decimal(parenteral.proteinGoalGramsKgDay()) + " G/KG/DIA.";
        }
        details += "\n- JUSTIFICATIVA CLÍNICA: " + parenteral.gastrointestinalFailureJustification().trim() + ".";
        return response(
            DietType.PARENTERAL,
            summary,
            details,
            List.of(
                "BOMBA DE INFUSÃO DEDICADA — CONFERIR TAXA DIÁRIA",
                "EQUIPO PARENTERAL E BOLSA DE NUTRIÇÃO PARENTERAL — CONFERIR LANÇAMENTO"
            ),
            List.of(
                "REVISAR INDICAÇÃO, VIA, COMPATIBILIDADE, MONITORAMENTO E JUSTIFICATIVA CLÍNICA COM A EMTN.",
                "A CLASSIFICAÇÃO E A COBRANÇA DEPENDEM DO CONTRATO E DAS REGRAS VIGENTES DA INSTITUIÇÃO."
            )
        );
    }

    private DietPrescriptionResponse fasting(DietPrescriptionRequest.FastingParameters fasting) {
        String summary = "JEJUM / NADA POR VIA ORAL (NVO) — " + fasting.reason() +
            ". REAVALIAR: " + fasting.reassessment() + ".";
        String details = "1. JEJUM / NADA POR VIA ORAL (NVO):\n" +
            "- MOTIVO: " + fasting.reason() + ".\n" +
            "- PREVISÃO DE REAVALIAÇÃO: " + fasting.reassessment() + ".";
        List<String> alerts = new ArrayList<>();
        alerts.add("REGISTRAR A REAVALIAÇÃO E EVITAR A MANUTENÇÃO DO JEJUM SEM NOVA JUSTIFICATIVA.");
        Matcher number = FIRST_NUMBER.matcher(fasting.reassessment());
        if (number.find() && Integer.parseInt(number.group(1)) >= 24) {
            alerts.add("REAVALIAR RISCO NUTRICIONAL COM A EMTN SE O JEJUM ATINGIR OU ULTRAPASSAR 24 HORAS.");
        }
        return response(DietType.JEJUM, summary, details, List.of(), alerts);
    }

    private DietPrescriptionResponse response(
        DietType type,
        String summary,
        String details,
        List<String> items,
        List<String> alerts
    ) {
        return new DietPrescriptionResponse(
            new DietPrescriptionResponse.StructuredDiet(type, summary, details),
            new DietPrescriptionResponse.BillingAudit(List.copyOf(items), List.copyOf(alerts))
        );
    }

    private void validate(DietPrescriptionRequest request) {
        if (request == null || request.type() == null) {
            throw new BusinessValidationException("Selecione o tipo de dieta");
        }
        switch (request.type()) {
            case ORAL -> validateOral(request.oral());
            case ENTERAL -> validateEnteral(request.enteral());
            case PARENTERAL -> validateParenteral(request.parenteral());
            case JEJUM -> validateFasting(request.fasting());
        }
    }

    private void validateOral(DietPrescriptionRequest.OralParameters oral) {
        if (oral == null) required("Informe os parâmetros da dieta oral");
        allowed(oral.consistency(), ORAL_CONSISTENCIES, "consistência da dieta oral");
        List<String> restrictions = oral.restrictions() == null ? List.of() : oral.restrictions();
        if (restrictions.size() > 8 || Set.copyOf(restrictions).size() != restrictions.size()) {
            throw new BusinessValidationException("As restrições da dieta oral são inválidas");
        }
        restrictions.forEach(value -> allowed(value, ORAL_RESTRICTIONS, "restrição da dieta oral"));
    }

    private void validateEnteral(DietPrescriptionRequest.EnteralParameters enteral) {
        if (enteral == null) required("Informe os parâmetros da dieta enteral");
        allowed(enteral.accessRoute(), ENTERAL_ACCESS_ROUTES, "via da dieta enteral");
        allowed(enteral.infusionRegimen(), ENTERAL_REGIMENS, "regime da dieta enteral");
        allowed(enteral.formulaType(), ENTERAL_FORMULAS, "fórmula da dieta enteral");
        if ("CONTÍNUO EM BOMBA DE INFUSÃO".equals(enteral.infusionRegimen())) {
            positive(enteral.rateMlHour(), "Informe uma vazão válida em ML/H");
        } else {
            positive(enteral.bolusVolumeMl(), "Informe um volume válido por bolus");
            text(enteral.bolusFrequency(), 40, "Informe a frequência dos bolus");
        }
        if (enteral.tubeFlushMl() != null) {
            positive(enteral.tubeFlushMl(), "Informe um volume válido para lavagem da sonda");
            text(enteral.flushInterval(), 40, "Informe o intervalo da lavagem da sonda");
        } else if (enteral.flushInterval() != null && !enteral.flushInterval().isBlank()) {
            throw new BusinessValidationException("Informe o volume da lavagem da sonda");
        }
    }

    private void validateParenteral(DietPrescriptionRequest.ParenteralParameters parenteral) {
        if (parenteral == null) required("Informe os parâmetros da nutrição parenteral");
        allowed(parenteral.accessRoute(), PARENTERAL_ACCESS_ROUTES, "via da nutrição parenteral");
        allowed(parenteral.preparationType(), PARENTERAL_PREPARATIONS, "tipo da nutrição parenteral");
        positive(parenteral.totalVolumeMl(), "Informe o volume total em ML/24H");
        positive(parenteral.rateMlHour(), "Informe a velocidade de infusão em ML/H");
        optionalPositive(parenteral.totalCaloriesKcalDay(), "A meta calórica deve ser positiva");
        optionalPositive(parenteral.proteinGoalGramsKgDay(), "A meta proteica deve ser positiva");
        text(
            parenteral.gastrointestinalFailureJustification(),
            1000,
            "Informe a justificativa clínica para a nutrição parenteral"
        );
    }

    private void validateFasting(DietPrescriptionRequest.FastingParameters fasting) {
        if (fasting == null) required("Informe os parâmetros do jejum");
        allowed(fasting.reason(), FASTING_REASONS, "motivo do jejum");
        text(fasting.reassessment(), 120, "Informe a previsão de reavaliação do jejum");
    }

    private void allowed(String value, List<String> allowed, String label) {
        if (value == null || !allowed.contains(value)) {
            throw new BusinessValidationException("Selecione uma opção válida para " + label);
        }
    }

    private void positive(BigDecimal value, String message) {
        if (value == null || value.signum() <= 0 || value.compareTo(new BigDecimal("100000")) > 0) {
            throw new BusinessValidationException(message);
        }
    }

    private void optionalPositive(BigDecimal value, String message) {
        if (value != null && value.signum() <= 0) throw new BusinessValidationException(message);
    }

    private void text(String value, int maximumLength, String message) {
        if (value == null || value.isBlank() || value.trim().length() > maximumLength) {
            throw new BusinessValidationException(message);
        }
    }

    private void required(String message) {
        throw new BusinessValidationException(message);
    }

    private String decimal(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
