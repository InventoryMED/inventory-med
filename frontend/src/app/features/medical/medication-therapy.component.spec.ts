import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { MedicationTherapyCatalog, MedicationTherapyResponse } from './medical.models';
import { MedicalService } from './medical.service';
import { MedicationTherapyComponent } from './medication-therapy.component';

describe('MedicationTherapyComponent', () => {
  const catalog: MedicationTherapyCatalog = {
    clinicalContexts: [{ code: 'ICU', label: 'UTI' }],
    antimicrobials: [{ code: 'CEFTRIAXONE', label: 'CEFTRIAXONA' }],
    antimicrobialRoutes: [{ code: 'EV', label: 'EV' }],
    antimicrobialAdministrationModes: [
      { code: 'INTERMITTENT_INFUSION', label: 'INFUSÃO INTERMITENTE' },
    ],
    diluents: [{ code: 'SF_09_100', label: 'SF 0,9% 100 ML' }],
    infusionSets: [{ code: 'MACRODRIP', label: 'EQUIPO MACROGOTAS' }],
    antimicrobialScheduling: [{ code: 'FIXED', label: 'FIXO' }],
    ccihStatuses: [{ code: 'AUTHORIZED', label: 'LIBERADO' }],
    renalFunctionMeasures: [{ code: 'EGFR', label: 'TFG' }],
    renalDoseAssessments: [{ code: 'ADJUSTED', label: 'DOSE AJUSTADA' }],
    prophylaxisOptions: [
      {
        code: 'ENOXAPARIN',
        label: 'ENOXAPARINA',
        category: 'TEV_FARMACOLOGICA',
        anticoagulant: true,
        filledSyringe: true,
        equipment: false,
      },
    ],
    prophylaxisRoutes: [{ code: 'SC', label: 'SC' }],
    prophylaxisScheduling: [{ code: 'FIXED', label: 'FIXO' }],
    reconciliationStatuses: [{ code: 'MAINTAINED_HOME', label: 'MANTIDO' }],
    continuousMedicationScheduling: [{ code: 'FIXED', label: 'FIXO' }],
    symptomaticMedications: [{ code: 'DIPYRONE', label: 'DIPIRONA' }],
    medicationRoutes: [{ code: 'EV', label: 'EV' }],
    symptomaticScheduling: [{ code: 'PRN', label: 'SN' }],
  };
  const response: MedicationTherapyResponse = {
    structuredMedicationTherapy: {
      summaryLine: 'D1 CEFTRIAXONA.',
      prescriptionDetails: '11. ANTIMICROBIANOS E ANTIBIOTICOTERAPIA:',
      orderRows: [],
    },
    billingAudit: { suppliesEquipmentForReview: [], auditAlerts: [] },
  };
  const medical = {
    medicationTherapyCatalog: vi.fn().mockResolvedValue(catalog),
    previewMedicationTherapy: vi.fn().mockResolvedValue(response),
  };

  beforeEach(async () => {
    medical.medicationTherapyCatalog.mockClear();
    medical.previewMedicationTherapy.mockClear();
    await TestBed.configureTestingModule({
      imports: [MedicationTherapyComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
  });

  it('creates and clears the four medication groups', async () => {
    const fixture = TestBed.createComponent(MedicationTherapyComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    component.addAntimicrobial();
    component.addProphylaxis();
    component.addContinuousMedication();
    component.addSymptomatic();
    expect(component.hasSelection()).toBe(true);
    expect(component.antimicrobials.length).toBe(1);
    expect(component.prophylaxes.length).toBe(1);
    expect(component.continuousMedications.length).toBe(1);
    expect(component.analgesiaSymptomatics.length).toBe(1);

    component.reset();
    expect(component.hasSelection()).toBe(false);
  });

  it('shows the loading state while the catalog is pending', () => {
    medical.medicationTherapyCatalog.mockImplementationOnce(() => new Promise(() => undefined));
    const fixture = TestBed.createComponent(MedicationTherapyComponent);

    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(
      'CARREGANDO CATÁLOGO DE TERAPIA MEDICAMENTOSA',
    );
    fixture.destroy();
  });

  it('shows the API error and allows the catalog to be requested again', async () => {
    medical.medicationTherapyCatalog.mockRejectedValueOnce({
      error: { message: 'Acesso não autorizado para este perfil' },
    });
    const fixture = TestBed.createComponent(MedicationTherapyComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('ACESSO NÃO AUTORIZADO PARA ESTE PERFIL');
    const retry = Array.from<HTMLButtonElement>(
      fixture.nativeElement.querySelectorAll('button'),
    ).find((button) => button.textContent?.includes('TENTAR NOVAMENTE'));
    expect(retry).toBeTruthy();

    retry?.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(medical.medicationTherapyCatalog).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('CONTEXTO CLÍNICO');
  });

  it('keeps the prescription invalid when the backend rejects the preview', async () => {
    medical.previewMedicationTherapy.mockRejectedValueOnce({
      error: { message: 'Informe a função renal' },
    });
    const fixture = TestBed.createComponent(MedicationTherapyComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    component.addAntimicrobial();

    expect(await component.validateAndPreview()).toBe(false);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('INFORME A FUNÇÃO RENAL');
  });
});
