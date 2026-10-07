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
public class NursingCarePrescriptionService {

    private static final List<String> HEAD_POSITIONS = List.of(
        "ELEVADA A 30° - 45° (PADRÃO / UTI)", "A 0° (PLANA / PÓS-PROCEDIMENTO)",
        "PROCLIVE", "TRENDELENBURG", "DECÚBITO VENTRAL (PRONA)"
    );
    private static final List<String> REPOSITIONING = List.of(
        "2/2 HORAS", "3/3 HORAS", "CONFORME TOLERÂNCIA / LIVRE"
    );
    private static final List<String> PRESSURE_PROTECTION = List.of(
        "PROTEÇÃO DE CALCÂNEOS / SACRO", "COLCHÃO PNEUMÁTICO / DE AR", "COXINS DE APOIO"
    );
    private static final List<String> BATHS = List.of(
        "NO LEITO", "NO LEITO COM LAVAGEM DE CRÂNIO", "CADEIRA DE BANHO / ASPERSÃO", "AUXÍLIO NO BANHO"
    );
    private static final List<String> ORAL_HYGIENE = List.of(
        "COM CLOREXIDINA 0,12% 12/12H (PROTOCOLO PAV)",
        "COM CLOREXIDINA 0,12% 8/8H", "HIGIENE ORAL SIMPLES / CREME DENTAL"
    );
    private static final List<String> SKIN_CARE = List.of(
        "HIDRATAÇÃO CORPORAL COM AGE / LOÇÃO", "CREME BARREIRA EM REGIÃO SACRA / PERINEAL", "BARREIRA CAVILON"
    );
    private static final List<String> CATHETER_DRESSINGS = List.of(
        "CVC / PAI — FILME TRANSPARENTE COM CLOREXIDINA; TROCAR A CADA 7 DIAS OU SE SUJO / SOLTO",
        "CVC / PAI — CURATIVO OCLUSIVO SECO COM GAZE E MICROPORE; TROCAR 24/24H",
        "CATETER DE HEMODIÁLISE / PERMCATH — GAZE E CLOREXIDINA ALCOÓLICA A CADA SESSÃO"
    );
    private static final List<String> ACUTE_WOUND_CARE = List.of(
        "LIMPEZA COM SF 0,9% + OCLUSÃO COM GAZE SECA 24/24H OU SE SATURADO"
    );
    private static final List<String> COMPLEX_COVERAGES = List.of(
        "PLACA DE HIDROCOLOIDE", "ESPUMA DE SILICONE COM PRATA", "ALGINATO DE CÁLCIO",
        "TERAPIA POR PRESSÃO NEGATIVA (VÁCUO)", "BOTA DE UNNA", "BIGUANIDA (PHMB)"
    );
    private static final List<String> DRESSING_FREQUENCIES = List.of(
        "DIÁRIA", "A CADA 3 DIAS", "A CADA 7 DIAS", "SE SATURADO / SOLTO"
    );
    private static final List<String> DRAIN_CARE = List.of(
        "MANTER DRENO EM ASPIRAÇÃO / SISTEMA FECHADO", "CURATIVO DIÁRIO NO ORIFÍCIO DE INSERÇÃO",
        "MENSURAÇÃO E ORDENHA CONFORME ROTINA"
    );
    private static final List<String> AIRWAY_SUCTION = List.of(
        "SISTEMA FECHADO (TRACH CARE) SE NECESSÁRIO", "SISTEMA ABERTO SE NECESSÁRIO"
    );
    private static final List<String> DEVICE_CARE = List.of(
        "TOT / TQT — TROCA DE FIXAÇÃO 24/24H",
        "TOT / TQT — CHECAGEM DA PRESSÃO DO CUFF (20-30 MMHG) 8/8H",
        "SVD / SVE — CUIDADOS, FIXAÇÃO E BOLSA ABAIXO DO NÍVEL DA BEXIGA",
        "SNE / SNG — CHECAGEM DE POSICIONAMENTO E FIXAÇÃO DIÁRIA"
    );
    private static final List<String> FLUID_BALANCE = List.of(
        "BALANÇO HÍDRICO DE 24H", "BALANÇO HÍDRICO PARCIAL 6/6H",
        "MENSURAÇÃO DE DIURESE / DÉBITO DE DRENOS DE 2/2H OU 4/4H"
    );

    private final ObjectMapper objectMapper;

    public NursingCarePrescriptionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NursingCarePrescriptionCatalog catalog() {
        return new NursingCarePrescriptionCatalog(
            HEAD_POSITIONS, REPOSITIONING, PRESSURE_PROTECTION, BATHS, ORAL_HYGIENE,
            SKIN_CARE, CATHETER_DRESSINGS, ACUTE_WOUND_CARE, COMPLEX_COVERAGES,
            DRESSING_FREQUENCIES, DRAIN_CARE, AIRWAY_SUCTION, DEVICE_CARE, FLUID_BALANCE
        );
    }

    public NursingCarePrescriptionResponse preview(NursingCarePrescriptionRequest request) {
        validate(request);
        List<String> details = new ArrayList<>();
        List<String> summary = new ArrayList<>();
        List<String> review = new ArrayList<>();
        List<String> quality = new ArrayList<>();

        appendPositioning(request.positioning(), details, summary, review, quality);
        appendHygiene(request.hygieneSkin(), details, summary, quality);
        appendDressings(request.dressingsDrains(), details, summary, review, quality);
        appendProcedures(request.procedures(), details, summary, quality);

        return new NursingCarePrescriptionResponse(
            new NursingCarePrescriptionResponse.StructuredCare(
                "CUIDADOS DE ENFERMAGEM: " + String.join("; ", summary) + ".",
                "CUIDADOS DE ENFERMAGEM:\n" + String.join("\n", details)
            ),
            new NursingCarePrescriptionResponse.BillingAudit(
                List.copyOf(new LinkedHashSet<>(review)),
                List.copyOf(new LinkedHashSet<>(quality))
            )
        );
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final NursingCarePrescriptionRequest request;
        try {
            request = objectMapper.convertValue(value, NursingCarePrescriptionRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados dos cuidados de enfermagem estão em formato inválido");
        }
        NursingCarePrescriptionResponse response = preview(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", request);
        stored.put("structuredCare", response.structuredCare());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private void appendPositioning(
        NursingCarePrescriptionRequest.Positioning value,
        List<String> details,
        List<String> summary,
        List<String> review,
        List<String> quality
    ) {
        if (value == null) return;
        add(details, summary, "POSICIONAMENTO — CABECEIRA", value.headPosition());
        add(details, summary, "MUDANÇA DE DECÚBITO", value.repositioningFrequency());
        addAll(details, summary, "PROTEÇÃO DE PROEMINÊNCIAS ÓSSEAS", value.pressureProtection());
        if (contains(value.pressureProtection(), "COLCHÃO PNEUMÁTICO / DE AR")) {
            review.add("COLCHÃO PNEUMÁTICO / DE AR — CONFERIR INDICAÇÃO, DISPONIBILIDADE E CONTRATO.");
        }
        if (notBlank(value.repositioningFrequency()) || !safe(value.pressureProtection()).isEmpty()) {
            quality.add("REGISTRAR AVALIAÇÃO DE RISCO E INTEGRIDADE DA PELE CONFORME PROTOCOLO INSTITUCIONAL.");
        }
    }

    private void appendHygiene(
        NursingCarePrescriptionRequest.HygieneSkin value,
        List<String> details,
        List<String> summary,
        List<String> quality
    ) {
        if (value == null) return;
        add(details, summary, "BANHO", value.bath());
        add(details, summary, "HIGIENE ORAL", value.oralHygiene());
        addAll(details, summary, "CUIDADOS COM A PELE", value.skinCare());
        if (notBlank(value.oralHygiene()) && value.oralHygiene().contains("CLOREXIDINA")) {
            quality.add("CONFERIR INDICAÇÃO E ADESÃO AO PROTOCOLO INSTITUCIONAL DE HIGIENE ORAL E PREVENÇÃO DE PAV.");
        }
    }

    private void appendDressings(
        NursingCarePrescriptionRequest.DressingsDrains value,
        List<String> details,
        List<String> summary,
        List<String> review,
        List<String> quality
    ) {
        if (value == null) return;
        add(details, summary, "CURATIVO DE CATETER", value.catheterDressing());
        add(details, summary, "FERIDA CIRÚRGICA / AGUDA", value.acuteWoundCare());
        if (notBlank(value.complexWoundCoverage())) {
            String wound = value.complexWoundCoverage() + " — TROCA: " + value.dressingChangeFrequency();
            add(details, summary, "LPP / FERIDA COMPLEXA", wound);
            review.add("COBERTURA ESPECIAL — CONFERIR AUTORIZAÇÃO, REGISTRO ASSISTENCIAL E REGRAS CONTRATUAIS.");
            quality.add("DOCUMENTAR LOCALIZAÇÃO, DIMENSÕES, ASPECTO E EVOLUÇÃO DA FERIDA NO REGISTRO ASSISTENCIAL.");
        }
        addAll(details, summary, "DRENOS", value.drainCare());
        if (notBlank(value.catheterDressing())) {
            review.add("MATERIAIS / KIT DE CURATIVO DE CATETER — CONFERIR CONSUMO E CONTRATO.");
        }
    }

    private void appendProcedures(
        NursingCarePrescriptionRequest.Procedures value,
        List<String> details,
        List<String> summary,
        List<String> quality
    ) {
        if (value == null) return;
        add(details, summary, "ASPIRAÇÃO DE VIAS AÉREAS", value.airwaySuction());
        addAll(details, summary, "MANUTENÇÃO DE DISPOSITIVOS", value.deviceCare());
        add(details, summary, "BALANÇO HÍDRICO / MENSURAÇÕES", value.fluidBalance());
        if (contains(value.deviceCare(), "CUFF")) {
            quality.add("REGISTRAR A PRESSÃO DO CUFF E AS INTERCORRÊNCIAS CONFORME PROTOCOLO INSTITUCIONAL.");
        }
    }

    private void validate(NursingCarePrescriptionRequest request) {
        if (request == null) throw new BusinessValidationException("Informe ao menos um cuidado de enfermagem");
        int selected = 0;
        if (request.positioning() != null) {
            selected += optional(request.positioning().headPosition(), HEAD_POSITIONS, "posição da cabeceira");
            selected += optional(request.positioning().repositioningFrequency(), REPOSITIONING, "mudança de decúbito");
            selected += multi(request.positioning().pressureProtection(), PRESSURE_PROTECTION, "proteções de proeminências");
        }
        if (request.hygieneSkin() != null) {
            selected += optional(request.hygieneSkin().bath(), BATHS, "banho");
            selected += optional(request.hygieneSkin().oralHygiene(), ORAL_HYGIENE, "higiene oral");
            selected += multi(request.hygieneSkin().skinCare(), SKIN_CARE, "cuidados com a pele");
        }
        if (request.dressingsDrains() != null) {
            var value = request.dressingsDrains();
            selected += optional(value.catheterDressing(), CATHETER_DRESSINGS, "curativo de cateter");
            selected += optional(value.acuteWoundCare(), ACUTE_WOUND_CARE, "ferida aguda");
            selected += optional(value.complexWoundCoverage(), COMPLEX_COVERAGES, "cobertura de ferida");
            selected += optional(value.dressingChangeFrequency(), DRESSING_FREQUENCIES, "frequência do curativo");
            selected += multi(value.drainCare(), DRAIN_CARE, "cuidados com drenos");
            if (notBlank(value.complexWoundCoverage()) != notBlank(value.dressingChangeFrequency())) {
                throw new BusinessValidationException("Informe a cobertura e a frequência de troca da ferida complexa");
            }
        }
        if (request.procedures() != null) {
            selected += optional(request.procedures().airwaySuction(), AIRWAY_SUCTION, "aspiração de vias aéreas");
            selected += multi(request.procedures().deviceCare(), DEVICE_CARE, "cuidados com dispositivos");
            selected += optional(request.procedures().fluidBalance(), FLUID_BALANCE, "balanço hídrico");
        }
        if (selected == 0) throw new BusinessValidationException("Selecione ao menos um cuidado de enfermagem");
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?> map)) invalidFormat();
        @SuppressWarnings("unchecked") Map<?, ?> values = (Map<?, ?>) value;
        allowedKeys(values, Set.of("positioning", "hygieneSkin", "dressingsDrains", "procedures"));
        nested(values.get("positioning"), Set.of("headPosition", "repositioningFrequency", "pressureProtection"));
        nested(values.get("hygieneSkin"), Set.of("bath", "oralHygiene", "skinCare"));
        nested(values.get("dressingsDrains"), Set.of(
            "catheterDressing", "acuteWoundCare", "complexWoundCoverage", "dressingChangeFrequency", "drainCare"
        ));
        nested(values.get("procedures"), Set.of("airwaySuction", "deviceCare", "fluidBalance"));
    }

    private void nested(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof Map<?, ?> map)) invalidFormat();
        allowedKeys((Map<?, ?>) value, allowed);
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("Os cuidados de enfermagem contêm campos não permitidos");
        }
    }

    private int optional(String value, List<String> allowed, String label) {
        if (!notBlank(value)) return 0;
        if (!allowed.contains(value)) throw new BusinessValidationException("Selecione uma opção válida para " + label);
        return 1;
    }

    private int multi(List<String> values, List<String> allowed, String label) {
        List<String> selected = safe(values);
        if (new LinkedHashSet<>(selected).size() != selected.size() || selected.stream().anyMatch(item -> !allowed.contains(item))) {
            throw new BusinessValidationException("Selecione opções válidas para " + label);
        }
        return selected.size();
    }

    private void add(List<String> details, List<String> summary, String label, String value) {
        if (!notBlank(value)) return;
        details.add("- " + label + ": " + value + ".");
        summary.add(value);
    }

    private void addAll(List<String> details, List<String> summary, String label, List<String> values) {
        if (safe(values).isEmpty()) return;
        details.add("- " + label + ": " + String.join("; ", values) + ".");
        summary.add(label + ": " + String.join(", ", values));
    }

    private boolean contains(List<String> values, String fragment) {
        return safe(values).stream().anyMatch(value -> value.contains(fragment));
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void invalidFormat() {
        throw new BusinessValidationException("Dados dos cuidados de enfermagem estão em formato inválido");
    }
}
