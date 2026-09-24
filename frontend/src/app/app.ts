import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DemoStore } from './demo-store';
import {
  AppScreen,
  Bed,
  BedStatus,
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
  protected readonly screen = signal<AppScreen>('login');
  protected readonly expandedRooms = signal<Set<string>>(new Set());
  protected readonly selectedBedId = signal<string | null>(null);
  protected readonly prescriptionPromptOpen = signal(false);
  protected readonly admissionOpen = signal(false);
  protected readonly prescriptionReady = signal(false);
  protected readonly searchTerm = signal('');
  protected readonly toast = signal<string | null>(null);

  protected loginEmail = 'lucas.galante@demo.com';
  protected loginPassword = 'demo123';
  protected patientName = '';
  protected patientBirthDate = '';
  protected patientSex = 'FEMININO';
  protected patientWeight: number | null = null;
  protected prescriptionDiagnosis = '';
  protected prescriptionComorbidities = '';
  protected prescriptionAllergies = '';
  protected prescriptionDiet = '';
  protected prescriptionNotes = '';
  protected prescriptionRows: PrescriptionDraftRow[] = [];
  protected vitalSignRows: VitalSignDraftRow[] = [];
  protected hydrationRow: PrescriptionDraftRow = this.emptyHydrationRow();
  protected selectedHydrationPreset = '';
  protected medicationGroups: MedicationOrderGroup[] = [];
  protected selectedTemplateId: PrescriptionTemplateId | '' = '';
  protected readonly prescriptionTemplates = PRESCRIPTION_TEMPLATES;
  protected readonly dietPresets = DIET_PRESETS;
  protected readonly hydrationPresets = HYDRATION_PRESETS;
  protected readonly currentDate = new Date();

  protected readonly activeHospital = this.store.activeHospital;
  protected readonly hospitals = this.store.hospitals;
  protected readonly selectedBed = computed(() => {
    const bedId = this.selectedBedId();
    return bedId ? this.store.findBed(bedId) : undefined;
  });
  protected readonly selectedRoom = computed(() => {
    const bedId = this.selectedBedId();
    return this.activeHospital()?.rooms.find((room) => room.beds.some((bed) => bed.id === bedId));
  });
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
    this.openRequestedView();
  }

  protected login(): void {
    if (!this.loginEmail.trim() || !this.loginPassword.trim()) {
      this.showToast('Informe e-mail e senha para continuar.');
      return;
    }
    this.screen.set('hospital-select');
  }

  protected enterHospital(hospitalId: string): void {
    this.store.selectHospital(hospitalId);
    this.screen.set('rooms');
    this.expandedRooms.set(new Set());
  }

  protected switchHospital(hospitalId: string): void {
    this.store.selectHospital(hospitalId);
    this.expandedRooms.set(new Set());
    this.selectedBedId.set(null);
    this.prescriptionPromptOpen.set(false);
    this.admissionOpen.set(false);
  }

  protected logout(): void {
    this.screen.set('login');
    this.store.activeHospitalId.set(null);
    this.expandedRooms.set(new Set());
    this.prescriptionPromptOpen.set(false);
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

  protected openBed(bed: Bed): void {
    if (bed.status === 'OCCUPIED') {
      this.openPrescriptionTab(bed.id);
      return;
    }

    if (bed.status === 'AVAILABLE') {
      this.selectedBedId.set(bed.id);
      this.prescriptionPromptOpen.set(true);
    }
  }

  protected confirmNewPrescription(): void {
    const bedId = this.selectedBedId();
    if (!bedId) return;

    this.prescriptionPromptOpen.set(false);
    this.resetPatientForm();
    this.admissionOpen.set(true);
  }

  protected closePrescriptionPrompt(): void {
    this.prescriptionPromptOpen.set(false);
    this.selectedBedId.set(null);
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
    this.openPrescriptionTab(bedId);
    this.selectedBedId.set(null);
    this.showToast('Paciente cadastrado. A prescrição foi aberta em uma nova guia.');
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

  protected addPrescriptionRow(): void {
    this.prescriptionRows = [...this.prescriptionRows, this.emptyPrescriptionRow()];
  }

  protected removePrescriptionRow(index: number): void {
    if (this.prescriptionRows.length === 1) return;
    this.prescriptionRows = this.prescriptionRows.filter((_, rowIndex) => rowIndex !== index);
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
    const validRows = this.prescriptionRows.filter((row) => row.description.trim());
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
      (!validRows.length &&
        !validVitalSigns.length &&
        !validHydrationRows.length &&
        !validMedicationSections.length)
    ) {
      this.showToast('Inclua ao menos um item na prescrição.');
      return;
    }
    this.store.addPrescription(
      bedId,
      validRows,
      this.prescriptionDiet.trim(),
      this.prescriptionNotes.trim(),
      validVitalSigns,
      validHydrationRows,
      validMedicationSections,
    );
    this.showToast('Prescrição salva no protótipo.');
    const patient = this.selectedBed()?.patient;
    if (patient) this.preparePrescription(patient);
  }

  protected startBlankPrescription(): void {
    this.selectedTemplateId = '';
    this.prescriptionRows = [
      this.emptyPrescriptionRow(),
      this.emptyPrescriptionRow(),
      this.emptyPrescriptionRow(),
    ];
    this.resetStructuredOrders();
    this.prescriptionReady.set(true);
  }

  protected applyPrescriptionTemplate(): void {
    const template = PRESCRIPTION_TEMPLATES.find((item) => item.id === this.selectedTemplateId);
    if (!template) {
      this.showToast('Selecione uma prescrição pré-pronta.');
      return;
    }

    this.prescriptionRows = [
      {
        description: `MODELO ${template.name} — REVISAR E COMPLETAR ITENS COM A EQUIPE CLÍNICA`,
        route: 'OUTRA',
        frequency: '',
        scheduling: 'FIXO',
      },
      this.emptyPrescriptionRow(),
      this.emptyPrescriptionRow(),
    ];
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

  private showToast(message: string): void {
    this.toast.set(message);
    window.setTimeout(() => {
      if (this.toast() === message) this.toast.set(null);
    }, 3200);
  }

  private preparePrescription(patient: Patient): void {
    this.preparePatientForm(patient);
    this.prescriptionDiet = '';
    this.prescriptionNotes = '';
    this.prescriptionRows = [];
    this.vitalSignRows = [];
    this.hydrationRow = this.emptyHydrationRow();
    this.selectedHydrationPreset = '';
    this.medicationGroups = [];
    this.selectedTemplateId = '';
    this.prescriptionReady.set(false);
  }

  private emptyPrescriptionRow(): PrescriptionDraftRow {
    return { description: '', route: 'VO', frequency: '', scheduling: 'FIXO' };
  }

  private emptyHydrationRow(): PrescriptionDraftRow {
    return { description: '', route: 'EV', frequency: '', scheduling: 'FIXO' };
  }

  private emptyMedicationRow(route: string): PrescriptionDraftRow {
    return { description: '', route, frequency: '', scheduling: 'FIXO' };
  }

  private resetStructuredOrders(): void {
    this.vitalSignRows = [
      { description: 'SINAIS VITAIS', frequency: '' },
      { description: 'DXT', frequency: '' },
    ];
    this.hydrationRow = this.emptyHydrationRow();
    this.selectedHydrationPreset = '';
    this.medicationGroups = this.buildMedicationGroups();
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

  private openRequestedView(): void {
    const params = new URLSearchParams(window.location.search);
    const hospitalId = params.get('hospital');
    const bedId = params.get('bed');
    const flow = params.get('flow');
    if (!hospitalId || !bedId || !flow) return;

    this.store.selectHospital(hospitalId);
    const bed = this.store.findBed(bedId);
    const room = this.activeHospital()?.rooms.find((item) =>
      item.beds.some((roomBed) => roomBed.id === bedId),
    );
    if (!bed || !room) return;

    this.selectedBedId.set(bedId);
    this.expandedRooms.set(new Set([room.id]));

    if (flow === 'prescription' && bed.status === 'OCCUPIED' && bed.patient) {
      this.preparePrescription(bed.patient);
      this.screen.set('prescription');
      return;
    }
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
