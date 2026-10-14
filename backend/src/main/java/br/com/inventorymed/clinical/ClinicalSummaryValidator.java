package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ClinicalSummaryValidator {

    private static final Set<String> OUTCOMES = Set.of(
        "ALTA_HOSPITALAR", "TRANSFERENCIA_HOSPITALAR", "OBITO"
    );
    private static final Set<String> DISCHARGE_CONDITIONS = Set.of(
        "MELHORADO", "CURADO", "A_PEDIDO", "ALTA_UTI_PARA_ENFERMARIA"
    );
    private static final Set<String> TRANSFER_REASONS = Set.of(
        "VAGA_UTI", "ESPECIALIDADE_INDISPONIVEL", "PEDIDO_FAMILIA"
    );
    private static final Set<String> DEVICES = Set.of(
        "TUBO_OROTRAQUEAL", "CVC", "DRENO_TORAX", "SONDA_ENTERAL", "SVD"
    );
    private static final Set<String> NECROPSY = Set.of("SIM", "NAO", "SVO_IML");

    public void validate(ClinicalSummaryRequest request) {
        if (request == null || !OUTCOMES.contains(trim(request.outcomeType()))) {
            invalid("Selecione um desfecho válido para o resumo clínico");
        }
        if (blank(request.clinicalSummary())) {
            invalid("Preencha o resumo da evolução clínica");
        }
        if (!request.physicianAttestation()) {
            invalid("Confirme a autoria médica antes de finalizar o resumo clínico");
        }
        validateMedicationRows(request.dischargeMedications());
        validateUniqueValues(request.dischargeOrientations(), 20, "orientações de alta");

        switch (request.outcomeType()) {
            case "ALTA_HOSPITALAR" -> validateDischarge(request.dischargeCondition());
            case "TRANSFERENCIA_HOSPITALAR" -> validateTransfer(request.transfer());
            case "OBITO" -> validateDeath(request.death());
            default -> invalid("Selecione um desfecho válido para o resumo clínico");
        }
    }

    private void validateDischarge(String condition) {
        if (!DISCHARGE_CONDITIONS.contains(trim(condition))) {
            invalid("Selecione a condição clínica na alta");
        }
    }

    private void validateTransfer(ClinicalSummaryRequest.TransferDetails transfer) {
        if (transfer == null || !TRANSFER_REASONS.contains(trim(transfer.reason()))) {
            invalid("Selecione o motivo da transferência hospitalar");
        }
        if (blank(transfer.destinationHospital())) {
            invalid("Informe a unidade ou hospital de destino");
        }
        validateAllowedValues(transfer.devices(), DEVICES, "dispositivos na saída");
    }

    private void validateDeath(ClinicalSummaryRequest.DeathDetails death) {
        if (death == null || death.occurredAt() == null) {
            invalid("Informe a data e a hora exatas do óbito");
            return;
        }
        if (death.occurredAt().isAfter(OffsetDateTime.now().plusMinutes(5))) {
            invalid("A data e a hora do óbito não podem estar no futuro");
        }
        if (blank(death.directCause()) || blank(death.underlyingCause())) {
            invalid("Informe a causa direta e a causa básica do óbito");
        }
        if (!NECROPSY.contains(trim(death.necropsy()))) {
            invalid("Informe a situação da necropsia");
        }
    }

    private void validateMedicationRows(List<ClinicalSummaryRequest.DischargeMedication> rows) {
        if (rows == null) return;
        for (ClinicalSummaryRequest.DischargeMedication row : rows) {
            if (row == null || blank(row.medication()) || blank(row.dosage())) {
                invalid("Informe o medicamento e a posologia em cada item da prescrição de alta");
            }
        }
    }

    private void validateUniqueValues(List<String> values, int maximum, String label) {
        if (values == null) return;
        if (values.size() > maximum || values.stream().anyMatch(this::blank)) {
            invalid("Revise as " + label);
        }
        if (new HashSet<>(values).size() != values.size()) {
            invalid("Não repita itens em " + label);
        }
    }

    private void validateAllowedValues(List<String> values, Set<String> allowed, String label) {
        validateUniqueValues(values, allowed.size(), label);
        if (values != null && values.stream().anyMatch(value -> !allowed.contains(value))) {
            invalid("Revise os " + label);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean blank(String value) {
        return trim(value).isBlank();
    }

    private void invalid(String message) {
        throw new BusinessValidationException(message);
    }
}
