package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class VentilatorySupportValidator {

    private static final Set<Integer> VENTURI_FIO2 = Set.of(24, 28, 31, 35, 40, 50);
    private final VentilatorySupportCatalogService catalog;

    public VentilatorySupportValidator(VentilatorySupportCatalogService catalog) {
        this.catalog = catalog;
    }

    public void validate(VentilatorySupportRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw invalid("Selecione ao menos um item de suporte ventilatório");
        }
        if (request.items().size() > 4) {
            throw invalid("A prescrição aceita no máximo quatro itens de suporte ventilatório");
        }
        if (request.selectedTemplate() != null && request.selectedTemplate().length() > 80) {
            throw invalid("O identificador do modelo ventilatório excede o limite permitido");
        }
        if (notBlank(request.selectedTemplate()) && !catalog.templateExists(request.selectedTemplate())) {
            throw invalid("Selecione um modelo ventilatório válido");
        }

        Set<Integer> identifiers = new HashSet<>();
        for (VentilatorySupportRequest.Item item : request.items()) {
            if (item == null || item.id() == null || item.id() < 1 || !identifiers.add(item.id())) {
                throw invalid("Os itens ventilatórios precisam de identificadores únicos");
            }
            requireOption(item.supportType(), VentilatorySupportCatalogService.SUPPORT_TYPES, "tipo de suporte");
            requireOption(item.scheduling(), VentilatorySupportCatalogService.SCHEDULING, "aprazamento");
            switch (item.supportType()) {
                case "ROOM_AIR" -> validateRoomAir(item);
                case "LOW_FLOW" -> validateLowFlow(item);
                case "HIGH_FLOW" -> validateHighFlow(item);
                case "NIV" -> validateNonInvasive(item);
                case "IMV" -> validateInvasive(item);
                default -> throw invalid("Selecione um tipo de suporte ventilatório válido");
            }
        }
    }

    private void validateRoomAir(VentilatorySupportRequest.Item item) {
        requireCommon(item, "CONTINUOUS", "CONTINUOUS");
        requireOnly(item, false, false, false, false);
    }

    private void validateLowFlow(VentilatorySupportRequest.Item item) {
        requireOnly(item, true, false, false, false);
        requireOption(item.frequency(), VentilatorySupportCatalogService.LOW_FLOW_FREQUENCIES, "frequência");
        validateScheduling(item.frequency(), item.scheduling());

        VentilatorySupportRequest.LowFlow value = item.lowFlow();
        requireOption(value.device(), VentilatorySupportCatalogService.LOW_FLOW_DEVICES, "dispositivo");
        switch (value.device()) {
            case "NASAL_CANNULA" -> {
                requireRange(value.oxygenFlowLitersMinute(), "0.5", "5", "fluxo do cateter nasal");
                requireNull(value.fio2Percent(), "FiO₂ não se aplica ao cateter nasal de baixo fluxo");
            }
            case "VENTURI_MASK" -> {
                requireNull(value.oxygenFlowLitersMinute(), "Informe a concentração da máscara de Venturi, não o fluxo");
                if (value.fio2Percent() == null || !VENTURI_FIO2.contains(value.fio2Percent())) {
                    throw invalid("A FiO₂ da máscara de Venturi deve ser 24%, 28%, 31%, 35%, 40% ou 50%");
                }
            }
            case "NON_REBREATHER_MASK" -> {
                requireRange(value.oxygenFlowLitersMinute(), "10", "15", "fluxo da máscara com reservatório");
                requireNull(value.fio2Percent(), "FiO₂ fixa não se aplica à máscara com reservatório");
            }
            case "SIMPLE_FACE_MASK" -> {
                requireRange(value.oxygenFlowLitersMinute(), "5", "8", "fluxo da máscara facial simples");
                requireNull(value.fio2Percent(), "FiO₂ fixa não se aplica à máscara facial simples");
            }
            default -> throw invalid("Selecione um dispositivo de baixo fluxo válido");
        }
    }

    private void validateHighFlow(VentilatorySupportRequest.Item item) {
        requireCommon(item, "CONTINUOUS", "CONTINUOUS");
        requireOnly(item, false, true, false, false);
        VentilatorySupportRequest.HighFlow value = item.highFlow();
        requireRange(value.flowLitersMinute(), "30", "60", "fluxo da CNAF");
        requireIntegerRange(value.fio2Percent(), 21, 100, "FiO₂ da CNAF");
        requireRange(value.temperatureCelsius(), "31", "37", "temperatura da CNAF");
        requireOption(value.interfaceSize(), VentilatorySupportCatalogService.HIGH_FLOW_SIZES, "tamanho da cânula");
    }

    private void validateNonInvasive(VentilatorySupportRequest.Item item) {
        requireOnly(item, false, false, true, false);
        requireOption(item.frequency(), VentilatorySupportCatalogService.NIV_FREQUENCIES, "frequência da VNI");
        validateScheduling(item.frequency(), item.scheduling());
        VentilatorySupportRequest.NonInvasive value = item.nonInvasive();
        requireOption(value.mode(), VentilatorySupportCatalogService.NIV_MODES, "modo da VNI");
        requireOption(value.interfaceType(), VentilatorySupportCatalogService.NIV_INTERFACES, "interface da VNI");
        requireIntegerRange(value.fio2Percent(), 21, 100, "FiO₂ da VNI");

        if ("EVERY_12H".equals(item.frequency())) {
            requireRange(value.sessionHours(), "0.5", "12", "duração da sessão de VNI");
        } else {
            requireNull(value.sessionHours(), "Duração da sessão só deve ser informada para VNI 12/12H");
        }

        switch (value.mode()) {
            case "CPAP" -> {
                requireRange(value.epapPeepCmH2o(), "1", "30", "CPAP/PEEP");
                requireNull(value.ipapCmH2o(), "IPAP não se aplica ao modo CPAP");
                requireNull(value.supportPressureCmH2o(), "Pressão de suporte não se aplica ao modo CPAP");
                requireNull(value.backupRate(), "Frequência de backup não se aplica ao modo CPAP");
            }
            case "BIPAP" -> {
                requireRange(value.ipapCmH2o(), "1", "40", "IPAP");
                requireRange(value.epapPeepCmH2o(), "1", "30", "EPAP");
                requireIntegerRange(value.backupRate(), 1, 60, "frequência de backup");
                requireNull(value.supportPressureCmH2o(), "Pressão de suporte isolada não se aplica ao BiPAP");
                if (value.ipapCmH2o().compareTo(value.epapPeepCmH2o()) <= 0) {
                    throw invalid("No BiPAP, a IPAP deve ser maior que a EPAP");
                }
            }
            case "PSV_PEEP" -> {
                requireRange(value.supportPressureCmH2o(), "1", "40", "pressão de suporte");
                requireRange(value.epapPeepCmH2o(), "1", "30", "PEEP");
                requireNull(value.ipapCmH2o(), "IPAP não deve ser informada no modo PSV + PEEP");
                requireNull(value.backupRate(), "Frequência de backup não deve ser informada no modo PSV + PEEP");
            }
            default -> throw invalid("Selecione um modo de VNI válido");
        }
    }

    private void validateInvasive(VentilatorySupportRequest.Item item) {
        requireCommon(item, "CONTINUOUS", "CONTINUOUS");
        requireOnly(item, false, false, false, true);
        VentilatorySupportRequest.Invasive value = item.invasive();
        requireOption(value.airway(), VentilatorySupportCatalogService.INVASIVE_AIRWAYS, "via aérea");
        if (!notBlank(value.airwayDetail())) throw invalid("Informe o número ou detalhe da via aérea invasiva");
        if (value.airwayDetail().length() > 80) {
            throw invalid("O detalhe da via aérea invasiva excede o limite permitido");
        }
        requireOption(value.mode(), VentilatorySupportCatalogService.INVASIVE_MODES, "modo da VMI");
        requireIntegerRange(value.fio2Percent(), 21, 100, "FiO₂ da VMI");
        requireRange(value.peepCmH2o(), "0", "30", "PEEP da VMI");
        validateGoals(value.protectiveGoals());

        switch (value.mode()) {
            case "VCV" -> validateVcv(value);
            case "PCV" -> validatePcv(value);
            case "PSV" -> validatePsv(value);
            default -> throw invalid("Selecione um modo de VMI válido");
        }
    }

    private void validateVcv(VentilatorySupportRequest.Invasive value) {
        requireIntegerRange(value.tidalVolumeMl(), 50, 1500, "volume corrente");
        requireIntegerRange(value.respiratoryRate(), 1, 80, "frequência respiratória");
        if (value.inspiratoryFlowLitersMinute() == null && value.inspiratoryTimeSeconds() == null) {
            throw invalid("Informe o fluxo ou o tempo inspiratório no modo VCV");
        }
        optionalRange(value.inspiratoryFlowLitersMinute(), "1", "120", "fluxo inspiratório");
        optionalRange(value.inspiratoryTimeSeconds(), "0.1", "5", "tempo inspiratório");
        optionalRange(value.pauseSeconds(), "0", "5", "pausa inspiratória");
        requireNull(value.inspiratoryPressureCmH2o(), "Pressão inspiratória não se aplica ao modo VCV");
        requireNull(value.supportPressureCmH2o(), "Pressão de suporte não se aplica ao modo VCV");
        requireNull(value.triggerSensitivity(), "Sensibilidade não deve ser informada no modo VCV");
    }

    private void validatePcv(VentilatorySupportRequest.Invasive value) {
        requireRange(value.inspiratoryPressureCmH2o(), "1", "50", "pressão inspiratória");
        requireRange(value.inspiratoryTimeSeconds(), "0.1", "5", "tempo inspiratório");
        requireIntegerRange(value.respiratoryRate(), 1, 80, "frequência respiratória");
        requireNull(value.tidalVolumeMl(), "Volume corrente ajustado não se aplica ao modo PCV");
        requireNull(value.inspiratoryFlowLitersMinute(), "Fluxo ajustado não se aplica ao modo PCV");
        requireNull(value.pauseSeconds(), "Pausa inspiratória não deve ser informada no modo PCV");
        requireNull(value.supportPressureCmH2o(), "Pressão de suporte não se aplica ao modo PCV");
        requireNull(value.triggerSensitivity(), "Sensibilidade não deve ser informada no modo PCV");
    }

    private void validatePsv(VentilatorySupportRequest.Invasive value) {
        requireRange(value.supportPressureCmH2o(), "1", "50", "pressão de suporte");
        requireRange(value.triggerSensitivity(), "0.1", "20", "sensibilidade do gatilho");
        requireNull(value.tidalVolumeMl(), "Volume corrente ajustado não se aplica ao modo PSV");
        requireNull(value.respiratoryRate(), "Frequência mandatória não se aplica ao modo PSV");
        requireNull(value.inspiratoryFlowLitersMinute(), "Fluxo ajustado não se aplica ao modo PSV");
        requireNull(value.inspiratoryTimeSeconds(), "Tempo inspiratório ajustado não se aplica ao modo PSV");
        requireNull(value.pauseSeconds(), "Pausa inspiratória não se aplica ao modo PSV");
        requireNull(value.inspiratoryPressureCmH2o(), "Pressão controlada não se aplica ao modo PSV");
    }

    private void requireOnly(
        VentilatorySupportRequest.Item item,
        boolean lowFlow,
        boolean highFlow,
        boolean nonInvasive,
        boolean invasive
    ) {
        if ((item.lowFlow() != null) != lowFlow || (item.highFlow() != null) != highFlow ||
            (item.nonInvasive() != null) != nonInvasive || (item.invasive() != null) != invasive) {
            throw invalid("Os parâmetros enviados não correspondem ao tipo de suporte ventilatório selecionado");
        }
    }

    private void requireCommon(VentilatorySupportRequest.Item item, String frequency, String scheduling) {
        if (!frequency.equals(item.frequency()) || !scheduling.equals(item.scheduling())) {
            throw invalid("Frequência ou aprazamento incompatível com o suporte selecionado");
        }
    }

    private void validateScheduling(String frequency, String scheduling) {
        String expected = switch (frequency) {
            case "CONTINUOUS" -> "CONTINUOUS";
            case "PRN", "PRN_SPO2_92", "PRN_SPO2_88_COPD" -> "PRN";
            default -> "FIXED";
        };
        if (!expected.equals(scheduling)) {
            throw invalid("O aprazamento não corresponde à frequência selecionada");
        }
    }

    private void validateGoals(List<String> goals) {
        List<String> selected = goals == null ? List.of() : goals;
        if (selected.size() > 3 || new HashSet<>(selected).size() != selected.size() || selected.stream().anyMatch(
            goal -> !catalog.contains(VentilatorySupportCatalogService.PROTECTIVE_GOALS, goal)
        )) {
            throw invalid("Selecione metas protetoras válidas");
        }
    }

    private void requireOption(
        String code,
        List<VentilatorySupportCatalog.Option> options,
        String label
    ) {
        if (!notBlank(code) || !catalog.contains(options, code)) {
            throw invalid("Selecione uma opção válida para " + label);
        }
    }

    private void requireRange(BigDecimal value, String min, String max, String label) {
        if (value == null || value.compareTo(new BigDecimal(min)) < 0 || value.compareTo(new BigDecimal(max)) > 0) {
            throw invalid("Informe " + label + " entre " + min + " e " + max);
        }
    }

    private void optionalRange(BigDecimal value, String min, String max, String label) {
        if (value != null) requireRange(value, min, max, label);
    }

    private void requireIntegerRange(Integer value, int min, int max, String label) {
        if (value == null || value < min || value > max) {
            throw invalid("Informe " + label + " entre " + min + " e " + max);
        }
    }

    private void requireNull(Object value, String message) {
        if (value != null) throw invalid(message);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private BusinessValidationException invalid(String message) {
        return new BusinessValidationException(message);
    }
}
