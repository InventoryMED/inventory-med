package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RehabilitationPrescriptionValidator {

    private static final Set<String> HIGH_FREQUENCIES = Set.of("EVERY_12H", "EVERY_8H");
    private static final Set<String> PHYSIOTHERAPY = Set.of(
        "RESPIRATORY_PHYSIOTHERAPY", "MOTOR_PHYSIOTHERAPY"
    );

    private final RehabilitationPrescriptionCatalogService catalog;

    public RehabilitationPrescriptionValidator(RehabilitationPrescriptionCatalogService catalog) {
        this.catalog = catalog;
    }

    public void validate(RehabilitationPrescriptionRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw invalid("Selecione ao menos uma conduta de reabilitação");
        }
        if (request.items().size() > 12) {
            throw invalid("A prescrição aceita no máximo doze condutas de reabilitação");
        }
        if (request.selectedTemplate() != null && request.selectedTemplate().length() > 80) {
            throw invalid("O identificador do modelo de reabilitação excede o limite permitido");
        }
        if (notBlank(request.selectedTemplate()) && !catalog.templateExists(request.selectedTemplate())) {
            throw invalid("Selecione um modelo de reabilitação válido");
        }

        Set<Integer> identifiers = new HashSet<>();
        for (RehabilitationPrescriptionRequest.Item item : request.items()) {
            validateItem(item, identifiers);
        }
    }

    private void validateItem(
        RehabilitationPrescriptionRequest.Item item,
        Set<Integer> identifiers
    ) {
        if (item == null || item.id() == null || item.id() < 1 || !identifiers.add(item.id())) {
            throw invalid("As condutas de reabilitação precisam de identificadores únicos");
        }
        requireOption(
            item.specialty(), RehabilitationPrescriptionCatalogService.SPECIALTIES, "especialidade"
        );
        requireOption(item.procedure(), catalog.procedures(item.specialty()), "conduta");
        requireOption(item.frequency(), catalog.frequencies(item.specialty()), "frequência");

        String expectedScheduling = Set.of("PRN", "AS_NEEDED").contains(item.frequency())
            ? "PRN"
            : "FIXED";
        if (!expectedScheduling.equals(item.scheduling())) {
            throw invalid("O aprazamento não corresponde à frequência de reabilitação selecionada");
        }

        String justification = item.clinicalJustification() == null
            ? ""
            : item.clinicalJustification().trim();
        if (justification.length() > 500) {
            throw invalid("A justificativa clínica excede o limite de 500 caracteres");
        }
        if (
            PHYSIOTHERAPY.contains(item.specialty()) &&
            HIGH_FREQUENCIES.contains(item.frequency()) &&
            justification.isBlank()
        ) {
            throw invalid("Informe a justificativa clínica para duas ou três sessões diárias de fisioterapia");
        }
    }

    private void requireOption(
        String code,
        java.util.List<RehabilitationPrescriptionCatalog.Option> options,
        String label
    ) {
        if (!notBlank(code) || !catalog.contains(options, code)) {
            throw invalid("Selecione uma opção válida para " + label);
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private BusinessValidationException invalid(String message) {
        return new BusinessValidationException(message);
    }
}
