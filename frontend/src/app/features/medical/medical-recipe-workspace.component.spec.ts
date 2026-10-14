import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MedicalRecipeWorkspaceComponent } from './medical-recipe-workspace.component';
import { MedicalService } from './medical.service';

describe('MedicalRecipeWorkspaceComponent', () => {
  let fixture: ComponentFixture<MedicalRecipeWorkspaceComponent>;
  let component: MedicalRecipeWorkspaceComponent;

  const medical = {
    medicalRecipeCatalog: vi.fn().mockResolvedValue({
      recipeTypes: [
        { code: 'COMMON', label: 'RECEITUÁRIO COMUM', enabled: true },
        { code: 'SPECIAL_CONTROL', label: 'CONTROLE ESPECIAL', enabled: true },
        { code: 'NOTIFICATION_B', label: 'NOTIFICAÇÃO B', enabled: false },
      ],
      routes: [{ code: 'ORAL', label: 'ORAL', enabled: true }],
      medicationCatalogIntegrated: false,
      sncrIntegrated: false,
      qualifiedDigitalSignatureIntegrated: false,
    }),
    templates: vi.fn().mockResolvedValue([
      {
        templateId: 'template-1',
        name: 'RECEITUÁRIO MÉDICO PADRÃO',
        kind: 'MEDICAL_RECIPE',
        active: true,
        versionId: 'version-1',
        versionNumber: 1,
        status: 'PUBLISHED',
        sections: [],
      },
    ]),
    documents: vi.fn().mockResolvedValue([]),
    previewMedicalRecipe: vi.fn().mockResolvedValue({
      request: {
        recipeType: 'COMMON',
        items: [
          {
            route: 'ORAL',
            medication: 'LOSARTANA 50 MG',
            quantity: '30 COMPRIMIDOS',
            dosage: 'TOMAR 1 COMPRIMIDO PELA MANHÃ',
            recommendations: null,
          },
        ],
        buyerName: null,
        buyerIdentityDocument: null,
        buyerAddress: null,
        physicianAttestation: true,
      },
      printTitle: 'RECEITA MÉDICA',
      copyLabels: ['VIA DO PACIENTE'],
      finalizationAllowed: true,
      allergyConflicts: [],
      blockingReasons: [],
      safetyNotices: [],
    }),
    createDocument: vi.fn().mockResolvedValue({
      id: 'document-1',
      createdAt: '2026-10-13T12:00:00Z',
      finalizedAt: '2026-10-13T12:00:00Z',
    }),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [MedicalRecipeWorkspaceComponent],
      providers: [{ provide: MedicalService, useValue: medical }],
    }).compileComponents();
    fixture = TestBed.createComponent(MedicalRecipeWorkspaceComponent);
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
      sex: 'FEMININO',
      allergies: 'DIPIRONA',
      admissionAt: '2026-10-13T10:00:00Z',
      prescriptions: [],
    };
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('starts blank and supports adding and removing selected rows', () => {
    const instance = component as unknown as {
      items: {
        length: number;
        at(index: number): { controls: { selected: { setValue(value: boolean): void } } };
      };
      addRow(): void;
      removeSelectedRows(): void;
    };
    expect(instance.items.length).toBe(1);
    instance.addRow();
    expect(instance.items.length).toBe(2);
    instance.items.at(0).controls.selected.setValue(true);
    instance.removeSelectedRows();
    expect(instance.items.length).toBe(1);
  });

  it('blocks finalization of Notification B while still presenting the draft warning', () => {
    const instance = component as unknown as {
      selectRecipeType(value: string): void;
      canFinalize(): boolean;
    };
    instance.selectRecipeType('NOTIFICATION_B');
    fixture.detectChanges();

    expect(instance.canFinalize()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('RASCUNHO DE APOIO');
    expect(fixture.nativeElement.textContent).toContain('SNCR');
  });
});
