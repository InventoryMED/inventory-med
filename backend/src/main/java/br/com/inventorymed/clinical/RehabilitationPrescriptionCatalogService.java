package br.com.inventorymed.clinical;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RehabilitationPrescriptionCatalogService {

    static final List<RehabilitationPrescriptionCatalog.Option> SPECIALTIES = List.of(
        option("RESPIRATORY_PHYSIOTHERAPY", "FISIOTERAPIA RESPIRATÓRIA"),
        option("MOTOR_PHYSIOTHERAPY", "FISIOTERAPIA MOTORA E MOBILIZAÇÃO PRECOCE"),
        option("SPEECH_THERAPY", "FONOAUDIOLOGIA E TERAPIA DE DEGLUTIÇÃO / VOZ"),
        option("OCCUPATIONAL_THERAPY", "TERAPIA OCUPACIONAL")
    );

    static final List<RehabilitationPrescriptionCatalog.Option> RESPIRATORY_PROCEDURES = List.of(
        option("BRONCHIAL_HYGIENE", "MANOBRAS DE HIGIENE BRÔNQUICA"),
        option("AIRWAY_SUCTION", "HIGIENE BRÔNQUICA — ASPIRAÇÃO DE VIAS AÉREAS"),
        option("VIBROCOMPRESSION", "HIGIENE BRÔNQUICA — VIBROCOMPRESSÃO"),
        option("CLAPPING", "HIGIENE BRÔNQUICA — TAPOTAGEM"),
        option("POSTURAL_DRAINAGE", "HIGIENE BRÔNQUICA — DRENAGEM POSTURAL"),
        option("PULMONARY_REEXPANSION", "MANOBRAS DE REEXPANSÃO PULMONAR"),
        option("VOLUME_INCENTIVE", "REEXPANSÃO — INCENTIVADOR A VOLUME"),
        option("FLOW_INCENTIVE", "REEXPANSÃO — INCENTIVADOR A FLUXO"),
        option("APL_REEXPANSION", "REEXPANSÃO — APL"),
        option("EPAP_REEXPANSION", "REEXPANSÃO — EPAP"),
        option("RESPIRATORY_MUSCLE_TRAINING", "TREINO MUSCULAR RESPIRATÓRIO"),
        option("TMR_THRESHOLD", "TREINO MUSCULAR RESPIRATÓRIO COM THRESHOLD"),
        option("TMR_POWERBREATHE", "TREINO MUSCULAR RESPIRATÓRIO COM POWERBREATHE"),
        option("VENTILATORY_WEANING_SBT", "DESMAME VENTILATÓRIO / TESTE DE RESPIRAÇÃO ESPONTÂNEA")
    );

    static final List<RehabilitationPrescriptionCatalog.Option> MOTOR_PROCEDURES = List.of(
        option("PASSIVE_MOBILIZATION", "MOBILIZAÇÃO PASSIVA NO LEITO"),
        option("ACTIVE_ASSISTED_MOBILIZATION", "MOBILIZAÇÃO ATIVO-ASSISTIDA / ATIVA NO LEITO"),
        option("SITTING_TRAINING", "TREINO DE SEDESTAÇÃO À BEIRA DO LEITO / POLTRONA"),
        option("STANDING_TRANSFERS", "TREINO DE ORTOSTATISMO E TRANSFERÊNCIAS"),
        option("GAIT_TRAINING", "TREINO DE DEAMBULAÇÃO / MARCHA COM OU SEM DISPOSITIVO"),
        option("CYCLE_ERGOMETER", "CINESIOTERAPIA COM CICLOERGÔMETRO PARA MMSS / MMII")
    );

    static final List<RehabilitationPrescriptionCatalog.Option> SPEECH_THERAPY_PROCEDURES = List.of(
        option("SWALLOWING_ASSESSMENT", "AVALIAÇÃO CLÍNICA DA DEGLUTIÇÃO"),
        option("SWALLOWING_TRAINING", "TREINO DE DEGLUTIÇÃO COM CONSISTÊNCIAS LÍQUIDA, PASTOSA E SÓLIDA"),
        option("TRACHEOSTOMY_WEANING", "DESMAME DE TQT — TROCA, VÁLVULA FONATÓRIA E TESTE DE BLUE DYE"),
        option("VOICE_REHABILITATION", "TERAPIA DE REABILITAÇÃO VOCAL E ARTICULATÓRIA PÓS-EXTUBAÇÃO")
    );

    static final List<RehabilitationPrescriptionCatalog.Option> OCCUPATIONAL_THERAPY_PROCEDURES = List.of(
        option("COGNITIVE_SENSORY_REHABILITATION", "REABILITAÇÃO COGNITIVA E ESTÍMULO SENSORIAL NO LEITO"),
        option("ORTHOSIS_POSITIONING_SPLINT", "ADEQUAÇÃO DE ÓRTESES / CONFECÇÃO DE CALHAS DE POSICIONAMENTO"),
        option("DAILY_LIVING_ACTIVITIES", "TREINO DE ATIVIDADES DA VIDA DIÁRIA E ADAPTAÇÃO DE UTENSÍLIOS")
    );

    static final List<RehabilitationPrescriptionCatalog.Option> RESPIRATORY_FREQUENCIES = List.of(
        option("DAILY", "1X AO DIA"),
        option("EVERY_12H", "12/12H — 2X AO DIA"),
        option("EVERY_8H", "8/8H — 3X AO DIA"),
        option("PRN", "SE NECESSÁRIO")
    );

    static final List<RehabilitationPrescriptionCatalog.Option> MOTOR_FREQUENCIES = List.of(
        option("DAILY", "1X AO DIA"),
        option("EVERY_12H", "12/12H — 2X AO DIA"),
        option("EVERY_8H", "8/8H — 3X AO DIA")
    );

    static final List<RehabilitationPrescriptionCatalog.Option> SPEECH_THERAPY_FREQUENCIES = List.of(
        option("SINGLE_ASSESSMENT", "AVALIAÇÃO PONTUAL"),
        option("DAILY", "1X AO DIA"),
        option("EVERY_12H", "12/12H — 2X AO DIA")
    );

    static final List<RehabilitationPrescriptionCatalog.Option> OCCUPATIONAL_THERAPY_FREQUENCIES = List.of(
        option("DAILY", "1X AO DIA"),
        option("THREE_WEEKLY", "3X POR SEMANA"),
        option("AS_NEEDED", "CONFORME DEMANDA")
    );

    static final List<RehabilitationPrescriptionCatalog.Option> SCHEDULING = List.of(
        option("FIXED", "FIXO"), option("PRN", "SN")
    );

    public RehabilitationPrescriptionCatalog catalog() {
        return new RehabilitationPrescriptionCatalog(
            SPECIALTIES,
            RESPIRATORY_PROCEDURES,
            MOTOR_PROCEDURES,
            SPEECH_THERAPY_PROCEDURES,
            OCCUPATIONAL_THERAPY_PROCEDURES,
            RESPIRATORY_FREQUENCIES,
            MOTOR_FREQUENCIES,
            SPEECH_THERAPY_FREQUENCIES,
            OCCUPATIONAL_THERAPY_FREQUENCIES,
            SCHEDULING,
            templates()
        );
    }

    public List<RehabilitationPrescriptionCatalog.Option> procedures(String specialty) {
        return switch (specialty) {
            case "RESPIRATORY_PHYSIOTHERAPY" -> RESPIRATORY_PROCEDURES;
            case "MOTOR_PHYSIOTHERAPY" -> MOTOR_PROCEDURES;
            case "SPEECH_THERAPY" -> SPEECH_THERAPY_PROCEDURES;
            case "OCCUPATIONAL_THERAPY" -> OCCUPATIONAL_THERAPY_PROCEDURES;
            default -> List.of();
        };
    }

    public List<RehabilitationPrescriptionCatalog.Option> frequencies(String specialty) {
        return switch (specialty) {
            case "RESPIRATORY_PHYSIOTHERAPY" -> RESPIRATORY_FREQUENCIES;
            case "MOTOR_PHYSIOTHERAPY" -> MOTOR_FREQUENCIES;
            case "SPEECH_THERAPY" -> SPEECH_THERAPY_FREQUENCIES;
            case "OCCUPATIONAL_THERAPY" -> OCCUPATIONAL_THERAPY_FREQUENCIES;
            default -> List.of();
        };
    }

    public String label(List<RehabilitationPrescriptionCatalog.Option> options, String code) {
        return options.stream()
            .filter(option -> option.code().equals(code))
            .findFirst()
            .map(RehabilitationPrescriptionCatalog.Option::label)
            .orElse(code);
    }

    public boolean contains(List<RehabilitationPrescriptionCatalog.Option> options, String code) {
        return options.stream().anyMatch(option -> option.code().equals(code));
    }

    public boolean templateExists(String code) {
        return templates().stream().anyMatch(template -> template.code().equals(code));
    }

    private List<RehabilitationPrescriptionCatalog.Template> templates() {
        return List.of(
            template(
                "RESPIRATORY_HYGIENE_DAILY",
                "FISIOTERAPIA RESPIRATÓRIA — HIGIENE BRÔNQUICA 1X/DIA",
                "RESPIRATORY_PHYSIOTHERAPY", "BRONCHIAL_HYGIENE", "DAILY", "FIXED"
            ),
            template(
                "MOTOR_PASSIVE_DAILY",
                "FISIOTERAPIA MOTORA — MOBILIZAÇÃO PASSIVA 1X/DIA",
                "MOTOR_PHYSIOTHERAPY", "PASSIVE_MOBILIZATION", "DAILY", "FIXED"
            ),
            template(
                "SPEECH_SWALLOWING_ASSESSMENT",
                "FONOAUDIOLOGIA — AVALIAÇÃO DA DEGLUTIÇÃO",
                "SPEECH_THERAPY", "SWALLOWING_ASSESSMENT", "SINGLE_ASSESSMENT", "FIXED"
            ),
            template(
                "OCCUPATIONAL_COGNITIVE_ON_DEMAND",
                "TERAPIA OCUPACIONAL — REABILITAÇÃO COGNITIVA CONFORME DEMANDA",
                "OCCUPATIONAL_THERAPY", "COGNITIVE_SENSORY_REHABILITATION", "AS_NEEDED", "PRN"
            )
        );
    }

    private RehabilitationPrescriptionCatalog.Template template(
        String code,
        String label,
        String specialty,
        String procedure,
        String frequency,
        String scheduling
    ) {
        return new RehabilitationPrescriptionCatalog.Template(
            code,
            label,
            new RehabilitationPrescriptionRequest.Item(
                1, specialty, procedure, frequency, scheduling, ""
            )
        );
    }

    private static RehabilitationPrescriptionCatalog.Option option(String code, String label) {
        return new RehabilitationPrescriptionCatalog.Option(code, label);
    }
}
