import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Patient } from '../../models';
import {
  ClinicalDocument,
  ClinicalFormTemplate,
  ReferralCatalog,
  ReferralDraft,
  ReferralResponse,
} from './medical.models';
import { MedicalService } from './medical.service';
import {
  ClinicalDocumentationTab,
  ClinicalDocumentationTabsComponent,
} from './clinical-documentation-tabs.component';

@Component({
  selector: 'app-referral-workspace',
  imports: [CommonModule, ReactiveFormsModule, ClinicalDocumentationTabsComponent],
  templateUrl: './referral-workspace.component.html',
  styleUrl: './referral-workspace.component.scss',
})
export class ReferralWorkspaceComponent implements OnInit {
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

  protected readonly catalog = signal<ReferralCatalog | null>(null);
  protected readonly templates = signal<ClinicalFormTemplate[]>([]);
  protected readonly documents = signal<ClinicalDocument[]>([]);
  protected readonly preview = signal<ReferralResponse | null>(null);
  protected readonly printDate = signal(new Date());
  protected readonly printStatus = signal('PRÉVIA NÃO FINALIZADA');
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly message = signal<string | null>(null);

  protected readonly form = new FormGroup({
    specialtyCode: new FormControl('', { nonNullable: true }),
    specialtyDetail: new FormControl('', { nonNullable: true }),
    priority: new FormControl<ReferralDraft['priority']>('ELECTIVE', { nonNullable: true }),
    deadline: new FormControl<ReferralDraft['deadline']>('UP_TO_30_DAYS', {
      nonNullable: true,
    }),
    clinicalJustification: new FormControl('', { nonNullable: true }),
    destination: new FormControl<ReferralDraft['destination']>('PUBLIC_REGULATION', {
      nonNullable: true,
    }),
    attachments: new FormControl<string[]>([], { nonNullable: true }),
    physicianAttestation: new FormControl(false, { nonNullable: true }),
  });

  async ngOnInit(): Promise<void> {
    await this.load();
  }

  protected toggleAttachment(code: string, checked: boolean): void {
    const selected = new Set(this.form.controls.attachments.value);
    if (checked) selected.add(code);
    else selected.delete(code);
    this.form.controls.attachments.setValue([...selected]);
    this.preview.set(null);
  }

  protected attachmentSelected(code: string): boolean {
    return this.form.controls.attachments.value.includes(code);
  }

  protected async generatePreview(): Promise<boolean> {
    this.busy.set(true);
    this.message.set(null);
    try {
      this.preview.set(await this.medical.previewReferral(this.admissionId, this.payload()));
      this.printDate.set(new Date());
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
      this.message.set('MODELO PUBLICADO DE ENCAMINHAMENTO NÃO ENCONTRADO.');
      return;
    }
    if (!window.confirm('FINALIZAR O ENCAMINHAMENTO? O DOCUMENTO FICARÁ IMUTÁVEL E AUDITÁVEL.')) {
      return;
    }
    this.busy.set(true);
    this.message.set(null);
    try {
      const payload = this.payload();
      const preview = await this.medical.previewReferral(this.admissionId, payload);
      if (!preview.finalizationAllowed) {
        this.preview.set(preview);
        this.message.set(preview.blockingReasons.join(' '));
        return;
      }
      const document = await this.medical.createDocument(
        this.admissionId,
        template,
        { 'REFERRAL.REQUEST': payload },
        true,
      );
      this.preview.set(preview);
      this.printDate.set(new Date(document.finalizedAt ?? document.createdAt));
      this.printStatus.set('FINALIZADO');
      await this.reloadDocuments();
      this.message.set('ENCAMINHAMENTO FINALIZADO E REGISTRADO NA AUDITORIA.');
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

  protected openDocument(document: ClinicalDocument): void {
    const stored = document.values['REFERRAL.REQUEST'];
    if (!stored || typeof stored !== 'object') return;
    const response = stored as unknown as ReferralResponse;
    if (!response.request) return;
    this.preview.set(response);
    this.printDate.set(new Date(document.finalizedAt ?? document.createdAt));
    this.printStatus.set(document.status === 'FINALIZED' ? 'FINALIZADO' : document.status);
    this.message.set(`ENCAMINHAMENTO FINALIZADO EM ${this.formatDate(document.finalizedAt)}.`);
    window.setTimeout(
      () => globalThis.document.querySelector('.referral-print-area')?.scrollIntoView(),
      0,
    );
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
        this.medical.referralCatalog(),
        this.medical.templates('REFERRAL'),
        this.medical.documents(this.admissionId),
      ]);
      this.catalog.set(catalog);
      this.templates.set(templates);
      this.documents.set(
        documents.filter(
          (document) => document.kind === 'REFERRAL' && document.status === 'FINALIZED',
        ),
      );
    } catch (error) {
      this.message.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  private async reloadDocuments(): Promise<void> {
    const documents = await this.medical.documents(this.admissionId);
    this.documents.set(
      documents.filter(
        (document) => document.kind === 'REFERRAL' && document.status === 'FINALIZED',
      ),
    );
  }

  private payload(): ReferralDraft {
    const values = this.form.getRawValue();
    return {
      specialtyCode: values.specialtyCode,
      specialtyDetail: values.specialtyDetail.trim() || null,
      priority: values.priority,
      deadline: values.deadline,
      clinicalJustification: values.clinicalJustification.trim(),
      destination: values.destination,
      attachments: values.attachments,
      physicianAttestation: values.physicianAttestation,
    };
  }

  private errorMessage(error: unknown): string {
    if (error && typeof error === 'object' && 'error' in error) {
      const body = (error as { error?: unknown }).error;
      if (body && typeof body === 'object' && 'message' in body) {
        return String((body as { message: unknown }).message).toLocaleUpperCase('pt-BR');
      }
    }
    return 'NÃO FOI POSSÍVEL PROCESSAR O ENCAMINHAMENTO.';
  }
}
