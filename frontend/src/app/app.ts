import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { environment } from '../environments/environment';
import { AuthService } from './auth/auth.service';
import { DemoStore } from './demo-store';
import {
  AppScreen,
  Bed,
  BedStatus,
  DischargeReason,
  MedicationSectionDraft,
  MedicationSectionId,
  Patient,
  Prescription,
  PrescriptionDraftRow,
  PrescriptionScheduling,
  Room,
  VitalSignDraftRow,
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

const DIET_PRESETS = [
  'DIETA LIVRE',
  'DIETA LÍQUIDA',
  'DIETA PASTOSA',
  'DIETA SNE',
  'DIETA PARA HAS',
  'DIETA PARA DM',
];

const DXT_GUIDANCE = {
  low: 'GH 50% 40ML EV SE DXT < 70 MG/DL',
  highTitle: 'INSULINA REGULAR SC CONFORME DXT',
  ranges: [
    { glucose: '180–230', insulin: '2 UI' },
    { glucose: '231–280', insulin: '4 UI' },
    { glucose: '281–330', insulin: '6 UI' },
    { glucose: '331–380', insulin: '8 UI' },
  ],
  fullText:
    'GH 50% 40ML EV SE DXT < 70 MG/DL. INSULINA REGULAR SC CONFORME DXT: 180–230: 2 UI; 231–280: 4 UI; 281–330: 6 UI; 331–380: 8 UI.',
};

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
  imports: [CommonModule, FormsModule],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit {
  protected readonly store = inject(DemoStore);
  protected readonly auth = inject(AuthService);
  protected readonly realApiEnabled = environment.useRealApi;
  protected readonly productionBuild = environment.production;
  protected readonly screen = signal<AppScreen>('login');
  protected readonly expandedRooms = signal<Set<string>>(new Set());
  protected readonly selectedBedId = signal<string | null>(null);
  protected readonly admissionOpen = signal(false);
  protected readonly dischargeOpen = signal(false);
  protected readonly transferOpen = signal(false);
  protected readonly prescriptionReady = signal(false);
  protected readonly searchTerm = signal('');
  protected readonly toast = signal<string | null>(null);
  protected readonly authBusy = signal(false);
  protected readonly authError = signal<string | null>(null);

  protected loginEmail = environment.useRealApi
    ? 'lucas.galante@inventorymed.local'
    : 'lucas.galante@demo.com';
  protected loginPassword = environment.useRealApi ? '' : 'demo123';
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
  protected evolutionDiet = '';
  protected evolutionAdmission = '';
  protected evolutionText = '';
  protected evolutionPosition = '';
  protected evolutionAccompaniment = '';
  protected evolutionConsciousness = '';
  protected evolutionOrientation = '';
  protected evolutionChiefComplaints: string[] = [];
  protected evolutionPainLocation = '';
  protected evolutionPainIntensity = '';
  protected evolutionShiftEvents: string[] = [];
  protected evolutionFoodAcceptance = '';
  protected evolutionUrinaryElimination = '';
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
  protected vitalSignRows: VitalSignDraftRow[] = [];
  protected hydrationRow: PrescriptionDraftRow = this.emptyHydrationRow();
  protected selectedHydrationPreset = '';
  protected medicationGroups: MedicationOrderGroup[] = [];
  protected selectedTemplateId: PrescriptionTemplateId | '' = '';
  protected readonly prescriptionTemplates = PRESCRIPTION_TEMPLATES;
  protected readonly dietPresets = DIET_PRESETS;
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
  protected readonly evolutionChiefComplaintOptions = [
    'NEGA NOVAS QUEIXAS ATIVAS',
    'DOR',
    'FALTA DE AR / DISPNEIA',
    'NÁUSEAS / ENJOO',
    'TONTURA / MAL-ESTAR',
  ];
  protected readonly evolutionShiftEventOptions = [
    'SEM INTERCORRÊNCIAS RELATADAS',
    'PICO FEBRIL',
    'EPISÓDIO DE HIPOTENSÃO',
    'AGITAÇÃO PSICOMOTORA',
    'QUEDA DA SATURAÇÃO / NECESSIDADE DE O₂',
    'EPISÓDIO DE VÔMITO / DIARREIA',
  ];
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
  protected readonly dxtGuidance = DXT_GUIDANCE;
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
    if (this.openRequestedView()) return;
    if (this.realApiEnabled) void this.restoreAuthenticatedSession();
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
      const response = await this.auth.login(this.loginEmail, this.loginPassword);
      if (response.selectedHospitalId) {
        const selected = response.hospitals.find(
          (hospital) => hospital.id === response.selectedHospitalId,
        );
        if (selected) {
          this.openRoomsForHospital(selected.name);
          return;
        }
      }
      this.screen.set('hospital-select');
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
      this.openRoomsForHospital(response.hospital.name);
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
        this.selectDemoHospitalByName(response.hospital.name);
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
    this.dischargeOpen.set(false);
    this.transferOpen.set(false);
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

  protected confirmDischarge(): void {
    const bedId = this.selectedBedId();
    if (!bedId || !this.dischargeReason) {
      this.showToast('SELECIONE O MOTIVO DA ALTA.');
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

  protected confirmTransfer(): void {
    const sourceBedId = this.selectedBedId();
    if (!sourceBedId || !this.transferTargetBedId) {
      this.showToast('SELECIONE O LEITO DE DESTINO.');
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

  protected admitPatient(): void {
    const bedId = this.selectedBedId();
    if (!bedId || !this.patientName.trim()) {
      this.showToast('Preencha o nome do paciente.');
      return;
    }
    if (this.patientWeight !== null && this.patientWeight <= 0) {
      this.showToast('Informe um peso válido ou deixe o campo vazio.');
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

  protected formattedEvolutionChiefComplaints(): string {
    if (!this.evolutionChiefComplaints.length) return 'NÃO INFORMADO';
    return this.evolutionChiefComplaints
      .map((complaint) => {
        if (complaint !== 'DOR') return complaint;
        const location = this.evolutionPainLocation.trim() || 'LOCAL NÃO INFORMADO';
        const intensity = this.evolutionPainIntensity.trim();
        return `DOR EM ${location}${intensity ? ` (INTENSIDADE ${intensity}/10)` : ''}`;
      })
      .join('; ');
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
    this.vitalSignRows
      .filter((row) => row.description.trim())
      .forEach((row) =>
        rows.push({
          section: 'DADOS VITAIS',
          description: row.description.trim(),
          route: '—',
          frequency: row.frequency.trim() || '—',
          scheduling: '—',
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

  protected savePrescription(): void {
    const bedId = this.selectedBedId();
    if (!this.persistPatientChanges(false)) return;
    const observations = this.observationRows.map((row) => row.trim()).filter(Boolean);
    const abnormalities = this.abnormalityRows.map((row) => row.trim()).filter(Boolean);
    const validVitalSigns = this.vitalSignRows.filter(
      (row) => row.description.trim() && row.frequency.trim(),
    );
    const validHydrationRows = this.hydrationRow.description.trim() ? [this.hydrationRow] : [];
    const validMedicationSections: MedicationSectionDraft[] = this.medicationGroups
      .map((group) => ({
        id: group.id,
        items: group.rows.filter((row) => row.description.trim()),
      }))
      .filter((section) => section.items.length);
    if (
      !bedId ||
      (!validVitalSigns.length &&
        !validHydrationRows.length &&
        !validMedicationSections.length &&
        !observations.length &&
        !abnormalities.length &&
        !this.prescriptionDiet.trim())
    ) {
      this.showToast('Inclua ao menos um item na prescrição.');
      return;
    }
    this.store.addPrescription(
      bedId,
      this.prescriptionDiet.trim(),
      observations,
      abnormalities,
      validVitalSigns,
      validHydrationRows,
      validMedicationSections,
    );
    this.showToast('Prescrição criada no protótipo.');
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

  protected savePatientChanges(): void {
    this.persistPatientChanges(true);
  }

  protected selectDiet(diet: string): void {
    this.prescriptionDiet = diet;
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

  protected resetDemo(): void {
    this.store.resetDemo();
    this.showToast('Dados de demonstração restaurados.');
  }

  private async restoreAuthenticatedSession(): Promise<void> {
    this.authBusy.set(true);
    const valid = await this.auth.validateSession();
    this.authBusy.set(false);
    if (!valid) return;

    const selected = this.auth.selectedHospital();
    if (selected) {
      this.openRoomsForHospital(selected.name);
      return;
    }
    this.screen.set('hospital-select');
  }

  private openRoomsForHospital(hospitalName: string): void {
    if (!this.selectDemoHospitalByName(hospitalName)) {
      this.authError.set(
        'A UNIDADE AUTORIZADA AINDA NÃO POSSUI UMA ESTRUTURA DEMONSTRATIVA DE LEITOS.',
      );
      this.screen.set('hospital-select');
      return;
    }
    this.authError.set(null);
    this.screen.set('rooms');
    this.expandedRooms.set(new Set());
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
    if (error.status === 403) return 'USUÁRIO SEM ACESSO ATIVO A ESTA UNIDADE.';
    return 'NÃO FOI POSSÍVEL CONCLUIR O ACESSO. TENTE NOVAMENTE.';
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
    this.observationRows = [];
    this.abnormalityRows = [];
    this.vitalSignRows = [];
    this.hydrationRow = this.emptyHydrationRow();
    this.selectedHydrationPreset = '';
    this.medicationGroups = [];
    this.selectedTemplateId = '';
    this.prescriptionReady.set(false);
  }

  private prepareEvolution(patient: Patient): void {
    this.preparePatientForm(patient);
    this.evolutionDiet = patient.prescriptions[0]?.diet ?? '';
    this.evolutionAdmission = '';
    this.evolutionText = '';
    this.evolutionPosition = '';
    this.evolutionAccompaniment = '';
    this.evolutionConsciousness = '';
    this.evolutionOrientation = '';
    this.evolutionChiefComplaints = [];
    this.evolutionPainLocation = '';
    this.evolutionPainIntensity = '';
    this.evolutionShiftEvents = [];
    this.evolutionFoodAcceptance = '';
    this.evolutionUrinaryElimination = '';
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

  private createEvolutionExamRows(count = 8): EvolutionExamRow[] {
    return Array.from({ length: count }, () => ({
      name: '',
      values: ['', '', '', ''],
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
    this.vitalSignRows = [
      { description: 'SINAIS VITAIS', frequency: '' },
      { description: 'DXT', frequency: '', guidance: DXT_GUIDANCE.fullText },
    ];
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

  private persistPatientChanges(showFeedback: boolean): boolean {
    const bedId = this.selectedBedId();
    if (!bedId || !this.patientName.trim()) {
      this.showToast('Preencha o nome do paciente.');
      return false;
    }
    if (this.patientWeight !== null && this.patientWeight <= 0) {
      this.showToast('Informe um peso válido ou deixe o campo vazio.');
      return false;
    }

    this.store.updatePatient(bedId, {
      name: this.patientName.trim(),
      birthDate: this.patientBirthDate.trim() || undefined,
      sex: this.patientSex,
      weightKg: this.patientWeight ?? undefined,
      diagnosis: this.prescriptionDiagnosis.trim() || undefined,
      comorbidities: this.prescriptionComorbidities.trim() || undefined,
      allergies: this.prescriptionAllergies.trim() || undefined,
    });
    if (showFeedback) this.showToast('Dados do paciente atualizados.');
    return true;
  }
}
