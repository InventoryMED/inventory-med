package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class AihService {

    private static final Set<String> ROOT_KEYS = Set.of(
        "clinicalContext", "patientId", "patient", "requestedProcedures", "manualData"
    );
    private static final Set<String> PATIENT_KEYS = Set.of(
        "name", "cns", "motherName", "medicalRecordNumber", "address", "bed", "hospital", "cnes"
    );
    private static final Set<String> PROCEDURE_KEYS = Set.of(
        "id", "procedureCode", "anatomicalSite", "laterality", "scheduling", "imageGuided",
        "imageAttachmentReference", "clinicalJustification"
    );
    private static final Set<String> MANUAL_KEYS = Set.of(
        "mainSignsSymptoms", "admissionConditions", "examResults", "initialDiagnosis", "primaryCid",
        "secondaryCids", "requestedProcedureCode", "admissionCharacter"
    );
    private static final Set<String> CONTEXTS = Set.of(
        "ADULT_ICU", "EMERGENCY_BOX", "MEDICAL_WARD", "PEDIATRICS", "OPERATING_ROOM"
    );
    private static final Set<String> ADMISSION_CHARACTERS = Set.of("URGENCY", "ELECTIVE");
    private static final Set<String> LATERALITIES = Set.of(
        "RIGHT", "LEFT", "BILATERAL", "NOT_APPLICABLE"
    );

    private final AihCatalogService catalog;
    private final ObjectMapper objectMapper;

    public AihService(AihCatalogService catalog, ObjectMapper objectMapper) {
        this.catalog = catalog;
        this.objectMapper = objectMapper;
    }

    public AihCatalog catalog() {
        return catalog.catalog();
    }

    public AihResponse preview(AihRequest request) {
        AihRequest safeRequest = request == null
            ? new AihRequest(null, null, null, null, null)
            : request;
        AihRequest.PatientIdentification patient = normalizePatient(safeRequest.patient());
        AihRequest.ManualData manual = normalizeManual(safeRequest.manualData());
        validateOptionalCode(safeRequest.clinicalContext(), CONTEXTS, "contexto clínico");
        validateOptionalCode(
            manual.admissionCharacter(),
            ADMISSION_CHARACTERS,
            "caráter da internação"
        );

        List<AihResponse.ResolvedProcedure> procedures = new ArrayList<>();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        List<AihRequest.RequestedProcedure> requested = safeRequest.requestedProcedures() == null
            ? List.of()
            : safeRequest.requestedProcedures();

        for (AihRequest.RequestedProcedure item : requested) {
            if (item == null || blank(item.procedureCode())) {
                if (item != null && hasProcedureContent(item)) {
                    alerts.add("LINHA SEM PROCEDIMENTO SELECIONADO NÃO FOI INCLUÍDA NO LAUDO.");
                }
                continue;
            }
            AihCatalog.ProcedureOption definition = catalog.procedure(item.procedureCode());
            if (definition == null) {
                throw invalid("Selecione um procedimento cadastrado no dicionário mestre");
            }
            String laterality = trim(item.laterality());
            if (!laterality.isBlank() && !LATERALITIES.contains(laterality)) {
                throw invalid("Selecione uma lateralidade válida");
            }
            if (definition.pairedSite() && !Set.of("RIGHT", "LEFT").contains(laterality)) {
                throw invalid("O procedimento selecionado exige lateralidade direita ou esquerda");
            }
            boolean imageGuided = Boolean.TRUE.equals(item.imageGuided());
            if (imageGuided && blank(item.imageAttachmentReference())) {
                throw invalid("Procedimento guiado por imagem exige a referência do anexo no PEP");
            }
            AihResponse.ResolvedProcedure resolved = new AihResponse.ResolvedProcedure(
                item.id(),
                definition.label(),
                trim(item.anatomicalSite()),
                catalog.lateralityLabel(laterality),
                trim(item.scheduling()),
                imageGuided,
                trim(item.imageAttachmentReference()),
                trim(item.clinicalJustification()),
                definition.tussCode(),
                definition.cbhpmCode(),
                definition.sigtapCode(),
                imageGuided ? AihCatalogService.IMAGE_GUIDANCE_TUSS : null
            );
            procedures.add(resolved);
            appendCodeAlerts(alerts, resolved);
        }

        alerts.add(
            "CÓDIGOS DEVEM SER CONFERIDOS COM A TABELA VIGENTE E COM AS REGRAS DE FATURAMENTO DO CONTRATO."
        );
        alerts.add(
            "CAMPOS EM BRANCO SÃO PERMITIDOS PARA IMPRESSÃO E PREENCHIMENTO MANUAL POSTERIOR."
        );

        String clinicalContext = blank(safeRequest.clinicalContext())
            ? ""
            : catalog.contextLabel(safeRequest.clinicalContext());
        String admissionCharacter = blank(manual.admissionCharacter())
            ? ""
            : catalog.admissionCharacterLabel(manual.admissionCharacter());
        return new AihResponse(
            new AihResponse.StructuredAih(
                patient,
                manual,
                clinicalContext,
                admissionCharacter,
                List.copyOf(procedures),
                reportText(patient, manual, clinicalContext, admissionCharacter, procedures)
            ),
            new AihResponse.BillingAudit(List.copyOf(alerts))
        );
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final AihRequest request;
        try {
            request = objectMapper.convertValue(value, AihRequest.class);
        } catch (IllegalArgumentException exception) {
            throw invalid("Dados da AIH estão em formato inválido");
        }
        AihResponse response = preview(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", request);
        stored.put("structuredAih", response.structuredAih());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private AihRequest.PatientIdentification normalizePatient(
        AihRequest.PatientIdentification patient
    ) {
        if (patient == null) {
            return new AihRequest.PatientIdentification("", "", "", "", "", "", "", "");
        }
        return new AihRequest.PatientIdentification(
            trim(patient.name()),
            trim(patient.cns()),
            trim(patient.motherName()),
            trim(patient.medicalRecordNumber()),
            trim(patient.address()),
            trim(patient.bed()),
            trim(patient.hospital()),
            trim(patient.cnes())
        );
    }

    private AihRequest.ManualData normalizeManual(AihRequest.ManualData manual) {
        if (manual == null) return new AihRequest.ManualData("", "", "", "", "", "", "", "");
        return new AihRequest.ManualData(
            trim(manual.mainSignsSymptoms()),
            trim(manual.admissionConditions()),
            trim(manual.examResults()),
            trim(manual.initialDiagnosis()),
            trim(manual.primaryCid()),
            trim(manual.secondaryCids()),
            trim(manual.requestedProcedureCode()),
            trim(manual.admissionCharacter())
        );
    }

    private void appendCodeAlerts(
        Set<String> alerts,
        AihResponse.ResolvedProcedure procedure
    ) {
        if (blank(procedure.cbhpmCode())) {
            alerts.add(procedure.name() + ": CÓDIGO CBHPM AINDA NÃO CADASTRADO NO DICIONÁRIO MESTRE.");
        }
        if (blank(procedure.sigtapCode())) {
            alerts.add(procedure.name() + ": CÓDIGO SIGTAP AINDA NÃO CADASTRADO NO DICIONÁRIO MESTRE.");
        }
        if (procedure.imageGuided()) {
            alerts.add(
                procedure.name() + ": GUIAGEM POR IMAGEM VINCULADA AO TUSS " +
                AihCatalogService.IMAGE_GUIDANCE_TUSS + "; CONFERIR ANEXO NO PEP."
            );
        }
    }

    private String reportText(
        AihRequest.PatientIdentification patient,
        AihRequest.ManualData manual,
        String clinicalContext,
        String admissionCharacter,
        List<AihResponse.ResolvedProcedure> procedures
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("LAUDO PARA SOLICITAÇÃO DE AUTORIZAÇÃO DE INTERNAÇÃO HOSPITALAR — AIH");
        lines.add("PACIENTE: " + patient.name());
        lines.add("CNS: " + patient.cns() + " | PRONTUÁRIO: " + patient.medicalRecordNumber());
        lines.add("NOME DA MÃE: " + patient.motherName());
        lines.add("ENDEREÇO: " + patient.address());
        lines.add("HOSPITAL: " + patient.hospital() + " | CNES: " + patient.cnes());
        lines.add("LEITO: " + patient.bed() + " | CONTEXTO: " + clinicalContext);
        lines.add("");
        lines.add("PRINCIPAIS SINAIS E SINTOMAS: " + manual.mainSignsSymptoms());
        lines.add("CONDIÇÕES QUE JUSTIFICAM A INTERNAÇÃO: " + manual.admissionConditions());
        lines.add("RESULTADOS DE EXAMES: " + manual.examResults());
        lines.add("DIAGNÓSTICO INICIAL: " + manual.initialDiagnosis());
        lines.add("CID PRINCIPAL: " + manual.primaryCid());
        lines.add("CIDS SECUNDÁRIOS: " + manual.secondaryCids());
        lines.add("PROCEDIMENTO PRINCIPAL SOLICITADO: " + manual.requestedProcedureCode());
        lines.add("CARÁTER DA INTERNAÇÃO: " + admissionCharacter);
        if (!procedures.isEmpty()) {
            lines.add("");
            lines.add("PROCEDIMENTOS SOLICITADOS:");
            for (AihResponse.ResolvedProcedure procedure : procedures) {
                lines.add("- " + procedure.name() + codeText(procedure));
                lines.add("  SÍTIO / LATERALIDADE: " + procedure.anatomicalSite() + optional(" / ", procedure.laterality()));
                lines.add("  MODO / APRAZAMENTO: " + procedure.scheduling());
                lines.add("  GUIADO POR IMAGEM: " + (procedure.imageGuided() ? "SIM" : "NÃO"));
                lines.add("  JUSTIFICATIVA CLÍNICA: " + procedure.clinicalJustification());
            }
        }
        return String.join("\n", lines);
    }

    private String codeText(AihResponse.ResolvedProcedure procedure) {
        List<String> codes = new ArrayList<>();
        if (!blank(procedure.tussCode())) codes.add("TUSS " + procedure.tussCode());
        if (!blank(procedure.cbhpmCode())) codes.add("CBHPM " + procedure.cbhpmCode());
        if (!blank(procedure.sigtapCode())) codes.add("SIGTAP " + procedure.sigtapCode());
        if (!blank(procedure.imageGuidanceTussCode())) {
            codes.add("GUIAGEM TUSS " + procedure.imageGuidanceTussCode());
        }
        return codes.isEmpty() ? "" : " — " + String.join(" | ", codes);
    }

    private boolean hasProcedureContent(AihRequest.RequestedProcedure item) {
        return !blank(item.anatomicalSite()) || !blank(item.laterality()) ||
            !blank(item.scheduling()) || Boolean.TRUE.equals(item.imageGuided()) ||
            !blank(item.imageAttachmentReference()) || !blank(item.clinicalJustification());
    }

    private void validateOptionalCode(String value, Set<String> allowed, String label) {
        if (!blank(value) && !allowed.contains(value)) {
            throw invalid("Selecione uma opção válida para " + label);
        }
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        Map<?, ?> root = (Map<?, ?>) value;
        allowedKeys(root, ROOT_KEYS);
        nestedMap(root.get("patient"), PATIENT_KEYS);
        nestedMap(root.get("manualData"), MANUAL_KEYS);
        Object procedures = root.get("requestedProcedures");
        if (procedures != null) {
            if (!(procedures instanceof List<?>)) invalidFormat();
            for (Object row : (List<?>) procedures) nestedMap(row, PROCEDURE_KEYS);
        }
    }

    private void nestedMap(Object value, Set<String> allowed) {
        if (value == null) return;
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        allowedKeys((Map<?, ?>) value, allowed);
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw invalid("O registro da AIH contém campos não permitidos");
        }
    }

    private String optional(String prefix, String value) {
        return blank(value) ? "" : prefix + value;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void invalidFormat() {
        throw invalid("Dados da AIH estão em formato inválido");
    }

    private BusinessValidationException invalid(String message) {
        return new BusinessValidationException(message);
    }
}
