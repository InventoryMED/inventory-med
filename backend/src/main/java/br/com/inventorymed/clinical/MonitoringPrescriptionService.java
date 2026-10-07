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
public class MonitoringPrescriptionService {

    private static final List<String> VITAL_SIGNS_FREQUENCIES = List.of(
        "12/12H", "8/8H", "6/6H", "4/4H", "2/2H", "1/1H",
        "CONTÍNUO / MONITORIZAÇÃO MULTIPARAMÉTRICA EM UTI"
    );
    private static final List<String> PAIN_SCALES = List.of("EVA", "BPS", "CPOT");
    private static final List<String> CONSCIOUSNESS_SEDATION_SCALES = List.of("GLASGOW", "RASS", "SAS");
    private static final List<String> FALL_RISK_SCALES = List.of("MORSE", "HUMPTY DUMPTY");
    private static final List<String> GLUCOSE_MONITORING_FREQUENCIES = List.of(
        "ANTES DAS REFEIÇÕES E AO DEITAR (07H / 11H / 17H / 22H)",
        "6/6H", "4/4H", "2/2H", "1/1H (BOMBA DE INSULINA)"
    );
    private static final Map<String, Integer> GLUCOSE_CHECKS_PER_DAY = Map.of(
        "ANTES DAS REFEIÇÕES E AO DEITAR (07H / 11H / 17H / 22H)", 4,
        "6/6H", 4,
        "4/4H", 6,
        "2/2H", 12,
        "1/1H (BOMBA DE INSULINA)", 24
    );
    private static final List<String> INSULIN_TYPES = List.of("REGULAR", "ULTRARRÁPIDA");
    private static final List<String> FLUID_BALANCE_OPTIONS = List.of(
        "SEM INDICAÇÃO", "BALANÇO HÍDRICO DE 24H", "BALANÇO HÍDRICO PARCIAL 6/6H",
        "BALANÇO HÍDRICO RIGOROSO 1/1H"
    );
    private static final List<String> URINE_OUTPUT_OPTIONS = List.of(
        "DIURESE ESPONTÂNEA", "SVD COM URÔMETRO", "SVD COM BOLSA COLETORA GRADUADA"
    );
    private static final List<String> DRAINS_TUBES = List.of(
        "DRENO TORÁCICO", "DRENO PORTOVAC", "DRENO DE PENROSE", "DRENO BLAKE",
        "SNG / SNE ABERTA PARA FRASCO COLETOR"
    );
    private static final List<String> OTHER_MEASUREMENTS = List.of(
        "CIRCUNFERÊNCIA ABDOMINAL 24/24H", "PESO DIÁRIO"
    );
    private static final List<String> HEMODYNAMIC_MONITORING = List.of(
        "PRESSÃO ARTERIAL INVASIVA (PAI)", "PRESSÃO VENOSA CENTRAL (PVC)", "CATETER DE SWAN-GANZ"
    );
    private static final List<String> NEUROLOGICAL_MONITORING = List.of(
        "PRESSÃO INTRACRANIANA (PIC)", "PRESSÃO DE PERFUSÃO CEREBRAL (PPC)"
    );

    private final ObjectMapper objectMapper;

    public MonitoringPrescriptionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MonitoringPrescriptionCatalog catalog() {
        return new MonitoringPrescriptionCatalog(
            VITAL_SIGNS_FREQUENCIES,
            PAIN_SCALES,
            CONSCIOUSNESS_SEDATION_SCALES,
            FALL_RISK_SCALES,
            GLUCOSE_MONITORING_FREQUENCIES,
            INSULIN_TYPES,
            FLUID_BALANCE_OPTIONS,
            URINE_OUTPUT_OPTIONS,
            DRAINS_TUBES,
            OTHER_MEASUREMENTS,
            HEMODYNAMIC_MONITORING,
            NEUROLOGICAL_MONITORING
        );
    }

    public MonitoringPrescriptionResponse preview(MonitoringPrescriptionRequest request) {
        validate(request);
        List<String> details = new ArrayList<>();
        List<String> summary = new ArrayList<>();
        List<String> review = new ArrayList<>();
        List<String> alerts = new ArrayList<>();

        appendVitalSigns(request.vitalSigns(), details, summary, review, alerts);
        appendGlucoseMonitoring(request.glucoseMonitoring(), details, summary, review, alerts);
        appendFluidBalance(request.fluidBalanceOutputs(), details, summary, review, alerts);
        appendInvasiveMonitoring(request.invasiveMonitoring(), details, summary, review, alerts);

        return new MonitoringPrescriptionResponse(
            new MonitoringPrescriptionResponse.StructuredMonitoring(
                "MONITORIZAÇÃO E CONTROLES GLOBAIS: " + String.join("; ", summary) + ".",
                "MONITORIZAÇÃO E CONTROLES GLOBAIS:\n" + String.join("\n", details)
            ),
            new MonitoringPrescriptionResponse.BillingAudit(
                List.copyOf(new LinkedHashSet<>(review)),
                List.copyOf(new LinkedHashSet<>(alerts))
            )
        );
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final MonitoringPrescriptionRequest request;
        try {
            request = objectMapper.convertValue(value, MonitoringPrescriptionRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados da monitorização estão em formato inválido");
        }
        MonitoringPrescriptionResponse response = preview(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", request);
        stored.put("structuredMonitoring", response.structuredMonitoring());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private void appendVitalSigns(
        MonitoringPrescriptionRequest.VitalSigns value,
        List<String> details,
        List<String> summary,
        List<String> review,
        List<String> alerts
    ) {
        if (value == null || !notBlank(value.frequency())) return;
        details.add("1. SINAIS VITAIS (SSVV) — " + value.frequency() +
            ": AFERIR PA, FC, FR, SPO₂ E TEMPERATURA.");
        summary.add("SSVV " + value.frequency());
        addScale(details, summary, "DOR", value.painScale());
        addScale(details, summary, "CONSCIÊNCIA / SEDAÇÃO", value.consciousnessSedationScale());
        addScale(details, summary, "RISCO DE QUEDA", value.fallRiskScale());
        if (value.frequency().contains("CONTÍNUO")) {
            review.add("MONITOR MULTIPARAMÉTRICO — CONFERIR DISPONIBILIDADE, USO REGISTRADO E REGRAS CONTRATUAIS.");
        }
        alerts.add("REGISTRAR OS PARÂMETROS AFERIDOS E COMUNICAR ALTERAÇÕES CONFORME O PROTOCOLO INSTITUCIONAL.");
    }

    private void appendGlucoseMonitoring(
        MonitoringPrescriptionRequest.GlucoseMonitoring value,
        List<String> details,
        List<String> summary,
        List<String> review,
        List<String> alerts
    ) {
        if (value == null || !notBlank(value.frequency())) return;
        details.add("2. DXT — " + value.frequency() + ".");
        summary.add("DXT " + value.frequency());
        int checks = GLUCOSE_CHECKS_PER_DAY.get(value.frequency());
        review.add("ESTIMATIVA DIÁRIA PARA CONFERÊNCIA: " + checks + " FITAS REAGENTES E " + checks +
            " LANCETAS; VALIDAR EXECUÇÃO REGISTRADA E REGRAS CONTRATUAIS.");

        if (Boolean.TRUE.equals(value.hypoglycemiaProtocol())) {
            details.add("- PROTOCOLO DE HIPOGLICEMIA: SE DXT < 70 MG/DL, ADMINISTRAR GLICOSE 50% " +
                "40 ML (20 G) EV IMEDIATAMENTE E REPETIR DXT APÓS 15 A 30 MINUTOS ATÉ NORMALIZAÇÃO, " +
                "CONFORME PROTOCOLO INSTITUCIONAL.");
            summary.add("PROTOCOLO DE HIPOGLICEMIA");
            review.add("GLICOSE 50% — CONFERIR DISPONIBILIDADE E REGISTRAR CONSUMO SOMENTE SE ADMINISTRADA.");
            alerts.add("REGISTRAR O VALOR, A CONDUTA, A REAVALIAÇÃO E A RESPOSTA AO PROTOCOLO DE HIPOGLICEMIA.");
        }
        if (Boolean.TRUE.equals(value.slidingScale())) {
            details.add("- INSULINA " + value.insulinType() + " SC CONFORME DXT: " +
                "180–230 MG/DL: 2 UI; 231–280 MG/DL: 4 UI; 281–330 MG/DL: 6 UI; " +
                "331–380 MG/DL: 8 UI; > 380 MG/DL: 10 UI E COMUNICAR A EQUIPE MÉDICA.");
            summary.add("ESCALA DE INSULINA " + value.insulinType());
            review.add("INSULINA, SERINGA E AGULHA — CONFERIR CONSUMO SOMENTE APÓS ADMINISTRAÇÃO REGISTRADA.");
            alerts.add("DXT > 380 MG/DL: COMUNICAR A EQUIPE MÉDICA E AVALIAR RISCO DE CAD / EHH CONFORME PROTOCOLO.");
        }
    }

    private void appendFluidBalance(
        MonitoringPrescriptionRequest.FluidBalanceOutputs value,
        List<String> details,
        List<String> summary,
        List<String> review,
        List<String> alerts
    ) {
        if (value == null) return;
        if (notBlank(value.fluidBalance())) {
            details.add("3. BALANÇO HÍDRICO: " + value.fluidBalance() + ".");
            summary.add(value.fluidBalance());
        }
        if (notBlank(value.urineOutput())) {
            details.add("- DIURESE: " + value.urineOutput() + ".");
            summary.add(value.urineOutput());
        }
        addList(details, summary, "DRENOS E SONDAS", value.drainsTubes());
        addList(details, summary, "OUTRAS MENSURAÇÕES", value.otherMeasurements());

        if ("BALANÇO HÍDRICO RIGOROSO 1/1H".equals(value.fluidBalance())) {
            review.add("MATERIAL PARA BALANÇO HÍDRICO RIGOROSO — CONFERIR DISPONIBILIDADE E USO REGISTRADO.");
            alerts.add("REGISTRAR ENTRADAS, SAÍDAS E SALDO HORÁRIO DO BALANÇO HÍDRICO.");
        }
        if ("SVD COM URÔMETRO".equals(value.urineOutput())) {
            review.add("URÔMETRO — CONFERIR DISPONIBILIDADE, INSTALAÇÃO E USO REGISTRADO.");
        }
        if ("SVD COM BOLSA COLETORA GRADUADA".equals(value.urineOutput())) {
            review.add("BOLSA COLETORA GRADUADA — CONFERIR DISPONIBILIDADE E USO REGISTRADO.");
        }
        if (!safe(value.drainsTubes()).isEmpty()) {
            alerts.add("REGISTRAR O DÉBITO E O ASPECTO DE CADA DRENO OU SONDA SELECIONADO.");
        }
    }

    private void appendInvasiveMonitoring(
        MonitoringPrescriptionRequest.InvasiveMonitoring value,
        List<String> details,
        List<String> summary,
        List<String> review,
        List<String> alerts
    ) {
        if (value == null) return;
        addList(details, summary, "MONITORIZAÇÃO HEMODINÂMICA INVASIVA", value.hemodynamic());
        addList(details, summary, "MONITORIZAÇÃO NEUROLÓGICA INVASIVA", value.neurological());
        if (!safe(value.hemodynamic()).isEmpty() || !safe(value.neurological()).isEmpty()) {
            review.add("TRANSDUTORES, DOMOS E KITS DE MONITORIZAÇÃO INVASIVA — CONFERIR USO REGISTRADO E CONTRATO.");
            alerts.add("REGISTRAR INSTALAÇÃO, CALIBRAÇÃO, SÍTIO, VALORES E INTERCORRÊNCIAS DA MONITORIZAÇÃO INVASIVA.");
        }
    }

    private void validate(MonitoringPrescriptionRequest request) {
        if (request == null) throw new BusinessValidationException("Selecione ao menos um item de monitorização");
        int selected = 0;
        if (request.vitalSigns() != null) {
            var value = request.vitalSigns();
            selected += optional(value.frequency(), VITAL_SIGNS_FREQUENCIES, "frequência dos sinais vitais");
            selected += optional(value.painScale(), PAIN_SCALES, "escala de dor");
            selected += optional(value.consciousnessSedationScale(), CONSCIOUSNESS_SEDATION_SCALES,
                "escala de consciência ou sedação");
            selected += optional(value.fallRiskScale(), FALL_RISK_SCALES, "escala de risco de queda");
            if (!notBlank(value.frequency()) && (
                notBlank(value.painScale()) || notBlank(value.consciousnessSedationScale()) ||
                    notBlank(value.fallRiskScale())
            )) {
                throw new BusinessValidationException("Informe a frequência dos sinais vitais para aplicar as escalas clínicas");
            }
        }
        if (request.glucoseMonitoring() != null) {
            var value = request.glucoseMonitoring();
            selected += optional(value.frequency(), GLUCOSE_MONITORING_FREQUENCIES, "frequência do DXT");
            boolean hasProtocol = Boolean.TRUE.equals(value.hypoglycemiaProtocol()) ||
                Boolean.TRUE.equals(value.slidingScale());
            if (hasProtocol && !notBlank(value.frequency())) {
                throw new BusinessValidationException("Informe a frequência do DXT para aplicar os protocolos");
            }
            if (Boolean.TRUE.equals(value.slidingScale())) {
                requiredAllowed(value.insulinType(), INSULIN_TYPES, "tipo de insulina da escala");
                selected++;
            } else if (notBlank(value.insulinType())) {
                throw new BusinessValidationException("Selecione a escala de correção antes de informar a insulina");
            }
            if (Boolean.TRUE.equals(value.hypoglycemiaProtocol())) selected++;
        }
        if (request.fluidBalanceOutputs() != null) {
            var value = request.fluidBalanceOutputs();
            selected += optional(value.fluidBalance(), FLUID_BALANCE_OPTIONS, "balanço hídrico");
            selected += optional(value.urineOutput(), URINE_OUTPUT_OPTIONS, "controle de diurese");
            selected += multi(value.drainsTubes(), DRAINS_TUBES, "drenos e sondas");
            selected += multi(value.otherMeasurements(), OTHER_MEASUREMENTS, "outras mensurações");
        }
        if (request.invasiveMonitoring() != null) {
            selected += multi(request.invasiveMonitoring().hemodynamic(), HEMODYNAMIC_MONITORING,
                "monitorização hemodinâmica");
            selected += multi(request.invasiveMonitoring().neurological(), NEUROLOGICAL_MONITORING,
                "monitorização neurológica");
        }
        if (selected == 0) throw new BusinessValidationException("Selecione ao menos um item de monitorização");
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?> map)) invalidFormat();
        @SuppressWarnings("unchecked") Map<?, ?> values = (Map<?, ?>) value;
        allowedKeys(values, Set.of("vitalSigns", "glucoseMonitoring", "fluidBalanceOutputs", "invasiveMonitoring"));
        nested(values.get("vitalSigns"), Set.of(
            "frequency", "painScale", "consciousnessSedationScale", "fallRiskScale"
        ));
        nested(values.get("glucoseMonitoring"), Set.of(
            "frequency", "hypoglycemiaProtocol", "slidingScale", "insulinType"
        ));
        nested(values.get("fluidBalanceOutputs"), Set.of(
            "fluidBalance", "urineOutput", "drainsTubes", "otherMeasurements"
        ));
        nested(values.get("invasiveMonitoring"), Set.of("hemodynamic", "neurological"));
    }

    private void nested(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof Map<?, ?> map)) invalidFormat();
        allowedKeys((Map<?, ?>) value, allowed);
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("A monitorização contém campos não permitidos");
        }
    }

    private int optional(String value, List<String> allowed, String label) {
        if (!notBlank(value)) return 0;
        requiredAllowed(value, allowed, label);
        return 1;
    }

    private void requiredAllowed(String value, List<String> allowed, String label) {
        if (!notBlank(value) || !allowed.contains(value)) {
            throw new BusinessValidationException("Selecione uma opção válida para " + label);
        }
    }

    private int multi(List<String> values, List<String> allowed, String label) {
        List<String> selected = safe(values);
        if (new LinkedHashSet<>(selected).size() != selected.size() ||
            selected.stream().anyMatch(item -> !allowed.contains(item))) {
            throw new BusinessValidationException("Selecione opções válidas para " + label);
        }
        return selected.size();
    }

    private void addScale(List<String> details, List<String> summary, String label, String value) {
        if (!notBlank(value)) return;
        details.add("- ESCALA DE " + label + ": " + value + ".");
        summary.add(value);
    }

    private void addList(List<String> details, List<String> summary, String label, List<String> values) {
        if (safe(values).isEmpty()) return;
        details.add("- " + label + ": " + String.join("; ", values) + ".");
        summary.add(label + ": " + String.join(", ", values));
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void invalidFormat() {
        throw new BusinessValidationException("Dados da monitorização estão em formato inválido");
    }
}
