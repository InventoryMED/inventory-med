package br.com.inventorymed.clinical;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ClinicalSummaryCatalogService {

    private static final ClinicalSummaryCatalog CATALOG = new ClinicalSummaryCatalog(
        List.of(
            option("ALTA_HOSPITALAR", "ALTA HOSPITALAR", null),
            option("TRANSFERENCIA_HOSPITALAR", "TRANSFERÊNCIA HOSPITALAR", null),
            option("OBITO", "ÓBITO", "61")
        ),
        List.of(
            option("MELHORADO", "MELHORADO", "12"),
            option("CURADO", "CURADO", "11"),
            option("A_PEDIDO", "A PEDIDO", null),
            option("ALTA_UTI_PARA_ENFERMARIA", "ALTA UTI PARA ENFERMARIA", "21")
        ),
        List.of(
            option("VAGA_UTI", "VAGA DE UTI", null),
            option("ESPECIALIDADE_INDISPONIVEL", "ESPECIALIDADE NÃO DISPONÍVEL", null),
            option("PEDIDO_FAMILIA", "PEDIDO DA FAMÍLIA", null)
        ),
        List.of(
            option("TUBO_OROTRAQUEAL", "TUBO OROTRAQUEAL", null),
            option("CVC", "CATETER VENOSO CENTRAL (CVC)", null),
            option("DRENO_TORAX", "DRENO DE TÓRAX", null),
            option("SONDA_ENTERAL", "SONDA ENTERAL", null),
            option("SVD", "SONDA VESICAL DE DEMORA (SVD)", null)
        ),
        List.of(
            option("SIM", "SIM", null),
            option("NAO", "NÃO", null),
            option("SVO_IML", "SVO / IML", null)
        ),
        List.of(
            option("DIETA", "DIETA / RECOMENDAÇÕES ALIMENTARES", null),
            option("CURATIVO", "CUIDADOS COM FERIDA OPERATÓRIA / CURATIVO", null),
            option("SINAIS_ALERTA", "SINAIS DE ALERTA PARA RETORNO IMEDIATO AO PRONTO-SOCORRO", null),
            option("ACOMPANHAMENTO", "ACOMPANHAMENTO AMBULATORIAL / RETORNO PROGRAMADO", null)
        )
    );

    public ClinicalSummaryCatalog catalog() {
        return CATALOG;
    }

    public String label(List<ClinicalSummaryCatalog.Option> options, String code) {
        if (code == null) return null;
        return options.stream()
            .filter(option -> option.code().equals(code))
            .map(ClinicalSummaryCatalog.Option::label)
            .findFirst()
            .orElse(null);
    }

    public String exitReferenceForCondition(String condition) {
        if (condition == null) return null;
        return CATALOG.dischargeConditions().stream()
            .filter(option -> option.code().equals(condition))
            .map(ClinicalSummaryCatalog.Option::exitReferenceCode)
            .findFirst()
            .orElse(null);
    }

    private static ClinicalSummaryCatalog.Option option(
        String code,
        String label,
        String exitReferenceCode
    ) {
        return new ClinicalSummaryCatalog.Option(code, label, exitReferenceCode);
    }
}
