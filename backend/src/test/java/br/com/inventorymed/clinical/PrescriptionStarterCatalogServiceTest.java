package br.com.inventorymed.clinical;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PrescriptionStarterCatalogServiceTest {

    private final PrescriptionStarterCatalogService service = new PrescriptionStarterCatalogService();

    @Test
    void exposesRequestedClinicsAndOnlyCompatibleModels() {
        PrescriptionStarterCatalog catalog = service.catalog();

        assertThat(catalog.clinics())
            .extracting(PrescriptionStarterCatalog.ClinicOption::label)
            .containsExactly(
                "CLÍNICA MÉDICA / UPA",
                "CLÍNICA MÉDICA / HOSPITAL",
                "BOX DE EMERGÊNCIA ADULTO",
                "UTI",
                "PEDIATRIA ENFERMARIA",
                "PEDIATRIA NEONATAL",
                "BOX DE EMERGÊNCIA PED",
                "GINECOLOGIA E OBSTETRÍCIA"
            );
        assertThat(catalog.templates())
            .filteredOn(template -> template.code().equals("EMERGENCY_BOX"))
            .singleElement()
            .satisfies(template -> assertThat(template.clinicCodes())
                .containsExactly("BOX_EMERGENCIA_ADULTO"));
        assertThat(catalog.templates())
            .noneMatch(template -> template.clinicCodes().contains("PEDIATRIA_NEONATAL"));
    }
}
