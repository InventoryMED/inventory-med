package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MedicationTherapyValidator {

    private static final int MAX_ITEMS_PER_GROUP = 20;
    private final MedicationTherapyCatalogService catalog;

    public MedicationTherapyValidator(MedicationTherapyCatalogService catalog) {
        this.catalog = catalog;
    }

    public void validate(MedicationTherapyRequest request) {
        if (request == null) throw invalid("Selecione ao menos um item de terapia medicamentosa");
        List<MedicationTherapyRequest.Antimicrobial> antimicrobials = safe(request.antimicrobials());
        List<MedicationTherapyRequest.HospitalProphylaxis> prophylaxes = safe(request.prophylaxes());
        List<MedicationTherapyRequest.ContinuousMedication> continuous = safe(request.continuousMedications());
        List<MedicationTherapyRequest.SymptomaticMedication> symptomatics = safe(request.analgesiaSymptomatics());
        if (antimicrobials.size() > 12 || prophylaxes.size() > 12 ||
            continuous.size() > MAX_ITEMS_PER_GROUP || symptomatics.size() > MAX_ITEMS_PER_GROUP) {
            throw invalid("A quantidade de itens excede o limite permitido por grupo");
        }
        if (antimicrobials.isEmpty() && prophylaxes.isEmpty() && continuous.isEmpty() && symptomatics.isEmpty()) {
            throw invalid("Selecione ao menos um item de terapia medicamentosa");
        }
        requireOption(request.clinicalContext(), MedicationTherapyCatalogService.CLINICAL_CONTEXTS,
            "contexto clínico");
        validateAntimicrobials(antimicrobials, request.renalFunction());
        validateProphylaxes(prophylaxes, request.bleedingRisk());
        validateContinuous(continuous);
        validateSymptomatics(symptomatics);
    }

    private void validateAntimicrobials(
        List<MedicationTherapyRequest.Antimicrobial> values,
        MedicationTherapyRequest.RenalFunction renalFunction
    ) {
        if (values.isEmpty()) return;
        if (renalFunction == null) throw invalid("Informe a função renal antes de prescrever antimicrobianos");
        requireOption(renalFunction.measure(), MedicationTherapyCatalogService.RENAL_FUNCTION_MEASURES,
            "medida da função renal");
        requireRange(renalFunction.valueMlMin(), BigDecimal.ONE, BigDecimal.valueOf(200),
            "valor da função renal");

        Set<Integer> identifiers = new HashSet<>();
        for (MedicationTherapyRequest.Antimicrobial value : values) {
            requireIdentifier(value == null ? null : value.id(), identifiers, "antimicrobianos");
            MedicationTherapyCatalog.MedicationOption drug = value == null ? null : catalog.antimicrobial(value.drug());
            if (drug == null) throw invalid("Selecione um antimicrobiano válido");
            requireCustomName(value.drug(), value.customDrug(), "antimicrobiano");
            requireText(value.dosePreparation(), "dose e preparo do antimicrobiano", 500);
            requireOption(value.route(), MedicationTherapyCatalogService.ANTIMICROBIAL_ROUTES,
                "via do antimicrobiano");
            requireOption(value.administrationMode(),
                MedicationTherapyCatalogService.ANTIMICROBIAL_ADMINISTRATION_MODES,
                "modalidade de administração do antimicrobiano");
            validateAntimicrobialRoute(value);
            requireOption(value.diluent(), MedicationTherapyCatalogService.DILUENTS, "diluente");
            requireOption(value.infusionSet(), MedicationTherapyCatalogService.INFUSION_SETS, "equipo");
            validateInjectableSupplies(value);
            requireOption(value.scheduling(), MedicationTherapyCatalogService.ANTIMICROBIAL_SCHEDULING,
                "aprazamento do antimicrobiano");
            validateAntimicrobialScheduling(value);
            if (value.treatmentDay() == null || value.treatmentDay() < 1 || value.treatmentDay() > 999) {
                throw invalid("Informe o dia de tratamento do antimicrobiano entre D1 e D999");
            }
            requireText(value.infectionFocus(), "foco infeccioso", 500);
            requireOption(value.ccihStatus(), MedicationTherapyCatalogService.CCIH_STATUSES,
                "situação da CCIH");
            if (!"NOT_APPLICABLE".equals(value.ccihStatus())) {
                requireText(value.ccihOpinion(), "referência ou parecer da CCIH", 500);
            }
            requireOption(value.renalDoseAssessment(), MedicationTherapyCatalogService.RENAL_DOSE_ASSESSMENTS,
                "avaliação do ajuste renal");
        }
    }

    private void validateAntimicrobialRoute(MedicationTherapyRequest.Antimicrobial value) {
        if ("VO".equals(value.route()) && !"ORAL".equals(value.administrationMode())) {
            throw invalid("Antimicrobiano por via oral exige modalidade de administração oral");
        }
        if ("IM".equals(value.route()) && !"INTRAMUSCULAR".equals(value.administrationMode())) {
            throw invalid("Antimicrobiano por via IM exige modalidade intramuscular");
        }
        if ("INHALED".equals(value.route()) && !"INHALED".equals(value.administrationMode())) {
            throw invalid("Antimicrobiano inalatório exige modalidade inalatória");
        }
        if ("EV".equals(value.route()) && Set.of("ORAL", "INTRAMUSCULAR", "INHALED")
            .contains(value.administrationMode())) {
            throw invalid("Selecione uma modalidade endovenosa compatível com a via EV");
        }
    }

    private void validateInjectableSupplies(MedicationTherapyRequest.Antimicrobial value) {
        boolean bolusOrRapid = Set.of("BOLUS", "RAPID_INFUSION").contains(value.administrationMode());
        if (bolusOrRapid && "NONE".equals(value.diluent())) {
            throw invalid("Bolus ou infusão rápida exige o registro do diluente");
        }
        if (bolusOrRapid && "NONE".equals(value.infusionSet())) {
            throw invalid("Bolus ou infusão rápida exige o registro do equipo para conferência");
        }
    }

    private void validateAntimicrobialScheduling(MedicationTherapyRequest.Antimicrobial value) {
        if (Set.of("FIXED", "LOADING_FIXED").contains(value.scheduling())) {
            requireText(value.frequency(), "frequência fixa do antimicrobiano", 80);
        }
        if ("LOADING_FIXED".equals(value.scheduling())) {
            requireText(value.loadingDose(), "dose de ataque", 300);
        }
        if ("ACM".equals(value.scheduling())) {
            requireText(value.conditionalTrigger(), "critério clínico do aprazamento ACM", 500);
        }
    }

    private void validateProphylaxes(
        List<MedicationTherapyRequest.HospitalProphylaxis> values,
        MedicationTherapyRequest.BleedingRisk bleedingRisk
    ) {
        Set<Integer> identifiers = new HashSet<>();
        for (MedicationTherapyRequest.HospitalProphylaxis value : values) {
            requireIdentifier(value == null ? null : value.id(), identifiers, "profilaxias hospitalares");
            MedicationTherapyCatalog.ProphylaxisOption intervention =
                value == null ? null : catalog.prophylaxis(value.intervention());
            if (intervention == null) throw invalid("Selecione uma profilaxia hospitalar válida");
            requireText(value.dosePreparation(), "dose, apresentação ou modo de uso da profilaxia", 500);
            requireOption(value.route(), MedicationTherapyCatalogService.PROPHYLAXIS_ROUTES,
                "via ou dispositivo da profilaxia");
            requireOption(value.scheduling(), MedicationTherapyCatalogService.PROPHYLAXIS_SCHEDULING,
                "aprazamento da profilaxia");
            if (intervention.equipment() && !"MECHANICAL".equals(value.route())) {
                throw invalid(intervention.label() + " exige a via dispositivo mecânico");
            }
            if (!intervention.equipment() && "MECHANICAL".equals(value.route())) {
                throw invalid("A profilaxia medicamentosa exige uma via farmacológica válida");
            }
            if ("FIXED".equals(value.scheduling())) {
                requireText(value.frequency(), "frequência fixa da profilaxia", 80);
            }
            if ("ACM".equals(value.scheduling())) {
                requireText(value.conditionalTrigger(), "critério clínico do aprazamento ACM", 500);
            }
            if ("SUSPEND".equals(value.scheduling())) {
                requireText(value.suspensionReason(), "motivo da suspensão da profilaxia", 500);
            }
            if (intervention.anticoagulant()) validateAnticoagulantSafety(value, bleedingRisk);
        }
    }

    private void validateAnticoagulantSafety(
        MedicationTherapyRequest.HospitalProphylaxis value,
        MedicationTherapyRequest.BleedingRisk bleedingRisk
    ) {
        if (bleedingRisk == null || bleedingRisk.plateletCount() == null || bleedingRisk.activeBleeding() == null) {
            throw invalid("Informe plaquetas e presença de sangramento antes da profilaxia farmacológica de TEV");
        }
        boolean contraindicated = bleedingRisk.plateletCount() < 50_000 || bleedingRisk.activeBleeding();
        if (contraindicated && !"SUSPEND".equals(value.scheduling())) {
            throw invalid("Profilaxia farmacológica de TEV bloqueada: plaquetas abaixo de 50.000 ou sangramento ativo");
        }
    }

    private void validateContinuous(List<MedicationTherapyRequest.ContinuousMedication> values) {
        Set<Integer> identifiers = new HashSet<>();
        for (MedicationTherapyRequest.ContinuousMedication value : values) {
            requireIdentifier(value == null ? null : value.id(), identifiers, "medicamentos de uso contínuo");
            if (value == null) throw invalid("Medicamento de uso contínuo inválido");
            requireText(value.medication(), "nome do medicamento de uso contínuo", 200);
            requireText(value.dosePreparation(), "dose do medicamento de uso contínuo", 500);
            requireOption(value.route(), MedicationTherapyCatalogService.MEDICATION_ROUTES,
                "via do medicamento de uso contínuo");
            requireOption(value.reconciliationStatus(), MedicationTherapyCatalogService.RECONCILIATION_STATUSES,
                "situação da reconciliação medicamentosa");
            requireOption(value.scheduling(), MedicationTherapyCatalogService.CONTINUOUS_MEDICATION_SCHEDULING,
                "aprazamento do medicamento de uso contínuo");
            boolean suspendedStatus = "TEMPORARILY_SUSPENDED".equals(value.reconciliationStatus());
            boolean suspendedScheduling = "TEMPORARILY_SUSPENDED".equals(value.scheduling());
            if (suspendedStatus != suspendedScheduling) {
                throw invalid("Medicamento suspenso temporariamente exige situação e aprazamento compatíveis");
            }
            if (suspendedStatus) requireText(value.suspensionReason(), "motivo da suspensão temporária", 500);
            if ("FIXED".equals(value.scheduling())) {
                requireText(value.frequency(), "frequência do medicamento de uso contínuo", 80);
            }
            if ("ACM".equals(value.scheduling())) {
                requireText(value.conditionalTrigger(), "parâmetro clínico do aprazamento ACM", 500);
            }
        }
    }

    private void validateSymptomatics(List<MedicationTherapyRequest.SymptomaticMedication> values) {
        Set<Integer> identifiers = new HashSet<>();
        for (MedicationTherapyRequest.SymptomaticMedication value : values) {
            requireIdentifier(value == null ? null : value.id(), identifiers,
                "analgesia, anti-inflamatórios e sintomáticos");
            MedicationTherapyCatalog.MedicationOption drug = value == null ? null : catalog.symptomatic(value.drug());
            if (drug == null) throw invalid("Selecione um medicamento sintomático válido");
            requireCustomName(value.drug(), value.customDrug(), "medicamento sintomático");
            requireText(value.dosePreparation(), "dose e preparo do medicamento sintomático", 500);
            requireOption(value.route(), MedicationTherapyCatalogService.MEDICATION_ROUTES,
                "via do medicamento sintomático");
            requireOption(value.scheduling(), MedicationTherapyCatalogService.SYMPTOMATIC_SCHEDULING,
                "aprazamento do medicamento sintomático");
            if (Set.of("FIXED", "PRN_FIXED").contains(value.scheduling())) {
                requireText(value.frequency(), "frequência fixa do medicamento sintomático", 80);
            }
            if (Set.of("PRN", "PRN_FIXED").contains(value.scheduling())) {
                requireText(value.trigger(), "gatilho de disparo do medicamento SN", 500);
                requireText(value.minimumInterval(), "intervalo mínimo do medicamento SN", 80);
            }
            if ("ACM".equals(value.scheduling())) {
                requireText(value.trigger(), "critério clínico do aprazamento ACM", 500);
            }
        }
    }

    private void requireCustomName(String code, String customName, String label) {
        if ("OTHER".equals(code)) requireText(customName, "nome do " + label, 160);
    }

    private void requireIdentifier(Integer id, Set<Integer> identifiers, String label) {
        if (id == null || id < 1 || !identifiers.add(id)) {
            throw invalid("Os itens de " + label + " precisam de identificadores únicos");
        }
    }

    private void requireOption(String code, List<MedicationTherapyCatalog.Option> options, String label) {
        if (!notBlank(code) || options.stream().noneMatch(option -> option.code().equals(code))) {
            throw invalid("Selecione uma opção válida para " + label);
        }
    }

    private void requireRange(BigDecimal value, BigDecimal minimum, BigDecimal maximum, String label) {
        if (value == null || value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
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
