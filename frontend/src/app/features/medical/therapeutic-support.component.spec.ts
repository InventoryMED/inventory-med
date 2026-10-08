import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { TherapeuticSupportCatalog, TherapeuticSupportResponse } from './medical.models';
import { MedicalService } from './medical.service';
import { TherapeuticSupportComponent } from './therapeutic-support.component';

describe('TherapeuticSupportComponent', () => {
  const catalog: TherapeuticSupportCatalog = {
    clinicalContexts: [{ code: 'ICU', label: 'UTI' }],
    baseSolutions: [{ code: 'SF09_500', label: 'SORO FISIOLÓGICO 0,9% 500 ML' }],
    electrolyteAdditives: [{ code: 'KCL191_10', label: 'KCL 19,1% 10 ML' }],
    hydrationFrequencies: [{ code: 'EVERY_12_HOURS', label: '12/12H' }],
    infusionModes: [{ code: 'PUMP', label: 'BOMBA DE INFUSÃO' }],
    rateUnits: [{ code: 'ML_H', label: 'ML/H' }],
    schedulingOptions: [
      { code: 'FIXED', label: 'FIXO' },
      { code: 'URGENT', label: 'URGENTE' },
    ],
    glucoseFrequencies: [{ code: 'EVERY_4_HOURS', label: '4/4H' }],
    insulinTypes: [{ code: 'REGULAR', label: 'INSULINA REGULAR' }],
    bloodProducts: [{ code: 'RBC', label: 'CONCENTRADO DE HEMÁCIAS', category: 'BLOOD_COMPONENT' }],
    bloodProductModifications: [{ code: 'LEUKOREDUCED', label: 'DESLEUCOCITADO' }],
    transfusionRoutes: [{ code: 'DEDICATED_ACCESS', label: 'ACESSO DEDICADO' }],
    quantityUnits: [{ code: 'UNIT', label: 'UI' }],
    preMedications: [],
  };

  const response: TherapeuticSupportResponse = {
    structuredTherapeuticSupport: {
      summaryLine: 'DXT 4/4H COM PROTOCOLO E ESCALA SC.',
      prescriptionDetails: '8. CONTROLE GLICÊMICO E INSULINOTERAPIA.',
      orderRows: [],
    },
    billingAudit: {
      suppliesEquipmentForReview: ['06 FITAS REAGENTES E 06 LANCETAS'],
      auditAlerts: ['DUPLA CHECAGEM DE INSULINA'],
    },
  };

  const medical = {
    therapeuticSupportCatalog: vi.fn().mockResolvedValue(catalog),
    previewTherapeuticSupport: vi.fn().mockResolvedValue(response),
  };

  beforeEach(async () => {
    medical.therapeuticSupportCatalog.mockClear();
    medical.previewTherapeuticSupport.mockClear();
    await TestBed.configureTestingModule({
      imports: [TherapeuticSupportComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
  });

  it('activates mandatory safety protocols whenever DXT is selected', async () => {
    const fixture = TestBed.createComponent(TherapeuticSupportComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    component.clinicalContext.setValue('ICU');
    component.selectGlucoseFrequency('EVERY_4_HOURS');

    expect(component.glucoseControl.getRawValue()).toMatchObject({
      frequency: 'EVERY_4_HOURS',
      hypoglycemiaProtocolActive: true,
      correctionScaleActive: true,
      insulinType: 'REGULAR',
    });
    expect(await component.validateAndPreview()).toBe(true);
    expect(medical.previewTherapeuticSupport).toHaveBeenCalledWith(
      expect.objectContaining({ clinicalContext: 'ICU' }),
    );
  });

  it('adds and clears structured hydration and blood product rows', async () => {
    const fixture = TestBed.createComponent(TherapeuticSupportComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    component.addHydration();
    component.addBloodProduct();
    expect(component.hasSelection()).toBe(true);
    expect(component.hydrationSolutions.length).toBe(1);
    expect(component.bloodProducts.length).toBe(1);

    component.reset();
    expect(component.hasSelection()).toBe(false);
    expect(component.hydrationSolutions.length).toBe(0);
    expect(component.bloodProducts.length).toBe(0);
  });
});
