package br.com.inventorymed.clinical;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PrescriptionStarterCatalogService {

    private static final String CLINICA_MEDICA_UPA = "CLINICA_MEDICA_UPA";
    private static final String CLINICA_MEDICA_HOSPITAL = "CLINICA_MEDICA_HOSPITAL";
    private static final String BOX_EMERGENCIA_ADULTO = "BOX_EMERGENCIA_ADULTO";
    private static final String UTI = "UTI";

    private static final List<PrescriptionStarterCatalog.ClinicOption> CLINICS = List.of(
        new PrescriptionStarterCatalog.ClinicOption(CLINICA_MEDICA_UPA, "CLÍNICA MÉDICA / UPA"),
        new PrescriptionStarterCatalog.ClinicOption(
            CLINICA_MEDICA_HOSPITAL,
            "CLÍNICA MÉDICA / HOSPITAL"
        ),
        new PrescriptionStarterCatalog.ClinicOption(
            BOX_EMERGENCIA_ADULTO,
            "BOX DE EMERGÊNCIA ADULTO"
        ),
        new PrescriptionStarterCatalog.ClinicOption(UTI, "UTI"),
        new PrescriptionStarterCatalog.ClinicOption(
            "PEDIATRIA_ENFERMARIA",
            "PEDIATRIA ENFERMARIA"
        ),
        new PrescriptionStarterCatalog.ClinicOption(
            "PEDIATRIA_NEONATAL",
            "PEDIATRIA NEONATAL"
        ),
        new PrescriptionStarterCatalog.ClinicOption(
            "BOX_EMERGENCIA_PEDIATRICA",
            "BOX DE EMERGÊNCIA PED"
        ),
        new PrescriptionStarterCatalog.ClinicOption(
            "GINECOLOGIA_OBSTETRICIA",
            "GINECOLOGIA E OBSTETRÍCIA"
        )
    );

    private static final List<String> ADULT_CLINICAL_UNITS = List.of(
        CLINICA_MEDICA_UPA,
        CLINICA_MEDICA_HOSPITAL,
        UTI
    );

    private static final List<PrescriptionStarterCatalog.TemplateOption> TEMPLATES = List.of(
        template(
            "ADMISSION",
            "ADMISSÃO",
            "MODELO INICIAL PARA ADMISSÃO HOSPITALAR",
            ADULT_CLINICAL_UNITS
        ),
        template("PAC", "PAC", "PNEUMONIA ADQUIRIDA NA COMUNIDADE", ADULT_CLINICAL_UNITS),
        template("CAD", "CAD", "CETOACIDOSE DIABÉTICA", ADULT_CLINICAL_UNITS),
        template("TVP", "TVP", "TROMBOSE VENOSA PROFUNDA", ADULT_CLINICAL_UNITS),
        template("TEP", "TEP", "TROMBOEMBOLISMO PULMONAR", ADULT_CLINICAL_UNITS),
        template(
            "EMERGENCY_BOX",
            "BOX DE EMERGÊNCIA",
            "ATENDIMENTO EM BOX DE EMERGÊNCIA ADULTO",
            List.of(BOX_EMERGENCIA_ADULTO)
        )
    );

    public PrescriptionStarterCatalog catalog() {
        return new PrescriptionStarterCatalog(CLINICS, TEMPLATES);
    }

    private static PrescriptionStarterCatalog.TemplateOption template(
        String code,
        String name,
        String description,
        List<String> clinicCodes
    ) {
        return new PrescriptionStarterCatalog.TemplateOption(code, name, description, clinicCodes);
    }
}
