import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { BedsideProcedureWorkspaceComponent } from './bedside-procedure-workspace.component';
import { MedicalService } from './medical.service';

describe('BedsideProcedureWorkspaceComponent', () => {
  const medical = {
    bedsideProcedureCatalog: vi.fn().mockResolvedValue({
      clinicalContexts: [{ code: 'ADULT_ICU', label: 'UTI ADULTO' }],
      recordTypes: [
        { code: 'REQUESTED', label: 'SOLICITADO / PLANEJADO' },
        { code: 'PERFORMED', label: 'REALIZADO' },
      ],
      procedures: [
        {
          code: 'CVC',
          label: 'CATETERISMO VENOSO CENTRAL POR PUNÇÃO',
          billingReference: 'TUSS 31401606',
          pairedSite: true,
          deviceTraceabilityRequired: true,
          postProcedureControlRequired: true,
          majorInvasiveProcedure: true,
          supplyKitItems: ['KIT CVC'],
          defaultSite: 'VEIA JUGULAR INTERNA',
          defaultAsepsis: 'CLOREXIDINA',
          defaultSterileBarrier: 'BARREIRA MÁXIMA',
          defaultAnesthesia: 'LIDOCAÍNA 2%',
          defaultImageGuidance: 'POCUS',
          defaultDevice: 'KIT CVC',
          defaultCaliber: '7 FR',
          defaultFixation: 'MONONYLON E FILME',
          defaultSamples: 'SEM AMOSTRAS',
          defaultPostProcedureControl: 'CHEST_XRAY',
          defaultMonitoring: 'ECG, SPO2 E PANI',
        },
      ],
      lateralities: [
        { code: 'RIGHT', label: 'DIREITA' },
        { code: 'LEFT', label: 'ESQUERDA' },
      ],
      urgencyOptions: [{ code: 'IMMEDIATE_URGENT', label: 'IMEDIATO / URGENTE' }],
      postProcedureControls: [{ code: 'CHEST_XRAY', label: 'RADIOGRAFIA DE TÓRAX' }],
      quickKits: [],
      templates: [],
    }),
    templates: vi.fn().mockResolvedValue([]),
    documents: vi.fn().mockResolvedValue([]),
    previewBedsideProcedure: vi.fn(),
    createDocument: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [BedsideProcedureWorkspaceComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
  });

  it('loads a hospital-scoped catalog and starts with one empty procedure', async () => {
    const fixture = TestBed.createComponent(BedsideProcedureWorkspaceComponent);
    fixture.componentRef.setInput('hospitalName', 'HOSPITAL TESTE');
    fixture.componentRef.setInput('unitName', 'CLÍNICA MÉDICA');
    fixture.componentRef.setInput('roomName', 'QUARTO 1');
    fixture.componentRef.setInput('bedCode', 'A');
    fixture.componentRef.setInput('admissionId', 'admission-1');
    fixture.componentRef.setInput('professionalName', 'DR. TESTE');
    fixture.componentRef.setInput('patient', {
      id: 'patient-1',
      admissionId: 'admission-1',
      name: 'PACIENTE TESTE',
      admissionAt: '2026-10-08T10:00:00Z',
      prescriptions: [],
    });

    fixture.detectChanges();
    await vi.waitFor(() =>
      expect((fixture.componentInstance as unknown as { loading: () => boolean }).loading()).toBe(
        false,
      ),
    );
    fixture.detectChanges();

    expect(medical.bedsideProcedureCatalog).toHaveBeenCalledOnce();
    expect(medical.templates).toHaveBeenCalledWith('PROCEDURE');
    expect(medical.documents).toHaveBeenCalledWith('admission-1');
    expect(fixture.nativeElement.textContent).toContain('PROCEDIMENTO 1');
    expect(fixture.nativeElement.textContent).toContain('CONTEXTO CLÍNICO');
    expect(fixture.nativeElement.textContent).toContain('NENHUM REGISTRO DE PROCEDIMENTO');
  });
});
