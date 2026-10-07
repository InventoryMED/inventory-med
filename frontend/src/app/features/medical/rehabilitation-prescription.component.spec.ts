import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { RehabilitationCatalog, RehabilitationResponse } from './medical.models';
import { MedicalService } from './medical.service';
import { RehabilitationPrescriptionComponent } from './rehabilitation-prescription.component';

describe('RehabilitationPrescriptionComponent', () => {
  const catalog: RehabilitationCatalog = {
    specialties: [
      { code: 'MOTOR_PHYSIOTHERAPY', label: 'FISIOTERAPIA MOTORA E MOBILIZAÇÃO PRECOCE' },
    ],
    respiratoryProcedures: [],
    motorProcedures: [{ code: 'PASSIVE_MOBILIZATION', label: 'MOBILIZAÇÃO PASSIVA NO LEITO' }],
    speechTherapyProcedures: [],
    occupationalTherapyProcedures: [],
    respiratoryFrequencies: [],
    motorFrequencies: [
      { code: 'DAILY', label: '1X AO DIA' },
      { code: 'EVERY_8H', label: '8/8H — 3X AO DIA' },
    ],
    speechTherapyFrequencies: [],
    occupationalTherapyFrequencies: [],
    schedulingOptions: [{ code: 'FIXED', label: 'FIXO' }],
    templates: [
      {
        code: 'MOTOR_PASSIVE_DAILY',
        label: 'FISIOTERAPIA MOTORA — MOBILIZAÇÃO PASSIVA 1X/DIA',
        item: {
          id: 1,
          specialty: 'MOTOR_PHYSIOTHERAPY',
          procedure: 'PASSIVE_MOBILIZATION',
          frequency: 'DAILY',
          scheduling: 'FIXED',
          clinicalJustification: '',
        },
      },
    ],
  };

  const response: RehabilitationResponse = {
    structuredRehabilitation: {
      summaryLine: 'FISIOTERAPIA MOTORA: MOBILIZAÇÃO PASSIVA — 1X AO DIA.',
      prescriptionDetails: 'REABILITAÇÃO MULTIDISCIPLINAR.',
      orderRows: [
        {
          description: 'MOBILIZAÇÃO PASSIVA NO LEITO',
          specialty: 'FISIOTERAPIA MOTORA',
          frequency: '1X AO DIA',
          scheduling: 'FIXO',
        },
      ],
    },
    billingAudit: { itemsForReview: ['CONFERIR SESSÃO'], auditAlerts: [] },
  };

  const medical = {
    rehabilitationCatalog: vi.fn().mockResolvedValue(catalog),
    previewRehabilitation: vi.fn().mockResolvedValue(response),
  };

  beforeEach(async () => {
    medical.rehabilitationCatalog.mockClear();
    medical.previewRehabilitation.mockClear();
    await TestBed.configureTestingModule({
      imports: [RehabilitationPrescriptionComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
  });

  it('applies a template and emits the backend-validated plan', async () => {
    const fixture = TestBed.createComponent(RehabilitationPrescriptionComponent);
    const component = fixture.componentInstance as any;
    const selections: unknown[] = [];
    component.selectionChange.subscribe((selection: unknown) => selections.push(selection));
    fixture.detectChanges();
    await fixture.whenStable();

    component.applyTemplate('MOTOR_PASSIVE_DAILY');
    expect(component.hasSelection()).toBe(true);
    expect(await component.validateAndPreview()).toBe(true);

    expect(medical.previewRehabilitation).toHaveBeenCalledWith(
      expect.objectContaining({
        selectedTemplate: 'MOTOR_PASSIVE_DAILY',
        items: [expect.objectContaining({ specialty: 'MOTOR_PHYSIOTHERAPY' })],
      }),
    );
    expect(selections.at(-1)).toEqual(expect.objectContaining({ response }));
  });

  it('shows that high-frequency physiotherapy needs a clinical justification', async () => {
    const fixture = TestBed.createComponent(RehabilitationPrescriptionComponent);
    const component = fixture.componentInstance as any;
    fixture.detectChanges();
    await fixture.whenStable();

    component.applyTemplate('MOTOR_PASSIVE_DAILY');
    const group = component.items.at(0);
    component.changeFrequency(group, 'EVERY_8H');

    expect(component.requiresJustification(group)).toBe(true);
    expect(group.controls.scheduling.value).toBe('FIXED');
  });
});
