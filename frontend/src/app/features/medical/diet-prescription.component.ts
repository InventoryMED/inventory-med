import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import {
  DietPrescriptionCatalog,
  DietPrescriptionDraft,
  DietPrescriptionResponse,
  DietPrescriptionSelection,
  DietType,
} from './medical.models';
import { MedicalService } from './medical.service';

@Component({
  selector: 'app-diet-prescription',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './diet-prescription.component.html',
  styleUrl: './diet-prescription.component.scss',
})
export class DietPrescriptionComponent implements OnInit {
  @Output() readonly selectionChange = new EventEmitter<DietPrescriptionSelection>();

  private readonly medical = inject(MedicalService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  protected readonly catalog = signal<DietPrescriptionCatalog | null>(null);
  protected readonly preview = signal<DietPrescriptionResponse | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly dietTypes: { value: DietType; label: string; description: string }[] = [
    { value: 'ORAL', label: 'DIETA ORAL', description: 'CONSISTÊNCIA E RESTRIÇÕES' },
    { value: 'ENTERAL', label: 'DIETA ENTERAL', description: 'VIA, FÓRMULA E INFUSÃO' },
    { value: 'PARENTERAL', label: 'NUTRIÇÃO PARENTERAL', description: 'VIA, VOLUME E METAS' },
    { value: 'JEJUM', label: 'JEJUM / NVO', description: 'MOTIVO E REAVALIAÇÃO' },
  ];

  protected readonly form = this.formBuilder.group({
    type: this.formBuilder.control<DietType | null>(null),
    oral: this.formBuilder.group({
      consistency: [''],
      restrictions: this.formBuilder.control<string[]>([]),
    }),
    enteral: this.formBuilder.group({
      accessRoute: [''],
      infusionRegimen: [''],
      formulaType: [''],
      rateMlHour: this.formBuilder.control<number | null>(null),
      bolusVolumeMl: this.formBuilder.control<number | null>(null),
      bolusFrequency: [''],
      tubeFlushMl: this.formBuilder.control<number | null>(null),
      flushInterval: [''],
    }),
    parenteral: this.formBuilder.group({
      accessRoute: [''],
      preparationType: [''],
      totalVolumeMl: this.formBuilder.control<number | null>(null),
      rateMlHour: this.formBuilder.control<number | null>(null),
      totalCaloriesKcalDay: this.formBuilder.control<number | null>(null),
      proteinGoalGramsKgDay: this.formBuilder.control<number | null>(null),
      gastrointestinalFailureJustification: [''],
    }),
    fasting: this.formBuilder.group({
      reason: [''],
      reassessment: [''],
    }),
  });

  ngOnInit(): void {
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.preview.set(null);
      this.error.set(null);
      this.emitSelection(null);
    });
    void this.loadCatalog();
  }

  async validateAndPreview(): Promise<boolean> {
    if (!this.form.controls.type.value) return true;
    return this.generatePreview();
  }

  protected selectType(type: DietType): void {
    if (this.form.controls.type.value === type) return;
    this.form.reset(this.emptyFormValue(type));
  }

  protected clear(): void {
    this.form.reset(this.emptyFormValue(null));
  }

  protected toggleRestriction(restriction: string, checked: boolean): void {
    const control = this.form.controls.oral.controls.restrictions;
    const selected = control.value;
    control.setValue(
      checked ? [...selected, restriction] : selected.filter((item) => item !== restriction),
    );
  }

  protected restrictionSelected(restriction: string): boolean {
    return this.form.controls.oral.controls.restrictions.value.includes(restriction);
  }

  protected async generatePreview(): Promise<boolean> {
    if (!this.form.controls.type.value) {
      this.error.set('SELECIONE O TIPO DE DIETA.');
      return false;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await this.medical.previewDiet(this.draft());
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
      this.catalog.set(await this.medical.dietCatalog());
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private emitSelection(response: DietPrescriptionResponse | null): void {
    this.selectionChange.emit({ draft: this.draft(), response });
  }

  private draft(): DietPrescriptionDraft {
    const value = this.form.getRawValue();
    return {
      type: value.type,
      oral: value.oral,
      enteral: value.enteral,
      parenteral: value.parenteral,
      fasting: value.fasting,
    };
  }

  private emptyFormValue(type: DietType | null): DietPrescriptionDraft {
    return {
      type,
      oral: { consistency: '', restrictions: [] },
      enteral: {
        accessRoute: '',
        infusionRegimen: '',
        formulaType: '',
        rateMlHour: null,
        bolusVolumeMl: null,
        bolusFrequency: '',
        tubeFlushMl: null,
        flushInterval: '',
      },
      parenteral: {
        accessRoute: '',
        preparationType: '',
        totalVolumeMl: null,
        rateMlHour: null,
        totalCaloriesKcalDay: null,
        proteinGoalGramsKgDay: null,
        gastrointestinalFailureJustification: '',
      },
      fasting: { reason: '', reassessment: '' },
    };
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
    return 'NÃO FOI POSSÍVEL VALIDAR A DIETA.';
  }
}
