package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticExamValidator {

    private final DiagnosticExamCatalogService catalog;

    public DiagnosticExamValidator(DiagnosticExamCatalogService catalog) {
        this.catalog = catalog;
    }

    public void validate(DiagnosticExamRequest request) {
        if (request == null) throw invalid("Informe a solicitação de exames");
        requireOption(
            request.clinicalContext(),
            catalog.catalog().clinicalContexts(),
            "Selecione o contexto clínico"
        );
        validateCodes(request.bloodExams(), true);
        validateCodes(request.urineExams(), false);
        validateHighCostLaboratory(request);
        validateTomography(request.tomography());
        validateXray(request.xray());
        validateUltrasound(request.ultrasound());
        validateEcg(request.electrocardiogram());
        if (selectionCount(request) == 0) {
            throw invalid("Selecione ao menos um exame diagnóstico");
        }
    }

    private void validateCodes(List<String> values, boolean blood) {
        if (values == null) return;
        Set<String> unique = new HashSet<>();
        for (String value : values) {
            DiagnosticExamCatalog.ExamOption option = blood
                ? catalog.bloodExam(value)
                : catalog.urineExam(value);
            if (option == null || !unique.add(value)) {
                throw invalid("A lista de exames contém item inválido ou duplicado");
            }
        }
    }

    private void validateHighCostLaboratory(DiagnosticExamRequest request) {
        boolean requiresJustification = safe(request.bloodExams()).stream()
            .map(catalog::bloodExam)
            .anyMatch(option -> option != null && option.highCost());
        if (requiresJustification) {
            requireText(
                request.laboratoryClinicalJustification(),
                "Informe a justificativa clínica para Troponina e/ou Procalcitonina"
            );
        }
    }

    private void validateTomography(DiagnosticExamRequest.Tomography value) {
        if (value == null) return;
        if (catalog.tomographyRegion(value.region()) == null) {
            throw invalid("Selecione uma região válida para a tomografia");
        }
        requireOption(
            value.contrast(),
            catalog.catalog().contrastOptions(),
            "Selecione o uso de contraste da tomografia"
        );
        requireText(value.clinicalJustification(), "Informe a justificativa clínica da tomografia");
        if (!"WITHOUT_CONTRAST".equals(value.contrast())) {
            requireText(
                value.previousCreatinineMgDl(),
                "Informe a creatinina prévia antes da tomografia com contraste"
            );
            try {
                if (new BigDecimal(value.previousCreatinineMgDl().replace(',', '.')).signum() < 0) {
                    throw invalid("A creatinina prévia deve ser um valor válido em mg/dL");
                }
            } catch (NumberFormatException exception) {
                throw invalid("A creatinina prévia deve ser um valor válido em mg/dL");
            }
        }
    }

    private void validateXray(DiagnosticExamRequest.Xray value) {
        if (value == null) return;
        DiagnosticExamCatalog.XrayProfile profile = catalog.xrayProfile(value.region());
        if (profile == null) throw invalid("Selecione uma região válida para o raio-X");
        requireText(value.clinicalJustification(), "Informe a justificativa clínica do raio-X");
        if (profile.pairedSite()) {
            if (!Set.of("RIGHT", "LEFT", "BILATERAL").contains(value.laterality())) {
                throw invalid("Informe a lateralidade do raio-X");
            }
        } else if (!"NOT_APPLICABLE".equals(value.laterality())) {
            throw invalid("A lateralidade informada não se aplica ao raio-X selecionado");
        }
        if (value.incidences() == null || value.incidences().isEmpty()) {
            throw invalid("Selecione ao menos uma incidência para o raio-X");
        }
        Set<String> allowed = profile.incidences().stream()
            .map(DiagnosticExamCatalog.Option::code).collect(java.util.stream.Collectors.toSet());
        if (new HashSet<>(value.incidences()).size() != value.incidences().size() ||
            value.incidences().stream().anyMatch(item -> !allowed.contains(item))) {
            throw invalid("As incidências informadas não pertencem ao perfil de raio-X selecionado");
        }
    }

    private void validateUltrasound(DiagnosticExamRequest.Ultrasound value) {
        if (value == null) return;
        DiagnosticExamCatalog.UltrasoundOption option = catalog.ultrasound(value.type());
        if (option == null) throw invalid("Selecione uma ultrassonografia válida");
        requireText(value.clinicalJustification(), "Informe a justificativa clínica da ultrassonografia");
        if (option.pairedSite()) {
            if (!Set.of("RIGHT", "LEFT", "BILATERAL").contains(value.laterality())) {
                throw invalid("Informe a lateralidade da ultrassonografia");
            }
        } else if (!"NOT_APPLICABLE".equals(value.laterality())) {
            throw invalid("A lateralidade informada não se aplica à ultrassonografia selecionada");
        }
    }

    private void validateEcg(DiagnosticExamRequest.Electrocardiogram value) {
        if (value == null) return;
        if (!"ECG_12_LEADS".equals(value.type())) {
            throw invalid("Selecione um eletrocardiograma válido");
        }
        requireOption(
            value.priority(),
            catalog.catalog().ecgPriorities(),
            "Selecione a prioridade do eletrocardiograma"
        );
        requireText(value.clinicalJustification(), "Informe a justificativa clínica do eletrocardiograma");
    }

    private int selectionCount(DiagnosticExamRequest request) {
        return safe(request.bloodExams()).size() + safe(request.urineExams()).size() +
            (request.tomography() == null ? 0 : 1) + (request.xray() == null ? 0 : 1) +
            (request.ultrasound() == null ? 0 : 1) +
            (request.electrocardiogram() == null ? 0 : 1);
    }

    private void requireOption(
        String value,
        List<DiagnosticExamCatalog.Option> options,
        String message
    ) {
        if (value == null || options.stream().noneMatch(item -> item.code().equals(value))) {
            throw invalid(message);
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw invalid(message);
    }

    private List<String> safe(List<String> values) {
        return values == null ? List.of() : values;
    }

    private BusinessValidationException invalid(String message) {
        return new BusinessValidationException(message);
    }
}
