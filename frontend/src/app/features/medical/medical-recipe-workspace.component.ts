import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, OnInit, Output, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Patient } from '../../models';
import {
  ClinicalDocument,
  ClinicalFormTemplate,
  MedicalRecipeCatalog,
  MedicalRecipeDraft,
  MedicalRecipeItem,
  MedicalRecipeResponse,
  MedicalRecipeType,
} from './medical.models';
import { MedicalService } from './medical.service';
import {
  ClinicalDocumentationTab,
  ClinicalDocumentationTabsComponent,
} from './clinical-documentation-tabs.component';

interface RecipeItemControls {
  selected: FormControl<boolean>;
  route: FormControl<string>;
  medication: FormControl<string>;
  quantity: FormControl<string>;
  dosage: FormControl<string>;
  recommendations: FormControl<string>;
}

type RecipeItemForm = FormGroup<RecipeItemControls>;

@Component({
  selector: 'app-medical-recipe-workspace',
  imports: [CommonModule, ReactiveFormsModule, ClinicalDocumentationTabsComponent],
  templateUrl: './medical-recipe-workspace.component.html',
  styleUrl: './medical-recipe-workspace.component.scss',
})
export class MedicalRecipeWorkspaceComponent implements OnInit {
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

  protected readonly catalog = signal<MedicalRecipeCatalog | null>(null);
  protected readonly templates = signal<ClinicalFormTemplate[]>([]);
  protected readonly documents = signal<ClinicalDocument[]>([]);
  protected readonly preview = signal<MedicalRecipeResponse | null>(null);
  protected readonly printDate = signal(new Date());
  protected readonly printStatus = signal('PRÉVIA NÃO FINALIZADA');
  protected readonly loading = signal(true);
  protected readonly busy = signal(false);
  protected readonly message = signal<string | null>(null);

  protected readonly form = new FormGroup({
    recipeType: new FormControl<MedicalRecipeType>('COMMON', { nonNullable: true }),
    items: new FormArray<RecipeItemForm>([]),
    buyerName: new FormControl('', { nonNullable: true }),
    buyerIdentityDocument: new FormControl('', { nonNullable: true }),
    buyerAddress: new FormControl('', { nonNullable: true }),
    physicianAttestation: new FormControl(false, { nonNullable: true }),
  });

  protected get items(): FormArray<RecipeItemForm> {
    return this.form.controls.items;
  }

  async ngOnInit(): Promise<void> {
    this.addRow();
    await this.load();
  }

  protected addRow(item?: MedicalRecipeItem): void {
    if (this.items.length >= 30) return;
    this.items.push(this.itemForm(item));
    this.preview.set(null);
  }

  protected removeSelectedRows(): void {
    const selected = this.items.controls
      .map((row, index) => (row.controls.selected.value ? index : -1))
      .filter((index) => index >= 0)
      .reverse();
    selected.forEach((index) => this.items.removeAt(index));
    if (!this.items.length) this.addRow();
    this.preview.set(null);
  }

  protected selectRecipeType(type: string): void {
    if (!this.isRecipeType(type)) return;
    this.form.controls.recipeType.setValue(type);
    this.preview.set(null);
    this.printStatus.set('PRÉVIA NÃO FINALIZADA');
  }

  protected canFinalize(): boolean {
    return (
      this.form.controls.recipeType.value !== 'NOTIFICATION_B' &&
      this.form.controls.physicianAttestation.value
    );
  }

  protected async generatePreview(): Promise<boolean> {
    this.busy.set(true);
    this.message.set(null);
    try {
      this.preview.set(await this.medical.previewMedicalRecipe(this.admissionId, this.payload()));
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
      this.message.set('MODELO PUBLICADO DE RECEITA MÉDICA NÃO ENCONTRADO.');
      return;
    }
    if (this.form.controls.recipeType.value === 'NOTIFICATION_B') {
      this.message.set(
        'A NOTIFICAÇÃO B NÃO PODE SER FINALIZADA SEM SNCR, NUMERAÇÃO CONTROLADA E ASSINATURA QUALIFICADA.',
      );
      return;
    }
    if (!window.confirm('FINALIZAR A RECEITA MÉDICA? O DOCUMENTO FICARÁ IMUTÁVEL E AUDITÁVEL.')) {
      return;
    }
    this.busy.set(true);
    this.message.set(null);
    try {
      const payload = this.payload();
      const preview = await this.medical.previewMedicalRecipe(this.admissionId, payload);
      if (!preview.finalizationAllowed) {
        this.preview.set(preview);
        this.message.set(preview.blockingReasons.join(' '));
        return;
      }
      const document = await this.medical.createDocument(
        this.admissionId,
        template,
        { 'MEDICAL_RECIPE.RECIPE': payload },
        true,
      );
      this.preview.set(preview);
      this.printDate.set(new Date(document.finalizedAt ?? document.createdAt));
      this.printStatus.set('FINALIZADA');
      await this.reloadDocuments();
      this.message.set('RECEITA MÉDICA FINALIZADA E REGISTRADA NA AUDITORIA.');
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

  protected duplicateLast(): void {
    const latest = this.documents()[0];
    if (!latest) {
      this.message.set('NÃO HÁ RECEITA FINALIZADA NESTA INTERNAÇÃO PARA DUPLICAR.');
      return;
    }
    this.loadDocument(latest, true);
    this.message.set(
      'ITENS DA ÚLTIMA RECEITA COPIADOS PARA UMA NOVA EMISSÃO. REVISE TODOS OS DADOS.',
    );
  }

  protected openDocument(document: ClinicalDocument): void {
    this.loadDocument(document, false);
  }

  protected formatDate(value: string | null | undefined): string {
    if (!value) return '—';
    return new Intl.DateTimeFormat('pt-BR', {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(value));
  }

  protected routeLabel(route: string): string {
    return this.catalog()?.routes.find((option) => option.code === route)?.label ?? route;
  }

  private async load(): Promise<void> {
    this.loading.set(true);
    this.message.set(null);
    try {
      const [catalog, templates, documents] = await Promise.all([
        this.medical.medicalRecipeCatalog(),
        this.medical.templates('MEDICAL_RECIPE'),
        this.medical.documents(this.admissionId),
      ]);
      this.catalog.set(catalog);
      this.templates.set(templates);
      this.documents.set(
        documents.filter(
          (document) => document.kind === 'MEDICAL_RECIPE' && document.status === 'FINALIZED',
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
        (document) => document.kind === 'MEDICAL_RECIPE' && document.status === 'FINALIZED',
      ),
    );
  }

  private loadDocument(document: ClinicalDocument, duplicate: boolean): void {
    const stored = document.values['MEDICAL_RECIPE.RECIPE'];
    if (!stored || typeof stored !== 'object') return;
    const response = stored as unknown as MedicalRecipeResponse;
    if (!response.request?.items?.length) return;
    if (duplicate) {
      this.applyRequest(response.request);
      this.preview.set(null);
      this.printStatus.set('PRÉVIA NÃO FINALIZADA');
      return;
    }
    this.preview.set(response);
    this.printDate.set(new Date(document.finalizedAt ?? document.createdAt));
    this.printStatus.set(document.status);
    this.message.set(`RECEITA FINALIZADA EM ${this.formatDate(document.finalizedAt)}.`);
    window.setTimeout(
      () => globalThis.document.querySelector('.recipe-print-area')?.scrollIntoView(),
      0,
    );
  }

  private applyRequest(request: MedicalRecipeDraft): void {
    this.form.controls.recipeType.setValue(request.recipeType);
    this.form.controls.buyerName.setValue(request.buyerName ?? '');
    this.form.controls.buyerIdentityDocument.setValue(request.buyerIdentityDocument ?? '');
    this.form.controls.buyerAddress.setValue(request.buyerAddress ?? '');
    this.form.controls.physicianAttestation.setValue(false);
    this.items.clear();
    request.items.forEach((item) => this.addRow(item));
  }

  private payload(): MedicalRecipeDraft {
    const values = this.form.getRawValue();
    return {
      recipeType: values.recipeType,
      items: values.items.map((item) => ({
        route: item.route,
        medication: item.medication.trim(),
        quantity: item.quantity.trim(),
        dosage: item.dosage.trim(),
        recommendations: item.recommendations.trim() || null,
      })),
      buyerName: values.buyerName.trim() || null,
      buyerIdentityDocument: values.buyerIdentityDocument.trim() || null,
      buyerAddress: values.buyerAddress.trim() || null,
      physicianAttestation: values.physicianAttestation,
    };
  }

  private itemForm(item?: MedicalRecipeItem): RecipeItemForm {
    return new FormGroup<RecipeItemControls>({
      selected: new FormControl(false, { nonNullable: true }),
      route: new FormControl(item?.route ?? '', { nonNullable: true }),
      medication: new FormControl(item?.medication ?? '', { nonNullable: true }),
      quantity: new FormControl(item?.quantity ?? '', { nonNullable: true }),
      dosage: new FormControl(item?.dosage ?? '', { nonNullable: true }),
      recommendations: new FormControl(item?.recommendations ?? '', { nonNullable: true }),
    });
  }

  private isRecipeType(value: string): value is MedicalRecipeType {
    return value === 'COMMON' || value === 'SPECIAL_CONTROL' || value === 'NOTIFICATION_B';
  }

  private errorMessage(error: unknown): string {
    if (error && typeof error === 'object' && 'error' in error) {
      const body = (error as { error?: unknown }).error;
      if (body && typeof body === 'object' && 'message' in body) {
        return String((body as { message: unknown }).message).toLocaleUpperCase('pt-BR');
      }
    }
    return 'NÃO FOI POSSÍVEL PROCESSAR A RECEITA MÉDICA.';
  }
}
