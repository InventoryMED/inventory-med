import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Patient } from '../../models';
import {
  ClinicalDocument,
  ClinicalFormTemplate,
  ClinicalSummaryCatalog,
  ClinicalSummaryDraft,
  ClinicalSummaryMedication,
  ClinicalSummaryOutcome,
  ClinicalSummaryResponse,
} from './medical.models';
import { MedicalService } from './medical.service';
import {
  ClinicalDocumentationTab,
  ClinicalDocumentationTabsComponent,
} from './clinical-documentation-tabs.component';

interface MedicationControls {
  medication: FormControl<string>;
  dosage: FormControl<string>;
  duration: FormControl<string>;
}

type MedicationForm = FormGroup<MedicationControls>;

@Component({
  selector: 'app-clinical-summary-workspace',
  imports: [CommonModule, ReactiveFormsModule, ClinicalDocumentationTabsComponent],
  templateUrl: './clinical-summary-workspace.component.html',
  styleUrl: './clinical-summary-workspace.component.scss',
})
export class ClinicalSummaryWorkspaceComponent implements OnInit {
  @Input({ required: true }) hospitalName = '';
  @Input({ required: true }) unitName = '';
  @Input({ required: true }) roomName = '';
  @Input({ required: true }) bedCode = '';
  @Input({ required: true }) admissionId = '';
  @Input({ required: true }) patient!: Patient;
  @Input({ required: true }) professionalName = '';
  @Input() initialOutcome: ClinicalSummaryOutcome | '' = '';
  @Output() readonly back = new EventEmitter<void>();
  @Output() readonly moduleChange = new EventEmitter<ClinicalDocumentationTab>();
  @Output() readonly outcomeCompleted = new EventEmitter<void>();

  private readonly medical = inject(MedicalService);

  protected readonly catalog = signal<ClinicalSummaryCatalog | null>(null);
  protected readonly templates = signal<ClinicalFormTemplate[]>([]);
  protected readonly documents = signal<ClinicalDocument[]>([]);
  protected readonly preview = signal<ClinicalSummaryResponse | null>(null);
  protected readonly selectedOrientations = signal<ReadonlySet<string>>(new Set());
  protected readonly selectedDevices = signal<ReadonlySet<string>>(new Set());
  protected readonly finalizedDocumentId = signal<string | null>(null);
  protected readonly printDocumentDate = signal(new Date());
  protected readonly printStatus = signal('PRÉVIA NÃO FINALIZADA');
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly completed = signal(false);
  protected readonly orientationDetails: Record<string, string> = {};

  protected readonly form = new FormGroup({
    outcomeType: new FormControl<ClinicalSummaryOutcome>('ALTA_HOSPITALAR', {
      nonNullable: true,
    }),
    dischargeCondition: new FormControl('', { nonNullable: true }),
    clinicalSummary: new FormControl('', { nonNullable: true }),
    primaryCid: new FormControl('', { nonNullable: true }),
    dischargeMedications: new FormArray<MedicationForm>([]),
    transferReason: new FormControl('', { nonNullable: true }),
    destinationHospital: new FormControl('', { nonNullable: true }),
    destinationCnes: new FormControl('', { nonNullable: true }),
    deathOccurredAt: new FormControl('', { nonNullable: true }),
    directCause: new FormControl('', { nonNullable: true }),
    antecedentCause: new FormControl('', { nonNullable: true }),
    underlyingCause: new FormControl('', { nonNullable: true }),
    necropsy: new FormControl('', { nonNullable: true }),
    physicianAttestation: new FormControl(false, { nonNullable: true }),
  });

  protected get medications(): FormArray<MedicationForm> {
    return this.form.controls.dischargeMedications;
  }

  async ngOnInit(): Promise<void> {
    this.form.controls.outcomeType.setValue(this.initialOutcome || 'ALTA_HOSPITALAR');
    this.form.controls.clinicalSummary.setValue(this.initialClinicalSummary());
    await this.load();
  }

  protected async retry(): Promise<void> {
    await this.load();
  }

  protected selectOutcome(outcome: string): void {
    if (!this.isOutcome(outcome)) return;
    this.form.controls.outcomeType.setValue(outcome);
    this.preview.set(null);
    this.finalizedDocumentId.set(null);
    this.printStatus.set('PRÉVIA NÃO FINALIZADA');
  }

  protected addMedication(): void {
    if (this.medications.length >= 30) return;
    this.medications.push(this.medicationForm());
    this.preview.set(null);
  }

  protected removeMedication(index: number): void {
    this.medications.removeAt(index);
    this.preview.set(null);
  }

  protected toggleOrientation(code: string, checked: boolean): void {
    const next = new Set(this.selectedOrientations());
    checked ? next.add(code) : next.delete(code);
    this.selectedOrientations.set(next);
    this.preview.set(null);
  }

  protected toggleDevice(code: string, checked: boolean): void {
    const next = new Set(this.selectedDevices());
    checked ? next.add(code) : next.delete(code);
    this.selectedDevices.set(next);
    this.preview.set(null);
  }

  protected selectionContains(selection: ReadonlySet<string>, code: string): boolean {
    return selection.has(code);
  }

  protected async generatePreview(): Promise<boolean> {
    this.busy.set(true);
    this.message.set(null);
    try {
      this.preview.set(await this.medical.previewClinicalSummary(this.payload()));
      this.printDocumentDate.set(new Date());
      this.printStatus.set('PRÉVIA NÃO FINALIZADA');
      this.finalizedDocumentId.set(null);
      return true;
    } catch (error) {
      this.message.set(this.errorMessage(error));
      return false;
    } finally {
      this.busy.set(false);
    }
  }

  protected async finalizeDocument(): Promise<void> {
    const template = this.templates()[0];
    if (!template) {
      this.message.set('MODELO PUBLICADO DE RESUMO CLÍNICO NÃO ENCONTRADO.');
      return;
    }
    if (!window.confirm('FINALIZAR O RESUMO CLÍNICO? O DOCUMENTO FICARÁ IMUTÁVEL E AUDITÁVEL.')) {
      return;
    }

    this.busy.set(true);
    this.message.set(null);
    try {
      const payload = this.payload();
      const preview = await this.medical.previewClinicalSummary(payload);
      const document = await this.medical.createDocument(
        this.admissionId,
        template,
        { 'CLINICAL_SUMMARY.REPORT': payload },
        true,
      );
      this.preview.set(preview);
      this.finalizedDocumentId.set(document.id);
      this.printDocumentDate.set(new Date(document.finalizedAt ?? document.createdAt));
      this.printStatus.set('FINALIZADO');
      await this.reloadDocuments();
      this.message.set('RESUMO CLÍNICO FINALIZADO E PRONTO PARA O DESFECHO.');
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  protected openDocument(document: ClinicalDocument): void {
    const stored = document.values['CLINICAL_SUMMARY.REPORT'];
    if (!stored || typeof stored !== 'object') return;
    const content = stored as Record<string, unknown>;
    if (!content['request'] || !content['outcomeReference']) return;
    this.preview.set(content as unknown as ClinicalSummaryResponse);
    this.finalizedDocumentId.set(document.id);
    this.printDocumentDate.set(new Date(document.finalizedAt ?? document.createdAt));
    this.printStatus.set(document.status);
    this.message.set(
      `RESUMO CLÍNICO FINALIZADO EM ${this.formatDate(document.finalizedAt ?? document.createdAt)}.`,
    );
    window.setTimeout(
      () => globalThis.document.querySelector('.clinical-summary-preview')?.scrollIntoView(),
      0,
    );
  }

  protected canConclude(): boolean {
    return Boolean(this.finalizedDocumentId() && this.preview()?.outcomeReference.closesAdmission);
  }

  protected async concludeOutcome(): Promise<void> {
    const documentId = this.finalizedDocumentId();
    const currentPreview = this.preview();
    if (!documentId || !currentPreview?.outcomeReference.closesAdmission) {
      this.message.set('FINALIZE UM RESUMO COMPATÍVEL ANTES DE ENCERRAR A INTERNAÇÃO.');
      return;
    }
    if (
      !window.confirm('CONFIRMAR O DESFECHO? A INTERNAÇÃO SERÁ ENCERRADA E O LEITO SERÁ LIBERADO.')
    ) {
      return;
    }
    this.busy.set(true);
    this.message.set(null);
    try {
      await this.medical.discharge(
        this.admissionId,
        currentPreview.outcomeReference.dischargeReason,
        documentId,
      );
      this.completed.set(true);
      this.message.set('DESFECHO REGISTRADO. A INTERNAÇÃO FOI ENCERRADA E O LEITO LIBERADO.');
      this.outcomeCompleted.emit();
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  protected async print(): Promise<void> {
    if (!this.preview() && !(await this.generatePreview())) return;
    window.setTimeout(() => window.print(), 0);
  }

  protected formatDate(value: string | null | undefined): string {
    if (!value) return '—';
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(value));
  }

  protected outcomeLabel(outcome: string): string {
    return this.catalog()?.outcomeTypes.find((option) => option.code === outcome)?.label ?? outcome;
  }

  protected optionLabel(options: Array<{ code: string; label: string }>, code: string): string {
    return options.find((option) => option.code === code)?.label ?? code;
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    this.message.set(null);
    try {
      const [catalog, templates, documents] = await Promise.all([
        this.medical.clinicalSummaryCatalog(),
        this.medical.templates('CLINICAL_SUMMARY'),
        this.medical.documents(this.admissionId),
      ]);
      this.catalog.set(catalog);
      this.templates.set(templates);
      this.documents.set(documents.filter((document) => document.kind === 'CLINICAL_SUMMARY'));
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private async reloadDocuments(): Promise<void> {
    const documents = await this.medical.documents(this.admissionId);
    this.documents.set(documents.filter((document) => document.kind === 'CLINICAL_SUMMARY'));
  }

  private payload(): ClinicalSummaryDraft {
    const values = this.form.getRawValue();
    const outcome = values.outcomeType;
    return {
      outcomeType: outcome,
      dischargeCondition: outcome === 'ALTA_HOSPITALAR' ? values.dischargeCondition || null : null,
      clinicalSummary: values.clinicalSummary,
      primaryCid: values.primaryCid || null,
      dischargeMedications: outcome === 'ALTA_HOSPITALAR' ? values.dischargeMedications : [],
      dischargeOrientations: outcome === 'ALTA_HOSPITALAR' ? this.dischargeOrientations() : [],
      transfer:
        outcome === 'TRANSFERENCIA_HOSPITALAR'
          ? {
              reason: values.transferReason,
              destinationHospital: values.destinationHospital,
              destinationCnes: values.destinationCnes || null,
              devices: [...this.selectedDevices()],
            }
          : null,
      death:
        outcome === 'OBITO'
          ? {
              occurredAt: this.offsetDateTime(values.deathOccurredAt),
              directCause: values.directCause,
              antecedentCause: values.antecedentCause || null,
              underlyingCause: values.underlyingCause,
              necropsy: values.necropsy,
            }
          : null,
      physicianAttestation: values.physicianAttestation,
    };
  }

  private dischargeOrientations(): string[] {
    const suggestions = this.catalog()?.orientationSuggestions ?? [];
    return [...this.selectedOrientations()].map((code) => {
      const label = suggestions.find((option) => option.code === code)?.label ?? code;
      const detail = this.orientationDetails[code]?.trim();
      return detail ? `${label}: ${detail}` : label;
    });
  }

  private medicationForm(item?: ClinicalSummaryMedication): MedicationForm {
    return new FormGroup<MedicationControls>({
      medication: new FormControl(item?.medication ?? '', { nonNullable: true }),
      dosage: new FormControl(item?.dosage ?? '', { nonNullable: true }),
      duration: new FormControl(item?.duration ?? '', { nonNullable: true }),
    });
  }

  private initialClinicalSummary(): string {
    const admissionDate = this.formatDate(this.patient.admissionAt);
    const diagnosis = this.patient.diagnosis?.trim();
    return [
      `Paciente admitido em ${admissionDate}${diagnosis ? ` por ${diagnosis}` : ''}.`,
      'Evolução clínica durante a internação:',
    ].join(' ');
  }

  private offsetDateTime(value: string): string | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date.toISOString();
  }

  private isOutcome(value: string): value is ClinicalSummaryOutcome {
    return value === 'ALTA_HOSPITALAR' || value === 'TRANSFERENCIA_HOSPITALAR' || value === 'OBITO';
  }

  private errorMessage(error: unknown): string {
    if (error && typeof error === 'object' && 'error' in error) {
      const body = (error as { error?: unknown }).error;
      if (body && typeof body === 'object' && 'message' in body) {
        return String((body as { message: unknown }).message).toLocaleUpperCase('pt-BR');
      }
    }
    return 'NÃO FOI POSSÍVEL PROCESSAR O RESUMO CLÍNICO.';
  }
}
