package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class MedicationTherapyService {

    private final MedicationTherapyCatalogService catalog;
    private final MedicationTherapyValidator validator;
    private final ObjectMapper objectMapper;

    public MedicationTherapyService(
        MedicationTherapyCatalogService catalog,
        MedicationTherapyValidator validator,
        ObjectMapper objectMapper
    ) {
        this.catalog = catalog;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public MedicationTherapyCatalog catalog() {
        return catalog.catalog();
    }

    public MedicationTherapyResponse preview(MedicationTherapyRequest request) {
        validator.validate(request);
        List<String> sections = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        List<MedicationTherapyResponse.OrderRow> rows = new ArrayList<>();
        List<String> supplies = new ArrayList<>();
        List<String> alerts = new ArrayList<>();

        appendAntimicrobials(request, sections, summaries, rows, supplies, alerts);
        appendProphylaxes(request, sections, summaries, rows, supplies, alerts);
        appendContinuous(request, sections, summaries, rows);
        appendSymptomatics(request, sections, summaries, rows);
        alerts.add("CONFIRMAR DOSE, APRESENTAÇÃO, VIA, FREQUÊNCIA, INTERAÇÕES E ALERGIAS ANTES DA FINALIZAÇÃO.");
        alerts.add("INSUMOS E EQUIPAMENTOS SÃO ITENS PARA CONFERÊNCIA; NENHUMA COBRANÇA É GERADA AUTOMATICAMENTE.");

        return new MedicationTherapyResponse(
            new MedicationTherapyResponse.StructuredMedicationTherapy(
                String.join(" + ", summaries) + ".",
                String.join("\n\n", sections),
                List.copyOf(rows)
            ),
            new MedicationTherapyResponse.BillingAudit(
                List.copyOf(new LinkedHashSet<>(supplies)),
                List.copyOf(new LinkedHashSet<>(alerts))
            )
        );
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final MedicationTherapyRequest request;
        try {
            request = objectMapper.convertValue(value, MedicationTherapyRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados da terapia medicamentosa estão em formato inválido");
        }
        MedicationTherapyResponse response = preview(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", request);
        stored.put("structuredMedicationTherapy", response.structuredMedicationTherapy());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private void appendAntimicrobials(
        MedicationTherapyRequest request,
        List<String> sections,
        List<String> summaries,
        List<MedicationTherapyResponse.OrderRow> rows,
        List<String> supplies,
        List<String> alerts
    ) {
        if (safe(request.antimicrobials()).isEmpty()) return;
        List<String> details = new ArrayList<>();
        MedicationTherapyRequest.RenalFunction renal = request.renalFunction();
        String renalDescription = label(MedicationTherapyCatalogService.RENAL_FUNCTION_MEASURES, renal.measure()) +
            ": " + decimal(renal.valueMlMin());
        for (MedicationTherapyRequest.Antimicrobial value : request.antimicrobials()) {
            String drug = antimicrobialName(value);
            String route = label(MedicationTherapyCatalogService.ANTIMICROBIAL_ROUTES, value.route());
            String scheduling = label(MedicationTherapyCatalogService.ANTIMICROBIAL_SCHEDULING,
                value.scheduling());
            String frequency = frequency(value.frequency(), value.scheduling());
            details.add("- D" + value.treatmentDay() + " — " + drug + ": " + value.dosePreparation().trim() +
                " — " + route + " — " + scheduling + frequencySuffix(frequency) + ".");
            details.add("  * FOCO INFECCIOSO: " + value.infectionFocus().trim() + ". CCIH: " +
                label(MedicationTherapyCatalogService.CCIH_STATUSES, value.ccihStatus()) +
                optionalDetail(value.ccihOpinion()) + ".");
            details.add("  * FUNÇÃO RENAL: " + renalDescription + ". " +
                label(MedicationTherapyCatalogService.RENAL_DOSE_ASSESSMENTS,
                    value.renalDoseAssessment()) + ".");
            if (notBlank(value.loadingDose())) details.add("  * DOSE DE ATAQUE: " + value.loadingDose().trim() + ".");
            if (notBlank(value.conditionalTrigger())) {
                details.add("  * CRITÉRIO CLÍNICO: " + value.conditionalTrigger().trim() + ".");
            }
            summaries.add("D" + value.treatmentDay() + " " + drug);
            rows.add(new MedicationTherapyResponse.OrderRow(
                "ANTIMICROBIANOS", "D" + value.treatmentDay() + " — " + drug + " — " +
                    value.dosePreparation().trim() + " — FOCO: " + value.infectionFocus().trim(),
                route, frequency, scheduling
            ));
            appendAntimicrobialSupplies(value, drug, supplies);
            if ("REQUESTED".equals(value.ccihStatus())) {
                alerts.add(drug + ": PARECER DA CCIH AINDA PENDENTE; REAVALIAR CONFORME PROTOCOLO INSTITUCIONAL.");
            }
        }
        alerts.add("ANTIMICROBIANOS: REAVALIAR DIARIAMENTE INDICAÇÃO, DURAÇÃO, CULTURAS E POSSIBILIDADE DE DESCALONAMENTO.");
        sections.add("11. ANTIMICROBIANOS E ANTIBIOTICOTERAPIA:\n" + String.join("\n", details));
    }

    private void appendAntimicrobialSupplies(
        MedicationTherapyRequest.Antimicrobial value,
        String drug,
        List<String> supplies
    ) {
        if (!Set.of("BOLUS", "RAPID_INFUSION").contains(value.administrationMode())) return;
        supplies.add(drug + " — CONFERIR SERINGA(S) E AMPOLA(S) EFETIVAMENTE DISPENSADAS E UTILIZADAS.");
        supplies.add(label(MedicationTherapyCatalogService.DILUENTS, value.diluent()) +
            " — CONFERIR UNIDADE EFETIVAMENTE UTILIZADA.");
        supplies.add(label(MedicationTherapyCatalogService.INFUSION_SETS, value.infusionSet()) +
            " — CONFERIR INSTALAÇÃO E USO REGISTRADO.");
    }

    private void appendProphylaxes(
        MedicationTherapyRequest request,
        List<String> sections,
        List<String> summaries,
        List<MedicationTherapyResponse.OrderRow> rows,
        List<String> supplies,
        List<String> alerts
    ) {
        if (safe(request.prophylaxes()).isEmpty()) return;
        List<String> details = new ArrayList<>();
        for (MedicationTherapyRequest.HospitalProphylaxis value : request.prophylaxes()) {
            MedicationTherapyCatalog.ProphylaxisOption option = catalog.prophylaxis(value.intervention());
            String route = label(MedicationTherapyCatalogService.PROPHYLAXIS_ROUTES, value.route());
            String scheduling = label(MedicationTherapyCatalogService.PROPHYLAXIS_SCHEDULING,
                value.scheduling());
            String frequency = frequency(value.frequency(), value.scheduling());
            details.add("- " + option.label() + ": " + value.dosePreparation().trim() + " — " + route +
                " — " + scheduling + frequencySuffix(frequency) + ".");
            if (notBlank(value.conditionalTrigger())) {
                details.add("  * CRITÉRIO CLÍNICO: " + value.conditionalTrigger().trim() + ".");
            }
            if (notBlank(value.suspensionReason())) {
                details.add("  * MOTIVO DA SUSPENSÃO: " + value.suspensionReason().trim() + ".");
            }
            summaries.add(option.label());
            rows.add(new MedicationTherapyResponse.OrderRow(
                "PROFILAXIAS TEV / LAMG", option.label() + " — " + value.dosePreparation().trim(),
                route, frequency, scheduling
            ));
            if (option.filledSyringe() && "ENOXAPARIN".equals(option.code())) {
                supplies.add("ENOXAPARINA — CONFERIR SERINGA PREENCHIDA DISPENSADA E DESCARTE SEGURO DE PERFUROCORTANTE.");
            }
            if ("IPC".equals(option.code())) {
                supplies.add("CPI — CONFERIR DIÁRIA DO APARELHO E PAR DE PERNEIRAS DESCARTÁVEIS CONFORME CONTRATO E TUSS VIGENTE.");
            }
        }
        if (request.bleedingRisk() != null && request.bleedingRisk().plateletCount() != null) {
            alerts.add("PROFILAXIA DE TEV: PLAQUETAS INFORMADAS " + request.bleedingRisk().plateletCount() +
                "/MM³; SANGRAMENTO ATIVO: " + (Boolean.TRUE.equals(request.bleedingRisk().activeBleeding())
                    ? "SIM" : "NÃO") + ".");
        }
        sections.add("12. PROFILAXIAS HOSPITALARES (TEV / LAMG):\n" + String.join("\n", details));
    }

    private void appendContinuous(
        MedicationTherapyRequest request,
        List<String> sections,
        List<String> summaries,
        List<MedicationTherapyResponse.OrderRow> rows
    ) {
        if (safe(request.continuousMedications()).isEmpty()) return;
        List<String> details = new ArrayList<>();
        for (MedicationTherapyRequest.ContinuousMedication value : request.continuousMedications()) {
            String route = label(MedicationTherapyCatalogService.MEDICATION_ROUTES, value.route());
            String scheduling = label(MedicationTherapyCatalogService.CONTINUOUS_MEDICATION_SCHEDULING,
                value.scheduling());
            String reconciliation = label(MedicationTherapyCatalogService.RECONCILIATION_STATUSES,
                value.reconciliationStatus());
            String frequency = frequency(value.frequency(), value.scheduling());
            details.add("- " + value.medication().trim() + ": " + value.dosePreparation().trim() +
                " — " + route + " — " + scheduling + frequencySuffix(frequency) + ".");
            details.add("  * RECONCILIAÇÃO: " + reconciliation +
                optionalClinicalDetail(value.conditionalTrigger(), value.suspensionReason()) + ".");
            summaries.add(value.medication().trim());
            rows.add(new MedicationTherapyResponse.OrderRow(
                "USO CONTÍNUO / RECONCILIAÇÃO", value.medication().trim() + " — " +
                    value.dosePreparation().trim() + " — " + reconciliation,
                route, frequency, scheduling
            ));
        }
        sections.add("13. MEDICAMENTOS DE USO CONTÍNUO E ROTINA:\n" + String.join("\n", details));
    }

    private void appendSymptomatics(
        MedicationTherapyRequest request,
        List<String> sections,
        List<String> summaries,
        List<MedicationTherapyResponse.OrderRow> rows
    ) {
        if (safe(request.analgesiaSymptomatics()).isEmpty()) return;
        List<String> details = new ArrayList<>();
        for (MedicationTherapyRequest.SymptomaticMedication value : request.analgesiaSymptomatics()) {
            String drug = symptomaticName(value);
            String route = label(MedicationTherapyCatalogService.MEDICATION_ROUTES, value.route());
            String scheduling = label(MedicationTherapyCatalogService.SYMPTOMATIC_SCHEDULING,
                value.scheduling());
            String frequency = frequency(value.frequency(), value.scheduling());
            details.add("- " + drug + ": " + value.dosePreparation().trim() + " — " + route +
                " — " + scheduling + frequencySuffix(frequency) + ".");
            if (notBlank(value.trigger())) details.add("  * GATILHO / CRITÉRIO: " + value.trigger().trim() + ".");
            if (notBlank(value.minimumInterval())) {
                details.add("  * INTERVALO MÍNIMO: " + value.minimumInterval().trim() + ".");
            }
            summaries.add(drug);
            rows.add(new MedicationTherapyResponse.OrderRow(
                "ANALGESIA / SINTOMÁTICOS", drug + " — " + value.dosePreparation().trim() +
                    (notBlank(value.trigger()) ? " — GATILHO: " + value.trigger().trim() : ""),
                route, frequency, scheduling
            ));
        }
        sections.add("14. ANALGESIA, ANTI-INFLAMATÓRIOS E SINTOMÁTICOS:\n" + String.join("\n", details));
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        Map<?, ?> root = (Map<?, ?>) value;
        allowedKeys(root, Set.of(
            "clinicalContext", "renalFunction", "bleedingRisk", "antimicrobials",
            "prophylaxes", "continuousMedications", "analgesiaSymptomatics"
        ));
        nestedObject(root.get("renalFunction"), Set.of("measure", "valueMlMin"));
        nestedObject(root.get("bleedingRisk"), Set.of("plateletCount", "activeBleeding"));
        nestedList(root.get("antimicrobials"), Set.of(
            "id", "drug", "customDrug", "dosePreparation", "route", "administrationMode",
            "diluent", "infusionSet", "frequency", "scheduling", "loadingDose",
            "conditionalTrigger", "treatmentDay", "infectionFocus", "ccihStatus",
            "ccihOpinion", "renalDoseAssessment"
        ));
        nestedList(root.get("prophylaxes"), Set.of(
            "id", "intervention", "dosePreparation", "route", "frequency", "scheduling",
            "conditionalTrigger", "suspensionReason"
        ));
        nestedList(root.get("continuousMedications"), Set.of(
            "id", "medication", "dosePreparation", "route", "frequency",
            "reconciliationStatus", "scheduling", "conditionalTrigger", "suspensionReason"
        ));
        nestedList(root.get("analgesiaSymptomatics"), Set.of(
            "id", "drug", "customDrug", "dosePreparation", "route", "frequency",
            "scheduling", "trigger", "minimumInterval"
        ));
    }

    private void nestedObject(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        allowedKeys((Map<?, ?>) value, allowed);
    }

    private void nestedList(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof List<?>)) invalidFormat();
        for (Object item : (List<?>) value) {
            if (!(item instanceof Map<?, ?>)) invalidFormat();
            allowedKeys((Map<?, ?>) item, allowed);
        }
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("A terapia medicamentosa contém campos não permitidos");
        }
    }

    private String antimicrobialName(MedicationTherapyRequest.Antimicrobial value) {
        return "OTHER".equals(value.drug())
            ? value.customDrug().trim()
            : catalog.antimicrobial(value.drug()).label();
    }

    private String symptomaticName(MedicationTherapyRequest.SymptomaticMedication value) {
        return "OTHER".equals(value.drug())
            ? value.customDrug().trim()
            : catalog.symptomatic(value.drug()).label();
    }

    private String frequency(String value, String scheduling) {
        if (notBlank(value)) return value.trim();
        if (Set.of("SINGLE_DOSE", "SUSPEND", "TEMPORARILY_SUSPENDED").contains(scheduling)) return "—";
        return "CONFORME CRITÉRIO";
    }

    private String frequencySuffix(String value) {
        return "—".equals(value) ? "" : " — FREQUÊNCIA: " + value;
    }

    private String optionalDetail(String value) {
        return notBlank(value) ? " — " + value.trim() : "";
    }

    private String optionalClinicalDetail(String trigger, String suspensionReason) {
        if (notBlank(trigger)) return " — CRITÉRIO: " + trigger.trim();
        if (notBlank(suspensionReason)) return " — MOTIVO: " + suspensionReason.trim();
        return "";
    }

    private String label(List<MedicationTherapyCatalog.Option> options, String code) {
        return catalog.optionLabel(options, code);
    }

    private String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void invalidFormat() {
        throw new BusinessValidationException("Dados da terapia medicamentosa estão em formato inválido");
    }
}
