import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../auth/auth.service';
import {
  ClinicalFormField,
  ClinicalFormKind,
  ClinicalFormSection,
  ClinicalFormTemplate,
  MedicalBed,
  MedicalRoom,
} from './medical.models';
import { MedicalService } from './medical.service';

type Dialog = 'ADMIT' | 'TRANSFER' | 'DISCHARGE' | 'DOCUMENT' | null;

@Component({
  selector: 'app-medical-workspace',
  imports: [CommonModule, FormsModule],
  templateUrl: './medical-workspace.component.html',
  styleUrl: './medical-workspace.component.scss',
})
export class MedicalWorkspaceComponent implements OnInit {
  @Output() readonly changeHospital = new EventEmitter<void>();
  @Output() readonly exit = new EventEmitter<void>();

  private readonly medical = inject(MedicalService);
  protected readonly auth = inject(AuthService);
  protected readonly workspace = signal<Awaited<ReturnType<MedicalService['workspace']>> | null>(
    null,
  );
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly feedback = signal<string | null>(null);
  protected readonly dialog = signal<Dialog>(null);
  protected readonly selectedBed = signal<MedicalBed | null>(null);
  protected readonly expandedRooms = signal<Set<string>>(new Set());
  protected readonly templates = signal<ClinicalFormTemplate[]>([]);
  protected readonly selectedTemplate = signal<ClinicalFormTemplate | null>(null);
  protected readonly documentKind = signal<ClinicalFormKind>('PRESCRIPTION');

  protected patientName = '';
  protected patientBirthDate = '';
  protected patientSex = 'NAO_INFORMADO';
  protected patientWeight: number | null = null;
  protected patientDiagnosis = '';
  protected patientComorbidities = '';
  protected patientAllergies = '';
  protected transferTargetBedId = '';
  protected dischargeReason = '';
  protected documentValues: Record<string, unknown> = {};

  protected readonly rooms = computed(() =>
    (this.workspace()?.careUnits ?? []).flatMap((unit) =>
      unit.rooms.map((room) => ({ ...room, unitName: unit.name })),
    ),
  );
  protected readonly beds = computed(() => this.rooms().flatMap((room) => room.beds));
  protected readonly totalBeds = computed(() => this.beds().length);
  protected readonly availableBeds = computed(() =>
    this.beds().filter((bed) => bed.status === 'AVAILABLE'),
  );
  protected readonly occupiedBeds = computed(() =>
    this.beds().filter((bed) => bed.status === 'OCCUPIED'),
  );
  protected readonly availableTransferBeds = computed(() =>
    this.rooms().flatMap((room) =>
      room.beds
        .filter((bed) => bed.status === 'AVAILABLE')
        .map((bed) => ({ id: bed.id, label: `${room.unitName} • ${room.name} • ${bed.code}` })),
    ),
  );

  ngOnInit(): void {
    void this.loadWorkspace();
  }

  protected toggleRoom(room: MedicalRoom): void {
    this.expandedRooms.update((current) => {
      const next = new Set(current);
      next.has(room.id) ? next.delete(room.id) : next.add(room.id);
      return next;
    });
  }

  protected roomExpanded(roomId: string): boolean {
    return this.expandedRooms().has(roomId);
  }

  protected roomAvailableCount(room: MedicalRoom): number {
    return room.beds.filter((bed) => bed.status === 'AVAILABLE').length;
  }

  protected openAdmission(bed: MedicalBed): void {
    if (bed.status !== 'AVAILABLE') return;
    this.resetMessages();
    this.selectedBed.set(bed);
    this.patientName = '';
    this.patientBirthDate = '';
    this.patientSex = 'NAO_INFORMADO';
    this.patientWeight = null;
    this.patientDiagnosis = '';
    this.patientComorbidities = '';
    this.patientAllergies = '';
    this.dialog.set('ADMIT');
  }

  protected async admit(): Promise<void> {
    const bed = this.selectedBed();
    if (!bed || !this.patientName.trim()) {
      this.error.set('INFORME O NOME DO PACIENTE.');
      return;
    }
    const birthDate = this.parseBirthDate(this.patientBirthDate);
    if (this.patientBirthDate && !birthDate) {
      this.error.set('INFORME A DATA DE NASCIMENTO NO FORMATO DD/MM/AAAA.');
      return;
    }
    await this.runChange(
      () =>
        this.medical.admit({
          bedId: bed.id,
          fullName: this.patientName,
          birthDate,
          sex: this.patientSex,
          weightKg: this.patientWeight,
          diagnosis: this.optional(this.patientDiagnosis),
          comorbidities: this.optional(this.patientComorbidities),
          allergies: this.optional(this.patientAllergies),
        }),
      'PACIENTE ADMITIDO NO LEITO.',
    );
  }

  protected openTransfer(bed: MedicalBed): void {
    this.selectedBed.set(bed);
    this.transferTargetBedId = '';
    this.resetMessages();
    this.dialog.set('TRANSFER');
  }

  protected async transfer(): Promise<void> {
    const admission = this.selectedBed()?.admission;
    if (!admission || !this.transferTargetBedId) {
      this.error.set('SELECIONE O LEITO DE DESTINO.');
      return;
    }
    await this.runChange(
      () => this.medical.transfer(admission.id, this.transferTargetBedId),
      'PACIENTE TRANSFERIDO.',
    );
  }

  protected openDischarge(bed: MedicalBed): void {
    this.selectedBed.set(bed);
    this.dischargeReason = '';
    this.resetMessages();
    this.dialog.set('DISCHARGE');
  }

  protected async discharge(): Promise<void> {
    const admission = this.selectedBed()?.admission;
    if (!admission || !this.dischargeReason) {
      this.error.set('SELECIONE O MOTIVO DA ALTA.');
      return;
    }
    await this.runChange(
      () => this.medical.discharge(admission.id, this.dischargeReason),
      'ALTA REGISTRADA E LEITO LIBERADO.',
    );
  }

  protected async openDocument(bed: MedicalBed, kind: ClinicalFormKind): Promise<void> {
    if (!bed.admission) return;
    this.selectedBed.set(bed);
    this.documentKind.set(kind);
    this.documentValues = {};
    this.templates.set([]);
    this.selectedTemplate.set(null);
    this.resetMessages();
    this.saving.set(true);
    try {
      const templates = await this.medical.templates(kind);
      this.templates.set(templates);
      this.selectedTemplate.set(templates[0] ?? null);
      if (!templates.length) {
        this.error.set('NÃO HÁ MODELO PUBLICADO PARA ESTE TIPO DE DOCUMENTO.');
      }
      this.dialog.set('DOCUMENT');
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  protected chooseTemplate(templateId: string): void {
    this.selectedTemplate.set(
      this.templates().find((template) => template.templateId === templateId) ?? null,
    );
    this.documentValues = {};
  }

  protected activeSections(template: ClinicalFormTemplate): ClinicalFormSection[] {
    return template.sections.filter((section) => section.active);
  }

  protected activeFields(section: ClinicalFormSection): ClinicalFormField[] {
    return section.fields.filter((field) => field.active);
  }

  protected fieldPath(section: ClinicalFormSection, field: ClinicalFormField): string {
    return `${section.key}.${field.key}`;
  }

  protected optionChecked(path: string, value: string): boolean {
    return (
      Array.isArray(this.documentValues[path]) &&
      (this.documentValues[path] as string[]).includes(value)
    );
  }

  protected toggleOption(path: string, value: string, checked: boolean): void {
    const current = Array.isArray(this.documentValues[path])
      ? [...(this.documentValues[path] as string[])]
      : [];
    this.documentValues[path] = checked
      ? [...new Set([...current, value])]
      : current.filter((item) => item !== value);
  }

  protected async saveDocument(finalizeDocument: boolean): Promise<void> {
    const admission = this.selectedBed()?.admission;
    const template = this.selectedTemplate();
    if (!admission || !template) return;
    this.saving.set(true);
    this.resetMessages();
    try {
      await this.medical.createDocument(
        admission.id,
        template,
        this.normalizedDocumentValues(template),
        finalizeDocument,
      );
      this.dialog.set(null);
      this.feedback.set(finalizeDocument ? 'DOCUMENTO CLÍNICO FINALIZADO.' : 'RASCUNHO SALVO.');
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  protected closeDialog(): void {
    this.dialog.set(null);
    this.selectedBed.set(null);
    this.resetMessages();
  }

  protected statusLabel(status: MedicalBed['status']): string {
    return {
      AVAILABLE: 'VAZIO',
      OCCUPIED: 'OCUPADO',
      CLEANING: 'HIGIENIZAÇÃO',
      MAINTENANCE: 'MANUTENÇÃO',
      BLOCKED: 'BLOQUEADO',
    }[status];
  }

  private async loadWorkspace(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const workspace = await this.medical.workspace();
      this.workspace.set(workspace);
      this.expandedRooms.set(new Set(this.rooms().map((room) => room.id)));
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private async runChange(operation: () => Promise<unknown>, success: string): Promise<void> {
    this.saving.set(true);
    this.resetMessages();
    try {
      await operation();
      this.dialog.set(null);
      this.selectedBed.set(null);
      await this.loadWorkspace();
      this.feedback.set(success);
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  private normalizedDocumentValues(template: ClinicalFormTemplate): Record<string, unknown> {
    const normalized: Record<string, unknown> = {};
    for (const section of this.activeSections(template)) {
      for (const field of this.activeFields(section)) {
        const path = this.fieldPath(section, field);
        const value = this.documentValues[path];
        if (value === '' || value === null || value === undefined) continue;
        normalized[path] =
          field.type === 'MEDICATION_LINE' || field.type === 'CLINICAL_TABLE'
            ? [{ description: value }]
            : value;
      }
    }
    return normalized;
  }

  private parseBirthDate(value: string): string | null {
    if (!value.trim()) return null;
    const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value.trim());
    return match ? `${match[3]}-${match[2]}-${match[1]}` : null;
  }

  private optional(value: string): string | null {
    return value.trim() || null;
  }

  private resetMessages(): void {
    this.error.set(null);
    this.feedback.set(null);
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message.toLocaleUpperCase('pt-BR');
    }
    return 'NÃO FOI POSSÍVEL CONCLUIR A OPERAÇÃO.';
  }
}
