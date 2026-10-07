import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule } from '@angular/forms';
import {
  NursingCarePrescriptionCatalog,
  NursingCarePrescriptionDraft,
  NursingCarePrescriptionResponse,
  NursingCarePrescriptionSelection,
} from './medical.models';
import { MedicalService } from './medical.service';

@Component({
  selector: 'app-nursing-care-prescription',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './nursing-care-prescription.component.html',
  styleUrl: './nursing-care-prescription.component.scss',
})
export class NursingCarePrescriptionComponent implements OnInit {
  @Output() readonly selectionChange = new EventEmitter<NursingCarePrescriptionSelection>();

  private readonly medical = inject(MedicalService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  protected readonly catalog = signal<NursingCarePrescriptionCatalog | null>(null);
  protected readonly preview = signal<NursingCarePrescriptionResponse | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.formBuilder.group({
    positioning: this.formBuilder.group({
      headPosition: [''],
      repositioningFrequency: [''],
      pressureProtection: this.formBuilder.control<string[]>([]),
    }),
    hygieneSkin: this.formBuilder.group({
      bath: [''],
      oralHygiene: [''],
      skinCare: this.formBuilder.control<string[]>([]),
    }),
    dressingsDrains: this.formBuilder.group({
      catheterDressing: [''],
      acuteWoundCare: [''],
      complexWoundCoverage: [''],
      dressingChangeFrequency: [''],
      drainCare: this.formBuilder.control<string[]>([]),
    }),
    procedures: this.formBuilder.group({
      airwaySuction: [''],
      deviceCare: this.formBuilder.control<string[]>([]),
      fluidBalance: [''],
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
    if (!this.hasSelection()) return true;
    return this.generatePreview();
  }

  hasSelection(): boolean {
    const value = this.form.getRawValue();
    return [
      value.positioning.headPosition,
      value.positioning.repositioningFrequency,
      ...value.positioning.pressureProtection,
      value.hygieneSkin.bath,
      value.hygieneSkin.oralHygiene,
      ...value.hygieneSkin.skinCare,
      value.dressingsDrains.catheterDressing,
      value.dressingsDrains.acuteWoundCare,
      value.dressingsDrains.complexWoundCoverage,
      value.dressingsDrains.dressingChangeFrequency,
      ...value.dressingsDrains.drainCare,
      value.procedures.airwaySuction,
      ...value.procedures.deviceCare,
    ].some(Boolean);
  }

  protected clear(): void {
    this.form.reset(this.emptyDraft());
  }

  protected toggle(control: FormControl<string[]>, option: string, checked: boolean): void {
    control.setValue(
      checked ? [...control.value, option] : control.value.filter((item) => item !== option),
    );
  }

  protected selected(control: FormControl<string[]>, option: string): boolean {
    return control.value.includes(option);
  }

  protected async generatePreview(): Promise<boolean> {
    if (!this.hasSelection()) {
      this.error.set('SELECIONE AO MENOS UM CUIDADO DE ENFERMAGEM.');
      return false;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await this.medical.previewNursingCare(this.draft());
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
      this.catalog.set(await this.medical.nursingCareCatalog());
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private emitSelection(response: NursingCarePrescriptionResponse | null): void {
    this.selectionChange.emit({ draft: this.draft(), response });
  }

  private draft(): NursingCarePrescriptionDraft {
    return this.form.getRawValue();
  }

  private emptyDraft(): NursingCarePrescriptionDraft {
    return {
      positioning: { headPosition: '', repositioningFrequency: '', pressureProtection: [] },
      hygieneSkin: { bath: '', oralHygiene: '', skinCare: [] },
      dressingsDrains: {
        catheterDressing: '',
        acuteWoundCare: '',
        complexWoundCoverage: '',
        dressingChangeFrequency: '',
        drainCare: [],
      },
      procedures: { airwaySuction: '', deviceCare: [], fluidBalance: '' },
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
    return 'NÃO FOI POSSÍVEL VALIDAR OS CUIDADOS DE ENFERMAGEM.';
  }
}
