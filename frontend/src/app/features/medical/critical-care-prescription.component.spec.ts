import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { CriticalCareCatalog, CriticalCareResponse } from './medical.models';
import { MedicalService } from './medical.service';
import { CriticalCarePrescriptionComponent } from './critical-care-prescription.component';

describe('CriticalCarePrescriptionComponent', () => {
  const catalog: CriticalCareCatalog = {
    clinicalContexts: [{ code: 'ICU', label: 'UTI' }],
    vasoactiveDrugs: [
      {
        code: 'NOREPINEPHRINE',
        label: 'NORADRENALINA',
        centralAccessRequired: true,
        specialTubing: 'STANDARD',
      },
    ],
    sedationDrugs: [
      {
        code: 'ROCURONIUM',
        label: 'ROCURÔNIO',
        category: 'NEUROMUSCULAR_BLOCKER',
        allowedModes: ['CONTINUOUS_BIC'],
      },
    ],
    emergencyDrugs: [{ code: 'SUGAMMADEX', label: 'SUGAMADEX', highCost: true }],
    vascularAccesses: [{ code: 'CVC', label: 'CVC' }],
    bloodPressureMonitoring: [{ code: 'INVASIVE_ARTERIAL', label: 'PAI' }],
    vasoactiveRateUnits: [{ code: 'ML_H', label: 'ML/H' }],
    administrationModes: [{ code: 'CONTINUOUS_BIC', label: 'INFUSÃO CONTÍNUA EM BIC' }],
    sedationRateUnits: [{ code: 'ML_H', label: 'ML/H' }],
    sedationTargets: [{ code: 'RASS_MINUS_4', label: 'RASS -4' }],
    ventilatoryStatuses: [{ code: 'VMI_TOT', label: 'VMI/TOT' }],
    sedationRoutes: [{ code: 'EV_BIC', label: 'EV/BIC' }],
    emergencyRoutes: [{ code: 'EV', label: 'EV' }],
    emergencyScheduling: [{ code: 'NOW', label: 'AGORA' }],
  };
  const response: CriticalCareResponse = {
    structuredCriticalCare: {
      summaryLine: 'NORADRENALINA EM BIC.',
      prescriptionDetails: '14. DROGAS VASOATIVAS E INOTRÓPICOS:',
      orderRows: [],
    },
    billingAudit: { suppliesEquipmentForReview: [], auditAlerts: [] },
  };
  const medical = {
    criticalCareCatalog: vi.fn().mockResolvedValue(catalog),
    previewCriticalCare: vi.fn().mockResolvedValue(response),
  };

  beforeEach(async () => {
    medical.criticalCareCatalog.mockClear();
    medical.previewCriticalCare.mockClear();
    await TestBed.configureTestingModule({
      imports: [CriticalCarePrescriptionComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
  });

  it('creates and clears the three critical care groups', async () => {
    const fixture = TestBed.createComponent(CriticalCarePrescriptionComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    component.addVasoactive();
    component.addSedation();
    component.addEmergency();
    expect(component.hasSelection()).toBe(true);
    expect(component.vasoactiveDrugs.length).toBe(1);
    expect(component.sedationAnalgesiaBnm.length).toBe(1);
    expect(component.emergencyMedications.length).toBe(1);

    component.reset();
    expect(component.hasSelection()).toBe(false);
  });
});
