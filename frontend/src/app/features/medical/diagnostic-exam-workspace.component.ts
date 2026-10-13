import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Patient } from '../../models';
import {
  ClinicalDocument,
  ClinicalFormTemplate,
  DiagnosticExamCatalog,
  DiagnosticExamDraft,
  DiagnosticExamResponse,
  DiagnosticXrayProfile,
} from './medical.models';
import { MedicalService } from './medical.service';
import {
  ClinicalDocumentationTab,
  ClinicalDocumentationTabsComponent,
} from './clinical-documentation-tabs.component';

@Component({
  selector: 'app-diagnostic-exam-workspace',
  imports: [CommonModule, ReactiveFormsModule, ClinicalDocumentationTabsComponent],
  templateUrl: './diagnostic-exam-workspace.component.html',
  styleUrl: './diagnostic-exam-workspace.component.scss',
})
export class DiagnosticExamWorkspaceComponent implements OnInit {
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

  protected readonly catalog = signal<DiagnosticExamCatalog | null>(null);
  protected readonly templates = signal<ClinicalFormTemplate[]>([]);
  protected readonly documents = signal<ClinicalDocument[]>([]);
  protected readonly preview = signal<DiagnosticExamResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly message = signal<string | null>(null);
  protected readonly printDocumentDate = signal(new Date());
  protected readonly printStatus = signal('PRÉVIA NÃO FINALIZADA');

  protected readonly form = new FormGroup({
    clinicalContext: new FormControl('', { nonNullable: true }),
    bloodExams: new FormControl<string[]>([], { nonNullable: true }),
    urineExams: new FormControl<string[]>([], { nonNullable: true }),
    laboratoryClinicalJustification: new FormControl('', { nonNullable: true }),
    tomographyEnabled: new FormControl(false, { nonNullable: true }),
    tomographyRegion: new FormControl('', { nonNullable: true }),
    tomographyContrast: new FormControl('WITHOUT_CONTRAST', { nonNullable: true }),
    tomographyJustification: new FormControl('', { nonNullable: true }),
    previousCreatinineMgDl: new FormControl('', { nonNullable: true }),
    xrayEnabled: new FormControl(false, { nonNullable: true }),
    xrayRegion: new FormControl('', { nonNullable: true }),
    xrayLaterality: new FormControl('NOT_APPLICABLE', { nonNullable: true }),
    xrayIncidences: new FormControl<string[]>([], { nonNullable: true }),
    xrayJustification: new FormControl('', { nonNullable: true }),
    ultrasoundEnabled: new FormControl(false, { nonNullable: true }),
    ultrasoundType: new FormControl('', { nonNullable: true }),
    ultrasoundLaterality: new FormControl('NOT_APPLICABLE', { nonNullable: true }),
    ultrasoundJustification: new FormControl('', { nonNullable: true }),
    ecgEnabled: new FormControl(false, { nonNullable: true }),
    ecgPriority: new FormControl('IMMEDIATE', { nonNullable: true }),
    ecgJustification: new FormControl('', { nonNullable: true }),
  });

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  protected async retry(): Promise<void> {
    await this.load();
  }

  protected toggleBloodExam(code: string, checked: boolean): void {
    this.toggle(this.form.controls.bloodExams, code, checked);
  }

  protected toggleUrineExam(code: string, checked: boolean): void {
    this.toggle(this.form.controls.urineExams, code, checked);
    if (code === 'URINE_CULTURE' && checked) {
      this.toggle(this.form.controls.urineExams, 'EAS', true);
    }
  }

  protected toggleXrayIncidence(code: string, checked: boolean): void {
    this.toggle(this.form.controls.xrayIncidences, code, checked);
  }

  protected applyQuickPack(pack: DiagnosticExamCatalog['quickPacks'][number]): void {
    this.form.patchValue({
      bloodExams: [...new Set([...this.form.controls.bloodExams.value, ...pack.bloodExamCodes])],
      urineExams: [...new Set([...this.form.controls.urineExams.value, ...pack.urineExamCodes])],
    });
    if (pack.tomographyRegion) {
      this.form.patchValue({ tomographyEnabled: true, tomographyRegion: pack.tomographyRegion });
    }
    if (pack.xrayRegion) {
      this.form.patchValue({ xrayEnabled: true, xrayRegion: pack.xrayRegion });
      this.xrayRegionChanged();
    }
    if (pack.ultrasoundCode) {
      this.form.patchValue({ ultrasoundEnabled: true, ultrasoundType: pack.ultrasoundCode });
      this.ultrasoundChanged();
    }
    if (pack.ecg) this.form.controls.ecgEnabled.setValue(true);
    this.preview.set(null);
    this.message.set('PACOTE ADICIONADO. CONFIRA AS INDICAÇÕES CLÍNICAS ANTES DE FINALIZAR.');
  }

  protected xrayRegionChanged(): void {
    const profile = this.xrayProfile();
    this.form.patchValue({
      xrayIncidences: profile?.defaultIncidences ?? [],
      xrayLaterality: profile?.pairedSite ? 'RIGHT' : 'NOT_APPLICABLE',
    });
    this.preview.set(null);
  }

  protected ultrasoundChanged(): void {
    const option = this.catalog()?.ultrasoundExams.find(
      (item) => item.code === this.form.controls.ultrasoundType.value,
    );
    this.form.patchValue({
      ultrasoundLaterality: option?.pairedSite ? 'RIGHT' : 'NOT_APPLICABLE',
      ultrasoundJustification: '',
    });
    this.preview.set(null);
  }

  protected xrayProfile(): DiagnosticXrayProfile | undefined {
    return this.catalog()?.xrayProfiles.find(
      (item) => item.code === this.form.controls.xrayRegion.value,
    );
  }

  protected selectedUltrasound(): DiagnosticExamCatalog['ultrasoundExams'][number] | undefined {
    return this.catalog()?.ultrasoundExams.find(
      (item) => item.code === this.form.controls.ultrasoundType.value,
    );
  }

  protected highCostLaboratorySelected(): boolean {
    return Boolean(
      this.catalog()?.bloodExams.some(
        (item) => item.highCost && this.form.controls.bloodExams.value.includes(item.code),
      ),
    );
  }

  protected contrastRequiresCreatinine(): boolean {
    return (
      this.form.controls.tomographyEnabled.value &&
      this.form.controls.tomographyContrast.value !== 'WITHOUT_CONTRAST'
    );
  }

  protected useTomographyJustification(label: string): void {
    this.form.controls.tomographyJustification.setValue(
      label === 'OUTRA JUSTIFICATIVA CLÍNICA' ? '' : label,
    );
  }

  protected useUltrasoundJustification(label: string): void {
    this.form.controls.ultrasoundJustification.setValue(label);
  }

  protected useEcgJustification(label: string): void {
    this.form.controls.ecgJustification.setValue(
      label === 'OUTRA JUSTIFICATIVA CLÍNICA' ? '' : label,
    );
  }

  protected async generatePreview(): Promise<void> {
    this.busy.set(true);
    this.message.set(null);
    try {
      this.preview.set(await this.medical.previewDiagnosticExams(this.admissionId, this.payload()));
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
      this.message.set('MODELO PUBLICADO DE EXAMES NÃO ENCONTRADO.');
      return;
    }
    if (
      !window.confirm('FINALIZAR ESTA SOLICITAÇÃO? APÓS A FINALIZAÇÃO ELA NÃO PODERÁ SER ALTERADA.')
    ) {
      return;
    }
    this.busy.set(true);
    this.message.set(null);
    try {
      const payload = this.payload();
      this.preview.set(await this.medical.previewDiagnosticExams(this.admissionId, payload));
      const document = await this.medical.createDocument(
        this.admissionId,
        template,
        { 'EXAMS.REQUEST': payload },
        true,
      );
      this.printDocumentDate.set(new Date(document.finalizedAt ?? document.createdAt));
      this.printStatus.set('FINALIZADO');
      await this.reloadDocuments();
      this.message.set('SOLICITAÇÃO DE EXAMES CRIADA E FINALIZADA COM SUCESSO.');
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  protected openDocument(document: ClinicalDocument): void {
    const stored = document.values['EXAMS.REQUEST'];
    if (!stored || typeof stored !== 'object') return;
    const structured = (stored as Record<string, unknown>)['structuredExams'];
    const billing = (stored as Record<string, unknown>)['billingAudit'];
    if (!structured || !billing) return;
    this.preview.set({
      structuredExams: structured as DiagnosticExamResponse['structuredExams'],
      billingAudit: billing as DiagnosticExamResponse['billingAudit'],
    });
    this.printDocumentDate.set(new Date(document.finalizedAt ?? document.createdAt));
    this.printStatus.set(document.status);
    this.message.set(
      `SOLICITAÇÃO FINALIZADA EM ${this.formatDate(document.finalizedAt ?? document.createdAt)}.`,
    );
    window.setTimeout(
      () => globalThis.document.querySelector('.exam-preview')?.scrollIntoView(),
      0,
    );
  }

  protected print(): void {
    if (!this.preview()) {
      this.message.set('GERE UMA PRÉVIA OU ABRA UMA SOLICITAÇÃO FINALIZADA ANTES DE IMPRIMIR.');
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
        this.medical.diagnosticExamCatalog(),
        this.medical.templates('EXAM'),
        this.medical.documents(this.admissionId),
      ]);
      this.catalog.set(catalog);
      this.templates.set(templates);
      this.documents.set(documents.filter((document) => document.kind === 'EXAM'));
      if (!this.form.controls.clinicalContext.value && catalog.clinicalContexts.length) {
        this.form.controls.clinicalContext.setValue(catalog.clinicalContexts[0].code);
      }
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private async reloadDocuments(): Promise<void> {
    const documents = await this.medical.documents(this.admissionId);
    this.documents.set(documents.filter((document) => document.kind === 'EXAM'));
  }

  private payload(): DiagnosticExamDraft {
    const values = this.form.getRawValue();
    return {
      clinicalContext: values.clinicalContext,
      bloodExams: values.bloodExams,
      urineExams: values.urineExams,
      laboratoryClinicalJustification: values.laboratoryClinicalJustification,
      tomography: values.tomographyEnabled
        ? {
            region: values.tomographyRegion,
            contrast: values.tomographyContrast,
            clinicalJustification: values.tomographyJustification,
            previousCreatinineMgDl: values.previousCreatinineMgDl,
          }
        : null,
      xray: values.xrayEnabled
        ? {
            region: values.xrayRegion,
            laterality: values.xrayLaterality,
            incidences: values.xrayIncidences,
            clinicalJustification: values.xrayJustification,
          }
        : null,
      ultrasound: values.ultrasoundEnabled
        ? {
            type: values.ultrasoundType,
            laterality: values.ultrasoundLaterality,
            clinicalJustification: values.ultrasoundJustification,
          }
        : null,
      electrocardiogram: values.ecgEnabled
        ? {
            type: 'ECG_12_LEADS',
            priority: values.ecgPriority,
            clinicalJustification: values.ecgJustification,
          }
        : null,
    };
  }

  private toggle(control: FormControl<string[]>, code: string, checked: boolean): void {
    const values = new Set(control.value);
    if (checked) values.add(code);
    else values.delete(code);
    control.setValue([...values]);
    this.preview.set(null);
  }

  private errorMessage(error: unknown): string {
    if (error && typeof error === 'object' && 'error' in error) {
      const body = (error as { error?: unknown }).error;
      if (body && typeof body === 'object' && 'message' in body) {
        return String((body as { message: unknown }).message).toLocaleUpperCase('pt-BR');
      }
    }
    return 'NÃO FOI POSSÍVEL PROCESSAR A SOLICITAÇÃO DE EXAMES.';
  }
}
