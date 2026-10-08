package br.com.inventorymed.clinical;

import br.com.inventorymed.common.BusinessValidationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TherapeuticSupportValidator {

    private final TherapeuticSupportCatalogService catalog;

    public TherapeuticSupportValidator(TherapeuticSupportCatalogService catalog) {
        this.catalog = catalog;
    }

    public void validate(TherapeuticSupportRequest request) {
        if (request == null) throw invalid("Selecione ao menos um item de suporte terapêutico");
        List<TherapeuticSupportRequest.HydrationSolution> hydration = safe(request.hydrationSolutions());
        List<TherapeuticSupportRequest.BloodProduct> bloodProducts = safe(request.bloodProducts());
        if (hydration.size() > 8 || bloodProducts.size() > 8) {
            throw invalid("Cada grupo aceita no máximo oito itens");
        }
        boolean hasGlucose = hasGlucose(request.glucoseControl());
        if (hydration.isEmpty() && bloodProducts.isEmpty() && !hasGlucose) {
            throw invalid("Selecione ao menos um item de suporte terapêutico");
        }
        requireOption(request.clinicalContext(), TherapeuticSupportCatalogService.CLINICAL_CONTEXTS,
            "contexto clínico");
        validateHydration(hydration);
        validateGlucose(request.glucoseControl());
        validateBloodProducts(bloodProducts);
    }

    private void validateHydration(List<TherapeuticSupportRequest.HydrationSolution> values) {
        Set<Integer> identifiers = new HashSet<>();
        for (TherapeuticSupportRequest.HydrationSolution value : values) {
            requireIdentifier(value == null ? null : value.id(), identifiers, "soluções");
            requireOption(value.baseSolution(), TherapeuticSupportCatalogService.BASE_SOLUTIONS, "solução base");
            requireUniqueOptions(value.additives(), TherapeuticSupportCatalogService.ELECTROLYTE_ADDITIVES,
                "aditivos eletrolíticos");
            if (safe(value.additives()).size() > 6) {
                throw invalid("Cada solução aceita no máximo seis aditivos eletrolíticos");
            }
            requireOption(value.route(), TherapeuticSupportCatalogService.HYDRATION_ROUTES, "via da solução");
            requireOption(value.frequency(), TherapeuticSupportCatalogService.HYDRATION_FREQUENCIES,
                "frequência da solução");
            requireOption(value.infusionMode(), TherapeuticSupportCatalogService.INFUSION_MODES,
                "modalidade de infusão");
            requireOption(value.scheduling(), TherapeuticSupportCatalogService.SCHEDULING_OPTIONS,
                "aprazamento da solução");
            validateInfusionRate(value);
        }
    }

    private void validateInfusionRate(TherapeuticSupportRequest.HydrationSolution value) {
        boolean pump = "PUMP".equals(value.infusionMode());
        boolean macrodrip = "MACRODRIP".equals(value.infusionMode());
        if (pump || macrodrip) {
            if (value.rateValue() == null || value.rateValue().signum() <= 0 ||
                value.rateValue().compareTo(java.math.BigDecimal.valueOf(2000)) > 0) {
                throw invalid("Informe uma vazão válida para a solução selecionada");
            }
            String expectedUnit = pump ? "ML_H" : "DROPS_MIN";
            if (!expectedUnit.equals(value.rateUnit())) {
                throw invalid("Selecione a unidade de vazão compatível com a modalidade de infusão");
            }
            return;
        }
        if (value.rateValue() != null || notBlank(value.rateUnit())) {
            throw invalid("Expansão rápida não deve possuir vazão adicional");
        }
    }

    private void validateGlucose(TherapeuticSupportRequest.GlucoseControl value) {
        if (!hasGlucose(value)) {
            if (value != null && (
                Boolean.TRUE.equals(value.hypoglycemiaProtocolActive()) ||
                Boolean.TRUE.equals(value.correctionScaleActive()) ||
                Boolean.TRUE.equals(value.continuousPump()) || notBlank(value.insulinType())
            )) throw invalid("Informe a frequência do DXT para ativar os protocolos glicêmicos");
            return;
        }
        requireOption(value.frequency(), TherapeuticSupportCatalogService.GLUCOSE_FREQUENCIES,
            "frequência do DXT");
        if (!Boolean.TRUE.equals(value.hypoglycemiaProtocolActive()) ||
            !Boolean.TRUE.equals(value.correctionScaleActive())) {
            throw invalid("Toda ordem de DXT deve manter ativos o protocolo de hipoglicemia e a escala de correção");
        }
        requireOption(value.insulinType(), TherapeuticSupportCatalogService.INSULIN_TYPES,
            "insulina da escala de correção");
        if (Boolean.TRUE.equals(value.continuousPump()) && (
            !"EVERY_1_HOUR".equals(value.frequency()) || !"REGULAR".equals(value.insulinType())
        )) {
            throw invalid("A bomba contínua exige DXT 1/1H e insulina regular");
        }
    }

    private void validateBloodProducts(List<TherapeuticSupportRequest.BloodProduct> values) {
        Set<Integer> identifiers = new HashSet<>();
        for (TherapeuticSupportRequest.BloodProduct value : values) {
            requireIdentifier(value == null ? null : value.id(), identifiers, "hemocomponentes");
            requireProductOption(value.product(), TherapeuticSupportCatalogService.BLOOD_PRODUCTS, "produto sanguíneo");
            requireUniqueOptions(value.modifications(), TherapeuticSupportCatalogService.BLOOD_MODIFICATIONS,
                "modificações do hemocomponente");
            if (safe(value.modifications()).size() > 4 || safe(value.preMedications()).size() > 3) {
                throw invalid("A seleção excede o limite permitido para o produto sanguíneo");
            }
            requireOption(value.quantityUnit(), TherapeuticSupportCatalogService.QUANTITY_UNITS,
                "unidade de quantidade");
            requireOption(value.route(), TherapeuticSupportCatalogService.TRANSFUSION_ROUTES,
                "via do produto sanguíneo");
            requireUniqueOptions(value.preMedications(), TherapeuticSupportCatalogService.PRE_MEDICATIONS,
                "pré-medicações");
            requireOption(value.scheduling(), TherapeuticSupportCatalogService.SCHEDULING_OPTIONS,
                "aprazamento do produto sanguíneo");
            if (value.quantity() == null || value.quantity() < 1 || value.quantity() > 1000 ||
                value.infusionMinutes() == null || value.infusionMinutes() < 15 ||
                value.infusionMinutes() > 240) {
                throw invalid("Informe quantidade e tempo de infusão do produto sanguíneo");
            }
            boolean plasmaDerivative = catalog.isPlasmaDerivative(value.product());
            if (plasmaDerivative && !safe(value.modifications()).isEmpty()) {
                throw invalid("Modificações de bolsa não se aplicam aos hemoderivados proteicos selecionados");
            }
            if (plasmaDerivative && !Set.of("BOTTLE", "ML").contains(value.quantityUnit())) {
                throw invalid("Albumina deve ser informada em frasco ou mililitros");
            }
            if (!plasmaDerivative && !Set.of("UNIT", "BAG").contains(value.quantityUnit())) {
                throw invalid("Hemocomponentes devem ser informados em unidade ou bolsa");
            }
            if ((!plasmaDerivative || !"ML".equals(value.quantityUnit())) && value.quantity() > 20) {
                throw invalid("A quantidade máxima é 20 unidades, bolsas ou frascos por item");
            }
        }
    }

    private boolean hasGlucose(TherapeuticSupportRequest.GlucoseControl value) {
        return value != null && notBlank(value.frequency());
    }

    private void requireIdentifier(Integer id, Set<Integer> identifiers, String label) {
        if (id == null || id < 1 || !identifiers.add(id)) {
            throw invalid("Os itens de " + label + " precisam de identificadores únicos");
        }
    }

    private void requireOption(
        String code,
        List<TherapeuticSupportCatalog.Option> options,
        String label
    ) {
        if (!notBlank(code) || options.stream().noneMatch(option -> option.code().equals(code))) {
            throw invalid("Selecione uma opção válida para " + label);
        }
    }

    private void requireProductOption(
        String code,
        List<TherapeuticSupportCatalog.ProductOption> options,
        String label
    ) {
        if (!notBlank(code) || options.stream().noneMatch(option -> option.code().equals(code))) {
            throw invalid("Selecione uma opção válida para " + label);
        }
    }

    private void requireUniqueOptions(
        List<String> values,
        List<TherapeuticSupportCatalog.Option> options,
        String label
    ) {
        List<String> selected = safe(values);
        Set<String> allowed = options.stream().map(TherapeuticSupportCatalog.Option::code)
            .collect(java.util.stream.Collectors.toSet());
        if (new HashSet<>(selected).size() != selected.size() ||
            selected.stream().anyMatch(value -> !allowed.contains(value))) {
            throw invalid("Selecione opções válidas e sem duplicidade para " + label);
        }
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
