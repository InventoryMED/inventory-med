import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  RehabilitationCatalog,
  RehabilitationDraft,
  RehabilitationItemDraft,
  RehabilitationResponse,
  RehabilitationSelection,
  VentilatorySupportOption,
} from './medical.models';
import { MedicalService } from './medical.service';

interface RehabilitationItemControls {
  id: FormControl<number>;
  specialty: FormControl<string>;
  procedure: FormControl<string>;
  frequency: FormControl<string>;
  scheduling: FormControl<string>;
  clinicalJustification: FormControl<string>;
}

type RehabilitationItemForm = FormGroup<RehabilitationItemControls>;

@Component({
  selector: 'app-rehabilitation-prescription',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './rehabilitation-prescription.component.html',
  styleUrl: './rehabilitation-prescription.component.scss',
})
export class RehabilitationPrescriptionComponent implements OnInit {
  @Output() readonly selectionChange = new EventEmitter<RehabilitationSelection>();

  private readonly medical = inject(MedicalService);
  private readonly destroyRef = inject(DestroyRef);
  private nextItemId = 1;

  protected readonly catalog = signal<RehabilitationCatalog | null>(null);
  protected readonly preview = signal<RehabilitationResponse | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly selectedTemplate = new FormControl('', { nonNullable: true });
  protected readonly items = new FormArray<RehabilitationItemForm>([]);

  ngOnInit(): void {
    this.selectedTemplate.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.invalidatePreview();
    });
    this.items.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.invalidatePreview();
    });
    void this.loadCatalog();
  }

  async validateAndPreview(): Promise<boolean> {
    if (!this.hasSelection()) return true;
    return this.generatePreview();
  }

  hasSelection(): boolean {
    return this.items.controls.some((item) => Boolean(item.controls.specialty.value));
  }

  reset(): void {
    this.items.clear();
    this.selectedTemplate.setValue('', { emitEvent: false });
    this.nextItemId = 1;
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  protected applyTemplate(code: string): void {
    this.selectedTemplate.setValue(code);
    const template = this.catalog()?.templates.find((item) => item.code === code);
    if (!template) return;
    this.items.clear();
    this.items.push(this.createItemForm(template.item));
    this.nextItemId = 2;
  }

  protected addItem(): void {
    if (this.items.length >= 12) return;
    this.items.push(this.createItemForm());
  }

  protected removeItem(index: number): void {
    this.items.removeAt(index);
  }

  protected changeSpecialty(group: RehabilitationItemForm, specialty: string): void {
    const id = group.controls.id.value;
    group.reset(this.emptyItem(id));
    group.controls.specialty.setValue(specialty);
  }

  protected changeFrequency(group: RehabilitationItemForm, frequency: string): void {
    group.controls.frequency.setValue(frequency);
    group.controls.scheduling.setValue(['PRN', 'AS_NEEDED'].includes(frequency) ? 'PRN' : 'FIXED');
  }

  protected proceduresFor(specialty: string): VentilatorySupportOption[] {
    const options = this.catalog();
    if (!options) return [];
    switch (specialty) {
      case 'RESPIRATORY_PHYSIOTHERAPY':
        return options.respiratoryProcedures;
      case 'MOTOR_PHYSIOTHERAPY':
        return options.motorProcedures;
      case 'SPEECH_THERAPY':
        return options.speechTherapyProcedures;
      case 'OCCUPATIONAL_THERAPY':
        return options.occupationalTherapyProcedures;
      default:
        return [];
    }
  }

  protected frequenciesFor(specialty: string): VentilatorySupportOption[] {
    const options = this.catalog();
    if (!options) return [];
    switch (specialty) {
      case 'RESPIRATORY_PHYSIOTHERAPY':
        return options.respiratoryFrequencies;
      case 'MOTOR_PHYSIOTHERAPY':
        return options.motorFrequencies;
      case 'SPEECH_THERAPY':
        return options.speechTherapyFrequencies;
      case 'OCCUPATIONAL_THERAPY':
        return options.occupationalTherapyFrequencies;
      default:
        return [];
    }
  }

  protected requiresJustification(group: RehabilitationItemForm): boolean {
    return (
      ['RESPIRATORY_PHYSIOTHERAPY', 'MOTOR_PHYSIOTHERAPY'].includes(
        group.controls.specialty.value,
      ) && ['EVERY_12H', 'EVERY_8H'].includes(group.controls.frequency.value)
    );
  }

  protected async generatePreview(): Promise<boolean> {
    if (!this.hasSelection()) {
      this.error.set('SELECIONE AO MENOS UMA CONDUTA DE REABILITAÇÃO.');
      return false;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await this.medical.previewRehabilitation(this.draft());
      this.preview.set(result);
      this.emitSelection(result);
      return true;
    } catch (error) {
      this.preview.set(null);
      this.error.set(this.errorMessage(error));
      this.emitSelection(null);
      return false;
    } finally {
      this.busy.set(false);
    }
  }

  private async loadCatalog(): Promise<void> {
    this.busy.set(true);
    try {
      this.catalog.set(await this.medical.rehabilitationCatalog());
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private createItemForm(item?: RehabilitationItemDraft): RehabilitationItemForm {
    const value = item ?? this.emptyItem(this.nextItemId++);
    return new FormGroup<RehabilitationItemControls>({
      id: new FormControl(value.id, { nonNullable: true }),
      specialty: new FormControl(value.specialty, { nonNullable: true }),
      procedure: new FormControl(value.procedure, { nonNullable: true }),
      frequency: new FormControl(value.frequency, { nonNullable: true }),
      scheduling: new FormControl(value.scheduling, { nonNullable: true }),
      clinicalJustification: new FormControl(value.clinicalJustification, { nonNullable: true }),
    });
  }

  private draft(): RehabilitationDraft {
    return {
      selectedTemplate: this.selectedTemplate.value,
      items: this.items.controls
        .filter((group) => Boolean(group.controls.specialty.value))
        .map((group) => group.getRawValue()),
    };
  }

  private emptyItem(id: number): RehabilitationItemDraft {
    return {
      id,
      specialty: '',
      procedure: '',
      frequency: '',
      scheduling: '',
      clinicalJustification: '',
    };
  }

  private invalidatePreview(): void {
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  private emitSelection(response: RehabilitationResponse | null): void {
    this.selectionChange.emit({ draft: this.draft(), response });
  }

  private errorMessage(error: unknown): string {
    if (
      typeof error === 'object' &&
      error !== null &&
      'error' in error &&
      typeof error.error === 'object' &&
      error.error !== null &&
      'message' in error.error &&
      typeof error.error.message === 'string'
    ) {
      return error.error.message.toLocaleUpperCase('pt-BR');
    }
    return 'NÃO FOI POSSÍVEL VALIDAR A REABILITAÇÃO MULTIDISCIPLINAR.';
  }
}
