import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { DiagnosticExamWorkspaceComponent } from './diagnostic-exam-workspace.component';
import { MedicalService } from './medical.service';

describe('DiagnosticExamWorkspaceComponent', () => {
  const catalog = {
    clinicalContexts: [{ code: 'ADULT_ICU', label: 'UTI ADULTO' }],
    quickPacks: [
      {
        code: 'ROUTINE_ICU',
        label: '+ PAINEL ROTINA / ADMISSÃO UTI',
        bloodExamCodes: ['HEMOGRAM'],
        urineExamCodes: [],
        tomographyRegion: null,
        xrayRegion: null,
        ultrasoundCode: null,
        ecg: false,
      },
    ],
    bloodExams: [
      {
        code: 'HEMOGRAM',
        label: 'HEMOGRAMA COMPLETO COM PLAQUETAS',
        tussCode: '40304361',
        sigtapCode: '0202020382',
        highCost: false,
        supplies: ['TUBO EDTA'],
      },
    ],
    urineExams: [],
    tomographyRegions: [],
    contrastOptions: [{ code: 'WITHOUT_CONTRAST', label: 'SEM CONTRASTE' }],
    tomographyJustifications: [],
    xrayProfiles: [],
    lateralities: [{ code: 'NOT_APPLICABLE', label: 'NÃO SE APLICA' }],
    ultrasoundExams: [],
    ecgPriorities: [{ code: 'IMMEDIATE', label: 'IMEDIATO / AGORA' }],
    ecgJustifications: [],
  };
  const medical = {
    diagnosticExamCatalog: vi.fn().mockResolvedValue(catalog),
    templates: vi.fn().mockResolvedValue([]),
    documents: vi.fn().mockResolvedValue([]),
    previewDiagnosticExams: vi.fn(),
    createDocument: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [DiagnosticExamWorkspaceComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
  });

  it('loads the hospital-scoped catalog and applies a quick pack', async () => {
    const fixture = TestBed.createComponent(DiagnosticExamWorkspaceComponent);
    fixture.componentRef.setInput('hospitalName', 'HOSPITAL TESTE');
    fixture.componentRef.setInput('unitName', 'UTI ADULTO');
    fixture.componentRef.setInput('roomName', 'QUARTO 1');
    fixture.componentRef.setInput('bedCode', 'LEITO A');
    fixture.componentRef.setInput('admissionId', 'admission-1');
    fixture.componentRef.setInput('professionalName', 'DR. TESTE');
    fixture.componentRef.setInput('patient', {
      id: 'patient-1',
      admissionId: 'admission-1',
      name: 'PACIENTE TESTE',
      admissionAt: '2026-10-13T12:00:00Z',
      prescriptions: [],
    });

    fixture.detectChanges();
    await vi.waitFor(() =>
      expect((fixture.componentInstance as unknown as { loading: () => boolean }).loading()).toBe(
        false,
      ),
    );
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(medical.diagnosticExamCatalog).toHaveBeenCalledOnce();
    expect(medical.templates).toHaveBeenCalledWith('EXAM');
    expect(element.textContent).toContain('PAINEL ROTINA / ADMISSÃO UTI');
    const quickPack = Array.from(
      element.querySelectorAll('.quick-pack-grid button'),
    )[0] as HTMLButtonElement;
    quickPack.click();
    fixture.detectChanges();
    const hemogram = element.querySelector('input[type="checkbox"]') as HTMLInputElement;
    expect(hemogram.checked).toBe(true);
  });
});
