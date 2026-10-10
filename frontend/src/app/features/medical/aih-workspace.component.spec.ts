import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { AihWorkspaceComponent } from './aih-workspace.component';
import { MedicalService } from './medical.service';

describe('AihWorkspaceComponent', () => {
  const medical = {
    aihCatalog: vi.fn().mockResolvedValue({
      clinicalContexts: [{ code: 'ADULT_ICU', label: 'UTI ADULTO' }],
      admissionCharacters: [{ code: 'URGENCY', label: 'URGÊNCIA' }],
      lateralities: [
        { code: 'RIGHT', label: 'DIREITO' },
        { code: 'LEFT', label: 'ESQUERDO' },
      ],
      procedures: [
        {
          code: 'CVC',
          label: 'CATETERISMO VENOSO CENTRAL POR PUNÇÃO',
          tussCode: '31401606',
          cbhpmCode: null,
          sigtapCode: null,
          pairedSite: true,
          defaultSite: 'VEIA JUGULAR INTERNA',
        },
      ],
      imageGuidance: { tussCode: '40901262', cbhpmCode: null, sigtapCode: null },
    }),
    templates: vi.fn().mockResolvedValue([]),
    documents: vi.fn().mockResolvedValue([]),
    previewAih: vi.fn().mockResolvedValue({
      structuredAih: {
        patient: { name: 'PACIENTE TESTE' },
        manualData: {},
        clinicalContext: '',
        admissionCharacter: '',
        requestedProcedures: [],
        reportText: 'LAUDO PARA SOLICITAÇÃO DE AIH',
      },
      billingAudit: { alerts: [] },
    }),
    createDocument: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [AihWorkspaceComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
  });

  it('autofills available census data and keeps the AIH printable with optional fields', async () => {
    const fixture = TestBed.createComponent(AihWorkspaceComponent);
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
      diagnosis: 'DIAGNÓSTICO TESTE',
      admissionAt: '2026-10-09T10:00:00Z',
      prescriptions: [],
    });

    fixture.detectChanges();
    await vi.waitFor(() =>
      expect(
        (
          fixture.componentInstance as unknown as {
            loading: () => boolean;
          }
        ).loading(),
      ).toBe(false),
    );
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      form: { getRawValue: () => Record<string, unknown> };
      generatePreview: () => Promise<boolean>;
    };
    const values = component.form.getRawValue() as {
      patient: { name: string; hospital: string; bed: string };
      manualData: { initialDiagnosis: string };
    };
    expect(values.patient).toMatchObject({
      name: 'PACIENTE TESTE',
      hospital: 'HOSPITAL TESTE',
      bed: 'QUARTO 1 • A',
    });
    expect(values.manualData.initialDiagnosis).toBe('DIAGNÓSTICO TESTE');

    await expect(component.generatePreview()).resolves.toBe(true);
    expect(medical.previewAih).toHaveBeenCalledWith(
      expect.objectContaining({ patientId: 'patient-1', requestedProcedures: [] }),
    );
    expect(fixture.nativeElement.textContent).toContain('TODOS OS CAMPOS SÃO OPCIONAIS');
  });
});
