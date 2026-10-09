import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Patient } from '../../models';
import {
  BedsideProcedureCatalog,
  BedsideProcedureDefinition,
  BedsideProcedureDraft,
  BedsideProcedureItemDraft,
  BedsideProcedureResponse,
  ClinicalDocument,
  ClinicalFormTemplate,
  ProcedureRecordType,
} from './medical.models';
import { MedicalService } from './medical.service';

interface ProcedureControls {
  id: FormControl<number>;
  recordType: FormControl<ProcedureRecordType>;
  procedureSearch: FormControl<string>;
  procedureCode: FormControl<string>;
  customProcedure: FormControl<string>;
  clinicalIndication: FormControl<string>;
  cid10Reference: FormControl<string>;
  anatomicalSite: FormControl<string>;
  laterality: FormControl<string>;
  asepsisAntisepsis: FormControl<string>;
  sterileBarrier: FormControl<string>;
  localAnesthesia: FormControl<string>;
  imageGuided: FormControl<boolean | null>;
  imageAttachmentReference: FormControl<string>;
  imageGuidance: FormControl<string>;
  deviceName: FormControl<string>;
  deviceBrand: FormControl<string>;
  deviceCaliber: FormControl<string>;
  deviceLot: FormControl<string>;
  anvisaRegistration: FormControl<string>;
  fixationDressingConnections: FormControl<string>;
  samplesLaboratory: FormControl<string>;
  postProcedureControl: FormControl<string>;
  postProcedureDetails: FormControl<string>;
  monitoringAssistance: FormControl<string>;
  urgency: FormControl<string>;
  techniqueOutcome: FormControl<string>;
  complications: FormControl<string>;
  performedAt: FormControl<string | null>;
}

type ProcedureForm = FormGroup<ProcedureControls>;

@Component({
  selector: 'app-bedside-procedure-workspace',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './bedside-procedure-workspace.component.html',
  styleUrl: './bedside-procedure-workspace.component.scss',
})
export class BedsideProcedureWorkspaceComponent implements OnInit {
  @Input({ required: true }) hospitalName = '';
  @Input({ required: true }) unitName = '';
  @Input({ required: true }) roomName = '';
  @Input({ required: true }) bedCode = '';
  @Input({ required: true }) admissionId = '';
  @Input({ required: true }) patient!: Patient;
  @Input({ required: true }) professionalName = '';
  @Output() readonly back = new EventEmitter<void>();

  private readonly medical = inject(MedicalService);
  private nextId = 1;

  protected readonly catalog = signal<BedsideProcedureCatalog | null>(null);
  protected readonly templates = signal<ClinicalFormTemplate[]>([]);
  protected readonly documents = signal<ClinicalDocument[]>([]);
  protected readonly preview = signal<BedsideProcedureResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly printDocumentDate = signal(new Date());
  protected readonly printStatus = signal('PRÉVIA NÃO FINALIZADA');
  protected readonly form = new FormGroup({
    clinicalContext: new FormControl('', { nonNullable: true }),
    selectedTemplate: new FormControl('', { nonNullable: true }),
    items: new FormArray<ProcedureForm>([]),
  });

  protected get items(): FormArray<ProcedureForm> {
    return this.form.controls.items;
  }

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  protected async retry(): Promise<void> {
    await this.load();
  }

  protected selectTemplate(): void {
    const selected = this.catalog()?.templates.find(
      (template) => template.code === this.form.controls.selectedTemplate.value,
    );
    if (!selected) return;
    this.items.clear();
    this.items.push(this.procedureForm(this.fromTemplate(selected.item)));
    this.preview.set(null);
    this.message.set(null);
  }

  protected addProcedure(): void {
    if (this.items.length >= 10) return;
    this.items.push(this.procedureForm(this.emptyItem()));
    this.preview.set(null);
  }

  protected applyQuickKit(kit: BedsideProcedureCatalog['quickKits'][number]): void {
    if (
      this.items.length === 1 &&
      !this.items.at(0).controls.procedureCode.value &&
      !this.items.at(0).controls.clinicalIndication.value
    ) {
      this.items.clear();
    }
    const availableSlots = 10 - this.items.length;
    if (availableSlots <= 0) return;
    const templates = kit.procedureCodes
      .map((code) => this.catalog()?.templates.find((template) => template.code === code))
      .filter((template) => template !== undefined)
      .slice(0, availableSlots);
    templates.forEach((template) =>
      this.items.push(this.procedureForm(this.fromTemplate(template.item))),
    );
    this.form.controls.selectedTemplate.setValue(
      kit.procedureCodes.length === 1 ? kit.procedureCodes[0] : '',
    );
    this.preview.set(null);
    this.message.set(null);
  }

  protected duplicateProcedure(item: BedsideProcedureItemDraft): void {
    if (this.items.length >= 10) return;
    this.items.push(this.procedureForm({ ...item, id: this.nextId++, performedAt: null }));
    this.preview.set(null);
  }

  protected removeProcedure(index: number): void {
    this.items.removeAt(index);
    this.preview.set(null);
  }

  protected procedureChanged(group: ProcedureForm): void {
    const item = group.controls;
    const definition = this.procedureDefinition(item.procedureCode.value);
    if (!definition) return;
    group.patchValue({
      procedureSearch: definition.label,
      customProcedure: definition.code === 'OTHER' ? item.customProcedure.value : '',
      anatomicalSite: definition.defaultSite,
      laterality: definition.pairedSite ? 'RIGHT' : 'NOT_APPLICABLE',
      asepsisAntisepsis: definition.defaultAsepsis,
      sterileBarrier: definition.defaultSterileBarrier,
      localAnesthesia: definition.defaultAnesthesia,
      imageGuided: null,
      imageAttachmentReference: '',
      imageGuidance: definition.defaultImageGuidance,
      deviceName: definition.defaultDevice,
      deviceCaliber: definition.defaultCaliber,
      fixationDressingConnections: definition.defaultFixation,
      samplesLaboratory: definition.defaultSamples,
      postProcedureControl: definition.defaultPostProcedureControl,
      monitoringAssistance: definition.defaultMonitoring,
    });
    this.preview.set(null);
  }

  protected procedureSearchChanged(group: ProcedureForm): void {
    const searched = group.controls.procedureSearch.value.trim().toLocaleUpperCase('pt-BR');
    const definition = this.catalog()?.procedures.find(
      (procedure) =>
        procedure.label.toLocaleUpperCase('pt-BR') === searched ||
        procedure.code.toLocaleUpperCase('pt-BR') === searched ||
        procedure.billingReference.toLocaleUpperCase('pt-BR') === searched,
    );
    if (!definition) {
      group.controls.procedureCode.setValue('');
      this.preview.set(null);
      return;
    }
    group.controls.procedureCode.setValue(definition.code);
    this.procedureChanged(group);
  }

  protected recordTypeChanged(group: ProcedureForm): void {
    const item = group.controls;
    if (item.recordType.value === 'PERFORMED' && !item.performedAt.value) {
      item.performedAt.setValue(this.localDateTimeValue(new Date()));
    }
    if (item.recordType.value === 'REQUESTED') {
      group.patchValue({ performedAt: null, techniqueOutcome: '', complications: '' });
    }
    this.preview.set(null);
  }

  protected procedureDefinition(code: string): BedsideProcedureDefinition | undefined {
    return this.catalog()?.procedures.find((procedure) => procedure.code === code);
  }

  protected requiresTraceability(group: ProcedureForm): boolean {
    const item = group.getRawValue();
    return Boolean(
      item.recordType === 'PERFORMED' &&
      this.procedureDefinition(item.procedureCode)?.deviceTraceabilityRequired,
    );
  }

  protected requiresPostControl(group: ProcedureForm): boolean {
    const item = group.getRawValue();
    return Boolean(this.procedureDefinition(item.procedureCode)?.postProcedureControlRequired);
  }

  protected isMajorInvasive(group: ProcedureForm): boolean {
    return Boolean(
      this.procedureDefinition(group.controls.procedureCode.value)?.majorInvasiveProcedure,
    );
  }

  protected async generatePreview(): Promise<void> {
    this.busy.set(true);
    this.message.set(null);
    try {
      this.preview.set(await this.medical.previewBedsideProcedure(this.payload()));
      this.printDocumentDate.set(new Date());
      this.printStatus.set('PRÉVIA NÃO FINALIZADA');
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  protected async finalizeDocument(): Promise<void> {
    const template = this.templates()[0];
    if (!template) {
      this.message.set('MODELO PUBLICADO DE PROCEDIMENTOS NÃO ENCONTRADO.');
      return;
    }
    if (
      !window.confirm('FINALIZAR ESTE REGISTRO? APÓS A FINALIZAÇÃO ELE NÃO PODERÁ SER ALTERADO.')
    ) {
      return;
    }
    this.busy.set(true);
    this.message.set(null);
    try {
      const preview = await this.medical.previewBedsideProcedure(this.payload());
      this.preview.set(preview);
      const document = await this.medical.createDocument(
        this.admissionId,
        template,
        { 'PROCEDURE.RECORD': this.payload() },
        true,
      );
      this.printDocumentDate.set(new Date(document.finalizedAt ?? document.createdAt));
      this.printStatus.set('FINALIZADO');
      await this.reloadDocuments();
      this.message.set('REGISTRO DE PROCEDIMENTOS CRIADO E FINALIZADO COM SUCESSO.');
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  protected openDocument(document: ClinicalDocument): void {
    const stored = document.values['PROCEDURE.RECORD'];
    if (!stored || typeof stored !== 'object') return;
    const structured = (stored as Record<string, unknown>)['structuredProcedures'];
    const billing = (stored as Record<string, unknown>)['billingAudit'];
    if (!structured || !billing) return;
    this.preview.set({
      structuredProcedures: structured as BedsideProcedureResponse['structuredProcedures'],
      billingAudit: billing as BedsideProcedureResponse['billingAudit'],
    });
    this.printDocumentDate.set(new Date(document.finalizedAt ?? document.createdAt));
    this.printStatus.set(document.status);
    this.message.set(
      `DOCUMENTO FINALIZADO EM ${this.formatDate(document.finalizedAt ?? document.createdAt)}.`,
    );
    window.setTimeout(
      () => globalThis.document.querySelector('.procedure-preview')?.scrollIntoView(),
      0,
    );
  }

  protected print(): void {
    if (!this.preview()) {
      this.message.set('GERE UMA PRÉVIA OU ABRA UM REGISTRO FINALIZADO ANTES DE IMPRIMIR.');
      return;
    }
    window.print();
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
        this.medical.bedsideProcedureCatalog(),
        this.medical.templates('PROCEDURE'),
        this.medical.documents(this.admissionId),
      ]);
      this.catalog.set(catalog);
      this.templates.set(templates);
      this.documents.set(documents.filter((document) => document.kind === 'PROCEDURE'));
      if (!this.items.length) this.addProcedure();
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private async reloadDocuments(): Promise<void> {
    const documents = await this.medical.documents(this.admissionId);
    this.documents.set(documents.filter((document) => document.kind === 'PROCEDURE'));
  }

  private payload(): BedsideProcedureDraft {
    const values = this.form.getRawValue();
    return {
      clinicalContext: values.clinicalContext,
      selectedTemplate: values.selectedTemplate,
      items: values.items.map((item) => {
        const { procedureSearch, ...procedure } = item;
        void procedureSearch;
        return {
          ...procedure,
          performedAt: item.performedAt ? new Date(item.performedAt).toISOString() : null,
        };
      }),
    };
  }

  private fromTemplate(
    template: BedsideProcedureCatalog['templates'][number]['item'],
  ): BedsideProcedureItemDraft {
    const item = this.emptyItem();
    return {
      ...item,
      ...template,
      id: item.id,
      recordType: 'REQUESTED',
    };
  }

  private emptyItem(): BedsideProcedureItemDraft {
    return {
      id: this.nextId++,
      recordType: 'REQUESTED' as ProcedureRecordType,
      procedureCode: '',
      customProcedure: '',
      clinicalIndication: '',
      cid10Reference: '',
      anatomicalSite: '',
      laterality: 'NOT_APPLICABLE',
      asepsisAntisepsis: '',
      sterileBarrier: '',
      localAnesthesia: '',
      imageGuided: null,
      imageAttachmentReference: '',
      imageGuidance: '',
      deviceName: '',
      deviceBrand: '',
      deviceCaliber: '',
      deviceLot: '',
      anvisaRegistration: '',
      fixationDressingConnections: '',
      samplesLaboratory: '',
      postProcedureControl: 'NOT_APPLICABLE',
      postProcedureDetails: '',
      monitoringAssistance: '',
      urgency: 'IMMEDIATE_URGENT',
      techniqueOutcome: '',
      complications: '',
      performedAt: null,
    };
  }

  private procedureForm(item: BedsideProcedureItemDraft): ProcedureForm {
    return new FormGroup<ProcedureControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      recordType: new FormControl(item.recordType, { nonNullable: true }),
      procedureSearch: new FormControl(this.procedureDefinition(item.procedureCode)?.label ?? '', {
        nonNullable: true,
      }),
      procedureCode: new FormControl(item.procedureCode, { nonNullable: true }),
      customProcedure: new FormControl(item.customProcedure, { nonNullable: true }),
      clinicalIndication: new FormControl(item.clinicalIndication, { nonNullable: true }),
      cid10Reference: new FormControl(item.cid10Reference, { nonNullable: true }),
      anatomicalSite: new FormControl(item.anatomicalSite, { nonNullable: true }),
      laterality: new FormControl(item.laterality, { nonNullable: true }),
      asepsisAntisepsis: new FormControl(item.asepsisAntisepsis, { nonNullable: true }),
      sterileBarrier: new FormControl(item.sterileBarrier, { nonNullable: true }),
      localAnesthesia: new FormControl(item.localAnesthesia, { nonNullable: true }),
      imageGuided: new FormControl(item.imageGuided),
      imageAttachmentReference: new FormControl(item.imageAttachmentReference, {
        nonNullable: true,
      }),
      imageGuidance: new FormControl(item.imageGuidance, { nonNullable: true }),
      deviceName: new FormControl(item.deviceName, { nonNullable: true }),
      deviceBrand: new FormControl(item.deviceBrand, { nonNullable: true }),
      deviceCaliber: new FormControl(item.deviceCaliber, { nonNullable: true }),
      deviceLot: new FormControl(item.deviceLot, { nonNullable: true }),
      anvisaRegistration: new FormControl(item.anvisaRegistration, { nonNullable: true }),
      fixationDressingConnections: new FormControl(item.fixationDressingConnections, {
        nonNullable: true,
      }),
      samplesLaboratory: new FormControl(item.samplesLaboratory, { nonNullable: true }),
      postProcedureControl: new FormControl(item.postProcedureControl, { nonNullable: true }),
      postProcedureDetails: new FormControl(item.postProcedureDetails, { nonNullable: true }),
      monitoringAssistance: new FormControl(item.monitoringAssistance, { nonNullable: true }),
      urgency: new FormControl(item.urgency, { nonNullable: true }),
      techniqueOutcome: new FormControl(item.techniqueOutcome, { nonNullable: true }),
      complications: new FormControl(item.complications, { nonNullable: true }),
      performedAt: new FormControl(item.performedAt),
    });
  }

  private localDateTimeValue(date: Date): string {
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 16);
  }

  private errorMessage(error: unknown): string {
    if (error && typeof error === 'object' && 'error' in error) {
      const body = (error as { error?: unknown }).error;
      if (body && typeof body === 'object' && 'message' in body) {
        return String((body as { message: unknown }).message).toLocaleUpperCase('pt-BR');
      }
    }
    return 'NÃO FOI POSSÍVEL PROCESSAR O REGISTRO DE PROCEDIMENTOS.';
  }
}
