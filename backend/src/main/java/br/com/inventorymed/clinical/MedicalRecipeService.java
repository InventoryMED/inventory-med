package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import br.com.inventorymed.tenancy.TenantJdbcExecutor;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class MedicalRecipeService {

    private static final Set<String> ROOT_KEYS = Set.of(
        "recipeType", "items", "buyerName", "buyerIdentityDocument", "buyerAddress",
        "physicianAttestation"
    );
    private static final Set<String> ITEM_KEYS = Set.of(
        "route", "medication", "quantity", "dosage", "recommendations"
    );
    private static final Set<String> NEGATIVE_ALLERGY_VALUES = Set.of(
        "NEGA", "NEGADA", "NEGADAS", "NENHUMA", "SEM ALERGIA", "SEM ALERGIAS",
        "NAO INFORMADO", "NAO INFORMADA", "DESCONHECIDA", "DESCONHECIDO"
    );

    private final TenantJdbcExecutor tenantJdbc;
    private final ObjectMapper objectMapper;

    public MedicalRecipeService(TenantJdbcExecutor tenantJdbc, ObjectMapper objectMapper) {
        this.tenantJdbc = tenantJdbc;
        this.objectMapper = objectMapper;
    }

    public MedicalRecipeCatalog catalog() {
        return new MedicalRecipeCatalog(
            List.of(
                new MedicalRecipeCatalog.Option("COMMON", "RECEITUÁRIO COMUM", true),
                new MedicalRecipeCatalog.Option(
                    "SPECIAL_CONTROL", "CONTROLE ESPECIAL — DUAS VIAS", true
                ),
                new MedicalRecipeCatalog.Option(
                    "NOTIFICATION_B", "NOTIFICAÇÃO B — RASCUNHO NÃO VÁLIDO", false
                )
            ),
            List.of(
                new MedicalRecipeCatalog.Option("ORAL", "ORAL", true),
                new MedicalRecipeCatalog.Option("INHALATION", "INALATÓRIO", true),
                new MedicalRecipeCatalog.Option("TOPICAL", "TÓPICO", true),
                new MedicalRecipeCatalog.Option("INJECTABLE_SC", "INJETÁVEL / SC", true),
                new MedicalRecipeCatalog.Option("OCULAR", "OCULAR", true),
                new MedicalRecipeCatalog.Option("NASAL", "NASAL", true),
                new MedicalRecipeCatalog.Option("RECTAL", "RETAL", true)
            ),
            false,
            false,
            false
        );
    }

    public MedicalRecipeResponse preview(
        UUID hospitalId,
        UUID admissionId,
        MedicalRecipeRequest request
    ) {
        return build(hospitalId, admissionId, normalize(request));
    }

    MedicalRecipeResponse previewWithRecordedAllergies(
        String allergies,
        MedicalRecipeRequest request
    ) {
        return build(allergies, normalize(request));
    }

    public Map<String, Object> normalizeForClinicalDocument(
        UUID hospitalId,
        UUID admissionId,
        Object value,
        boolean finalizing
    ) {
        validateDocumentShape(value);
        final MedicalRecipeRequest request;
        try {
            request = objectMapper.convertValue(value, MedicalRecipeRequest.class);
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException("Dados da receita médica estão em formato inválido");
        }
        MedicalRecipeResponse response = build(hospitalId, admissionId, normalize(request));
        if (finalizing && !response.finalizationAllowed()) {
            throw new BusinessValidationException(response.blockingReasons().getFirst());
        }
        Map<String, Object> stored = new LinkedHashMap<>();
        stored.put("request", response.request());
        stored.put("printTitle", response.printTitle());
        stored.put("copyLabels", response.copyLabels());
        stored.put("finalizationAllowed", response.finalizationAllowed());
        stored.put("allergyConflicts", response.allergyConflicts());
        stored.put("blockingReasons", response.blockingReasons());
        stored.put("safetyNotices", response.safetyNotices());
        return stored;
    }

    private MedicalRecipeRequest normalize(MedicalRecipeRequest request) {
        List<MedicalRecipeRequest.Item> items = request.items() == null
            ? List.of()
            : request.items().stream().map(item -> new MedicalRecipeRequest.Item(
                trim(item.route()),
                trim(item.medication()),
                trim(item.quantity()),
                trim(item.dosage()),
                nullable(item.recommendations())
            )).toList();
        if (items.isEmpty()) {
            throw new BusinessValidationException("Inclua pelo menos um medicamento na receita");
        }
        if (items.size() > 30) {
            throw new BusinessValidationException("A receita aceita no máximo 30 medicamentos");
        }
        if (items.stream().anyMatch(item ->
            item.route().isBlank() || item.medication().isBlank() ||
                item.quantity().isBlank() || item.dosage().isBlank()
        )) {
            throw new BusinessValidationException(
                "Informe via, medicamento, quantidade e posologia em todas as linhas"
            );
        }
        if (items.stream().anyMatch(item ->
            item.medication().length() > 180 || item.quantity().length() > 80 ||
                item.dosage().length() > 500 ||
                (item.recommendations() != null && item.recommendations().length() > 1000)
        )) {
            throw new BusinessValidationException("Um dos campos da receita excede o limite permitido");
        }
        Set<String> allowedRoutes = catalog().routes().stream()
            .map(MedicalRecipeCatalog.Option::code)
            .collect(java.util.stream.Collectors.toSet());
        if (items.stream().anyMatch(item -> !allowedRoutes.contains(item.route()))) {
            throw new BusinessValidationException("Via de administração inválida");
        }
        if (Set.of("COMMON", "SPECIAL_CONTROL", "NOTIFICATION_B").stream()
            .noneMatch(item -> item.equals(request.recipeType()))) {
            throw new BusinessValidationException("Tipo de receituário inválido");
        }
        String buyerName = nullable(request.buyerName());
        String buyerIdentityDocument = nullable(request.buyerIdentityDocument());
        String buyerAddress = nullable(request.buyerAddress());
        if (
            (buyerName != null && buyerName.length() > 180) ||
            (buyerIdentityDocument != null && buyerIdentityDocument.length() > 40) ||
            (buyerAddress != null && buyerAddress.length() > 300)
        ) {
            throw new BusinessValidationException("A identificação do comprador excede o limite permitido");
        }
        return new MedicalRecipeRequest(
            request.recipeType(),
            items,
            buyerName,
            buyerIdentityDocument,
            buyerAddress,
            request.physicianAttestation()
        );
    }

    private MedicalRecipeResponse build(
        UUID hospitalId,
        UUID admissionId,
        MedicalRecipeRequest request
    ) {
        return build(patientAllergies(hospitalId, admissionId), request);
    }

    private MedicalRecipeResponse build(String allergies, MedicalRecipeRequest request) {
        List<String> conflicts = allergyConflicts(allergies, request.items());
        List<String> blocking = new ArrayList<>();
        if (!conflicts.isEmpty()) {
            blocking.add(
                "A medicação coincide com uma alergia registrada. Revise o cadastro e a receita antes de finalizar"
            );
        }
        if (!request.physicianAttestation()) {
            blocking.add("Confirme a revisão e a responsabilidade médica antes de finalizar");
        }
        if ("NOTIFICATION_B".equals(request.recipeType())) {
            blocking.add(
                "A Notificação B válida exige numeração controlada, integração SNCR e assinatura qualificada; este ambiente ainda não possui essas integrações"
            );
        }

        List<String> notices = new ArrayList<>();
        notices.add(
            "A CHECAGEM AUTOMÁTICA DE ALERGIAS É TEXTUAL E NÃO SUBSTITUI A REVISÃO CLÍNICA E FARMACOLÓGICA."
        );
        notices.add(
            "O CATÁLOGO OFICIAL DE MEDICAMENTOS AINDA NÃO ESTÁ INTEGRADO; CONFIRA PRINCÍPIO ATIVO, CONCENTRAÇÃO E APRESENTAÇÃO."
        );
        if ("SPECIAL_CONTROL".equals(request.recipeType())) {
            notices.add(
                "AS DUAS VIAS DEVEM SER ASSINADAS FISICAMENTE OU POR ASSINATURA ELETRÔNICA QUALIFICADA EM DOCUMENTO VERIFICÁVEL."
            );
        }
        if ("NOTIFICATION_B".equals(request.recipeType())) {
            notices.add(
                "QUALQUER IMPRESSÃO DESTA OPÇÃO É SOMENTE RASCUNHO DE APOIO E NÃO É VÁLIDA PARA DISPENSAÇÃO."
            );
        }

        String title = switch (request.recipeType()) {
            case "SPECIAL_CONTROL" -> "RECEITUÁRIO DE CONTROLE ESPECIAL";
            case "NOTIFICATION_B" -> "RASCUNHO DE NOTIFICAÇÃO B — NÃO VÁLIDO";
            default -> "RECEITA MÉDICA";
        };
        List<String> copies = "SPECIAL_CONTROL".equals(request.recipeType())
            ? List.of("1ª VIA — RETIDA NA FARMÁCIA", "2ª VIA — PACIENTE")
            : List.of("VIA DO PACIENTE");
        return new MedicalRecipeResponse(
            request,
            title,
            copies,
            blocking.isEmpty(),
            conflicts,
            List.copyOf(blocking),
            List.copyOf(notices)
        );
    }

    private String patientAllergies(UUID hospitalId, UUID admissionId) {
        List<String> results = tenantJdbc.read(hospitalId, jdbc -> jdbc.query(
            "SELECT p.allergies FROM dbo.admission a " +
                "INNER JOIN dbo.patient p ON p.id = a.patient_id " +
                "WHERE a.id = ? AND a.status = 'ACTIVE'",
            (row, number) -> row.getString("allergies"),
            admissionId
        ));
        if (results.isEmpty()) {
            throw new BusinessValidationException("Internação não encontrada para emissão da receita");
        }
        return results.getFirst();
    }

    private List<String> allergyConflicts(
        String allergies,
        List<MedicalRecipeRequest.Item> items
    ) {
        if (allergies == null || allergies.isBlank()) return List.of();
        Set<String> markers = new LinkedHashSet<>();
        for (String raw : allergies.split("[,;\\n/]+")) {
            String marker = normalizeComparison(raw)
                .replaceFirst("^ALERGIAS?\\s+(A|AO)?\\s*", "")
                .trim();
            if (marker.length() >= 4 && !NEGATIVE_ALLERGY_VALUES.contains(marker)) {
                markers.add(marker);
            }
        }
        Set<String> conflicts = new LinkedHashSet<>();
        for (MedicalRecipeRequest.Item item : items) {
            String medication = normalizeComparison(item.medication());
            markers.stream()
                .filter(marker -> medication.contains(marker) || marker.contains(medication))
                .forEach(marker -> conflicts.add(item.medication() + " ↔ " + marker));
        }
        return List.copyOf(conflicts);
    }

    private String normalizeComparison(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "")
            .replaceAll("[^A-Za-z0-9 ]", " ")
            .replaceAll("\\s+", " ")
            .trim()
            .toUpperCase(Locale.ROOT);
    }

    private void validateDocumentShape(Object value) {
        if (!(value instanceof Map<?, ?>)) invalidShape();
        Map<?, ?> root = (Map<?, ?>) value;
        validateKeys(root, ROOT_KEYS);
        Object itemsValue = root.get("items");
        if (!(itemsValue instanceof Collection<?>)) invalidShape();
        Collection<?> items = (Collection<?>) itemsValue;
        if (items.size() > 30) invalidShape();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?>)) invalidShape();
            Map<?, ?> row = (Map<?, ?>) item;
            validateKeys(row, ITEM_KEYS);
        }
    }

    private void validateKeys(Map<?, ?> values, Set<String> allowed) {
        if (values.keySet().stream().anyMatch(key ->
            !(key instanceof String) || !allowed.contains(key)
        )) invalidShape();
    }

    private void invalidShape() {
        throw new BusinessValidationException("Dados da receita médica estão em formato inválido");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullable(String value) {
        String normalized = trim(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
