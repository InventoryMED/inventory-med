import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { IsolationPrecautionCatalog, IsolationPrecautionResponse } from './medical.models';
import { MedicalService } from './medical.service';
import { IsolationPrecautionsComponent } from './isolation-precautions.component';

describe('IsolationPrecautionsComponent', () => {
  const catalog: IsolationPrecautionCatalog = {
    precautionTypes: [{ code: 'CONTACT', label: 'PRECAUÇÃO DE CONTATO' }],
    durationOptions: [{ code: 'ENTIRE_HOSPITALIZATION', label: 'DURANTE TODA A INTERNAÇÃO' }],
    schedulingOptions: [{ code: 'CONTINUOUS', label: 'CONTÍNUO' }],
    templates: [
      {
        code: 'CONTACT_KPC_MDR',
        label: 'PRECAUÇÃO DE CONTATO — KPC / ENTEROBACTÉRIAS MDR',
        items: [
          {
            id: 1,
            precautionType: 'CONTACT',
            reasonPathogen: 'COLONIZAÇÃO POR KPC',
            durationReview: 'ENTIRE_HOSPITALIZATION',
            scheduling: 'CONTINUOUS',
          },
        ],
      },
    ],
  };

  const response: IsolationPrecautionResponse = {
    structuredIsolation: {
      summaryLine: 'PRECAUÇÃO DE CONTATO — COLONIZAÇÃO POR KPC.',
      prescriptionDetails: '1. PRECAUÇÕES E ISOLAMENTO:\n- PRECAUÇÃO DE CONTATO.',
      orderRows: [
        {
          description: 'PRECAUÇÃO DE CONTATO — COLONIZAÇÃO POR KPC',
          durationReview: 'DURANTE TODA A INTERNAÇÃO',
          scheduling: 'CONTÍNUO',
        },
      ],
    },
    billingAudit: {
      suppliesEquipmentForReview: ['AVENTAL / CAPOTE E LUVAS'],
      auditAlerts: ['CONFIRMAR CULTURA PRÉVIA OU SWAB'],
    },
  };

  const medical = {
    isolationPrecautionCatalog: vi.fn().mockResolvedValue(catalog),
    previewIsolationPrecautions: vi.fn().mockResolvedValue(response),
  };

  beforeEach(async () => {
    medical.isolationPrecautionCatalog.mockClear();
    medical.previewIsolationPrecautions.mockClear();
    await TestBed.configureTestingModule({
      imports: [IsolationPrecautionsComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
  });

  it('applies a template and emits the backend-validated isolation plan', async () => {
    const fixture = TestBed.createComponent(IsolationPrecautionsComponent);
    const component = fixture.componentInstance;
    const selections: unknown[] = [];
    component.selectionChange.subscribe((selection: unknown) => selections.push(selection));
    fixture.detectChanges();
    await fixture.whenStable();

    component.applyTemplate('CONTACT_KPC_MDR');
    expect(component.hasSelection()).toBe(true);
    expect(await component.validateAndPreview()).toBe(true);

    expect(medical.previewIsolationPrecautions).toHaveBeenCalledWith(
      expect.objectContaining({
        selectedTemplate: 'CONTACT_KPC_MDR',
        items: [expect.objectContaining({ precautionType: 'CONTACT' })],
      }),
    );
    expect(selections.at(-1)).toEqual(expect.objectContaining({ response }));
  });

  it('resets the plan without retaining the selected pathogen', async () => {
    const fixture = TestBed.createComponent(IsolationPrecautionsComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    component.applyTemplate('CONTACT_KPC_MDR');
    component.reset();

    expect(component.hasSelection()).toBe(false);
    expect(component.items.length).toBe(0);
    expect(component.preview()).toBeNull();
  });
});
