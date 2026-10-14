package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ClinicalSummaryService {

    private static final Set<String> ROOT_KEYS = Set.of(
        "outcomeType", "dischargeCondition", "clinicalSummary", "primaryCid",
        "dischargeMedications", "dischargeOrientations", "transfer", "death",
        "physicianAttestation"
    );
    private static final Set<String> MEDICATION_KEYS = Set.of("medication", "dosage", "duration");
    private static final Set<String> TRANSFER_KEYS = Set.of(
        "reason", "destinationHospital", "destinationCnes", "devices"
    );
    private static final Set<String> DEATH_KEYS = Set.of(
        "occurredAt", "directCause", "antecedentCause", "underlyingCause", "necropsy"
    );

    private final ClinicalSummaryCatalogService catalog;
    private final ClinicalSummaryValidator validator;
    private final ObjectMapper objectMapper;

    public ClinicalSummaryService(
        ClinicalSummaryCatalogService catalog,
        ClinicalSummaryValidator validator,
        ObjectMapper objectMapper
    ) {
        this.catalog = catalog;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public ClinicalSummaryCatalog catalog() {
        return catalog.catalog();
    }

    public ClinicalSummaryResponse preview(ClinicalSummaryRequest request) {
        return build(normalize(request));
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final ClinicalSummaryRequest request;
        try {
            request = objectMapper.convertValue(value, ClinicalSummaryRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados do resumo clínico estão em formato inválido");
        }
        ClinicalSummaryResponse response = build(normalize(request));
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", response.request());
        stored.put("outcomeReference", response.outcomeReference());
        stored.put("reportText", response.reportText());
        stored.put("auditAlerts", response.auditAlerts());
        return stored;
    }

    private ClinicalSummaryRequest normalize(ClinicalSummaryRequest request) {
        validator.validate(request);
        return new ClinicalSummaryRequest(
            trim(request.outcomeType()),
            nullable(request.dischargeCondition()),
            trim(request.clinicalSummary()),
            nullable(request.primaryCid()),
            normalizeMedications(request.dischargeMedications()),
            normalizeStrings(request.dischargeOrientations()),
            normalizeTransfer(request.transfer()),
            normalizeDeath(request.death()),
            request.physicianAttestation()
        );
    }

    private ClinicalSummaryResponse build(ClinicalSummaryRequest request) {
        ClinicalSummaryResponse.OutcomeReference reference = outcomeReference(request);
        List<String> lines = new ArrayList<>();
        lines.add("DESFECHO: " + label(catalog.catalog().outcomeTypes(), request.outcomeType()) + ".");
        if (request.dischargeCondition() != null) {
            lines.add("CONDIÇÃO NA ALTA: " + label(
                catalog.catalog().dischargeConditions(), request.dischargeCondition()
            ) + ".");
        }
        lines.add("RESUMO DA EVOLUÇÃO CLÍNICA: " + request.clinicalSummary());
        if (request.primaryCid() != null) lines.add("CID PRINCIPAL INFORMADO: " + request.primaryCid() + ".");
        appendDischarge(lines, request);
        appendTransfer(lines, request.transfer());
        appendDeath(lines, request.death());

        List<String> alerts = new ArrayList<>();
        alerts.add("A REFERÊNCIA DE MOTIVO DE SAÍDA DEVE SER CONFERIDA NA COMPETÊNCIA SIGTAP E NO CONTRATO VIGENTES.");
        alerts.add("O RESUMO FINALIZADO REGISTRA AUTORIA E DATA NA AUDITORIA INTERNA DO INVENTORY MED.");
        alerts.add("ASSINATURA ICP-BRASIL AINDA DEPENDE DE INTEGRAÇÃO COM PROVEDOR DE CERTIFICAÇÃO HOMOLOGADO.");
        if (reference.sigtapExitReferenceCode() == null) {
            alerts.add("NÃO HÁ CÓDIGO DE SAÍDA AUTOMÁTICO PARA ESTE DESFECHO; EXIJA CONFERÊNCIA DO FATURAMENTO.");
        }
        if (!reference.closesAdmission()) {
            alerts.add("ALTA DA UTI PARA ENFERMARIA NÃO ENCERRA A INTERNAÇÃO; USE A TRANSFERÊNCIA INTERNA DE LEITO.");
        }
        return new ClinicalSummaryResponse(request, reference, String.join("\n", lines), List.copyOf(alerts));
    }

    private ClinicalSummaryResponse.OutcomeReference outcomeReference(ClinicalSummaryRequest request) {
        if ("OBITO".equals(request.outcomeType())) {
            return new ClinicalSummaryResponse.OutcomeReference(
                "OBITO", "61", "ÓBITO COM DECLARAÇÃO", true
            );
        }
        if ("TRANSFERENCIA_HOSPITALAR".equals(request.outcomeType())) {
            return new ClinicalSummaryResponse.OutcomeReference(
                "TRANSFERENCIA", null, "TRANSFERÊNCIA HOSPITALAR — CONFERIR", true
            );
        }
        boolean closesAdmission = !"ALTA_UTI_PARA_ENFERMARIA".equals(request.dischargeCondition());
        String code = catalog.exitReferenceForCondition(request.dischargeCondition());
        return new ClinicalSummaryResponse.OutcomeReference(
            "ALTA_MELHORA",
            code,
            label(catalog.catalog().dischargeConditions(), request.dischargeCondition()),
            closesAdmission
        );
    }

    private void appendDischarge(List<String> lines, ClinicalSummaryRequest request) {
        if (!"ALTA_HOSPITALAR".equals(request.outcomeType())) return;
        if (!request.dischargeMedications().isEmpty()) {
            lines.add("PRESCRIÇÃO DE ALTA:");
            request.dischargeMedications().forEach(item -> lines.add(
                "- " + item.medication() + " — " + item.dosage() +
                    (item.duration() == null ? "" : " — " + item.duration())
            ));
        }
        if (!request.dischargeOrientations().isEmpty()) {
            lines.add("ORIENTAÇÕES PÓS-ALTA:");
            request.dischargeOrientations().forEach(item -> lines.add("- " + item));
        }
    }

    private void appendTransfer(List<String> lines, ClinicalSummaryRequest.TransferDetails transfer) {
        if (transfer == null) return;
        lines.add("MOTIVO DA TRANSFERÊNCIA: " + label(catalog.catalog().transferReasons(), transfer.reason()) + ".");
        lines.add("DESTINO: " + transfer.destinationHospital() +
            (transfer.destinationCnes() == null ? "" : " — CNES " + transfer.destinationCnes()) + ".");
        if (!transfer.devices().isEmpty()) {
            lines.add("DISPOSITIVOS NA SAÍDA: " + transfer.devices().stream()
                .map(code -> label(catalog.catalog().devices(), code))
                .collect(java.util.stream.Collectors.joining(", ")) + ".");
        }
    }

    private void appendDeath(List<String> lines, ClinicalSummaryRequest.DeathDetails death) {
        if (death == null) return;
        lines.add("DATA E HORA DO ÓBITO: " + death.occurredAt() + ".");
        lines.add("CAUSA DIRETA (A): " + death.directCause() + ".");
        if (death.antecedentCause() != null) lines.add("CAUSA ANTECEDENTE (B): " + death.antecedentCause() + ".");
        lines.add("CAUSA BÁSICA (C): " + death.underlyingCause() + ".");
        lines.add("NECROPSIA / ENCAMINHAMENTO: " + label(catalog.catalog().necropsyOptions(), death.necropsy()) + ".");
    }

    private List<ClinicalSummaryRequest.DischargeMedication> normalizeMedications(
        List<ClinicalSummaryRequest.DischargeMedication> values
    ) {
        if (values == null) return List.of();
        return values.stream().map(item -> new ClinicalSummaryRequest.DischargeMedication(
            trim(item.medication()), trim(item.dosage()), nullable(item.duration())
        )).toList();
    }

    private List<String> normalizeStrings(List<String> values) {
        if (values == null) return List.of();
        return List.copyOf(new LinkedHashSet<>(values.stream().map(this::trim).toList()));
    }

    private ClinicalSummaryRequest.TransferDetails normalizeTransfer(
        ClinicalSummaryRequest.TransferDetails transfer
    ) {
        if (transfer == null) return null;
        return new ClinicalSummaryRequest.TransferDetails(
            trim(transfer.reason()),
            trim(transfer.destinationHospital()),
            nullable(transfer.destinationCnes()),
            normalizeStrings(transfer.devices())
        );
    }

    private ClinicalSummaryRequest.DeathDetails normalizeDeath(
        ClinicalSummaryRequest.DeathDetails death
    ) {
        if (death == null) return null;
        return new ClinicalSummaryRequest.DeathDetails(
            death.occurredAt(),
            trim(death.directCause()),
            nullable(death.antecedentCause()),
            trim(death.underlyingCause()),
            trim(death.necropsy())
        );
    }

    private String label(List<ClinicalSummaryCatalog.Option> options, String code) {
        String label = catalog.label(options, code);
        if (label == null) throw new BusinessValidationException("Opção inválida no resumo clínico");
        return label;
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?>)) invalidShape();
        Map<?, ?> root = (Map<?, ?>) value;
        validateKeys(root, ROOT_KEYS);
        validateRows(root.get("dischargeMedications"), MEDICATION_KEYS, 30);
        validateNested(root.get("transfer"), TRANSFER_KEYS);
        validateNested(root.get("death"), DEATH_KEYS);
    }

    private void validateNested(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof Map<?, ?>)) invalidShape();
        Map<?, ?> nested = (Map<?, ?>) value;
        validateKeys(nested, allowed);
    }

    private void validateRows(Object value, Set<String> allowed, int maximum) {
        if (value == null) return;
        if (!(value instanceof Collection<?>)) invalidShape();
        Collection<?> rows = (Collection<?>) value;
        if (rows.size() > maximum) invalidShape();
        for (Object row : rows) validateNested(row, allowed);
    }

    private void validateKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            invalidShape();
        }
    }

    private void invalidShape() {
        throw new BusinessValidationException("Dados do resumo clínico estão em formato inválido");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(String value) {
        String normalized = trim(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
