import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ClinicalSummaryWorkspaceComponent } from './clinical-summary-workspace.component';
import { MedicalService } from './medical.service';

describe('ClinicalSummaryWorkspaceComponent', () => {
  const catalog = {
    outcomeTypes: [
      { code: 'ALTA_HOSPITALAR', label: 'ALTA HOSPITALAR', exitReferenceCode: null },
      {
        code: 'TRANSFERENCIA_HOSPITALAR',
        label: 'TRANSFERÊNCIA HOSPITALAR',
        exitReferenceCode: null,
      },
      { code: 'OBITO', label: 'ÓBITO', exitReferenceCode: '61' },
    ],
    dischargeConditions: [{ code: 'MELHORADO', label: 'MELHORADO', exitReferenceCode: '12' }],
    transferReasons: [{ code: 'VAGA_UTI', label: 'VAGA DE UTI', exitReferenceCode: null }],
    devices: [{ code: 'CVC', label: 'CATETER VENOSO CENTRAL (CVC)', exitReferenceCode: null }],
    necropsyOptions: [{ code: 'NAO', label: 'NÃO', exitReferenceCode: null }],
    orientationSuggestions: [
      { code: 'DIETA', label: 'DIETA / RECOMENDAÇÕES ALIMENTARES', exitReferenceCode: null },
    ],
  };
  const template = {
    templateId: 'template-1',
    name: 'RESUMO CLÍNICO',
    kind: 'CLINICAL_SUMMARY' as const,
    active: true,
    versionId: 'version-1',
    versionNumber: 1,
    status: 'PUBLISHED',
    sections: [],
  };
  const response = {
    request: {
      outcomeType: 'ALTA_HOSPITALAR' as const,
      dischargeCondition: 'MELHORADO',
      clinicalSummary: 'Paciente evoluiu com melhora clínica.',
      primaryCid: 'J18.9',
      dischargeMedications: [],
      dischargeOrientations: [],
      transfer: null,
      death: null,
      physicianAttestation: true,
    },
    outcomeReference: {
      dischargeReason: 'ALTA_MELHORA' as const,
      sigtapExitReferenceCode: '12',
      sigtapExitReferenceLabel: 'MELHORADO',
      closesAdmission: true,
    },
    reportText: 'RESUMO DA EVOLUÇÃO CLÍNICA: Paciente evoluiu com melhora clínica.',
    auditAlerts: [],
  };
  const medical = {
    clinicalSummaryCatalog: vi.fn().mockResolvedValue(catalog),
    templates: vi.fn().mockResolvedValue([template]),
    documents: vi.fn().mockResolvedValue([]),
    previewClinicalSummary: vi.fn().mockResolvedValue(response),
    createDocument: vi.fn().mockResolvedValue({
      id: 'summary-1',
      finalizedAt: '2026-10-13T15:00:00Z',
      createdAt: '2026-10-13T15:00:00Z',
    }),
    discharge: vi.fn().mockResolvedValue(undefined),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    medical.documents.mockResolvedValue([]);
    await TestBed.configureTestingModule({
      imports: [ClinicalSummaryWorkspaceComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
  });

  function createComponent(initialOutcome = '') {
    const fixture = TestBed.createComponent(ClinicalSummaryWorkspaceComponent);
    fixture.componentRef.setInput('hospitalName', 'HOSPITAL TESTE');
    fixture.componentRef.setInput('unitName', 'CLÍNICA MÉDICA');
    fixture.componentRef.setInput('roomName', 'QUARTO 1');
    fixture.componentRef.setInput('bedCode', 'LEITO A');
    fixture.componentRef.setInput('admissionId', 'admission-1');
    fixture.componentRef.setInput('professionalName', 'DR. TESTE');
    fixture.componentRef.setInput('initialOutcome', initialOutcome);
    fixture.componentRef.setInput('patient', {
      id: 'patient-1',
      admissionId: 'admission-1',
      name: 'PACIENTE TESTE',
      diagnosis: 'PNEUMONIA',
      allergies: 'DIPIRONA',
      admissionAt: '2026-10-13T12:00:00Z',
      prescriptions: [],
    });
    return fixture;
  }

  it('loads the summary tab with patient data and the requested outcome', async () => {
    const fixture = createComponent('TRANSFERENCIA_HOSPITALAR');
    fixture.detectChanges();
    await vi.waitFor(() =>
      expect((fixture.componentInstance as unknown as { loading: () => boolean }).loading()).toBe(
        false,
      ),
    );
    fixture.detectChanges();

    expect(medical.templates).toHaveBeenCalledWith('CLINICAL_SUMMARY');
    expect(fixture.nativeElement.textContent).toContain('PACIENTE TESTE');
    expect(fixture.nativeElement.textContent).toContain('REGULAÇÃO E CONTINUIDADE ASSISTENCIAL');
    expect(fixture.nativeElement.querySelector('.allergy-alert')).toBeTruthy();
  });

  it('finalizes the document before sending the discharge command', async () => {
    const fixture = createComponent();
    const component = fixture.componentInstance as unknown as {
      form: {
        controls: {
          dischargeCondition: { setValue(value: string): void };
          physicianAttestation: { setValue(value: boolean): void };
        };
      };
      finalizeDocument(): Promise<void>;
      concludeOutcome(): Promise<void>;
    };
    fixture.detectChanges();
    await vi.waitFor(() =>
      expect(
        (
          fixture.componentInstance as unknown as {
            loading(): boolean;
          }
        ).loading(),
      ).toBe(false),
    );
    component.form.controls.dischargeCondition.setValue('MELHORADO');
    component.form.controls.physicianAttestation.setValue(true);
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    await component.finalizeDocument();
    expect(medical.createDocument).toHaveBeenCalledWith(
      'admission-1',
      template,
      expect.objectContaining({ 'CLINICAL_SUMMARY.REPORT': expect.any(Object) }),
      true,
    );

    await component.concludeOutcome();
    expect(medical.discharge).toHaveBeenCalledWith('admission-1', 'ALTA_MELHORA', 'summary-1');
  });
});
