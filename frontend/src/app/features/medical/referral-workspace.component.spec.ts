import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReferralWorkspaceComponent } from './referral-workspace.component';
import { MedicalService } from './medical.service';

describe('ReferralWorkspaceComponent', () => {
  let fixture: ComponentFixture<ReferralWorkspaceComponent>;
  let component: ReferralWorkspaceComponent;

  const catalog = {
    specialties: [
      { code: 'PULMONOLOGY', label: 'PNEUMOLOGIA' },
      { code: 'OTHER', label: 'OUTRA ESPECIALIDADE / ÁREA' },
    ],
    priorities: [{ code: 'ELECTIVE', label: 'ELETIVO / ROTINA' }],
    deadlines: [{ code: 'UP_TO_30_DAYS', label: 'ATÉ 30 DIAS' }],
    destinations: [{ code: 'PUBLIC_REGULATION', label: 'REDE PÚBLICA / REGULAÇÃO SUS' }],
    attachments: [{ code: 'DISCHARGE_SUMMARY', label: 'SUMÁRIO / RELATÓRIO DE ALTA' }],
    regulationSystemIntegrated: false,
  };
  const medical = {
    referralCatalog: vi.fn().mockResolvedValue(catalog),
    templates: vi.fn().mockResolvedValue([
      {
        templateId: 'template-1',
        name: 'ENCAMINHAMENTO MÉDICO PADRÃO',
        kind: 'REFERRAL',
        active: true,
        versionId: 'version-1',
        versionNumber: 1,
        status: 'PUBLISHED',
        sections: [],
      },
    ]),
    documents: vi.fn().mockResolvedValue([]),
    previewReferral: vi.fn().mockImplementation((_admissionId, request) =>
      Promise.resolve({
        request,
        specialtyLabel: 'PNEUMOLOGIA',
        priorityLabel: 'ELETIVO / ROTINA',
        deadlineLabel: 'ATÉ 30 DIAS',
        destinationLabel: 'REDE PÚBLICA / REGULAÇÃO SUS',
        attachmentLabels: [],
        summaryLine: 'GUIA DE ENCAMINHAMENTO — PNEUMOLOGIA',
        finalizationAllowed: request.physicianAttestation,
        blockingReasons: request.physicianAttestation ? [] : ['CONFIRME A REVISÃO MÉDICA'],
        safetyNotices: [],
      }),
    ),
    createDocument: vi.fn(),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [ReferralWorkspaceComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
    fixture = TestBed.createComponent(ReferralWorkspaceComponent);
    component = fixture.componentInstance;
    component.hospitalName = 'HOSPITAL DE JOÃO PINHEIRO';
    component.unitName = 'CLÍNICA MÉDICA';
    component.roomName = 'QUARTO 01';
    component.bedCode = 'LEITO 01';
    component.admissionId = 'admission-1';
    component.professionalName = 'DR. LUCAS GALANTE';
    component.patient = {
      id: 'patient-1',
      admissionId: 'admission-1',
      name: 'PACIENTE TESTE',
      diagnosis: 'PNEUMONIA',
      admissionAt: '2026-10-15T12:00:00Z',
      prescriptions: [],
    };
    fixture.detectChanges();
    await component.ngOnInit();
    fixture.detectChanges();
  });

  it('shows the patient context and warns when regulation integration is unavailable', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('PACIENTE TESTE');
    expect(text).toContain('PNEUMONIA');
    expect(text).toContain('SEM INTEGRAÇÃO AUTOMÁTICA');
  });

  it('keeps delivered documents optional in the preview payload', async () => {
    const instance = component as unknown as {
      form: {
        controls: {
          specialtyCode: { setValue(value: string): void };
          clinicalJustification: { setValue(value: string): void };
        };
      };
      generatePreview(): Promise<boolean>;
    };
    instance.form.controls.specialtyCode.setValue('PULMONOLOGY');
    instance.form.controls.clinicalJustification.setValue(
      'REAVALIAÇÃO ESPECIALIZADA APÓS A INTERNAÇÃO.',
    );
    await instance.generatePreview();

    expect(medical.previewReferral).toHaveBeenCalledWith(
      'admission-1',
      expect.objectContaining({ attachments: [] }),
    );
  });
});
