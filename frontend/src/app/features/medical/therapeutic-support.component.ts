import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  TherapeuticBloodProductDraft,
  TherapeuticHydrationDraft,
  TherapeuticSupportCatalog,
  TherapeuticSupportDraft,
  TherapeuticSupportResponse,
  TherapeuticSupportSelection,
} from './medical.models';
import { MedicalService } from './medical.service';

interface HydrationControls {
  id: FormControl<number>;
  baseSolution: FormControl<string>;
  additives: FormControl<string[]>;
  route: FormControl<string>;
  frequency: FormControl<string>;
  infusionMode: FormControl<string>;
  rateValue: FormControl<number | null>;
  rateUnit: FormControl<string>;
  scheduling: FormControl<string>;
}

interface BloodProductControls {
  id: FormControl<number>;
  product: FormControl<string>;
  modifications: FormControl<string[]>;
  quantity: FormControl<number | null>;
  quantityUnit: FormControl<string>;
  route: FormControl<string>;
  infusionMinutes: FormControl<number | null>;
  preMedications: FormControl<string[]>;
  scheduling: FormControl<string>;
}

type HydrationForm = FormGroup<HydrationControls>;
type BloodProductForm = FormGroup<BloodProductControls>;

@Component({
  selector: 'app-therapeutic-support',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './therapeutic-support.component.html',
  styleUrl: './therapeutic-support.component.scss',
})
export class TherapeuticSupportComponent implements OnInit {
  @Output() readonly selectionChange = new EventEmitter<TherapeuticSupportSelection>();

  private readonly medical = inject(MedicalService);
  private readonly destroyRef = inject(DestroyRef);
  private nextHydrationId = 1;
  private nextBloodProductId = 1;

  protected readonly catalog = signal<TherapeuticSupportCatalog | null>(null);
  readonly preview = signal<TherapeuticSupportResponse | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  readonly clinicalContext = new FormControl('', { nonNullable: true });
  readonly hydrationSolutions = new FormArray<HydrationForm>([]);
  readonly bloodProducts = new FormArray<BloodProductForm>([]);
  readonly glucoseControl = new FormGroup({
    frequency: new FormControl('', { nonNullable: true }),
    hypoglycemiaProtocolActive: new FormControl(false, { nonNullable: true }),
    correctionScaleActive: new FormControl(false, { nonNullable: true }),
    insulinType: new FormControl('', { nonNullable: true }),
    continuousPump: new FormControl(false, { nonNullable: true }),
  });

  ngOnInit(): void {
    this.clinicalContext.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.hydrationSolutions.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.glucoseControl.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.bloodProducts.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    void this.loadCatalog();
  }

  async validateAndPreview(): Promise<boolean> {
    if (!this.hasSelection()) return true;
    return this.generatePreview();
  }

  hasSelection(): boolean {
    return (
      this.hydrationSolutions.length > 0 ||
      Boolean(this.glucoseControl.controls.frequency.value) ||
      this.bloodProducts.length > 0
    );
  }

  reset(): void {
    this.clinicalContext.setValue('', { emitEvent: false });
    this.hydrationSolutions.clear({ emitEvent: false });
    this.bloodProducts.clear({ emitEvent: false });
    this.glucoseControl.reset(this.emptyGlucoseControl(), { emitEvent: false });
    this.nextHydrationId = 1;
    this.nextBloodProductId = 1;
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  addHydration(): void {
    if (this.hydrationSolutions.length >= 8) return;
    this.hydrationSolutions.push(this.createHydrationForm());
  }

  protected removeHydration(index: number): void {
    this.hydrationSolutions.removeAt(index);
  }

  addBloodProduct(): void {
    if (this.bloodProducts.length >= 8) return;
    this.bloodProducts.push(this.createBloodProductForm());
  }

  protected removeBloodProduct(index: number): void {
    this.bloodProducts.removeAt(index);
  }

  protected toggleList(control: FormControl<string[]>, code: string, checked: boolean): void {
    control.setValue(
      checked ? [...control.value, code] : control.value.filter((item) => item !== code),
    );
  }

  protected selected(control: FormControl<string[]>, code: string): boolean {
    return control.value.includes(code);
  }

  protected selectInfusionMode(group: HydrationForm, mode: string): void {
    group.controls.infusionMode.setValue(mode);
    if (mode === 'PUMP') group.controls.rateUnit.setValue('ML_H');
    if (mode === 'MACRODRIP') group.controls.rateUnit.setValue('DROPS_MIN');
    if (mode.startsWith('RAPID_')) {
      group.controls.rateValue.setValue(null);
      group.controls.rateUnit.setValue('');
      group.controls.frequency.setValue('RAPID_PHASE');
    }
  }

  selectGlucoseFrequency(frequency: string): void {
    const controls = this.glucoseControl.controls;
    controls.frequency.setValue(frequency);
    const active = Boolean(frequency);
    controls.hypoglycemiaProtocolActive.setValue(active);
    controls.correctionScaleActive.setValue(active);
    if (active && !controls.insulinType.value) controls.insulinType.setValue('REGULAR');
    if (!active) {
      controls.insulinType.setValue('');
      controls.continuousPump.setValue(false);
    }
  }

  protected toggleContinuousPump(enabled: boolean): void {
    const controls = this.glucoseControl.controls;
    controls.continuousPump.setValue(enabled);
    if (enabled) {
      controls.frequency.setValue('EVERY_1_HOUR');
      controls.hypoglycemiaProtocolActive.setValue(true);
      controls.correctionScaleActive.setValue(true);
      controls.insulinType.setValue('REGULAR');
    }
  }

  protected selectBloodProduct(group: BloodProductForm, product: string): void {
    group.controls.product.setValue(product);
    const plasmaDerivative = this.productCategory(product) === 'PLASMA_DERIVATIVE';
    if (plasmaDerivative) {
      group.controls.modifications.setValue([]);
      group.controls.quantityUnit.setValue('BOTTLE');
    } else if (group.controls.quantityUnit.value === 'BOTTLE') {
      group.controls.quantityUnit.setValue('UNIT');
    }
  }

  protected productCategory(product: string): string {
    return this.catalog()?.bloodProducts.find((item) => item.code === product)?.category ?? '';
  }

  protected async generatePreview(): Promise<boolean> {
    if (!this.hasSelection()) {
      this.error.set('SELECIONE AO MENOS UM ITEM DE SUPORTE TERAPÊUTICO.');
      return false;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await this.medical.previewTherapeuticSupport(this.draft());
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
      this.catalog.set(await this.medical.therapeuticSupportCatalog());
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private createHydrationForm(value?: TherapeuticHydrationDraft): HydrationForm {
    const item = value ?? this.emptyHydration(this.nextHydrationId++);
    return new FormGroup<HydrationControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      baseSolution: new FormControl(item.baseSolution, { nonNullable: true }),
      additives: new FormControl(item.additives, { nonNullable: true }),
      route: new FormControl(item.route, { nonNullable: true }),
      frequency: new FormControl(item.frequency, { nonNullable: true }),
      infusionMode: new FormControl(item.infusionMode, { nonNullable: true }),
      rateValue: new FormControl(item.rateValue),
      rateUnit: new FormControl(item.rateUnit, { nonNullable: true }),
      scheduling: new FormControl(item.scheduling, { nonNullable: true }),
    });
  }

  private createBloodProductForm(value?: TherapeuticBloodProductDraft): BloodProductForm {
    const item = value ?? this.emptyBloodProduct(this.nextBloodProductId++);
    return new FormGroup<BloodProductControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      product: new FormControl(item.product, { nonNullable: true }),
      modifications: new FormControl(item.modifications, { nonNullable: true }),
      quantity: new FormControl(item.quantity),
      quantityUnit: new FormControl(item.quantityUnit, { nonNullable: true }),
      route: new FormControl(item.route, { nonNullable: true }),
      infusionMinutes: new FormControl(item.infusionMinutes),
      preMedications: new FormControl(item.preMedications, { nonNullable: true }),
      scheduling: new FormControl(item.scheduling, { nonNullable: true }),
    });
  }

  private draft(): TherapeuticSupportDraft {
    return {
      clinicalContext: this.clinicalContext.value,
      hydrationSolutions: this.hydrationSolutions.getRawValue(),
      glucoseControl: this.glucoseControl.getRawValue(),
      bloodProducts: this.bloodProducts.getRawValue(),
    };
  }

  private emptyHydration(id: number): TherapeuticHydrationDraft {
    return {
      id,
      baseSolution: '',
      additives: [],
      route: 'EV',
      frequency: '',
      infusionMode: '',
      rateValue: null,
      rateUnit: '',
      scheduling: 'FIXED',
    };
  }

  private emptyBloodProduct(id: number): TherapeuticBloodProductDraft {
    return {
      id,
      product: '',
      modifications: [],
      quantity: 1,
      quantityUnit: 'UNIT',
      route: 'DEDICATED_ACCESS',
      infusionMinutes: 120,
      preMedications: [],
      scheduling: 'URGENT',
    };
  }

  private emptyGlucoseControl(): TherapeuticSupportDraft['glucoseControl'] {
    return {
      frequency: '',
      hypoglycemiaProtocolActive: false,
      correctionScaleActive: false,
      insulinType: '',
      continuousPump: false,
    };
  }

  private invalidatePreview(): void {
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  private emitSelection(response: TherapeuticSupportResponse | null): void {
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
    return 'NÃO FOI POSSÍVEL VALIDAR O SUPORTE TERAPÊUTICO.';
  }
}
