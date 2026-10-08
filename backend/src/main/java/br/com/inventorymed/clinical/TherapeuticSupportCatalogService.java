package br.com.inventorymed.clinical;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TherapeuticSupportCatalogService {

    static final List<TherapeuticSupportCatalog.Option> CLINICAL_CONTEXTS = List.of(
        option("WARD_UPA", "CLÍNICA MÉDICA / UPA"),
        option("ICU", "UTI"),
        option("ADULT_EMERGENCY", "BOX DE EMERGÊNCIA ADULTO"),
        option("PEDIATRIC_WARD", "PEDIATRIA / ENFERMARIA")
    );
    static final List<TherapeuticSupportCatalog.Option> BASE_SOLUTIONS = List.of(
        option("SG5_500", "SORO GLICOSADO 5% 500 ML"),
        option("SG10_500", "SORO GLICOSADO 10% 500 ML"),
        option("SF09_250", "SORO FISIOLÓGICO 0,9% 250 ML"),
        option("SF09_500", "SORO FISIOLÓGICO 0,9% 500 ML"),
        option("SF09_1000", "SORO FISIOLÓGICO 0,9% 1.000 ML"),
        option("RL_500", "RINGER COM LACTATO 500 ML"),
        option("RL_1000", "RINGER COM LACTATO 1.000 ML"),
        option("GLYCOSALINE_1000", "SORO GLICOFISIOLÓGICO 1.000 ML")
    );
    static final List<TherapeuticSupportCatalog.Option> ELECTROLYTE_ADDITIVES = List.of(
        option("KCL10_10", "CLORETO DE POTÁSSIO (KCL) 10% 10 ML"),
        option("KCL191_10", "CLORETO DE POTÁSSIO (KCL) 19,1% 10 ML"),
        option("NACL20_10", "CLORETO DE SÓDIO (NACL) 20% 10 ML"),
        option("BICARB84_10", "BICARBONATO DE SÓDIO 8,4% 10 ML"),
        option("CALCIUM_GLUCONATE10_10", "GLUCONATO DE CÁLCIO 10% 10 ML"),
        option("MAGNESIUM_SULFATE50_10", "SULFATO DE MAGNÉSIO 50% 10 ML")
    );
    static final List<TherapeuticSupportCatalog.Option> HYDRATION_ROUTES = List.of(
        option("EV", "ENDOVENOSA (EV)")
    );
    static final List<TherapeuticSupportCatalog.Option> HYDRATION_FREQUENCIES = List.of(
        option("EVERY_24_HOURS", "24/24H"),
        option("EVERY_12_HOURS", "12/12H"),
        option("EVERY_8_HOURS", "8/8H"),
        option("EVERY_6_HOURS", "6/6H"),
        option("CONTINUOUS", "CONTÍNUO"),
        option("RAPID_PHASE", "FASE RÁPIDA")
    );
    static final List<TherapeuticSupportCatalog.Option> INFUSION_MODES = List.of(
        option("PUMP", "BOMBA DE INFUSÃO"),
        option("MACRODRIP", "EQUIPO MACROGOTAS"),
        option("RAPID_30_MIN", "EXPANSÃO RÁPIDA — CORRER EM 30 MIN"),
        option("RAPID_60_MIN", "EXPANSÃO RÁPIDA — CORRER EM 60 MIN")
    );
    static final List<TherapeuticSupportCatalog.Option> RATE_UNITS = List.of(
        option("ML_H", "ML/H"),
        option("DROPS_MIN", "MACROGOTAS/MIN")
    );
    static final List<TherapeuticSupportCatalog.Option> SCHEDULING_OPTIONS = List.of(
        option("FIXED", "FIXO"),
        option("URGENT", "URGENTE")
    );
    static final List<TherapeuticSupportCatalog.Option> GLUCOSE_FREQUENCIES = List.of(
        option("EVERY_1_HOUR", "1/1H"),
        option("EVERY_2_HOURS", "2/2H"),
        option("EVERY_4_HOURS", "4/4H"),
        option("EVERY_6_HOURS", "6/6H"),
        option("BEFORE_MEALS", "ANTES DAS REFEIÇÕES"),
        option("AS_NEEDED", "SE NECESSÁRIO (SN)")
    );
    static final List<TherapeuticSupportCatalog.Option> INSULIN_TYPES = List.of(
        option("REGULAR", "INSULINA REGULAR"),
        option("LISPRO", "INSULINA LISPRO (HUMALOG)")
    );
    static final List<TherapeuticSupportCatalog.ProductOption> BLOOD_PRODUCTS = List.of(
        product("RBC", "CONCENTRADO DE HEMÁCIAS", "BLOOD_COMPONENT"),
        product("PLATELET_POOL", "CONCENTRADO DE PLAQUETAS — POOL", "BLOOD_COMPONENT"),
        product("PLATELET_APHERESIS", "CONCENTRADO DE PLAQUETAS — AFÉRESE", "BLOOD_COMPONENT"),
        product("FFP", "PLASMA FRESCO CONGELADO", "BLOOD_COMPONENT"),
        product("CRYOPRECIPITATE", "CRIOPRECIPITADO", "BLOOD_COMPONENT"),
        product("ALBUMIN20_50", "ALBUMINA HUMANA 20% — FRASCO 50 ML", "PLASMA_DERIVATIVE"),
        product("ALBUMIN20_100", "ALBUMINA HUMANA 20% — FRASCO 100 ML", "PLASMA_DERIVATIVE"),
        product("ALBUMIN5_100", "ALBUMINA HUMANA 5% — FRASCO 100 ML", "PLASMA_DERIVATIVE")
    );
    static final List<TherapeuticSupportCatalog.Option> BLOOD_MODIFICATIONS = List.of(
        option("LEUKOREDUCED", "DESLEUCOCITADO / FILTRADO"),
        option("IRRADIATED", "IRRADIADO"),
        option("WASHED", "LAVADO"),
        option("PHENOTYPED", "FENOTIPADO")
    );
    static final List<TherapeuticSupportCatalog.Option> TRANSFUSION_ROUTES = List.of(
        option("EV", "ENDOVENOSA (EV)"),
        option("DEDICATED_ACCESS", "ACESSO ENDOVENOSO EXCLUSIVO / DEDICADO")
    );
    static final List<TherapeuticSupportCatalog.Option> QUANTITY_UNITS = List.of(
        option("UNIT", "UI"),
        option("BAG", "BOLSA"),
        option("BOTTLE", "FRASCO"),
        option("ML", "ML")
    );
    static final List<TherapeuticSupportCatalog.Option> PRE_MEDICATIONS = List.of(
        option("DIPYRONE_1G_EV", "DIPIRONA 1 G EV — 30 MIN ANTES"),
        option("DEXCHLORPHENIRAMINE_5MG_EV", "DEXCLORFENIRAMINA 5 MG EV — 30 MIN ANTES"),
        option("HYDROCORTISONE_100MG_EV", "HIDROCORTISONA 100 MG EV — 30 MIN ANTES")
    );

    public TherapeuticSupportCatalog catalog() {
        return new TherapeuticSupportCatalog(
            CLINICAL_CONTEXTS,
            BASE_SOLUTIONS,
            ELECTROLYTE_ADDITIVES,
            HYDRATION_FREQUENCIES,
            INFUSION_MODES,
            RATE_UNITS,
            SCHEDULING_OPTIONS,
            GLUCOSE_FREQUENCIES,
            INSULIN_TYPES,
            BLOOD_PRODUCTS,
            BLOOD_MODIFICATIONS,
            TRANSFUSION_ROUTES,
            QUANTITY_UNITS,
            PRE_MEDICATIONS
        );
    }

    public String label(List<TherapeuticSupportCatalog.Option> options, String code) {
        return options.stream().filter(option -> option.code().equals(code)).findFirst()
            .map(TherapeuticSupportCatalog.Option::label).orElse(code);
    }

    public String productLabel(String code) {
        return BLOOD_PRODUCTS.stream().filter(option -> option.code().equals(code)).findFirst()
            .map(TherapeuticSupportCatalog.ProductOption::label).orElse(code);
    }

    public boolean isPlasmaDerivative(String code) {
        return BLOOD_PRODUCTS.stream().filter(option -> option.code().equals(code)).findFirst()
            .map(option -> "PLASMA_DERIVATIVE".equals(option.category())).orElse(false);
    }

    private static TherapeuticSupportCatalog.Option option(String code, String label) {
        return new TherapeuticSupportCatalog.Option(code, label);
    }

    private static TherapeuticSupportCatalog.ProductOption product(
        String code,
        String label,
        String category
    ) {
        return new TherapeuticSupportCatalog.ProductOption(code, label, category);
    }
}
