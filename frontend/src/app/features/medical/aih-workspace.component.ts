import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Patient } from '../../models';
import {
  AihCatalog,
  AihDraft,
  AihRequestedProcedure,
  AihResponse,
  ClinicalDocument,
  ClinicalFormTemplate,
} from './medical.models';
import { MedicalService } from './medical.service';
import {
  ClinicalDocumentationTab,
  ClinicalDocumentationTabsComponent,
} from './clinical-documentation-tabs.component';

interface AihPatientControls {
  name: FormControl<string>;
  cns: FormControl<string>;
  motherName: FormControl<string>;
  medicalRecordNumber: FormControl<string>;
  address: FormControl<string>;
  bed: FormControl<string>;
  hospital: FormControl<string>;
  cnes: FormControl<string>;
}

interface AihManualControls {
  mainSignsSymptoms: FormControl<string>;
  admissionConditions: FormControl<string>;
  examResults: FormControl<string>;
  initialDiagnosis: FormControl<string>;
  primaryCid: FormControl<string>;
  secondaryCids: FormControl<string>;
  requestedProcedureCode: FormControl<string>;
  admissionCharacter: FormControl<string>;
}

interface AihProcedureControls {
  id: FormControl<number>;
  procedureCode: FormControl<string>;
  anatomicalSite: FormControl<string>;
  laterality: FormControl<string>;
  scheduling: FormControl<string>;
  imageGuided: FormControl<boolean>;
  imageAttachmentReference: FormControl<string>;
  clinicalJustification: FormControl<string>;
}

type AihProcedureForm = FormGroup<AihProcedureControls>;

@Component({
  selector: 'app-aih-workspace',
  imports: [CommonModule, ReactiveFormsModule, ClinicalDocumentationTabsComponent],
  templateUrl: './aih-workspace.component.html',
  styleUrl: './aih-workspace.component.scss',
})
export class AihWorkspaceComponent implements OnInit {
  @Input({ required: true }) hospitalName = '';
  @Input({ required: true }) unitName = '';
  @Input({ required: true }) roomName = '';
  @Input({ required: true }) bedCode = '';
  @Input({ required: true }) admissionId = '';
  @Input({ required: true }) patient!: Patient;
  @Input({ required: true }) professionalName = '';
  @Output() readonly back = new EventEmitter<void>();
  @Output() readonly moduleChange = new EventEmitter<ClinicalDocumentationTab>();

  private readonly medical = inject(MedicalService);
  private nextProcedureId = 1;

  protected readonly catalog = signal<AihCatalog | null>(null);
  protected readonly templates = signal<ClinicalFormTemplate[]>([]);
  protected readonly documents = signal<ClinicalDocument[]>([]);
  protected readonly preview = signal<AihResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly printDocumentDate = signal(new Date());
  protected readonly printStatus = signal('PRÉVIA NÃO FINALIZADA');

  protected readonly form = new FormGroup({
    clinicalContext: new FormControl('', { nonNullable: true }),
    patient: new FormGroup<AihPatientControls>({
      name: new FormControl('', { nonNullable: true }),
      cns: new FormControl('', { nonNullable: true }),
      motherName: new FormControl('', { nonNullable: true }),
      medicalRecordNumber: new FormControl('', { nonNullable: true }),
      address: new FormControl('', { nonNullable: true }),
      bed: new FormControl('', { nonNullable: true }),
      hospital: new FormControl('', { nonNullable: true }),
      cnes: new FormControl('', { nonNullable: true }),
    }),
    requestedProcedures: new FormArray<AihProcedureForm>([]),
    manualData: new FormGroup<AihManualControls>({
      mainSignsSymptoms: new FormControl('', { nonNullable: true }),
      admissionConditions: new FormControl('', { nonNullable: true }),
      examResults: new FormControl('', { nonNullable: true }),
      initialDiagnosis: new FormControl('', { nonNullable: true }),
      primaryCid: new FormControl('', { nonNullable: true }),
      secondaryCids: new FormControl('', { nonNullable: true }),
      requestedProcedureCode: new FormControl('', { nonNullable: true }),
      admissionCharacter: new FormControl('', { nonNullable: true }),
    }),
  });

  protected get procedures(): FormArray<AihProcedureForm> {
    return this.form.controls.requestedProcedures;
  }

  async ngOnInit(): Promise<void> {
    this.form.controls.patient.patchValue({
      name: this.patient.name,
      bed: `${this.roomName} • ${this.bedCode}`,
      hospital: this.hospitalName,
    });
    this.form.controls.manualData.controls.initialDiagnosis.setValue(this.patient.diagnosis ?? '');
    await this.load();
  }

  protected async retry(): Promise<void> {
    await this.load();
  }

  protected addProcedure(): void {
    if (this.procedures.length >= 20) return;
    this.procedures.push(this.procedureForm(this.emptyProcedure()));
    this.preview.set(null);
  }

  protected removeProcedure(index: number): void {
    this.procedures.removeAt(index);
    this.preview.set(null);
  }

  protected procedureChanged(group: AihProcedureForm): void {
    const definition = this.catalog()?.procedures.find(
      (item) => item.code === group.controls.procedureCode.value,
    );
    if (!definition) return;
    group.patchValue({
      anatomicalSite: definition.defaultSite,
      laterality: definition.pairedSite ? 'RIGHT' : 'NOT_APPLICABLE',
    });
    this.preview.set(null);
  }

  protected codeReferences(code: string): string {
    const definition = this.catalog()?.procedures.find((item) => item.code === code);
    if (!definition) return 'SELECIONE UM PROCEDIMENTO';
    const codes = [
      definition.tussCode ? `TUSS ${definition.tussCode}` : null,
      definition.cbhpmCode ? `CBHPM ${definition.cbhpmCode}` : null,
      definition.sigtapCode ? `SIGTAP ${definition.sigtapCode}` : null,
    ].filter((value): value is string => Boolean(value));
    return codes.length ? codes.join(' • ') : 'CÓDIGOS NÃO CADASTRADOS';
  }

  protected async generatePreview(): Promise<boolean> {
    this.busy.set(true);
    this.message.set(null);
    try {
      this.preview.set(await this.medical.previewAih(this.payload()));
      this.printDocumentDate.set(new Date());
      this.printStatus.set('PRÉVIA NÃO FINALIZADA');
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
      this.message.set('MODELO PUBLICADO DE AIH NÃO ENCONTRADO.');
      return;
    }
    if (!window.confirm('FINALIZAR ESTA AIH? O DOCUMENTO FINALIZADO FICARÁ AUDITÁVEL.')) return;

    this.busy.set(true);
    this.message.set(null);
    try {
      const payload = this.payload();
      this.preview.set(await this.medical.previewAih(payload));
      const document = await this.medical.createDocument(
        this.admissionId,
        template,
        { 'AIH.REPORT': payload },
        true,
      );
      this.printDocumentDate.set(new Date(document.finalizedAt ?? document.createdAt));
      this.printStatus.set('FINALIZADO');
      await this.reloadDocuments();
      this.message.set('AIH CRIADA E FINALIZADA COM SUCESSO.');
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  protected openDocument(document: ClinicalDocument): void {
    const stored = document.values['AIH.REPORT'];
    if (!stored || typeof stored !== 'object') return;
    const content = stored as Record<string, unknown>;
    const structuredAih = content['structuredAih'];
    const billingAudit = content['billingAudit'];
    if (!structuredAih || !billingAudit) return;
    this.preview.set({
      structuredAih: structuredAih as AihResponse['structuredAih'],
      billingAudit: billingAudit as AihResponse['billingAudit'],
    });
    this.printDocumentDate.set(new Date(document.finalizedAt ?? document.createdAt));
    this.printStatus.set(document.status);
    this.message.set(
      `AIH FINALIZADA EM ${this.formatDate(document.finalizedAt ?? document.createdAt)}.`,
    );
    window.setTimeout(() => globalThis.document.querySelector('.aih-preview')?.scrollIntoView(), 0);
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

  private async load(): Promise<void> {
    this.loading.set(true);
    this.message.set(null);
    try {
      const [catalog, templates, documents] = await Promise.all([
        this.medical.aihCatalog(),
        this.medical.templates('AIH'),
        this.medical.documents(this.admissionId),
      ]);
      this.catalog.set(catalog);
      this.templates.set(templates);
      this.documents.set(documents.filter((document) => document.kind === 'AIH'));
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private async reloadDocuments(): Promise<void> {
    const documents = await this.medical.documents(this.admissionId);
    this.documents.set(documents.filter((document) => document.kind === 'AIH'));
  }

  private payload(): AihDraft {
    const values = this.form.getRawValue();
    return {
      clinicalContext: values.clinicalContext,
      patientId: this.patient.id,
      patient: values.patient,
      requestedProcedures: values.requestedProcedures,
      manualData: values.manualData,
    };
  }

  private emptyProcedure(): AihRequestedProcedure {
    return {
      id: this.nextProcedureId++,
      procedureCode: '',
      anatomicalSite: '',
      laterality: '',
      scheduling: '',
      imageGuided: false,
      imageAttachmentReference: '',
      clinicalJustification: '',
    };
  }

  private procedureForm(item: AihRequestedProcedure): AihProcedureForm {
    return new FormGroup<AihProcedureControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      procedureCode: new FormControl(item.procedureCode, { nonNullable: true }),
      anatomicalSite: new FormControl(item.anatomicalSite, { nonNullable: true }),
      laterality: new FormControl(item.laterality, { nonNullable: true }),
      scheduling: new FormControl(item.scheduling, { nonNullable: true }),
      imageGuided: new FormControl(item.imageGuided, { nonNullable: true }),
      imageAttachmentReference: new FormControl(item.imageAttachmentReference, {
        nonNullable: true,
      }),
      clinicalJustification: new FormControl(item.clinicalJustification, {
        nonNullable: true,
      }),
    });
  }

  private errorMessage(error: unknown): string {
    if (error && typeof error === 'object' && 'error' in error) {
      const body = (error as { error?: unknown }).error;
      if (body && typeof body === 'object' && 'message' in body) {
        return String((body as { message: unknown }).message).toLocaleUpperCase('pt-BR');
      }
    }
    return 'NÃO FOI POSSÍVEL PROCESSAR A AIH.';
  }
}
