package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class IsolationPrecautionValidator {

    private final IsolationPrecautionCatalogService catalog;

    public IsolationPrecautionValidator(IsolationPrecautionCatalogService catalog) {
        this.catalog = catalog;
    }

    public void validate(IsolationPrecautionRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw invalid("Selecione ao menos uma precaução ou isolamento");
        }
        if (request.items().size() > 8) {
            throw invalid("A prescrição aceita no máximo oito precauções ou isolamentos");
        }
        if (notBlank(request.selectedTemplate()) && !catalog.templateExists(request.selectedTemplate())) {
            throw invalid("Selecione um modelo de precaução válido");
        }

        Set<Integer> identifiers = new HashSet<>();
        for (IsolationPrecautionRequest.Item item : request.items()) {
            validateItem(item, identifiers);
        }
    }

    private void validateItem(IsolationPrecautionRequest.Item item, Set<Integer> identifiers) {
        if (item == null || item.id() == null || item.id() < 1 || !identifiers.add(item.id())) {
            throw invalid("As precauções precisam de identificadores únicos");
        }
        requireOption(
            item.precautionType(), IsolationPrecautionCatalogService.PRECAUTION_TYPES,
            "tipo de precaução"
        );
        if (!notBlank(item.reasonPathogen())) {
            throw invalid("Informe o motivo, patógeno ou condição clínica da precaução");
        }
        if (item.reasonPathogen().trim().length() > 500) {
            throw invalid("O motivo ou patógeno excede o limite de 500 caracteres");
        }
        requireOption(
            item.durationReview(), IsolationPrecautionCatalogService.DURATIONS,
            "duração ou reavaliação"
        );
        requireOption(
            item.scheduling(), IsolationPrecautionCatalogService.SCHEDULING,
            "aprazamento"
        );
    }

    private void requireOption(
        String code,
        java.util.List<IsolationPrecautionCatalog.Option> options,
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
