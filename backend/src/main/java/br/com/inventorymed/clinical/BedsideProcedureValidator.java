package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class BedsideProcedureValidator {

    private static final Set<String> RECORD_TYPES = Set.of("REQUESTED", "PERFORMED");
    private static final Set<String> LATERALITIES = Set.of("RIGHT", "LEFT", "MIDLINE", "NOT_APPLICABLE");
    private static final Set<String> URGENCIES = Set.of("NOW", "IMMEDIATE", "URGENT", "EMERGENCY");
    private static final Set<String> POST_CONTROLS = Set.of(
        "CHEST_XRAY", "ULTRASOUND", "OTHER_IMAGE", "CLINICAL_JUSTIFICATION", "NOT_APPLICABLE"
    );

    private final BedsideProcedureCatalogService catalog;

    public BedsideProcedureValidator(BedsideProcedureCatalogService catalog) {
        this.catalog = catalog;
    }

    public void validate(BedsideProcedureRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw invalid("Inclua ao menos um procedimento");
        }
        if (request.items().size() > 10) throw invalid("São permitidos até 10 procedimentos por documento");
        Set<Integer> ids = new HashSet<>();
        for (BedsideProcedureRequest.Item item : request.items()) validateItem(item, ids);
    }

    private void validateItem(BedsideProcedureRequest.Item item, Set<Integer> ids) {
        if (item == null || item.id() == null || item.id() < 1 || !ids.add(item.id())) {
            throw invalid("Os procedimentos precisam de identificadores únicos");
        }
        requireOption(item.recordType(), RECORD_TYPES, "situação do registro");
        BedsideProcedureCatalog.ProcedureOption procedure = catalog.procedure(item.procedureCode());
        if (procedure == null) throw invalid("Selecione um procedimento válido");
        if ("OTHER".equals(procedure.code())) requireText(item.customProcedure(), "nome do procedimento", 200);
        requireText(item.clinicalIndication(), "indicação clínica", 1000);
        requireText(item.cid10Reference(), "referência CID-10", 40);
        requireText(item.anatomicalSite(), "sítio anatômico", 300);
        requireOption(item.laterality(), LATERALITIES, "lateralidade");
        if (procedure.pairedSite() && !Set.of("RIGHT", "LEFT").contains(item.laterality())) {
            throw invalid("O procedimento selecionado exige lateralidade direita ou esquerda");
        }
        requireText(item.asepsisAntisepsis(), "assepsia e antissepsia", 1000);
        requireText(item.sterileBarrier(), "barreira estéril", 1000);
        requireText(item.monitoringAssistance(), "monitorização e assistência", 1000);
        requireOption(item.urgency(), URGENCIES, "prioridade do procedimento");
        requireOption(item.postProcedureControl(), POST_CONTROLS, "controle pós-procedimento");

        if (procedure.postProcedureControlRequired()) {
            if ("NOT_APPLICABLE".equals(item.postProcedureControl())) {
                throw invalid("O procedimento exige exame de controle ou justificativa clínica");
            }
            if ("CLINICAL_JUSTIFICATION".equals(item.postProcedureControl())) {
                requireText(item.postProcedureDetails(), "justificativa para ausência do exame de controle", 1000);
            }
        }
        if ("OTHER_IMAGE".equals(item.postProcedureControl())) {
            requireText(item.postProcedureDetails(), "descrição do exame de imagem de controle", 1000);
        }
        if ("PERFORMED".equals(item.recordType())) validatePerformed(item, procedure);
    }

    private void validatePerformed(
        BedsideProcedureRequest.Item item,
        BedsideProcedureCatalog.ProcedureOption procedure
    ) {
        if (item.performedAt() == null || item.performedAt().isAfter(OffsetDateTime.now().plusMinutes(5))) {
            throw invalid("Informe uma data e hora válidas para o procedimento realizado");
        }
        requireText(item.techniqueOutcome(), "técnica e resultado do procedimento realizado", 3000);
        if (procedure.deviceTraceabilityRequired()) {
            requireText(item.deviceName(), "dispositivo utilizado", 300);
            requireText(item.deviceBrand(), "marca do dispositivo", 200);
            requireText(item.deviceCaliber(), "calibre do dispositivo", 100);
            requireText(item.deviceLot(), "lote do dispositivo", 160);
            requireText(item.anvisaRegistration(), "registro Anvisa do dispositivo", 160);
        }
    }

    private void requireOption(String value, Set<String> values, String label) {
        if (value == null || !values.contains(value)) throw invalid("Selecione uma opção válida para " + label);
    }

    private void requireText(String value, String label, int maximumLength) {
        if (value == null || value.isBlank()) throw invalid("Informe " + label);
        if (value.length() > maximumLength) throw invalid("O campo " + label + " excede o limite permitido");
    }

    private BusinessValidationException invalid(String message) {
        return new BusinessValidationException(message);
    }
}
