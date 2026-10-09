package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class BedsideProcedureService {

    private static final Set<String> ROOT_KEYS = Set.of("clinicalContext", "selectedTemplate", "items");
    private static final Set<String> ITEM_KEYS = Set.of(
        "id", "recordType", "procedureCode", "customProcedure", "clinicalIndication",
        "cid10Reference", "anatomicalSite", "laterality", "asepsisAntisepsis",
        "sterileBarrier", "localAnesthesia", "imageGuided", "imageAttachmentReference",
        "imageGuidance", "deviceName", "deviceBrand",
        "deviceCaliber", "deviceLot", "anvisaRegistration", "fixationDressingConnections",
        "samplesLaboratory", "postProcedureControl", "postProcedureDetails",
        "monitoringAssistance", "urgency", "techniqueOutcome", "complications", "performedAt"
    );
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId CLINICAL_ZONE = ZoneId.of("America/Sao_Paulo");

    private final BedsideProcedureCatalogService catalog;
    private final BedsideProcedureValidator validator;
    private final ObjectMapper objectMapper;

    public BedsideProcedureService(
        BedsideProcedureCatalogService catalog,
        BedsideProcedureValidator validator,
        ObjectMapper objectMapper
    ) {
        this.catalog = catalog;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    public BedsideProcedureCatalog catalog() {
        return catalog.catalog();
    }

    public BedsideProcedureResponse preview(BedsideProcedureRequest request) {
        validator.validate(request);
        List<String> details = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        List<String> supplies = new ArrayList<>();
        List<String> alerts = new ArrayList<>();
        String clinicalContext = catalog.optionLabel(
            BedsideProcedureCatalogService.CLINICAL_CONTEXTS,
            request.clinicalContext()
        );
        details.add("CONTEXTO CLÍNICO: " + clinicalContext + ".");

        for (BedsideProcedureRequest.Item item : request.items()) {
            BedsideProcedureCatalog.ProcedureOption procedure = catalog.procedure(item.procedureCode());
            String name = "OTHER".equals(procedure.code()) ? item.customProcedure().trim() : procedure.label();
            String record = "PERFORMED".equals(item.recordType()) ? "REALIZADO" : "SOLICITADO / PLANEJADO";
            String laterality = catalog.optionLabel(BedsideProcedureCatalogService.LATERALITIES, item.laterality());
            String urgency = catalog.optionLabel(BedsideProcedureCatalogService.URGENCY_OPTIONS, item.urgency());
            String site = item.anatomicalSite().trim() +
                ("NOT_APPLICABLE".equals(item.laterality()) ? "" : " — " + laterality);
            summaries.add(name + " — " + site + " — " + procedure.billingReference());

            details.add("- " + name + " — " + record + " — " + urgency + ".");
            details.add("  * INDICAÇÃO: " + item.clinicalIndication().trim() + " — CID-10: " +
                item.cid10Reference().trim() + ".");
            details.add("  * SÍTIO / LATERALIDADE: " + site + ".");
            details.add("  * ASSEPSIA E ANTISSEPSIA: " + item.asepsisAntisepsis().trim() + ".");
            details.add("  * BARREIRA ESTÉRIL: " + item.sterileBarrier().trim() + ".");
            append(details, "ANESTESIA LOCAL", item.localAnesthesia());
            details.add("  * GUIADO POR IMAGEM: " + (Boolean.TRUE.equals(item.imageGuided()) ? "SIM" : "NÃO") + ".");
            if (Boolean.TRUE.equals(item.imageGuided())) {
                append(details, "REFERÊNCIA DO ANEXO NO PEP", item.imageAttachmentReference());
                append(details, "TÉCNICA DE GUIAGEM", item.imageGuidance());
                supplies.add("ULTRASSONOGRAFIA DE ACOMPANHAMENTO — TUSS 40901262 — VALIDAR EXECUÇÃO E ANEXO NO PEP.");
                alerts.add("GUIAGEM POR IMAGEM SELECIONADA: CONFERIR O ANEXO NO PEP PELA REFERÊNCIA " +
                    item.imageAttachmentReference().trim() + ".");
            }
            appendDevice(details, item, procedure, supplies, name);
            append(details, "FIXAÇÃO, CURATIVO E CONEXÕES", item.fixationDressingConnections());
            append(details, "AMOSTRAS / LABORATÓRIO", item.samplesLaboratory());
            details.add("  * CONTROLE PÓS-PROCEDIMENTO: " + postControl(item) + ".");
            details.add("  * MONITORIZAÇÃO E ASSISTÊNCIA: " + item.monitoringAssistance().trim() + ".");

            if ("PERFORMED".equals(item.recordType())) {
                details.add("  * DATA/HORA: " +
                    DATE_TIME.format(item.performedAt().atZoneSameInstant(CLINICAL_ZONE)) + ".");
                details.add("  * TÉCNICA E RESULTADO: " + item.techniqueOutcome().trim() + ".");
                details.add("  * INTERCORRÊNCIAS / COMPLICAÇÕES: " +
                    textOr(item.complications(), "NENHUMA INFORMADA") + ".");
            } else {
                details.add("  * OBSERVAÇÃO: ITENS TÉCNICOS ACIMA CONSTITUEM PLANO PARA CONFERÊNCIA; " +
                    "NÃO REPRESENTAM EXECUÇÃO DO PROCEDIMENTO.");
            }

            supplies.add(name + " — " + procedure.billingReference() +
                " — REFERÊNCIA INFORMADA; VALIDAR VIGÊNCIA, CONTRATO E ELEGIBILIDADE.");
            procedure.supplyKitItems().forEach(supply -> supplies.add(
                name + " — " + supply + " — CONFERIR DISPENSAÇÃO E USO EFETIVO."
            ));
            if (notBlank(item.localAnesthesia())) supplies.add(item.localAnesthesia().trim() + " — CONFERIR USO EFETIVO.");
            if (notBlank(item.fixationDressingConnections())) supplies.add(
                item.fixationDressingConnections().trim() + " — CONFERIR ITENS EFETIVAMENTE UTILIZADOS."
            );
            if (procedure.postProcedureControlRequired()) supplies.add(
                "EXAME DE CONTROLE PÓS-PROCEDIMENTO — CONFERIR SOLICITAÇÃO, EXECUÇÃO E LAUDO."
            );
            if (procedure.majorInvasiveProcedure()) {
                alerts.add(name + ": CHECAR TP, TTPA E PLAQUETAS ANTES DO PROCEDIMENTO.");
                alerts.add(name + ": CONFERIR RADIOGRAFIA DE TÓRAX DE CONTROLE APÓS O PROCEDIMENTO.");
            }
            if (BedsideProcedureCatalogService.NO_BILLING_REFERENCE.equals(procedure.billingReference())) {
                alerts.add(name + ": REFERÊNCIA TUSS/CBHPM/SIGTAP DEVE SER CADASTRADA E VALIDADA PELA AUDITORIA.");
            }
        }

        alerts.add("CÓDIGOS TUSS/SIGTAP EXIBIDOS SÃO REFERÊNCIAS INFORMADAS E DEVEM SER VALIDADOS NA TABELA VIGENTE E NO CONTRATO.");
        alerts.add("NENHUMA COBRANÇA, HONORÁRIO, TAXA OU INSUMO É LANÇADO AUTOMATICAMENTE.");
        alerts.add("CONFIRMAR CONSENTIMENTO, CHECKLIST, LATERALIDADE, RASTREABILIDADE E CONTROLE PÓS-PROCEDIMENTO.");
        alerts.add("REGISTROS FINALIZADOS SÃO IMUTÁVEIS; RETIFICAÇÕES DEVEM GERAR NOVA VERSÃO AUDITÁVEL.");

        return new BedsideProcedureResponse(
            new BedsideProcedureResponse.StructuredProcedures(
                String.join("; ", summaries) + ".",
                "11. PROCEDIMENTOS E INTERVENÇÕES BEIRA-LEITO:\n" + String.join("\n", details)
            ),
            new BedsideProcedureResponse.BillingAudit(
                List.copyOf(new LinkedHashSet<>(supplies)),
                List.copyOf(new LinkedHashSet<>(alerts))
            )
        );
    }

    public Map<String, Object> normalizeForClinicalDocument(Object value) {
        validateDocumentShape(value);
        final BedsideProcedureRequest request;
        try {
            request = objectMapper.convertValue(value, BedsideProcedureRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados dos procedimentos estão em formato inválido");
        }
        BedsideProcedureResponse response = preview(request);
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", request);
        stored.put("structuredProcedures", response.structuredProcedures());
        stored.put("billingAudit", response.billingAudit());
        return stored;
    }

    private void appendDevice(
        List<String> details,
        BedsideProcedureRequest.Item item,
        BedsideProcedureCatalog.ProcedureOption procedure,
        List<String> supplies,
        String name
    ) {
        if (!notBlank(item.deviceName())) return;
        String traceability = "PERFORMED".equals(item.recordType()) && procedure.deviceTraceabilityRequired()
            ? " — MARCA: " + item.deviceBrand().trim() + " — CALIBRE: " + item.deviceCaliber().trim() +
                " — LOTE: " + item.deviceLot().trim() + " — REGISTRO ANVISA: " + item.anvisaRegistration().trim()
            : optional(" — CALIBRE: ", item.deviceCaliber());
        details.add("  * DISPOSITIVO / OPME: " + item.deviceName().trim() + traceability + ".");
        supplies.add(name + " — " + item.deviceName().trim() + " — CONFERIR DISPENSAÇÃO, USO E RASTREABILIDADE.");
    }

    private String postControl(BedsideProcedureRequest.Item item) {
        String label = catalog.optionLabel(BedsideProcedureCatalogService.POST_CONTROLS, item.postProcedureControl());
        return label + optional(" — ", item.postProcedureDetails());
    }

    private void append(List<String> details, String label, String value) {
        if (notBlank(value)) details.add("  * " + label + ": " + value.trim() + ".");
    }

    private String optional(String prefix, String value) {
        return notBlank(value) ? prefix + value.trim() : "";
    }

    private String textOr(String value, String fallback) {
        return notBlank(value) ? value.trim() : fallback;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?>)) invalidFormat();
        Map<?, ?> root = (Map<?, ?>) value;
        allowedKeys(root, ROOT_KEYS);
        Object itemsValue = root.get("items");
        if (!(itemsValue instanceof List<?>)) invalidFormat();
        for (Object item : (List<?>) itemsValue) {
            if (!(item instanceof Map<?, ?>)) invalidFormat();
            allowedKeys((Map<?, ?>) item, ITEM_KEYS);
        }
    }

    private void allowedKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key -> !(key instanceof String) || !allowed.contains(key))) {
            throw new BusinessValidationException("O registro de procedimentos contém campos não permitidos");
        }
    }

    private void invalidFormat() {
        throw new BusinessValidationException("Dados dos procedimentos estão em formato inválido");
    }
}
