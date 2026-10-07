import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { VentilatorySupportCatalog, VentilatorySupportResponse } from './medical.models';
import { MedicalService } from './medical.service';
import { VentilatorySupportComponent } from './ventilatory-support.component';

describe('VentilatorySupportComponent', () => {
  const catalog: VentilatorySupportCatalog = {
    supportTypes: [
      { code: 'ROOM_AIR', label: 'AR AMBIENTE' },
      { code: 'LOW_FLOW', label: 'OXIGENOTERAPIA DE BAIXO FLUXO' },
    ],
    lowFlowDevices: [{ code: 'NASAL_CANNULA', label: 'CATETER NASAL DE O₂' }],
    lowFlowFrequencies: [{ code: 'PRN_SPO2_92', label: 'SE NECESSÁRIO SE SPO₂ < 92%' }],
    highFlowInterfaceSizes: [],
    nonInvasiveModes: [],
    nonInvasiveInterfaces: [],
    nonInvasiveFrequencies: [],
    invasiveAirways: [],
    invasiveModes: [],
    schedulingOptions: [{ code: 'PRN', label: 'SN' }],
    protectiveGoals: [],
    templates: [
      {
        code: 'NASAL_CANNULA_2L_PRN_92',
        label: 'CATETER NASAL 2 L/MIN — SN SE SPO₂ < 92%',
        item: {
          id: 1,
          supportType: 'LOW_FLOW',
          frequency: 'PRN_SPO2_92',
          scheduling: 'PRN',
          lowFlow: {
            device: 'NASAL_CANNULA',
            oxygenFlowLitersMinute: 2,
            fio2Percent: null,
          },
          highFlow: null,
          nonInvasive: null,
          invasive: null,
        },
      },
    ],
  };

  const response: VentilatorySupportResponse = {
    structuredVentilatorySupport: {
      summaryLine: 'CATETER NASAL DE O₂ 2 L/MIN, SN SE SPO₂ < 92%.',
      prescriptionDetails: 'CATETER NASAL DE O₂ 2 L/MIN.',
      orderRows: [
        {
          description: 'CATETER NASAL DE O₂ 2 L/MIN',
          interfaceRoute: 'CATETER NASAL',
          frequency: 'SN SE SPO₂ < 92%',
          scheduling: 'SN',
        },
      ],
    },
    billingAudit: { itemsForReview: ['CONFERIR CATETER'], auditAlerts: [] },
  };

  const medical = {
    ventilatorySupportCatalog: vi.fn().mockResolvedValue(catalog),
    previewVentilatorySupport: vi.fn().mockResolvedValue(response),
  };

  beforeEach(async () => {
    medical.ventilatorySupportCatalog.mockClear();
    medical.previewVentilatorySupport.mockClear();
    await TestBed.configureTestingModule({
      imports: [VentilatorySupportComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
  });

  it('applies a validated template and emits the backend response', async () => {
    const fixture = TestBed.createComponent(VentilatorySupportComponent);
    const component = fixture.componentInstance as any;
    const selections: unknown[] = [];
    component.selectionChange.subscribe((selection: unknown) => selections.push(selection));
    fixture.detectChanges();
    await fixture.whenStable();

    component.applyTemplate('NASAL_CANNULA_2L_PRN_92');
    expect(component.hasSelection()).toBe(true);
    expect(await component.validateAndPreview()).toBe(true);

    expect(medical.previewVentilatorySupport).toHaveBeenCalledWith(
      expect.objectContaining({
        selectedTemplate: 'NASAL_CANNULA_2L_PRN_92',
        items: [
          expect.objectContaining({
            supportType: 'LOW_FLOW',
            scheduling: 'PRN',
          }),
        ],
      }),
    );
    expect(selections.at(-1)).toEqual(expect.objectContaining({ response }));
  });

  it('resets the structured plan without retaining stale respiratory parameters', async () => {
    const fixture = TestBed.createComponent(VentilatorySupportComponent);
    const component = fixture.componentInstance as any;
    fixture.detectChanges();
    await fixture.whenStable();

    component.applyTemplate('NASAL_CANNULA_2L_PRN_92');
    component.reset();

    expect(component.hasSelection()).toBe(false);
    expect(component.items.length).toBe(0);
    expect(component.preview()).toBeNull();
  });
});
