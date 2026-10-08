import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { environment } from '../environments/environment';
import { AuthService } from './auth/auth.service';
import { DemoStore } from './demo-store';
import { AdministrationComponent } from './features/administration/administration.component';
import { DietPrescriptionComponent } from './features/medical/diet-prescription.component';
import { IsolationPrecautionsComponent } from './features/medical/isolation-precautions.component';
import { MonitoringPrescriptionComponent } from './features/medical/monitoring-prescription.component';
import { NursingCarePrescriptionComponent } from './features/medical/nursing-care-prescription.component';
import { RehabilitationPrescriptionComponent } from './features/medical/rehabilitation-prescription.component';
import { VentilatorySupportComponent } from './features/medical/ventilatory-support.component';
import {
  ClinicalFormKind,
  ClinicalFormTemplate,
  DietPrescriptionDraft,
  DietPrescriptionResponse,
  DietPrescriptionSelection,
  IsolationPrecautionDraft,
  IsolationPrecautionResponse,
  IsolationPrecautionSelection,
  MonitoringPrescriptionDraft,
  MonitoringPrescriptionResponse,
  MonitoringPrescriptionSelection,
  NursingCarePrescriptionDraft,
  NursingCarePrescriptionResponse,
  NursingCarePrescriptionSelection,
  RehabilitationDraft,
  RehabilitationResponse,
  RehabilitationSelection,
  VentilatorySupportDraft,
  VentilatorySupportResponse,
  VentilatorySupportSelection,
} from './features/medical/medical.models';
import { MedicalService } from './features/medical/medical.service';
import {
  AppScreen,
  Bed,
  BedStatus,
  DischargeReason,
  Hospital,
  MedicationSectionDraft,
  MedicationSectionId,
  Patient,
  Prescription,
  PrescriptionDraftRow,
  PrescriptionScheduling,
  Room,
} from './models';

type PrescriptionTemplateId = 'ADMISSION' | 'PAC' | 'CAD' | 'TVP' | 'TEP' | 'EMERGENCY_BOX';

interface PrescriptionTemplate {
  id: PrescriptionTemplateId;
  name: string;
  description: string;
}

interface PrescriptionRowPreset {
  description: string;
  route: string;
  frequency: string;
  scheduling: PrescriptionScheduling;
}

interface MedicationOrderGroup {
  id: MedicationSectionId;
  eyebrow: string;
  title: string;
  helper: string;
  defaultRoute: string;
  presets: PrescriptionRowPreset[];
  selectedPreset: string;
  rows: PrescriptionDraftRow[];
}

interface PrintablePrescriptionRow {
  section: string;
  description: string;
  route: string;
  frequency: string;
  scheduling: string;
}

interface EvolutionExamRow {
  name: string;
  values: string[];
}

interface InfusionMedication {
  id: string;
  label: string;
  selected: boolean;
  rateMlHour: string;
}

type RespiratoryTab = 'PATTERN' | 'SUPPORT';

interface HospitalOption {
  id: string;
  name: string;
  shortName: string;
  city: string;
  detail: string;
}

const PRESCRIPTION_TEMPLATES: PrescriptionTemplate[] = [
  { id: 'ADMISSION', name: 'ADMISSÃO', description: 'MODELO INICIAL PARA ADMISSÃO HOSPITALAR' },
  { id: 'PAC', name: 'PAC', description: 'PNEUMONIA ADQUIRIDA NA COMUNIDADE' },
  { id: 'CAD', name: 'CAD', description: 'CETOACIDOSE DIABÉTICA' },
  { id: 'TVP', name: 'TVP', description: 'TROMBOSE VENOSA PROFUNDA' },
  { id: 'TEP', name: 'TEP', description: 'TROMBOEMBOLISMO PULMONAR' },
  {
    id: 'EMERGENCY_BOX',
    name: 'BOX DE EMERGÊNCIA',
    description: 'ATENDIMENTO EM BOX DE EMERGÊNCIA',
  },
];

const HYDRATION_PRESETS: PrescriptionRowPreset[] = [
  {
    description: 'SORO FISIOLÓGICO 0,9% 500ML',
    route: 'EV',
    frequency: '12/12HR',
    scheduling: 'FIXO',
  },
  {
    description: 'SORO FISIOLÓGICO 0,9% 250ML',
    route: 'EV',
    frequency: '6/6HR',
    scheduling: 'FIXO',
  },
  {
    description: 'RINGER LACTATO 500ML',
    route: 'EV',
    frequency: '12/12HR',
    scheduling: 'FIXO',
  },
];

const ANALGESIA_PRESETS: PrescriptionRowPreset[] = [
  {
    description: 'DIPIRONA 1 AMPOLA + 18ML DE SF 0,9%',
    route: 'EV',
    frequency: '6/6HR',
    scheduling: 'FIXO',
  },
  {
    description: 'PARACETAMOL 500MG 1 COMPRIMIDO',
    route: 'VO',
    frequency: '6/6HR',
    scheduling: 'FIXO',
  },
  {
    description: 'TRAMAL 1 AMPOLA + 100ML DE SF 0,9%',
    route: 'EV',
    frequency: '8/8HR',
    scheduling: 'SN',
  },
];

const SYMPTOMATIC_PRESETS: PrescriptionRowPreset[] = [
  {
    description: 'PLASIL 1 AMPOLA + 18ML DE ABD',
    route: 'EV',
    frequency: '8/8HR',
    scheduling: 'SN',
  },
  {
    description: 'ONDANSETRONA 1 AMPOLA + 100ML DE SF 0,9%',
    route: 'EV',
    frequency: '8/8HR',
    scheduling: 'SN',
  },
];

const PROPHYLAXIS_PRESETS: PrescriptionRowPreset[] = [
  {
    description: 'OMEPRAZOL 1 AMPOLA — FAZER DE MANHÃ',
    route: 'EV',
    frequency: '24/24HR',
    scheduling: 'FIXO',
  },
  {
    description: 'ENOXAPARINA 40MG',
    route: 'SC',
    frequency: '24/24HR',
    scheduling: 'FIXO',
  },
  {
    description: 'HNF 5000UI',
    route: 'EV',
    frequency: '12/12HR',
    scheduling: 'FIXO',
  },
];

const ANTIBIOTIC_PRESETS: PrescriptionRowPreset[] = [
  {
    description: 'CEFTRIAXONA 1G + 100ML DE SF 0,9%',
    route: 'EV',
    frequency: '12/12HR',
    scheduling: 'FIXO',
  },
  {
    description: 'AZITROMICINA 500MG',
    route: 'VO',
    frequency: '24/24HR',
    scheduling: 'FIXO',
  },
];

@Component({
  imports: [
    CommonModule,
    FormsModule,
    AdministrationComponent,
    DietPrescriptionComponent,
    NursingCarePrescriptionComponent,
    MonitoringPrescriptionComponent,
    VentilatorySupportComponent,
    RehabilitationPrescriptionComponent,
    IsolationPrecautionsComponent,
  ],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit {
  protected readonly store = inject(DemoStore);
  protected readonly auth = inject(AuthService);
  private readonly medical = inject(MedicalService);
  protected readonly realApiEnabled = environment.useRealApi;
  protected readonly screen = signal<AppScreen>('login');
  protected readonly expandedRooms = signal<Set<string>>(new Set());
  protected readonly selectedBedId = signal<string | null>(null);
  protected readonly admissionOpen = signal(false);
  protected readonly patientEditOpen = signal(false);
  protected readonly dischargeOpen = signal(false);
  protected readonly transferOpen = signal(false);
  protected readonly prescriptionReady = signal(false);
  protected readonly searchTerm = signal('');
  protected readonly toast = signal<string | null>(null);
  protected readonly authBusy = signal(false);
  protected readonly authError = signal<string | null>(null);
  protected readonly dietPreview = signal<DietPrescriptionResponse | null>(null);
  private readonly dietComponent = viewChild(DietPrescriptionComponent);
  protected readonly nursingCarePreview = signal<NursingCarePrescriptionResponse | null>(null);
  private readonly nursingCareComponent = viewChild(NursingCarePrescriptionComponent);
  protected readonly monitoringPreview = signal<MonitoringPrescriptionResponse | null>(null);
  private readonly monitoringComponent = viewChild(MonitoringPrescriptionComponent);
  protected readonly ventilatorySupportPreview = signal<VentilatorySupportResponse | null>(null);
  private readonly ventilatorySupportComponent = viewChild(VentilatorySupportComponent);
  protected readonly rehabilitationPreview = signal<RehabilitationResponse | null>(null);
  private readonly rehabilitationComponent = viewChild(RehabilitationPrescriptionComponent);
  protected readonly isolationPrecautionPreview = signal<IsolationPrecautionResponse | null>(null);
  private readonly isolationPrecautionComponent = viewChild(IsolationPrecautionsComponent);

  protected loginEmail = '';
  protected loginPassword = '';
  protected currentPassword = '';
  protected newPassword = '';
  protected confirmNewPassword = '';
  protected patientName = '';
  protected patientBirthDate = '';
  protected patientSex = 'FEMININO';
  protected patientWeight: number | null = null;
  protected dischargeReason: DischargeReason | '' = '';
  protected transferTargetBedId = '';
  protected prescriptionDiagnosis = '';
  protected prescriptionComorbidities = '';
  protected prescriptionAllergies = '';
  protected prescriptionDiet = '';
  protected dietDraft: DietPrescriptionDraft = this.emptyDietDraft();
  protected nursingCareDraft: NursingCarePrescriptionDraft = this.emptyNursingCareDraft();
  protected monitoringDraft: MonitoringPrescriptionDraft = this.emptyMonitoringDraft();
  protected ventilatorySupportDraft: VentilatorySupportDraft = this.emptyVentilatorySupportDraft();
  protected rehabilitationDraft: RehabilitationDraft = this.emptyRehabilitationDraft();
  protected isolationPrecautionDraft: IsolationPrecautionDraft =
    this.emptyIsolationPrecautionDraft();
  protected evolutionDiet = '';
  protected evolutionAdmission = '';
  protected evolutionText = '';
  protected evolutionPosition = '';
  protected evolutionAccompaniment = '';
  protected evolutionHygiene = '';
  protected evolutionConsciousness = '';
  protected evolutionOrientation = '';
  protected evolutionInteraction = '';
  protected evolutionSedationStatus = '';
  protected evolutionSedatives: InfusionMedication[] = this.createSedativeMedications();
  protected evolutionRespiratoryTab: RespiratoryTab = 'PATTERN';
  protected evolutionRespiratoryPatterns: string[] = [];
  protected evolutionVentilatorySupport = '';
  protected evolutionHemodynamicStability = '';
  protected evolutionPressureProfile = '';
  protected evolutionPamWithinTarget = false;
  protected evolutionNoVasoactiveDrugs = false;
  protected evolutionVasoactiveMedications: InfusionMedication[] =
    this.createVasoactiveMedications();
  protected evolutionFoodAcceptance = '';
  protected evolutionUrinaryElimination = '';
  protected evolutionUrinaryVolume24h = '';
  protected evolutionUrineAppearance = '';
  protected evolutionIntestinalElimination = '';
  protected evolutionNoBowelMovementDays = '';
  protected evolutionStoolAppearance = '';
  protected evolutionSleepPattern = '';
  protected evolutionVitalSaturation = '';
  protected evolutionHeartRate = '';
  protected evolutionRespiratoryRate = '';
  protected evolutionBloodPressureSystolic = '';
  protected evolutionBloodPressureDiastolic = '';
  protected evolutionTemperature = '';
  protected evolutionGeneralState = '';
  protected evolutionCyanosis = '';
  protected evolutionJaundice = '';
  protected evolutionFever = '';
  protected evolutionColoring = '';
  protected evolutionHydration = '';
  protected evolutionNeurological = '';
  protected evolutionRespiratoryExam = '';
  protected evolutionCardiovascularExam = '';
  protected evolutionAbdomen = '';
  protected evolutionLowerLimbs = '';
  protected evolutionUpperLimbPerfusion = '';
  protected evolutionLowerLimbPerfusion = '';
  protected evolutionComplementaryNotes = '';
  protected evolutionConduct = '';
  protected evolutionAntibioticCurrent = '';
  protected evolutionAntibioticPrevious = '';
  protected evolutionExamDates: string[] = ['', '', '', ''];
  protected evolutionExamRows: EvolutionExamRow[] = this.createEvolutionExamRows();
  protected observationRows: string[] = [];
  protected abnormalityRows: string[] = [];
  protected hydrationRow: PrescriptionDraftRow = this.emptyHydrationRow();
  protected selectedHydrationPreset = '';
  protected medicationGroups: MedicationOrderGroup[] = [];
  protected selectedTemplateId: PrescriptionTemplateId | '' = '';
  protected readonly prescriptionTemplates = PRESCRIPTION_TEMPLATES;
  protected readonly evolutionPositionOptions = [
    'ACAMADO EM DECÚBITO DORSAL',
    'SENTADO NA POLTRONA',
    'DEAMBULANDO NO QUARTO',
    'RESTRITO AO LEITO POR CONTENÇÃO MECÂNICA',
  ];
  protected readonly evolutionAccompanimentOptions = [
    'ACOMPANHADO POR FAMILIAR / CUIDADOR',
    'DESACOMPANHADO NO MOMENTO',
  ];
  protected readonly evolutionConsciousnessOptions = [
    'VIGIL / ACORDADO',
    'SONOLENTO (DESPERTA AO CHAMADO)',
    'TORPOROSO (DESPERTA APENAS COM ESTÍMULO VIGOROSO)',
    'COMATOSO / SEDADO',
  ];
  protected readonly evolutionOrientationOptions = [
    'ORIENTADO NO TEMPO E NO ESPAÇO',
    'DESORIENTADO NO TEMPO',
    'DESORIENTADO NO ESPAÇO',
    'GLOBALMENTE DESORIENTADO',
  ];
  protected readonly evolutionHygieneOptions = ['BOA HIGIENE', 'MÁ HIGIENE'];
  protected readonly evolutionInteractionOptions = [
    'CONTACTUANTE E COOPERATIVO',
    'CONTACTUANTE NÃO-VERBAL (INTERAGE POR GESTOS / OLHAR)',
    'POUCO CONTACTUANTE / APÁTICO',
    'NÃO CONTACTUANTE',
    'INCONTACTÁVEL POR SEDAÇÃO CONTÍNUA',
  ];
  protected readonly evolutionSedationStatusOptions = [
    'MANUTENÇÃO EM DOSE ESTÁVEL',
    'EM PROCESSO DE DESMAME / REDUÇÃO GRADUAL DE DOSE',
    'EM ESCALONAMENTO / AUMENTO DE DOSE (POR AGITAÇÃO OU ASSINCRONIA COM O VENTILADOR)',
    'PAUSA PROGRAMADA DA SEDAÇÃO (TESTE DO DESPERTAR DIÁRIO)',
    'SEDAÇÃO SUSPENSA NAS ÚLTIMAS 24H',
  ];
  protected readonly evolutionRespiratoryPatternOptions = [
    'SEM SINAIS DE ESFORÇO RESPIRATÓRIO (EXPANSIBILIDADE PRESERVADA E SIMÉTRICA)',
    'USO DE MUSCULATURA ACESSÓRIA (TIRAGEM INTERCOSTAL, SUBCOSTAL OU SUPRACLAVICULAR)',
    'BATIMENTO DE ASAS DO NARIZ (BAN)',
    'DISSOCIAÇÃO TORACOABDOMINAL',
    'GEMIDO EXPIRATÓRIO',
  ];
  protected readonly evolutionVentilatorySupportOptions = [
    'AR AMBIENTE (AA)',
    'CATETER NASAL DE O₂',
    'MÁSCARA SIMPLES DE O₂',
    'MÁSCARA DE VENTURI',
    'VENTILAÇÃO NÃO INVASIVA (VNI / CPAP / BIPAP)',
    'VENTILAÇÃO MECÂNICA INVASIVA (VMI)',
    'TRAQUEOSTOMIA EM AR AMBIENTE / EM MÁSCARA DE TQT / TUBO EM T',
  ];
  protected readonly evolutionHemodynamicStabilityOptions = [
    'HEMODINAMICAMENTE ESTÁVEL',
    'HEMODINAMICAMENTE ESTÁVEL SOB MEDICAÇÃO',
    'HEMODINAMICAMENTE LIMÍTROFE',
    'HEMODINAMICAMENTE INSTÁVEL',
  ];
  protected readonly evolutionPressureProfileOptions = ['NORMOTENSO', 'HIPOTENSO', 'HIPERTENSO'];
  protected readonly evolutionFoodAcceptanceOptions = [
    'BOA (>75% DA REFEIÇÃO)',
    'PARCIAL / REGULAR (~50%)',
    'INAPETENTE / RECUSA ALIMENTAR (<25%)',
    'EM JEJUM PARA PROCEDIMENTO / EXAME',
  ];
  protected readonly evolutionUrinaryEliminationOptions = [
    'ESPONTÂNEA',
    'POR SONDA VESICAL DE DEMORA (SVD)',
    'POR SONDA VESICAL DE ALÍVIO (SVA)',
    'AUSENTE / ANÚRIA',
  ];
  protected readonly evolutionUrineAppearanceOptions = [
    'CLARA / CITRINA',
    'CONCENTRADA / COLÚRICA',
    'HEMATÚRICA (COM SANGUE)',
    'PIÚRICA / TURVA',
  ];
  protected readonly evolutionIntestinalEliminationOptions = [
    'PRESENTES E PRESERVADAS NAS ÚLTIMAS 24H',
    'AUSENTES',
    'EPISÓDIOS DIARREICOS',
  ];
  protected readonly evolutionStoolAppearanceOptions = [
    'PASTOSAS / FORMADAS',
    'LÍQUIDAS',
    'ESCÍBALAS (ENDURECIDAS)',
    'MELENA / ENTERORRAGIA (COM SANGUE)',
  ];
  protected readonly evolutionSleepPatternOptions = [
    'PRESERVADO / DORMIU BEM',
    'INSÔNIA / AGITADO DURANTE A NOITE',
  ];
  protected readonly dischargeReasons: Array<{
    value: DischargeReason;
    label: string;
    description: string;
  }> = [
    { value: 'ÓBITO', label: 'Óbito', description: 'Encerramento da internação por óbito.' },
    {
      value: 'TRANSFERÊNCIA',
      label: 'Transferência',
      description: 'Saída para atendimento em outra unidade.',
    },
    {
      value: 'ALTA MELHORA',
      label: 'Alta melhora',
      description: 'Paciente liberado após melhora clínica.',
    },
  ];
  protected readonly hydrationPresets = HYDRATION_PRESETS;
  protected readonly printHours = Array.from({ length: 24 }, (_, hour) =>
    hour.toString().padStart(2, '0'),
  );
  protected readonly currentDate = new Date();

  protected readonly activeHospital = this.store.activeHospital;
  protected readonly hospitals = computed<HospitalOption[]>(() => {
    if (this.realApiEnabled) {
      return this.auth.hospitals().map((hospital) => ({
        id: hospital.id,
        name: hospital.name,
        shortName: hospital.shortName,
        city: hospital.city,
        detail: this.roleLabel(hospital.role),
      }));
    }
    return this.store.hospitals().map((hospital) => ({
      id: hospital.id,
      name: hospital.name,
      shortName: hospital.shortName,
      city: hospital.city,
      detail: `${hospital.rooms.length} QUARTOS CADASTRADOS`,
    }));
  });
  protected readonly selectedHospitalOptionId = computed(() =>
    this.realApiEnabled
      ? (this.auth.selectedHospitalId() ?? '')
      : (this.activeHospital()?.id ?? ''),
  );
  protected readonly professionalName = computed(() => {
    const name = this.auth.user()?.name ?? 'LUCAS GALANTE';
    const role = this.auth.selectedHospital()?.role ?? this.auth.hospitals()[0]?.role ?? 'MEDICO';
    return role === 'MEDICO' && !name.toLocaleUpperCase('pt-BR').startsWith('DR.')
      ? `DR. ${name}`
      : name;
  });
  protected readonly professionalRole = computed(() =>
    this.roleLabel(
      this.auth.selectedHospital()?.role ?? this.auth.hospitals()[0]?.role ?? 'MEDICO',
    ),
  );
  protected readonly professionalInitials = computed(() =>
    this.professionalName()
      .replace(/^DR\.\s*/i, '')
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toLocaleUpperCase('pt-BR'),
  );
  protected readonly selectedBed = computed(() => {
    const bedId = this.selectedBedId();
    return bedId ? this.store.findBed(bedId) : undefined;
  });
  protected readonly selectedRoom = computed(() => {
    const bedId = this.selectedBedId();
    return this.activeHospital()?.rooms.find((room) => room.beds.some((bed) => bed.id === bedId));
  });
  protected readonly availableTransferBeds = computed(() =>
    (this.activeHospital()?.rooms ?? []).flatMap((room) =>
      room.beds
        .filter((bed) => bed.status === 'AVAILABLE')
        .map((bed) => ({
          id: bed.id,
          label: `${room.name} • ${bed.code} • ${room.unit}`,
        })),
    ),
  );
  protected readonly visibleRooms = computed(() => {
    const hospital = this.activeHospital();
    const term = this.searchTerm().trim().toLocaleLowerCase('pt-BR');
    if (!hospital) return [];
    if (!term) return hospital.rooms;

    return hospital.rooms.filter((room) =>
      [room.name, room.floor, room.unit, ...room.beds.map((bed) => bed.patient?.name ?? '')]
        .join(' ')
        .toLocaleLowerCase('pt-BR')
        .includes(term),
    );
  });
  protected readonly stats = computed(() => {
    const beds = this.activeHospital()?.rooms.flatMap((room) => room.beds) ?? [];
    const count = (status: BedStatus) => beds.filter((bed) => bed.status === status).length;
    return {
      total: beds.length,
      available: count('AVAILABLE'),
      occupied: count('OCCUPIED'),
      unavailable: count('CLEANING') + count('MAINTENANCE'),
    };
  });

  ngOnInit(): void {
    if (this.realApiEnabled) {
      void this.restoreAuthenticatedSession();
      return;
    }
    this.openRequestedView();
  }

  protected async login(): Promise<void> {
    if (!this.loginEmail.trim() || !this.loginPassword.trim()) {
      this.authError.set('INFORME E-MAIL E SENHA PARA CONTINUAR.');
      return;
    }
    this.authError.set(null);

    if (!this.realApiEnabled) {
      this.screen.set('hospital-select');
      return;
    }

    this.authBusy.set(true);
    try {
      await this.auth.login(this.loginEmail, this.loginPassword);
      this.loginPassword = '';
      this.continueAfterAuthentication();
    } catch (error) {
      this.authError.set(this.authErrorMessage(error));
    } finally {
      this.authBusy.set(false);
    }
  }

  protected async changeInitialPassword(): Promise<void> {
    this.authError.set(null);
    if (!this.currentPassword || !this.newPassword || !this.confirmNewPassword) {
      this.authError.set('PREENCHA OS TRÊS CAMPOS DE SENHA.');
      return;
    }
    if (this.newPassword !== this.confirmNewPassword) {
      this.authError.set('A CONFIRMAÇÃO NÃO É IGUAL À NOVA SENHA.');
      return;
    }

    this.authBusy.set(true);
    try {
      await this.auth.changePassword(this.currentPassword, this.newPassword);
      this.currentPassword = '';
      this.newPassword = '';
      this.confirmNewPassword = '';
      this.continueAfterAuthentication();
    } catch (error) {
      this.authError.set(this.authErrorMessage(error));
    } finally {
      this.authBusy.set(false);
    }
  }

  protected async enterHospital(hospitalId: string): Promise<void> {
    if (!this.realApiEnabled) {
      this.store.selectHospital(hospitalId);
      this.screen.set('rooms');
      this.expandedRooms.set(new Set());
      return;
    }

    this.authBusy.set(true);
    this.authError.set(null);
    try {
      const response = await this.auth.selectHospital(hospitalId);
      await this.openRoomsForHospital(response.hospital.name);
    } catch (error) {
      this.authError.set(this.authErrorMessage(error));
    } finally {
      this.authBusy.set(false);
    }
  }

  protected async switchHospital(hospitalId: string): Promise<void> {
    if (this.realApiEnabled) {
      this.authBusy.set(true);
      try {
        const response = await this.auth.selectHospital(hospitalId);
        await this.openRoomsForHospital(response.hospital.name);
      } catch (error) {
        this.showToast(this.authErrorMessage(error));
        return;
      } finally {
        this.authBusy.set(false);
      }
    } else {
      this.store.selectHospital(hospitalId);
    }
    this.expandedRooms.set(new Set());
    this.selectedBedId.set(null);
    this.admissionOpen.set(false);
    this.patientEditOpen.set(false);
    this.dischargeOpen.set(false);
    this.transferOpen.set(false);
  }

  protected async logout(): Promise<void> {
    await this.auth.logout();
    this.authError.set(null);
    this.screen.set('login');
    this.store.activeHospitalId.set(null);
    this.expandedRooms.set(new Set());
    this.admissionOpen.set(false);
    this.patientEditOpen.set(false);
    this.dischargeOpen.set(false);
    this.transferOpen.set(false);
  }

  protected backToHospitalSelection(): void {
    this.authError.set(null);
    this.screen.set('hospital-select');
  }

  protected toggleRoom(room: Room): void {
    this.expandedRooms.update((current) => {
      const next = new Set(current);
      next.has(room.id) ? next.delete(room.id) : next.add(room.id);
      return next;
    });
  }

  protected isRoomExpanded(roomId: string): boolean {
    return this.expandedRooms().has(roomId);
  }

  protected availableBedsCount(room: Room): number {
    return room.beds.filter((bed) => bed.status === 'AVAILABLE').length;
  }

  protected openAdmission(bed: Bed): void {
    if (bed.status !== 'AVAILABLE') return;
    this.selectedBedId.set(bed.id);
    this.resetPatientForm();
    this.admissionOpen.set(true);
  }

  protected openPatientEditor(bed: Bed): void {
    if (bed.status !== 'OCCUPIED' || !bed.patient) return;
    this.selectedBedId.set(bed.id);
    this.preparePatientForm(bed.patient);
    this.patientEditOpen.set(true);
  }

  protected closePatientEditor(): void {
    this.patientEditOpen.set(false);
    this.selectedBedId.set(null);
  }

  protected closePatientModal(): void {
    if (this.patientEditOpen()) {
      this.closePatientEditor();
      return;
    }
    this.closeAdmission();
  }

  protected submitPatientForm(): void {
    if (this.patientEditOpen()) {
      void this.updatePatient();
      return;
    }
    void this.admitPatient();
  }

  protected openPrescription(bed: Bed): void {
    if (bed.status === 'OCCUPIED' && bed.patient) this.openPrescriptionTab(bed.id);
  }

  protected startEvolution(bed: Bed): void {
    if (!bed.patient) return;
    this.openEvolutionTab(bed.id);
  }

  protected openDischarge(bed: Bed): void {
    if (!bed.patient) return;
    this.selectedBedId.set(bed.id);
    this.dischargeReason = '';
    this.dischargeOpen.set(true);
  }

  protected closeDischarge(): void {
    this.dischargeOpen.set(false);
    this.dischargeReason = '';
    this.selectedBedId.set(null);
  }

  protected async confirmDischarge(): Promise<void> {
    const bedId = this.selectedBedId();
    if (!bedId || !this.dischargeReason) {
      this.showToast('SELECIONE O MOTIVO DA ALTA.');
      return;
    }
    const bed = this.store.findBed(bedId);
    if (this.realApiEnabled) {
      if (!bed?.patient?.admissionId) {
        this.showToast('NÃO FOI POSSÍVEL IDENTIFICAR A INTERNAÇÃO.');
        return;
      }
      const patientName = bed.patient.name;
      const reason = this.dischargeReason;
      try {
        await this.medical.discharge(bed.patient.admissionId, this.dischargeReasonCode(reason));
        await this.refreshRealWorkspace();
        this.dischargeOpen.set(false);
        this.dischargeReason = '';
        this.selectedBedId.set(null);
        this.showToast(`ALTA DE ${patientName} REGISTRADA: ${reason}.`);
      } catch (error) {
        this.showToast(this.clinicalErrorMessage(error));
      }
      return;
    }

    const patientName = this.store.dischargePatient(bedId, this.dischargeReason);
    if (!patientName) {
      this.showToast('NÃO FOI POSSÍVEL CONCLUIR A ALTA.');
      return;
    }
    const reason = this.dischargeReason;
    this.dischargeOpen.set(false);
    this.dischargeReason = '';
    this.selectedBedId.set(null);
    this.showToast(`ALTA DE ${patientName} REGISTRADA: ${reason}.`);
  }

  protected openTransfer(bed: Bed): void {
    if (!bed.patient) return;
    this.selectedBedId.set(bed.id);
    this.transferTargetBedId = '';
    this.transferOpen.set(true);
  }

  protected closeTransfer(): void {
    this.transferOpen.set(false);
    this.transferTargetBedId = '';
    this.selectedBedId.set(null);
  }

  protected async confirmTransfer(): Promise<void> {
    const sourceBedId = this.selectedBedId();
    if (!sourceBedId || !this.transferTargetBedId) {
      this.showToast('SELECIONE O LEITO DE DESTINO.');
      return;
    }
    if (this.realApiEnabled) {
      const admissionId = this.store.findBed(sourceBedId)?.patient?.admissionId;
      if (!admissionId) {
        this.showToast('NÃO FOI POSSÍVEL IDENTIFICAR A INTERNAÇÃO.');
        return;
      }
      try {
        await this.medical.transfer(admissionId, this.transferTargetBedId);
        await this.refreshRealWorkspace();
        this.transferOpen.set(false);
        this.transferTargetBedId = '';
        this.selectedBedId.set(null);
        this.showToast('PACIENTE TRANSFERIDO PARA O NOVO LEITO.');
      } catch (error) {
        this.showToast(this.clinicalErrorMessage(error));
      }
      return;
    }

    if (!this.store.transferPatient(sourceBedId, this.transferTargetBedId)) {
      this.showToast('NÃO FOI POSSÍVEL TRANSFERIR O PACIENTE.');
      return;
    }
    this.transferOpen.set(false);
    this.transferTargetBedId = '';
    this.selectedBedId.set(null);
    this.showToast('PACIENTE TRANSFERIDO PARA O NOVO LEITO.');
  }

  protected async admitPatient(): Promise<void> {
    const bedId = this.selectedBedId();
    if (!bedId || !this.patientName.trim()) {
      this.showToast('Preencha o nome do paciente.');
      return;
    }
    if (this.patientWeight !== null && this.patientWeight <= 0) {
      this.showToast('Informe um peso válido ou deixe o campo vazio.');
      return;
    }

    if (this.realApiEnabled) {
      const birthDate = this.parseBirthDate(this.patientBirthDate);
      if (this.patientBirthDate.trim() && !birthDate) {
        this.showToast('INFORME A DATA DE NASCIMENTO NO FORMATO DD/MM/AAAA.');
        return;
      }
      try {
        await this.medical.admit({
          bedId,
          fullName: this.patientName.trim(),
          birthDate,
          sex: this.patientSex === 'NÃO INFORMADO' ? 'NAO_INFORMADO' : this.patientSex,
          weightKg: this.patientWeight,
          diagnosis: this.prescriptionDiagnosis.trim() || null,
          comorbidities: this.prescriptionComorbidities.trim() || null,
          allergies: this.prescriptionAllergies.trim() || null,
        });
        await this.refreshRealWorkspace();
        this.admissionOpen.set(false);
        this.selectedBedId.set(null);
        this.showToast('PACIENTE ADMITIDO. USE ABRIR PRESCRIÇÃO NO LEITO PARA CONTINUAR.');
      } catch (error) {
        this.showToast(this.clinicalErrorMessage(error));
      }
      return;
    }

    this.store.admitPatient(bedId, {
      name: this.patientName.trim(),
      birthDate: this.patientBirthDate.trim() || undefined,
      sex: this.patientSex,
      weightKg: this.patientWeight ?? undefined,
      diagnosis: this.prescriptionDiagnosis.trim() || undefined,
      comorbidities: this.prescriptionComorbidities.trim() || undefined,
      allergies: this.prescriptionAllergies.trim() || undefined,
    });
    this.admissionOpen.set(false);
    this.selectedBedId.set(null);
    this.showToast('PACIENTE ADMITIDO. USE ABRIR PRESCRIÇÃO NO LEITO PARA CONTINUAR.');
  }

  protected async updatePatient(): Promise<void> {
    if (!(await this.persistPatientChanges(false))) return;
    this.patientEditOpen.set(false);
    this.selectedBedId.set(null);
    this.showToast('CADASTRO DO PACIENTE ATUALIZADO E REGISTRADO NA AUDITORIA.');
  }

  protected closeAdmission(): void {
    this.admissionOpen.set(false);
    this.selectedBedId.set(null);
  }

  protected backToRooms(): void {
    this.screen.set('rooms');
    this.selectedBedId.set(null);
    window.scrollTo({ top: 0, left: 0 });
  }

  protected printPrescription(): void {
    window.print();
  }

  protected printEvolution(): void {
    window.print();
  }

  protected formatEvolutionConduct(value: string): string {
    return value
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => `- ${line.replace(/^-\s*/, '')}`)
      .join('\n');
  }

  protected toggleEvolutionSelection(selection: string[], option: string, checked: boolean): void {
    const optionIndex = selection.indexOf(option);
    if (checked && optionIndex === -1) selection.push(option);
    if (!checked && optionIndex >= 0) selection.splice(optionIndex, 1);
  }

  protected hasEvolutionSelection(selection: string[], option: string): boolean {
    return selection.includes(option);
  }

  protected setRespiratoryTab(tab: RespiratoryTab): void {
    this.evolutionRespiratoryTab = tab;
  }

  protected toggleInfusionMedication(medication: InfusionMedication, checked: boolean): void {
    medication.selected = checked;
    if (!checked) medication.rateMlHour = '';
  }

  protected toggleVasoactiveMedication(medication: InfusionMedication, checked: boolean): void {
    this.toggleInfusionMedication(medication, checked);
    if (checked) this.evolutionNoVasoactiveDrugs = false;
  }

  protected toggleNoVasoactiveDrugs(checked: boolean): void {
    this.evolutionNoVasoactiveDrugs = checked;
    if (!checked) return;
    this.evolutionVasoactiveMedications.forEach((medication) => {
      medication.selected = false;
      medication.rateMlHour = '';
    });
  }

  protected formattedInfusionMedications(medications: InfusionMedication[]): string {
    return (
      medications
        .filter((medication) => medication.selected)
        .map(
          (medication) =>
            `${medication.label}${
              medication.rateMlHour.trim()
                ? ` — VAZÃO ${medication.rateMlHour.trim()} ML/H`
                : ' — VAZÃO NÃO INFORMADA'
            }`,
        )
        .join('; ') || 'NÃO INFORMADO'
    );
  }

  protected formattedVasoactiveSupport(): string {
    if (this.evolutionNoVasoactiveDrugs) return 'SEM USO DE DROGAS VASOATIVAS';
    return this.formattedInfusionMedications(this.evolutionVasoactiveMedications);
  }

  protected formattedUrinaryElimination(): string {
    if (!this.evolutionUrinaryElimination) return 'NÃO INFORMADO';
    if (
      this.evolutionUrinaryElimination !== 'POR SONDA VESICAL DE DEMORA (SVD)' ||
      !this.evolutionUrinaryVolume24h.trim()
    ) {
      return this.evolutionUrinaryElimination;
    }
    return `${this.evolutionUrinaryElimination} — ${this.evolutionUrinaryVolume24h.trim()} ML/24H`;
  }

  protected formattedEvolutionIntestinalElimination(): string {
    if (!this.evolutionIntestinalElimination) return 'NÃO INFORMADO';
    if (this.evolutionIntestinalElimination !== 'AUSENTES') {
      return this.evolutionIntestinalElimination;
    }
    const days = this.evolutionNoBowelMovementDays.trim();
    return days ? `AUSENTES HÁ ${days} DIA(S)` : 'AUSENTES';
  }

  protected addEvolutionExamRow(): void {
    this.evolutionExamRows = [
      ...this.evolutionExamRows,
      { name: '', values: this.evolutionExamDates.map(() => '') },
    ];
  }

  protected removeEvolutionExamRow(index: number): void {
    if (this.evolutionExamRows.length === 1) {
      this.evolutionExamRows = this.createEvolutionExamRows(1);
      return;
    }
    this.evolutionExamRows = this.evolutionExamRows.filter((_, rowIndex) => rowIndex !== index);
  }

  protected async saveEvolution(): Promise<void> {
    const admissionId = this.selectedBed()?.patient?.admissionId;
    if (!admissionId) {
      this.showToast('NÃO FOI POSSÍVEL IDENTIFICAR A INTERNAÇÃO.');
      return;
    }
    if (!this.hasEvolutionContent()) {
      this.showToast('PREENCHA AO MENOS UM CAMPO DA EVOLUÇÃO.');
      return;
    }
    if (
      [...this.evolutionSedatives, ...this.evolutionVasoactiveMedications].some(
        (medication) => medication.selected && !medication.rateMlHour.trim(),
      )
    ) {
      this.showToast('INFORME A VAZÃO EM ML/H DE CADA MEDICAMENTO SELECIONADO.');
      return;
    }
    if (!this.realApiEnabled) {
      this.showToast('EVOLUÇÃO PREPARADA. O REGISTRO DEFINITIVO REQUER A API LOCAL.');
      return;
    }

    try {
      const template = await this.publishedTemplate('EVOLUTION');
      await this.medical.createDocument(
        admissionId,
        template,
        this.evolutionDocumentValues(template),
        true,
      );
      this.showToast('EVOLUÇÃO CRIADA E FINALIZADA COM SUCESSO.');
    } catch (error) {
      this.showToast(this.clinicalErrorMessage(error));
    }
  }

  protected hasMedicationItems(group: MedicationOrderGroup): boolean {
    return group.rows.some((row) => row.description.trim());
  }

  protected hasTextRows(rows: string[]): boolean {
    return rows.some((row) => row.trim());
  }

  protected filledTextRows(rows: string[]): string[] {
    return rows.map((row) => row.trim()).filter(Boolean);
  }

  protected printOrderRows(): PrintablePrescriptionRow[] {
    const rows: PrintablePrescriptionRow[] = [];
    if (this.monitoringDraft.vitalSigns.frequency) {
      rows.push({
        section: 'MONITORIZAÇÃO',
        description: 'SINAIS VITAIS: PA, FC, FR, SPO₂ E TEMPERATURA',
        route: '—',
        frequency: this.monitoringDraft.vitalSigns.frequency,
        scheduling: '—',
      });
    }
    if (this.monitoringDraft.glucoseMonitoring.frequency) {
      rows.push({
        section: 'MONITORIZAÇÃO',
        description: 'DXT',
        route: '—',
        frequency: this.monitoringDraft.glucoseMonitoring.frequency,
        scheduling: '—',
      });
    }
    this.ventilatorySupportPreview()?.structuredVentilatorySupport.orderRows.forEach((row) =>
      rows.push({
        section: 'SUPORTE VENTILATÓRIO',
        description: row.description,
        route: row.interfaceRoute,
        frequency: row.frequency,
        scheduling: row.scheduling,
      }),
    );
    this.rehabilitationPreview()?.structuredRehabilitation.orderRows.forEach((row) =>
      rows.push({
        section: 'REABILITAÇÃO',
        description: `${row.specialty}: ${row.description}`,
        route: '—',
        frequency: row.frequency,
        scheduling: row.scheduling,
      }),
    );
    this.isolationPrecautionPreview()?.structuredIsolation.orderRows.forEach((row) =>
      rows.push({
        section: 'PRECAUÇÕES / ISOLAMENTO',
        description: row.description,
        route: '—',
        frequency: row.durationReview,
        scheduling: row.scheduling,
      }),
    );
    if (this.hydrationRow.description.trim()) {
      rows.push({
        section: 'HIDRATAÇÃO',
        description: this.hydrationRow.description.trim(),
        route: this.hydrationRow.route,
        frequency: this.hydrationRow.frequency.trim() || '—',
        scheduling: this.hydrationRow.scheduling,
      });
    }

    this.medicationGroups.forEach((group) =>
      group.rows
        .filter((row) => row.description.trim())
        .forEach((row) =>
          rows.push({
            section: group.title,
            description: row.description.trim(),
            route: row.route,
            frequency: row.frequency.trim() || '—',
            scheduling: row.scheduling,
          }),
        ),
    );

    return rows;
  }

  protected addObservationRow(): void {
    this.observationRows = [...this.observationRows, ''];
  }

  protected removeObservationRow(index: number): void {
    this.observationRows = this.removeTextRow(this.observationRows, index);
  }

  protected addAbnormalityRow(): void {
    this.abnormalityRows = [...this.abnormalityRows, ''];
  }

  protected removeAbnormalityRow(index: number): void {
    this.abnormalityRows = this.removeTextRow(this.abnormalityRows, index);
  }

  protected selectHydrationPreset(description: string): void {
    const preset = HYDRATION_PRESETS.find((item) => item.description === description);
    this.hydrationRow = preset ? { ...preset } : this.emptyHydrationRow();
  }

  protected selectMedicationPreset(group: MedicationOrderGroup, description: string): void {
    const preset = group.presets.find((item) => item.description === description);
    if (!preset) return;

    const emptyRowIndex = group.rows.findIndex((row) => !row.description.trim());
    group.rows =
      emptyRowIndex >= 0
        ? group.rows.map((row, index) => (index === emptyRowIndex ? { ...preset } : row))
        : [...group.rows, { ...preset }];
    group.selectedPreset = '';
  }

  protected addMedicationRow(group: MedicationOrderGroup): void {
    group.rows = [...group.rows, this.emptyMedicationRow(group.defaultRoute)];
  }

  protected removeMedicationRow(group: MedicationOrderGroup, index: number): void {
    if (group.rows.length === 1) {
      group.rows = [this.emptyMedicationRow(group.defaultRoute)];
      group.selectedPreset = '';
      return;
    }
    group.rows = group.rows.filter((_, rowIndex) => rowIndex !== index);
  }

  protected async savePrescription(): Promise<void> {
    const bedId = this.selectedBedId();
    if (!(await this.persistPatientChanges(false))) return;
    const dietIsValid = await this.dietComponent()?.validateAndPreview();
    if (dietIsValid === false) return;
    const nursingCareIsValid = await this.nursingCareComponent()?.validateAndPreview();
    if (nursingCareIsValid === false) return;
    const monitoringIsValid = await this.monitoringComponent()?.validateAndPreview();
    if (monitoringIsValid === false) return;
    const ventilatorySupportIsValid =
      await this.ventilatorySupportComponent()?.validateAndPreview();
    if (ventilatorySupportIsValid === false) return;
    const rehabilitationIsValid = await this.rehabilitationComponent()?.validateAndPreview();
    if (rehabilitationIsValid === false) return;
    const isolationPrecautionIsValid =
      await this.isolationPrecautionComponent()?.validateAndPreview();
    if (isolationPrecautionIsValid === false) return;
    const observations = this.observationRows.map((row) => row.trim()).filter(Boolean);
    const abnormalities = this.abnormalityRows.map((row) => row.trim()).filter(Boolean);
    const validHydrationRows = this.hydrationRow.description.trim() ? [this.hydrationRow] : [];
    const validMedicationSections: MedicationSectionDraft[] = this.medicationGroups
      .map((group) => ({
        id: group.id,
        items: group.rows.filter((row) => row.description.trim()),
      }))
      .filter((section) => section.items.length);
    if (
      !bedId ||
      (!validHydrationRows.length &&
        !validMedicationSections.length &&
        !observations.length &&
        !abnormalities.length &&
        !this.dietDraft.type &&
        !this.nursingCareComponent()?.hasSelection() &&
        !this.monitoringComponent()?.hasSelection() &&
        !this.ventilatorySupportComponent()?.hasSelection() &&
        !this.rehabilitationComponent()?.hasSelection() &&
        !this.isolationPrecautionComponent()?.hasSelection())
    ) {
      this.showToast('Inclua ao menos um item na prescrição.');
      return;
    }
    if (this.realApiEnabled) {
      const admissionId = this.selectedBed()?.patient?.admissionId;
      if (!admissionId) {
        this.showToast('NÃO FOI POSSÍVEL IDENTIFICAR A INTERNAÇÃO.');
        return;
      }
      try {
        const template = await this.publishedTemplate('PRESCRIPTION');
        await this.medical.createDocument(
          admissionId,
          template,
          this.prescriptionDocumentValues(
            validHydrationRows,
            validMedicationSections,
            observations,
            abnormalities,
          ),
          true,
        );
      } catch (error) {
        this.showToast(this.clinicalErrorMessage(error));
        return;
      }
    }

    this.store.addPrescription(
      bedId,
      this.prescriptionDiet.trim(),
      observations,
      abnormalities,
      [],
      validHydrationRows,
      validMedicationSections,
    );
    this.showToast('PRESCRIÇÃO CRIADA.');
    const patient = this.selectedBed()?.patient;
    if (patient) this.preparePrescription(patient);
  }

  protected startBlankPrescription(): void {
    this.selectedTemplateId = '';
    this.resetStructuredOrders();
    this.prescriptionReady.set(true);
  }

  protected applyPrescriptionTemplate(): void {
    const template = PRESCRIPTION_TEMPLATES.find((item) => item.id === this.selectedTemplateId);
    if (!template) {
      this.showToast('Selecione uma prescrição pré-pronta.');
      return;
    }

    this.resetStructuredOrders();
    this.prescriptionReady.set(true);
    this.showToast(`MODELO ${template.name} CARREGADO.`);
  }

  protected async savePatientChanges(): Promise<void> {
    await this.persistPatientChanges(true);
  }

  protected updateDietSelection(selection: DietPrescriptionSelection): void {
    this.dietDraft = selection.draft;
    this.dietPreview.set(selection.response);
    this.prescriptionDiet = selection.response?.structuredDiet.summaryLine ?? '';
  }

  protected updateNursingCareSelection(selection: NursingCarePrescriptionSelection): void {
    this.nursingCareDraft = selection.draft;
    this.nursingCarePreview.set(selection.response);
  }

  protected updateMonitoringSelection(selection: MonitoringPrescriptionSelection): void {
    this.monitoringDraft = selection.draft;
    this.monitoringPreview.set(selection.response);
  }

  protected updateVentilatorySupportSelection(selection: VentilatorySupportSelection): void {
    this.ventilatorySupportDraft = selection.draft;
    this.ventilatorySupportPreview.set(selection.response);
  }

  protected updateRehabilitationSelection(selection: RehabilitationSelection): void {
    this.rehabilitationDraft = selection.draft;
    this.rehabilitationPreview.set(selection.response);
  }

  protected updateIsolationPrecautionSelection(selection: IsolationPrecautionSelection): void {
    this.isolationPrecautionDraft = selection.draft;
    this.isolationPrecautionPreview.set(selection.response);
  }

  protected prescriptionItemCount(prescription: Prescription): number {
    const medicationSectionItems =
      prescription.medicationSections?.reduce(
        (total, section) => total + section.items.length,
        0,
      ) ?? 0;
    return (
      prescription.items.length +
      (prescription.hydrationItems?.length ?? 0) +
      (prescription.vitalSigns?.length ?? 0) +
      (prescription.analgesiaItems?.length ?? 0) +
      medicationSectionItems
    );
  }

  protected formatBirthDateInput(value: string): void {
    const digits = value.replace(/\D/g, '').slice(0, 8);
    const parts = [digits.slice(0, 2), digits.slice(2, 4), digits.slice(4, 8)].filter(Boolean);
    this.patientBirthDate = parts.join('/');
  }

  protected statusLabel(status: BedStatus): string {
    return {
      AVAILABLE: 'Vazio',
      OCCUPIED: 'Ocupado',
      CLEANING: 'Higienização',
      MAINTENANCE: 'Manutenção',
    }[status];
  }

  private async restoreAuthenticatedSession(): Promise<void> {
    this.authBusy.set(true);
    const valid = await this.auth.validateSession();
    this.authBusy.set(false);
    if (!valid) return;

    if (this.auth.user()?.mustChangePassword) {
      this.screen.set('password-change');
      return;
    }

    if (this.auth.isSystemAdministrator()) {
      this.screen.set('administration');
      return;
    }

    const selected = this.auth.selectedHospital();
    if (selected) {
      await this.openRoomsForHospital(selected.name);
      return;
    }
    this.screen.set('hospital-select');
  }

  private continueAfterAuthentication(): void {
    if (this.auth.user()?.mustChangePassword) {
      this.screen.set('password-change');
      return;
    }
    if (this.auth.isSystemAdministrator()) {
      this.screen.set('administration');
      return;
    }
    const selected = this.auth.selectedHospital();
    if (selected) {
      void this.openRoomsForHospital(selected.name);
      return;
    }
    this.screen.set('hospital-select');
  }

  private async openRoomsForHospital(hospitalName: string): Promise<void> {
    if (this.realApiEnabled) {
      try {
        await this.loadRealWorkspace(hospitalName);
        this.authError.set(null);
        if (!this.openRequestedView()) {
          this.screen.set('rooms');
          this.expandedRooms.set(new Set());
        }
      } catch (error) {
        this.authError.set(this.clinicalErrorMessage(error));
        this.screen.set('hospital-select');
      }
      return;
    }
    if (!this.selectDemoHospitalByName(hospitalName)) {
      this.authError.set('A UNIDADE AUTORIZADA AINDA NÃO POSSUI QUARTOS E LEITOS CADASTRADOS.');
      this.screen.set('hospital-select');
      return;
    }
    this.authError.set(null);
    this.screen.set('rooms');
    this.expandedRooms.set(new Set());
  }

  private async loadRealWorkspace(hospitalName: string): Promise<void> {
    const workspace = await this.medical.workspace();
    const selectedHospital = this.auth.selectedHospital();
    const hospital: Hospital = {
      id: workspace.hospitalId,
      name: hospitalName,
      shortName: selectedHospital?.shortName ?? hospitalName.slice(0, 3),
      city: selectedHospital?.city ?? '',
      rooms: workspace.careUnits.flatMap((careUnit) =>
        careUnit.rooms.map((room) => ({
          id: room.id,
          name: room.name,
          floor: room.floorName ?? 'SEM ANDAR INFORMADO',
          unit: careUnit.name,
          beds: room.beds.map((bed) => ({
            id: bed.id,
            code: /^LEITO\b/i.test(bed.code) ? bed.code : `LEITO ${bed.code}`,
            status: bed.status === 'BLOCKED' ? 'MAINTENANCE' : bed.status,
            patient: bed.admission
              ? {
                  id: bed.admission.patient.id,
                  admissionId: bed.admission.id,
                  name: bed.admission.patient.fullName,
                  birthDate: this.displayBirthDate(bed.admission.patient.birthDate),
                  sex: this.displaySex(bed.admission.patient.sex),
                  weightKg: bed.admission.patient.weightKg ?? undefined,
                  diagnosis: bed.admission.patient.diagnosis ?? undefined,
                  comorbidities: bed.admission.patient.comorbidities ?? undefined,
                  allergies: bed.admission.patient.allergies ?? undefined,
                  admissionAt: bed.admission.admittedAt,
                  prescriptions: [],
                }
              : undefined,
          })),
        })),
      ),
    };
    this.store.replaceHospital(hospital);
  }

  private async refreshRealWorkspace(): Promise<void> {
    const selectedHospital = this.auth.selectedHospital();
    if (!selectedHospital) throw new Error('Hospital não selecionado');
    await this.loadRealWorkspace(selectedHospital.name);
  }

  private displayBirthDate(value: string | null): string | undefined {
    if (!value) return undefined;
    const [year, month, day] = value.split('-');
    return year && month && day ? `${day}/${month}/${year}` : value;
  }

  private displaySex(value: string): string {
    return value === 'NAO_INFORMADO' ? 'NÃO INFORMADO' : value;
  }

  private selectDemoHospitalByName(hospitalName: string): boolean {
    const normalize = (value: string) =>
      value
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .trim()
        .toLocaleUpperCase('pt-BR');
    const hospital = this.store
      .hospitals()
      .find((item) => normalize(item.name) === normalize(hospitalName));
    if (!hospital) return false;
    this.store.selectHospital(hospital.id);
    return true;
  }

  private authErrorMessage(error: unknown): string {
    if (!(error instanceof HttpErrorResponse) || error.status === 0) {
      return 'NÃO FOI POSSÍVEL CONECTAR À API LOCAL. CONFIRA SE O DOCKER ESTÁ EM EXECUÇÃO.';
    }
    if (error.status === 401) return 'E-MAIL OU SENHA INVÁLIDOS.';
    if (error.status === 403 && typeof error.error?.message === 'string') {
      return error.error.message.toLocaleUpperCase('pt-BR');
    }
    if (error.status === 403) return 'ACESSO NEGADO PARA ESTA OPERAÇÃO.';
    if (error.status === 400 && typeof error.error?.message === 'string') {
      return error.error.message.toLocaleUpperCase('pt-BR');
    }
    return 'NÃO FOI POSSÍVEL CONCLUIR O ACESSO. TENTE NOVAMENTE.';
  }

  private clinicalErrorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message.toLocaleUpperCase('pt-BR');
    }
    if (error instanceof HttpErrorResponse && error.status === 0) {
      return 'NÃO FOI POSSÍVEL CONECTAR À API LOCAL.';
    }
    return 'NÃO FOI POSSÍVEL CONCLUIR A OPERAÇÃO.';
  }

  private dischargeReasonCode(reason: DischargeReason): string {
    return {
      ÓBITO: 'OBITO',
      TRANSFERÊNCIA: 'TRANSFERENCIA',
      'ALTA MELHORA': 'ALTA_MELHORA',
    }[reason];
  }

  private parseBirthDate(value: string): string | null {
    if (!value.trim()) return null;
    const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value.trim());
    return match ? `${match[3]}-${match[2]}-${match[1]}` : null;
  }

  private validatePatientForm(): boolean {
    if (!this.patientName.trim()) {
      this.showToast('PREENCHA O NOME DO PACIENTE.');
      return false;
    }
    if (this.patientWeight !== null && this.patientWeight <= 0) {
      this.showToast('INFORME UM PESO VÁLIDO OU DEIXE O CAMPO VAZIO.');
      return false;
    }
    return true;
  }

  private async publishedTemplate(kind: ClinicalFormKind): Promise<ClinicalFormTemplate> {
    const templates = await this.medical.templates(kind);
    const template = templates[0];
    if (!template) throw new Error(`Modelo ${kind} não publicado`);
    return template;
  }

  private prescriptionDocumentValues(
    hydration: PrescriptionDraftRow[],
    medicationSections: MedicationSectionDraft[],
    observations: string[],
    abnormalities: string[],
  ): Record<string, unknown> {
    const values: Record<string, unknown> = {};
    if (this.dietDraft.type) values['ORIENTACOES.DIETA'] = this.dietDraft;
    if (this.nursingCareComponent()?.hasSelection()) {
      values['CUIDADOS_ENFERMAGEM.CUIDADOS'] = this.nursingCareDraft;
    }
    if (this.monitoringComponent()?.hasSelection()) {
      values['MONITORIZACAO.CONTROLES'] = this.monitoringDraft;
    }
    if (this.ventilatorySupportComponent()?.hasSelection()) {
      values['SUPORTE_VENTILATORIO.PLANO'] = this.ventilatorySupportDraft;
    }
    if (this.rehabilitationComponent()?.hasSelection()) {
      values['REABILITACAO.PLANO'] = this.rehabilitationDraft;
    }
    if (this.isolationPrecautionComponent()?.hasSelection()) {
      values['PRECAUCOES_ISOLAMENTO.PLANO'] = this.isolationPrecautionDraft;
    }
    if (hydration.length) {
      values['MEDICAMENTOS.HIDRATACAO'] = hydration.map((row) => this.medicationDocumentRow(row));
    }

    const sectionKeys: Record<MedicationSectionId, string> = {
      ANALGESIA: 'ANALGESIA',
      SYMPTOMATICS: 'SINTOMATICOS',
      PROPHYLAXIS: 'PROFILAXIA',
      ANTIBIOTICS: 'ATB',
      CONTINUOUS_USE: 'USO_CONTINUO',
      OTHER_MEDICATIONS: 'DEMAIS_MEDICAMENTOS',
    };
    for (const section of medicationSections) {
      values[`MEDICAMENTOS.${sectionKeys[section.id]}`] = section.items.map((row) =>
        this.medicationDocumentRow(row),
      );
    }

    const notes = [
      ...observations,
      ...(abnormalities.length
        ? ['COMUNICAR ANORMALIDADES:', ...abnormalities.map((item) => `- ${item}`)]
        : []),
    ].join('\n');
    if (notes) values['OBSERVACOES.OBSERVACOES'] = notes;
    return values;
  }

  private medicationDocumentRow(row: PrescriptionDraftRow): Record<string, string> {
    return {
      description: row.description,
      route: 'route' in row ? row.route : '',
      frequency: row.frequency,
      scheduling: 'scheduling' in row ? row.scheduling : '',
    };
  }

  private roleLabel(role: string): string {
    return (
      {
        ADMIN_SISTEMA: 'ADMINISTRADOR DO SISTEMA',
        ADMIN_HOSPITAL: 'ADMINISTRADOR HOSPITALAR',
        RESPONSAVEL_CLINICO: 'RESPONSÁVEL CLÍNICO',
        MEDICO: 'MÉDICO',
        ENFERMAGEM: 'ENFERMAGEM',
        RECEPCAO: 'RECEPÇÃO',
      }[role] ?? role
    );
  }

  private showToast(message: string): void {
    this.toast.set(message);
    window.setTimeout(() => {
      if (this.toast() === message) this.toast.set(null);
    }, 3200);
  }

  private preparePrescription(patient: Patient): void {
    this.preparePatientForm(patient);
    this.prescriptionDiet = '';
    this.dietDraft = this.emptyDietDraft();
    this.dietPreview.set(null);
    this.nursingCareDraft = this.emptyNursingCareDraft();
    this.nursingCarePreview.set(null);
    this.monitoringDraft = this.emptyMonitoringDraft();
    this.monitoringPreview.set(null);
    this.ventilatorySupportDraft = this.emptyVentilatorySupportDraft();
    this.ventilatorySupportPreview.set(null);
    this.rehabilitationDraft = this.emptyRehabilitationDraft();
    this.rehabilitationPreview.set(null);
    this.isolationPrecautionDraft = this.emptyIsolationPrecautionDraft();
    this.isolationPrecautionPreview.set(null);
    this.observationRows = [];
    this.abnormalityRows = [];
    this.hydrationRow = this.emptyHydrationRow();
    this.selectedHydrationPreset = '';
    this.medicationGroups = [];
    this.selectedTemplateId = '';
    this.prescriptionReady.set(false);
  }

  private emptyDietDraft(): DietPrescriptionDraft {
    return {
      type: null,
      oral: { consistency: '', restrictions: [] },
      enteral: {
        accessRoute: '',
        infusionRegimen: '',
        formulaType: '',
        rateMlHour: null,
        bolusVolumeMl: null,
        bolusFrequency: '',
        tubeFlushMl: null,
        flushInterval: '',
      },
      parenteral: {
        accessRoute: '',
        preparationType: '',
        totalVolumeMl: null,
        rateMlHour: null,
        totalCaloriesKcalDay: null,
        proteinGoalGramsKgDay: null,
        gastrointestinalFailureJustification: '',
      },
      fasting: { reason: '', reassessment: '' },
    };
  }

  private emptyNursingCareDraft(): NursingCarePrescriptionDraft {
    return {
      positioning: { headPosition: '', repositioningFrequency: '', pressureProtection: [] },
      hygieneSkin: { bath: '', oralHygiene: '', skinCare: [] },
      dressingsDrains: {
        catheterDressing: '',
        acuteWoundCare: '',
        complexWoundCoverage: '',
        dressingChangeFrequency: '',
        drainCare: [],
      },
      procedures: { airwaySuction: '', deviceCare: [], fluidBalance: '' },
    };
  }

  private emptyMonitoringDraft(): MonitoringPrescriptionDraft {
    return {
      vitalSigns: {
        frequency: '',
        painScale: '',
        consciousnessSedationScale: '',
        fallRiskScale: '',
      },
      glucoseMonitoring: {
        frequency: '',
        hypoglycemiaProtocol: false,
        slidingScale: false,
        insulinType: '',
      },
      fluidBalanceOutputs: {
        fluidBalance: '',
        urineOutput: '',
        drainsTubes: [],
        otherMeasurements: [],
      },
      invasiveMonitoring: { hemodynamic: [], neurological: [] },
    };
  }

  private emptyVentilatorySupportDraft(): VentilatorySupportDraft {
    return { selectedTemplate: '', items: [] };
  }

  private emptyRehabilitationDraft(): RehabilitationDraft {
    return { selectedTemplate: '', items: [] };
  }

  private emptyIsolationPrecautionDraft(): IsolationPrecautionDraft {
    return { selectedTemplate: '', items: [] };
  }

  private prepareEvolution(patient: Patient): void {
    this.preparePatientForm(patient);
    this.evolutionDiet = patient.prescriptions[0]?.diet ?? '';
    this.evolutionAdmission = '';
    this.evolutionText = '';
    this.evolutionPosition = '';
    this.evolutionAccompaniment = '';
    this.evolutionHygiene = '';
    this.evolutionConsciousness = '';
    this.evolutionOrientation = '';
    this.evolutionInteraction = '';
    this.evolutionSedationStatus = '';
    this.evolutionSedatives = this.createSedativeMedications();
    this.evolutionRespiratoryTab = 'PATTERN';
    this.evolutionRespiratoryPatterns = [];
    this.evolutionVentilatorySupport = '';
    this.evolutionHemodynamicStability = '';
    this.evolutionPressureProfile = '';
    this.evolutionPamWithinTarget = false;
    this.evolutionNoVasoactiveDrugs = false;
    this.evolutionVasoactiveMedications = this.createVasoactiveMedications();
    this.evolutionFoodAcceptance = '';
    this.evolutionUrinaryElimination = '';
    this.evolutionUrinaryVolume24h = '';
    this.evolutionUrineAppearance = '';
    this.evolutionIntestinalElimination = '';
    this.evolutionNoBowelMovementDays = '';
    this.evolutionStoolAppearance = '';
    this.evolutionSleepPattern = '';
    this.evolutionVitalSaturation = '';
    this.evolutionHeartRate = '';
    this.evolutionRespiratoryRate = '';
    this.evolutionBloodPressureSystolic = '';
    this.evolutionBloodPressureDiastolic = '';
    this.evolutionTemperature = '';
    this.evolutionGeneralState = '';
    this.evolutionCyanosis = '';
    this.evolutionJaundice = '';
    this.evolutionFever = '';
    this.evolutionColoring = '';
    this.evolutionHydration = '';
    this.evolutionNeurological = '';
    this.evolutionRespiratoryExam = '';
    this.evolutionCardiovascularExam = '';
    this.evolutionAbdomen = '';
    this.evolutionLowerLimbs = '';
    this.evolutionUpperLimbPerfusion = '';
    this.evolutionLowerLimbPerfusion = '';
    this.evolutionComplementaryNotes = '';
    this.evolutionConduct = '';
    this.evolutionAntibioticCurrent = '';
    this.evolutionAntibioticPrevious = '';
    this.evolutionExamDates = ['', '', '', ''];
    this.evolutionExamRows = this.createEvolutionExamRows();
  }

  private createSedativeMedications(): InfusionMedication[] {
    return [
      { id: 'PROPOFOL', label: 'PROPOFOL (10 MG/ML)', selected: false, rateMlHour: '' },
      {
        id: 'MIDAZOLAM',
        label: 'MIDAZOLAM (DORMONID)',
        selected: false,
        rateMlHour: '',
      },
      { id: 'FENTANIL', label: 'FENTANIL', selected: false, rateMlHour: '' },
      {
        id: 'DEXMEDETOMIDINA',
        label: 'DEXMEDETOMIDINA (PRECEDEX)',
        selected: false,
        rateMlHour: '',
      },
      { id: 'KETAMINA', label: 'KETAMINA (CETAMINA)', selected: false, rateMlHour: '' },
    ];
  }

  private createVasoactiveMedications(): InfusionMedication[] {
    return [
      { id: 'NORADRENALINA', label: 'NORADRENALINA', selected: false, rateMlHour: '' },
      { id: 'VASOPRESSINA', label: 'VASOPRESSINA', selected: false, rateMlHour: '' },
      { id: 'DOBUTAMINA', label: 'DOBUTAMINA', selected: false, rateMlHour: '' },
    ];
  }

  private createEvolutionExamRows(count = 8): EvolutionExamRow[] {
    return Array.from({ length: count }, () => ({
      name: '',
      values: ['', '', '', ''],
    }));
  }

  private hasEvolutionContent(): boolean {
    const textValues = [
      this.evolutionAdmission,
      this.evolutionText,
      this.evolutionPosition,
      this.evolutionAccompaniment,
      this.evolutionHygiene,
      this.evolutionConsciousness,
      this.evolutionOrientation,
      this.evolutionInteraction,
      this.evolutionSedationStatus,
      this.evolutionFoodAcceptance,
      this.evolutionUrinaryElimination,
      this.evolutionUrinaryVolume24h,
      this.evolutionUrineAppearance,
      this.evolutionIntestinalElimination,
      this.evolutionNoBowelMovementDays,
      this.evolutionStoolAppearance,
      this.evolutionSleepPattern,
      this.evolutionVentilatorySupport,
      this.evolutionHemodynamicStability,
      this.evolutionPressureProfile,
      this.evolutionNeurological,
      this.evolutionRespiratoryExam,
      this.evolutionCardiovascularExam,
      this.evolutionAbdomen,
      this.evolutionLowerLimbs,
      this.evolutionComplementaryNotes,
      this.evolutionConduct,
      this.evolutionAntibioticCurrent,
      this.evolutionAntibioticPrevious,
    ];
    return (
      textValues.some((value) => value.trim()) ||
      this.evolutionRespiratoryPatterns.length > 0 ||
      this.evolutionPamWithinTarget ||
      this.evolutionNoVasoactiveDrugs ||
      this.evolutionSedatives.some((item) => item.selected) ||
      this.evolutionVasoactiveMedications.some((item) => item.selected) ||
      this.evolutionExamRows.some(
        (row) => row.name.trim() || row.values.some((value) => value.trim()),
      )
    );
  }

  private evolutionDocumentValues(template: ClinicalFormTemplate): Record<string, unknown> {
    const values: Record<string, unknown> = {};
    const put = (path: string, value: unknown): void => {
      if (this.templateField(template, path) && this.documentValuePresent(value))
        values[path] = value;
    };
    const putSelection = (path: string, label: string): void => {
      if (!label) return;
      const option = this.templateOptionValue(template, path, label);
      if (option) put(path, option);
    };
    const putSelections = (path: string, labels: string[]): void => {
      const options = labels
        .map((label) => this.templateOptionValue(template, path, label))
        .filter((value): value is string => Boolean(value));
      put(path, options);
    };

    putSelection('ACOMPANHAMENTO.POSICAO', this.evolutionPosition);
    putSelection('ACOMPANHAMENTO.ACOMPANHAMENTO', this.evolutionAccompaniment);
    putSelection('ACOMPANHAMENTO.HIGIENE', this.evolutionHygiene);
    put('FISIOLOGICO.ADMISSAO', this.evolutionAdmission.trim());
    putSelection('FISIOLOGICO.ACEITACAO_ALIMENTAR', this.evolutionFoodAcceptance);
    putSelection('FISIOLOGICO.ELIMINACAO_URINARIA', this.evolutionUrinaryElimination);
    put('FISIOLOGICO.VOLUME_URINARIO', this.optionalNumber(this.evolutionUrinaryVolume24h));
    putSelection('FISIOLOGICO.ASPECTO_URINA', this.evolutionUrineAppearance);
    putSelection('FISIOLOGICO.ELIMINACAO_INTESTINAL', this.evolutionIntestinalElimination);
    put('FISIOLOGICO.DIAS_SEM_EVACUAR', this.optionalNumber(this.evolutionNoBowelMovementDays));
    putSelection('FISIOLOGICO.ASPECTO_FEZES', this.evolutionStoolAppearance);
    putSelection('FISIOLOGICO.PADRAO_SONO', this.evolutionSleepPattern);
    put('FISIOLOGICO.ATB_ATUAL', this.evolutionAntibioticCurrent.trim());
    put('FISIOLOGICO.ATB_PREVIA', this.evolutionAntibioticPrevious.trim());
    putSelection('NEUROLOGICO.CONSCIENCIA', this.evolutionConsciousness);
    putSelection('NEUROLOGICO.ORIENTACAO', this.evolutionOrientation);
    putSelection('NEUROLOGICO.INTERACAO', this.evolutionInteraction);
    put('NEUROLOGICO.OBSERVACOES', this.evolutionNeurological.trim());
    put('SEDACAO.MEDICAMENTOS', this.infusionDocumentRows(this.evolutionSedatives));
    putSelection('SEDACAO.DINAMICA', this.evolutionSedationStatus);
    putSelections('RESPIRATORIO.PADRAO', this.evolutionRespiratoryPatterns);
    putSelection('RESPIRATORIO.SUPORTE', this.evolutionVentilatorySupport);
    put('RESPIRATORIO.OBSERVACOES', this.evolutionRespiratoryExam.trim());
    putSelection('CARDIOVASCULAR.ESTABILIDADE', this.evolutionHemodynamicStability);
    putSelection('CARDIOVASCULAR.PERFIL_PRESSORICO', this.evolutionPressureProfile);
    if (this.evolutionPamWithinTarget) put('CARDIOVASCULAR.PAM_ALVO', true);
    put(
      'CARDIOVASCULAR.DROGAS_VASOATIVAS',
      this.evolutionNoVasoactiveDrugs
        ? [this.medicationDocumentRowFromValues('SEM USO DE DROGAS VASOATIVAS', '')]
        : this.infusionDocumentRows(this.evolutionVasoactiveMedications),
    );
    put('CARDIOVASCULAR.OBSERVACOES', this.evolutionCardiovascularExam.trim());
    put('FISIOLOGICO.EVOLUCAO', this.evolutionText.trim());
    put('EXAME_FISICO.SSVV', this.evolutionVitalSignsText());
    put('EXAME_FISICO.ECTOSCOPIA', this.evolutionEctoscopyText());
    put('EXAME_FISICO.ABDOME', this.evolutionAbdomen.trim());
    put('EXAME_FISICO.MMII', this.evolutionLowerLimbs.trim());
    put('EXAME_FISICO.PERFUSAO', this.evolutionPerfusionText());
    put('EXAMES.TABELA_EXAMES', this.evolutionExamDocumentRows());
    put('EXAMES.OUTROS_EXAMES', this.evolutionComplementaryNotes.trim());
    put('CONDUTA.CONDUTA', this.formatEvolutionConduct(this.evolutionConduct));
    return values;
  }

  private templateField(template: ClinicalFormTemplate, path: string) {
    const [sectionKey, fieldKey] = path.split('.');
    return template.sections
      .find((section) => section.key === sectionKey)
      ?.fields.find((field) => field.key === fieldKey);
  }

  private templateOptionValue(
    template: ClinicalFormTemplate,
    path: string,
    label: string,
  ): string | undefined {
    return this.templateField(template, path)?.options.find((option) => option.label === label)
      ?.value;
  }

  private documentValuePresent(value: unknown): boolean {
    if (value === null || value === undefined || value === '') return false;
    if (Array.isArray(value)) return value.length > 0;
    return true;
  }

  private optionalNumber(value: string): number | null {
    const normalized = value.trim().replace(',', '.');
    if (!normalized) return null;
    const parsed = Number(normalized);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private infusionDocumentRows(medications: InfusionMedication[]): Record<string, string>[] {
    return medications
      .filter((medication) => medication.selected)
      .map((medication) =>
        this.medicationDocumentRowFromValues(medication.label, medication.rateMlHour.trim()),
      );
  }

  private medicationDocumentRowFromValues(
    description: string,
    rateMlHour: string,
  ): Record<string, string> {
    return {
      description,
      route: 'EV',
      frequency: rateMlHour ? `${rateMlHour} ML/H` : '',
      scheduling: 'CONTÍNUO',
    };
  }

  private evolutionVitalSignsText(): string {
    const values = [
      this.evolutionVitalSaturation && `SATO₂ ${this.evolutionVitalSaturation}%`,
      this.evolutionHeartRate && `FC ${this.evolutionHeartRate} BPM`,
      this.evolutionRespiratoryRate && `FR ${this.evolutionRespiratoryRate} IRPM`,
      (this.evolutionBloodPressureSystolic || this.evolutionBloodPressureDiastolic) &&
        `PA ${this.evolutionBloodPressureSystolic || '___'} × ${this.evolutionBloodPressureDiastolic || '___'} MMHG`,
      this.evolutionTemperature && `TAX ${this.evolutionTemperature} °C`,
    ].filter(Boolean);
    return values.join('; ');
  }

  private evolutionEctoscopyText(): string {
    return [
      this.evolutionGeneralState,
      this.evolutionCyanosis,
      this.evolutionJaundice,
      this.evolutionFever,
      this.evolutionColoring,
      this.evolutionHydration,
    ]
      .filter(Boolean)
      .join('; ');
  }

  private evolutionPerfusionText(): string {
    const values = [
      this.evolutionUpperLimbPerfusion && `MMSS: ${this.evolutionUpperLimbPerfusion}`,
      this.evolutionLowerLimbPerfusion && `MMII: ${this.evolutionLowerLimbPerfusion}`,
    ].filter(Boolean);
    return values.join('; ');
  }

  private evolutionExamDocumentRows(): Record<string, string>[] {
    return this.evolutionExamRows
      .filter((row) => row.name.trim() || row.values.some((value) => value.trim()))
      .map((row) => ({
        exam: row.name.trim(),
        date1: this.evolutionExamDates[0] || '',
        result1: row.values[0] || '',
        date2: this.evolutionExamDates[1] || '',
        result2: row.values[1] || '',
        date3: this.evolutionExamDates[2] || '',
        result3: row.values[2] || '',
        date4: this.evolutionExamDates[3] || '',
        result4: row.values[3] || '',
      }));
  }

  private emptyHydrationRow(): PrescriptionDraftRow {
    return { description: '', route: 'EV', frequency: '', scheduling: 'FIXO' };
  }

  private emptyMedicationRow(route: string): PrescriptionDraftRow {
    return { description: '', route, frequency: '', scheduling: 'FIXO' };
  }

  private removeTextRow(rows: string[], index: number): string[] {
    if (rows.length === 1) return [''];
    return rows.filter((_, rowIndex) => rowIndex !== index);
  }

  private resetStructuredOrders(): void {
    this.monitoringDraft = this.emptyMonitoringDraft();
    this.monitoringPreview.set(null);
    this.monitoringComponent()?.reset();
    this.ventilatorySupportDraft = this.emptyVentilatorySupportDraft();
    this.ventilatorySupportPreview.set(null);
    this.ventilatorySupportComponent()?.reset();
    this.rehabilitationDraft = this.emptyRehabilitationDraft();
    this.rehabilitationPreview.set(null);
    this.rehabilitationComponent()?.reset();
    this.isolationPrecautionDraft = this.emptyIsolationPrecautionDraft();
    this.isolationPrecautionPreview.set(null);
    this.isolationPrecautionComponent()?.reset();
    this.hydrationRow = this.emptyHydrationRow();
    this.selectedHydrationPreset = '';
    this.medicationGroups = this.buildMedicationGroups();
    this.observationRows = [''];
    this.abnormalityRows = [''];
  }

  private buildMedicationGroups(): MedicationOrderGroup[] {
    return [
      {
        id: 'ANALGESIA',
        eyebrow: 'CONTROLE DA DOR',
        title: 'ANALGESIA',
        helper: 'PREENCHA VIA, FREQUÊNCIA E APRAZAMENTO',
        defaultRoute: 'EV',
        presets: ANALGESIA_PRESETS,
        selectedPreset: '',
        rows: [this.emptyMedicationRow('EV')],
      },
      {
        id: 'SYMPTOMATICS',
        eyebrow: 'CONTROLE DE SINTOMAS',
        title: 'SINTOMÁTICOS',
        helper: 'SELECIONE UM MODELO OU PREENCHA LIVREMENTE',
        defaultRoute: 'EV',
        presets: SYMPTOMATIC_PRESETS,
        selectedPreset: '',
        rows: [this.emptyMedicationRow('EV')],
      },
      {
        id: 'PROPHYLAXIS',
        eyebrow: 'PREVENÇÃO',
        title: 'PROFILAXIA',
        helper: 'OMEPRAZOL INICIAL PODE SER EDITADO OU REMOVIDO',
        defaultRoute: 'EV',
        presets: PROPHYLAXIS_PRESETS,
        selectedPreset: '',
        rows: [{ ...PROPHYLAXIS_PRESETS[0] }],
      },
      {
        id: 'ANTIBIOTICS',
        eyebrow: 'ANTIMICROBIANOS',
        title: 'ATB',
        helper: 'TÓPICO OPCIONAL — ADICIONE SOMENTE QUANDO NECESSÁRIO',
        defaultRoute: 'EV',
        presets: ANTIBIOTIC_PRESETS,
        selectedPreset: '',
        rows: [this.emptyMedicationRow('EV')],
      },
      {
        id: 'CONTINUOUS_USE',
        eyebrow: 'TRATAMENTO HABITUAL',
        title: 'MEDICAÇÕES DE USO CONTÍNUO',
        helper: 'PREENCHIMENTO LIVRE',
        defaultRoute: 'VO',
        presets: [],
        selectedPreset: '',
        rows: [this.emptyMedicationRow('VO')],
      },
      {
        id: 'OTHER_MEDICATIONS',
        eyebrow: 'ITENS ADICIONAIS',
        title: 'DEMAIS MEDICAMENTOS',
        helper: 'PREENCHIMENTO LIVRE',
        defaultRoute: 'VO',
        presets: [],
        selectedPreset: '',
        rows: [this.emptyMedicationRow('VO')],
      },
    ];
  }

  private openPrescriptionTab(bedId: string): void {
    const hospitalId = this.store.activeHospitalId();
    if (!hospitalId) return;

    const url = new URL(window.location.href);
    url.search = '';
    url.hash = '';
    url.searchParams.set('hospital', hospitalId);
    url.searchParams.set('bed', bedId);
    url.searchParams.set('flow', 'prescription');

    window.open(url.toString(), '_blank', 'noopener');
  }

  private openEvolutionTab(bedId: string): void {
    const hospitalId = this.store.activeHospitalId();
    if (!hospitalId) return;

    const url = new URL(window.location.href);
    url.search = '';
    url.hash = '';
    url.searchParams.set('hospital', hospitalId);
    url.searchParams.set('bed', bedId);
    url.searchParams.set('flow', 'evolution');

    window.open(url.toString(), '_blank', 'noopener');
  }

  private openRequestedView(): boolean {
    const params = new URLSearchParams(window.location.search);
    const hospitalId = params.get('hospital');
    const bedId = params.get('bed');
    const flow = params.get('flow');
    if (!hospitalId || !bedId || !flow) return false;

    this.store.selectHospital(hospitalId);
    const bed = this.store.findBed(bedId);
    const room = this.activeHospital()?.rooms.find((item) =>
      item.beds.some((roomBed) => roomBed.id === bedId),
    );
    if (!bed || !room) return false;

    this.selectedBedId.set(bedId);
    this.expandedRooms.set(new Set([room.id]));

    if (flow === 'prescription' && bed.status === 'OCCUPIED' && bed.patient) {
      this.preparePrescription(bed.patient);
      this.screen.set('prescription');
      return true;
    }
    if (flow === 'evolution' && bed.status === 'OCCUPIED' && bed.patient) {
      this.prepareEvolution(bed.patient);
      this.screen.set('evolution');
      return true;
    }
    return false;
  }

  private resetPatientForm(): void {
    this.patientName = '';
    this.patientBirthDate = '';
    this.patientSex = 'FEMININO';
    this.patientWeight = null;
    this.prescriptionDiagnosis = '';
    this.prescriptionComorbidities = '';
    this.prescriptionAllergies = '';
  }

  private preparePatientForm(patient: Patient): void {
    this.patientName = patient.name;
    this.patientBirthDate = patient.birthDate || '';
    this.patientSex = patient.sex || 'NÃO INFORMADO';
    this.patientWeight = patient.weightKg ?? null;
    this.prescriptionDiagnosis = patient.diagnosis || '';
    this.prescriptionComorbidities = patient.comorbidities || '';
    this.prescriptionAllergies = patient.allergies || '';
  }

  private async persistPatientChanges(showFeedback: boolean): Promise<boolean> {
    const bed = this.selectedBed();
    if (!bed?.patient || !this.validatePatientForm()) return false;

    const birthDate = this.parseBirthDate(this.patientBirthDate);
    if (this.patientBirthDate.trim() && !birthDate) {
      this.showToast('INFORME A DATA DE NASCIMENTO NO FORMATO DD/MM/AAAA.');
      return false;
    }

    const patientData = {
      name: this.patientName.trim(),
      birthDate: this.patientBirthDate.trim() || undefined,
      sex: this.patientSex,
      weightKg: this.patientWeight ?? undefined,
      diagnosis: this.prescriptionDiagnosis.trim() || undefined,
      comorbidities: this.prescriptionComorbidities.trim() || undefined,
      allergies: this.prescriptionAllergies.trim() || undefined,
    };

    try {
      if (this.realApiEnabled) {
        if (!bed.patient.admissionId) {
          this.showToast('NÃO FOI POSSÍVEL IDENTIFICAR A INTERNAÇÃO.');
          return false;
        }
        await this.medical.updatePatient(bed.patient.admissionId, {
          fullName: patientData.name,
          birthDate,
          sex: patientData.sex === 'NÃO INFORMADO' ? 'NAO_INFORMADO' : patientData.sex,
          weightKg: this.patientWeight,
          diagnosis: patientData.diagnosis ?? null,
          comorbidities: patientData.comorbidities ?? null,
          allergies: patientData.allergies ?? null,
        });
        await this.refreshRealWorkspace();
      } else {
        this.store.updatePatient(bed.id, patientData);
      }
      if (showFeedback) this.showToast('DADOS DO PACIENTE ATUALIZADOS E REGISTRADOS NA AUDITORIA.');
      return true;
    } catch (error) {
      this.showToast(this.clinicalErrorMessage(error));
      return false;
    }
  }
}
