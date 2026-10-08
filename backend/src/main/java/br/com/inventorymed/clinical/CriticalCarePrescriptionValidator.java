package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CriticalCarePrescriptionValidator {

    private static final Set<String> DEEP_SEDATION_TARGETS = Set.of(
        "RASS_MINUS_4", "RASS_MINUS_5", "BIS_40_60"
    );
    private final CriticalCareCatalogService catalog;

    public CriticalCarePrescriptionValidator(CriticalCareCatalogService catalog) {
        this.catalog = catalog;
    }

    public void validate(CriticalCarePrescriptionRequest request) {
        if (request == null) throw invalid("Selecione ao menos um item de cuidados críticos");
        List<CriticalCarePrescriptionRequest.VasoactiveDrug> vasoactives = safe(request.vasoactiveDrugs());
        List<CriticalCarePrescriptionRequest.SedationDrug> sedation = safe(request.sedationAnalgesiaBnm());
        List<CriticalCarePrescriptionRequest.EmergencyMedication> emergency = safe(request.emergencyMedications());
        if (vasoactives.size() > 8 || sedation.size() > 8 || emergency.size() > 8) {
            throw invalid("Cada grupo aceita no máximo oito itens");
        }
        if (vasoactives.isEmpty() && sedation.isEmpty() && emergency.isEmpty()) {
            throw invalid("Selecione ao menos um item de cuidados críticos");
        }
        requireOption(request.clinicalContext(), CriticalCareCatalogService.CLINICAL_CONTEXTS, "contexto clínico");
        validateVasoactives(vasoactives);
        validateSedation(sedation);
        validateEmergency(emergency);
    }

    private void validateVasoactives(List<CriticalCarePrescriptionRequest.VasoactiveDrug> values) {
        Set<Integer> identifiers = new HashSet<>();
        for (CriticalCarePrescriptionRequest.VasoactiveDrug value : values) {
            requireIdentifier(value == null ? null : value.id(), identifiers, "drogas vasoativas");
            CriticalCareCatalog.VasoactiveOption drug = value == null ? null : catalog.vasoactive(value.drug());
            if (drug == null) throw invalid("Selecione uma droga vasoativa válida");
            requireText(value.dilution(), "diluição da droga vasoativa", 500);
            requireText(value.finalConcentration(), "concentração final da droga vasoativa", 160);
            requireRate(value.initialRate(), "vazão inicial da droga vasoativa");
            requireOption(value.rateUnit(), CriticalCareCatalogService.VASOACTIVE_RATE_UNITS, "unidade da vazão");
            requireOption(value.vascularAccess(), CriticalCareCatalogService.VASCULAR_ACCESSES, "acesso vascular");
            requireOption(value.bloodPressureMonitoring(), CriticalCareCatalogService.BLOOD_PRESSURE_MONITORING,
                "monitorização pressórica");
            requireText(value.therapeuticGoal(), "meta terapêutica da droga vasoativa", 400);
            if (!"CONTINUOUS".equals(value.scheduling())) {
                throw invalid("Drogas vasoativas deste módulo exigem aprazamento contínuo");
            }
            if (drug.centralAccessRequired() && !"CVC".equals(value.vascularAccess())) {
                throw invalid(drug.label() + " exige CVC neste módulo por segurança do acesso");
            }
        }
    }

    private void validateSedation(List<CriticalCarePrescriptionRequest.SedationDrug> values) {
        Set<Integer> identifiers = new HashSet<>();
        boolean hasNeuromuscularBlocker = false;
        boolean hasDeepSedation = false;
        for (CriticalCarePrescriptionRequest.SedationDrug value : values) {
            requireIdentifier(value == null ? null : value.id(), identifiers, "sedação e bloqueio neuromuscular");
            CriticalCareCatalog.SedationOption drug = value == null ? null : catalog.sedation(value.drug());
            if (drug == null) throw invalid("Selecione um medicamento válido de sedação, analgesia ou BNM");
            requireText(value.preparation(), "diluição ou apresentação do medicamento", 500);
            requireOption(value.administrationMode(), CriticalCareCatalogService.ADMINISTRATION_MODES,
                "modalidade de administração");
            if (!drug.allowedModes().contains(value.administrationMode())) {
                throw invalid("Modalidade de administração incompatível com " + drug.label());
            }
            requireRate(value.rateDoseValue(), "vazão ou dose do medicamento");
            requireOption(value.rateDoseUnit(), CriticalCareCatalogService.SEDATION_RATE_UNITS,
                "unidade da vazão ou dose");
            requireOption(value.sedationTarget(), CriticalCareCatalogService.SEDATION_TARGETS,
                "meta de sedação RASS ou BIS");
            requireOption(value.ventilatoryStatus(), CriticalCareCatalogService.VENTILATORY_STATUSES,
                "status de ventilação mecânica invasiva");
            requireOption(value.route(), CriticalCareCatalogService.SEDATION_ROUTES, "via do medicamento");
            if ("CONTINUOUS_BIC".equals(value.administrationMode()) && !"EV_BIC".equals(value.route())) {
                throw invalid("Toda infusão contínua de sedação, analgesia ou BNM exige via EV em BIC");
            }
            if ("SINGLE_DOSE".equals(value.administrationMode()) && !"EV".equals(value.route())) {
                throw invalid("A dose única selecionada deve utilizar via endovenosa");
            }
            String expectedScheduling = "CONTINUOUS_BIC".equals(value.administrationMode())
                ? "CONTINUOUS" : "SINGLE_DOSE";
            if (!expectedScheduling.equals(value.scheduling())) {
                throw invalid("Selecione o aprazamento compatível com a modalidade de administração");
            }
            boolean blocker = "NEUROMUSCULAR_BLOCKER".equals(drug.category());
            hasNeuromuscularBlocker |= blocker;
            hasDeepSedation |= !blocker && DEEP_SEDATION_TARGETS.contains(value.sedationTarget());
        }
        if (hasNeuromuscularBlocker && !hasDeepSedation) {
            throw invalid("BNM exige analgesia ou sedação profunda associada com meta RASS -4/-5 ou BIS 40–60");
        }
    }

    private void validateEmergency(List<CriticalCarePrescriptionRequest.EmergencyMedication> values) {
        Set<Integer> identifiers = new HashSet<>();
        for (CriticalCarePrescriptionRequest.EmergencyMedication value : values) {
            requireIdentifier(value == null ? null : value.id(), identifiers, "antídotos e emergência");
            if (value == null || catalog.emergency(value.drug()) == null) {
                throw invalid("Selecione um antídoto ou medicamento de emergência válido");
            }
            requireText(value.doseAdministration(), "dose e administração do medicamento de emergência", 300);
            requireOption(value.route(), CriticalCareCatalogService.EMERGENCY_ROUTES, "via de emergência");
            requireText(value.emergencyIndication(), "indicação clínica ou motivo da reversão", 500);
            requireOption(value.scheduling(), CriticalCareCatalogService.EMERGENCY_SCHEDULING,
                "aprazamento de emergência");
        }
    }

    private void requireIdentifier(Integer id, Set<Integer> identifiers, String label) {
        if (id == null || id < 1 || !identifiers.add(id)) {
            throw invalid("Os itens de " + label + " precisam de identificadores únicos");
        }
    }

    private void requireOption(String code, List<CriticalCareCatalog.Option> options, String label) {
        if (!notBlank(code) || options.stream().noneMatch(option -> option.code().equals(code))) {
            throw invalid("Selecione uma opção válida para " + label);
        }
    }

    private void requireRate(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0 || value.compareTo(BigDecimal.valueOf(5000)) > 0) {
            throw invalid("Informe " + label + " dentro do limite permitido");
        }
    }

    private void requireText(String value, String label, int maximumLength) {
        if (!notBlank(value)) throw invalid("Informe " + label);
        if (value.length() > maximumLength) throw invalid("O campo " + label + " excede o limite permitido");
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private BusinessValidationException invalid(String message) {
        return new BusinessValidationException(message);
    }
}
