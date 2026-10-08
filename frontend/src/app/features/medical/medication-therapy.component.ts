import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  MedicationTherapyAntimicrobialDraft,
  MedicationTherapyCatalog,
  MedicationTherapyContinuousDraft,
  MedicationTherapyDraft,
  MedicationTherapyProphylaxisDraft,
  MedicationTherapyResponse,
  MedicationTherapySelection,
  MedicationTherapySymptomaticDraft,
} from './medical.models';
import { MedicalService } from './medical.service';

interface AntimicrobialControls {
  id: FormControl<number>;
  drug: FormControl<string>;
  customDrug: FormControl<string>;
  dosePreparation: FormControl<string>;
  route: FormControl<string>;
  administrationMode: FormControl<string>;
  diluent: FormControl<string>;
  infusionSet: FormControl<string>;
  frequency: FormControl<string>;
  scheduling: FormControl<string>;
  loadingDose: FormControl<string>;
  conditionalTrigger: FormControl<string>;
  treatmentDay: FormControl<number | null>;
  infectionFocus: FormControl<string>;
  ccihStatus: FormControl<string>;
  ccihOpinion: FormControl<string>;
  renalDoseAssessment: FormControl<string>;
}

interface ProphylaxisControls {
  id: FormControl<number>;
  intervention: FormControl<string>;
  dosePreparation: FormControl<string>;
  route: FormControl<string>;
  frequency: FormControl<string>;
  scheduling: FormControl<string>;
  conditionalTrigger: FormControl<string>;
  suspensionReason: FormControl<string>;
}

interface ContinuousControls {
  id: FormControl<number>;
  medication: FormControl<string>;
  dosePreparation: FormControl<string>;
  route: FormControl<string>;
  frequency: FormControl<string>;
  reconciliationStatus: FormControl<string>;
  scheduling: FormControl<string>;
  conditionalTrigger: FormControl<string>;
  suspensionReason: FormControl<string>;
}

interface SymptomaticControls {
  id: FormControl<number>;
  drug: FormControl<string>;
  customDrug: FormControl<string>;
  dosePreparation: FormControl<string>;
  route: FormControl<string>;
  frequency: FormControl<string>;
  scheduling: FormControl<string>;
  trigger: FormControl<string>;
  minimumInterval: FormControl<string>;
}

type AntimicrobialForm = FormGroup<AntimicrobialControls>;
type ProphylaxisForm = FormGroup<ProphylaxisControls>;
type ContinuousForm = FormGroup<ContinuousControls>;
type SymptomaticForm = FormGroup<SymptomaticControls>;

@Component({
  selector: 'app-medication-therapy',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './medication-therapy.component.html',
  styleUrl: './medication-therapy.component.scss',
})
export class MedicationTherapyComponent implements OnInit {
  @Output() readonly selectionChange = new EventEmitter<MedicationTherapySelection>();

  private readonly medical = inject(MedicalService);
  private readonly destroyRef = inject(DestroyRef);
  private nextAntimicrobialId = 1;
  private nextProphylaxisId = 1;
  private nextContinuousId = 1;
  private nextSymptomaticId = 1;

  protected readonly catalog = signal<MedicationTherapyCatalog | null>(null);
  readonly preview = signal<MedicationTherapyResponse | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  readonly clinicalContext = new FormControl('', { nonNullable: true });
  readonly renalMeasure = new FormControl('', { nonNullable: true });
  readonly renalValueMlMin = new FormControl<number | null>(null);
  readonly plateletCount = new FormControl<number | null>(null);
  readonly activeBleeding = new FormControl<boolean | null>(null);
  readonly antimicrobials = new FormArray<AntimicrobialForm>([]);
  readonly prophylaxes = new FormArray<ProphylaxisForm>([]);
  readonly continuousMedications = new FormArray<ContinuousForm>([]);
  readonly analgesiaSymptomatics = new FormArray<SymptomaticForm>([]);

  ngOnInit(): void {
    this.clinicalContext.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.renalMeasure.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.renalValueMlMin.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.plateletCount.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.activeBleeding.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.antimicrobials.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.prophylaxes.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.continuousMedications.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.analgesiaSymptomatics.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    void this.loadCatalog();
  }

  hasSelection(): boolean {
    return (
      this.antimicrobials.length > 0 ||
      this.prophylaxes.length > 0 ||
      this.continuousMedications.length > 0 ||
      this.analgesiaSymptomatics.length > 0
    );
  }

  hasAnticoagulantProphylaxis(): boolean {
    const options = this.catalog()?.prophylaxisOptions ?? [];
    return this.prophylaxes.controls.some((group) =>
      options.some(
        (option) => option.code === group.controls.intervention.value && option.anticoagulant,
      ),
    );
  }

  async validateAndPreview(): Promise<boolean> {
    if (!this.hasSelection()) return true;
    return this.generatePreview();
  }

  reset(): void {
    this.clinicalContext.setValue('', { emitEvent: false });
    this.renalMeasure.setValue('', { emitEvent: false });
    this.renalValueMlMin.setValue(null, { emitEvent: false });
    this.plateletCount.setValue(null, { emitEvent: false });
    this.activeBleeding.setValue(null, { emitEvent: false });
    this.antimicrobials.clear({ emitEvent: false });
    this.prophylaxes.clear({ emitEvent: false });
    this.continuousMedications.clear({ emitEvent: false });
    this.analgesiaSymptomatics.clear({ emitEvent: false });
    this.nextAntimicrobialId = 1;
    this.nextProphylaxisId = 1;
    this.nextContinuousId = 1;
    this.nextSymptomaticId = 1;
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  addAntimicrobial(): void {
    if (this.antimicrobials.length < 12) this.antimicrobials.push(this.createAntimicrobialForm());
  }

  addProphylaxis(): void {
    if (this.prophylaxes.length < 12) this.prophylaxes.push(this.createProphylaxisForm());
  }

  addContinuousMedication(): void {
    if (this.continuousMedications.length < 20)
      this.continuousMedications.push(this.createContinuousForm());
  }

  addSymptomatic(): void {
    if (this.analgesiaSymptomatics.length < 20)
      this.analgesiaSymptomatics.push(this.createSymptomaticForm());
  }

  protected removeAntimicrobial(index: number): void {
    this.antimicrobials.removeAt(index);
  }

  protected removeProphylaxis(index: number): void {
    this.prophylaxes.removeAt(index);
  }

  protected removeContinuous(index: number): void {
    this.continuousMedications.removeAt(index);
  }

  protected removeSymptomatic(index: number): void {
    this.analgesiaSymptomatics.removeAt(index);
  }

  protected selectAntimicrobialRoute(group: AntimicrobialForm, route: string): void {
    group.controls.route.setValue(route);
    const modes: Record<string, string> = {
      EV: 'INTERMITTENT_INFUSION',
      VO: 'ORAL',
      IM: 'INTRAMUSCULAR',
      INHALED: 'INHALED',
    };
    group.controls.administrationMode.setValue(modes[route] ?? 'INTERMITTENT_INFUSION');
    const intravenous = route === 'EV';
    group.controls.diluent.setValue(intravenous ? 'SF_09_100' : 'NONE');
    group.controls.infusionSet.setValue(intravenous ? 'MACRODRIP' : 'NONE');
  }

  protected selectProphylaxis(group: ProphylaxisForm, code: string): void {
    group.controls.intervention.setValue(code);
    const selected = this.catalog()?.prophylaxisOptions.find((option) => option.code === code);
    if (selected?.equipment) group.controls.route.setValue('MECHANICAL');
    else if (selected?.category === 'TEV_FARMACOLOGICA') group.controls.route.setValue('SC');
    else group.controls.route.setValue('VO');
  }

  protected selectContinuousScheduling(group: ContinuousForm, scheduling: string): void {
    group.controls.scheduling.setValue(scheduling);
    if (scheduling === 'TEMPORARILY_SUSPENDED') {
      group.controls.reconciliationStatus.setValue('TEMPORARILY_SUSPENDED');
    } else if (group.controls.reconciliationStatus.value === 'TEMPORARILY_SUSPENDED') {
      group.controls.reconciliationStatus.setValue('MAINTAINED_HOME');
    }
  }

  protected async generatePreview(): Promise<boolean> {
    if (!this.hasSelection()) {
      this.error.set('SELECIONE AO MENOS UM ITEM DE TERAPIA MEDICAMENTOSA.');
      return false;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await this.medical.previewMedicationTherapy(this.draft());
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

  protected retryCatalog(): void {
    this.error.set(null);
    void this.loadCatalog();
  }

  private async loadCatalog(): Promise<void> {
    this.busy.set(true);
    try {
      this.catalog.set(await this.medical.medicationTherapyCatalog());
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private createAntimicrobialForm(value?: MedicationTherapyAntimicrobialDraft): AntimicrobialForm {
    const item = value ?? this.emptyAntimicrobial(this.nextAntimicrobialId++);
    return new FormGroup<AntimicrobialControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      drug: new FormControl(item.drug, { nonNullable: true }),
      customDrug: new FormControl(item.customDrug, { nonNullable: true }),
      dosePreparation: new FormControl(item.dosePreparation, { nonNullable: true }),
      route: new FormControl(item.route, { nonNullable: true }),
      administrationMode: new FormControl(item.administrationMode, { nonNullable: true }),
      diluent: new FormControl(item.diluent, { nonNullable: true }),
      infusionSet: new FormControl(item.infusionSet, { nonNullable: true }),
      frequency: new FormControl(item.frequency, { nonNullable: true }),
      scheduling: new FormControl(item.scheduling, { nonNullable: true }),
      loadingDose: new FormControl(item.loadingDose, { nonNullable: true }),
      conditionalTrigger: new FormControl(item.conditionalTrigger, { nonNullable: true }),
      treatmentDay: new FormControl(item.treatmentDay),
      infectionFocus: new FormControl(item.infectionFocus, { nonNullable: true }),
      ccihStatus: new FormControl(item.ccihStatus, { nonNullable: true }),
      ccihOpinion: new FormControl(item.ccihOpinion, { nonNullable: true }),
      renalDoseAssessment: new FormControl(item.renalDoseAssessment, { nonNullable: true }),
    });
  }

  private createProphylaxisForm(value?: MedicationTherapyProphylaxisDraft): ProphylaxisForm {
    const item = value ?? this.emptyProphylaxis(this.nextProphylaxisId++);
    return new FormGroup<ProphylaxisControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      intervention: new FormControl(item.intervention, { nonNullable: true }),
      dosePreparation: new FormControl(item.dosePreparation, { nonNullable: true }),
      route: new FormControl(item.route, { nonNullable: true }),
      frequency: new FormControl(item.frequency, { nonNullable: true }),
      scheduling: new FormControl(item.scheduling, { nonNullable: true }),
      conditionalTrigger: new FormControl(item.conditionalTrigger, { nonNullable: true }),
      suspensionReason: new FormControl(item.suspensionReason, { nonNullable: true }),
    });
  }

  private createContinuousForm(value?: MedicationTherapyContinuousDraft): ContinuousForm {
    const item = value ?? this.emptyContinuous(this.nextContinuousId++);
    return new FormGroup<ContinuousControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      medication: new FormControl(item.medication, { nonNullable: true }),
      dosePreparation: new FormControl(item.dosePreparation, { nonNullable: true }),
      route: new FormControl(item.route, { nonNullable: true }),
      frequency: new FormControl(item.frequency, { nonNullable: true }),
      reconciliationStatus: new FormControl(item.reconciliationStatus, { nonNullable: true }),
      scheduling: new FormControl(item.scheduling, { nonNullable: true }),
      conditionalTrigger: new FormControl(item.conditionalTrigger, { nonNullable: true }),
      suspensionReason: new FormControl(item.suspensionReason, { nonNullable: true }),
    });
  }

  private createSymptomaticForm(value?: MedicationTherapySymptomaticDraft): SymptomaticForm {
    const item = value ?? this.emptySymptomatic(this.nextSymptomaticId++);
    return new FormGroup<SymptomaticControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      drug: new FormControl(item.drug, { nonNullable: true }),
      customDrug: new FormControl(item.customDrug, { nonNullable: true }),
      dosePreparation: new FormControl(item.dosePreparation, { nonNullable: true }),
      route: new FormControl(item.route, { nonNullable: true }),
      frequency: new FormControl(item.frequency, { nonNullable: true }),
      scheduling: new FormControl(item.scheduling, { nonNullable: true }),
      trigger: new FormControl(item.trigger, { nonNullable: true }),
      minimumInterval: new FormControl(item.minimumInterval, { nonNullable: true }),
    });
  }

  private draft(): MedicationTherapyDraft {
    const renalFunction =
      this.renalMeasure.value || this.renalValueMlMin.value !== null
        ? { measure: this.renalMeasure.value, valueMlMin: this.renalValueMlMin.value }
        : null;
    const bleedingRisk =
      this.plateletCount.value !== null || this.activeBleeding.value !== null
        ? { plateletCount: this.plateletCount.value, activeBleeding: this.activeBleeding.value }
        : null;
    return {
      clinicalContext: this.clinicalContext.value,
      renalFunction,
      bleedingRisk,
      antimicrobials: this.antimicrobials.getRawValue(),
      prophylaxes: this.prophylaxes.getRawValue(),
      continuousMedications: this.continuousMedications.getRawValue(),
      analgesiaSymptomatics: this.analgesiaSymptomatics.getRawValue(),
    };
  }

  private emptyAntimicrobial(id: number): MedicationTherapyAntimicrobialDraft {
    return {
      id,
      drug: '',
      customDrug: '',
      dosePreparation: '',
      route: 'EV',
      administrationMode: 'INTERMITTENT_INFUSION',
      diluent: 'SF_09_100',
      infusionSet: 'MACRODRIP',
      frequency: '',
      scheduling: 'FIXED',
      loadingDose: '',
      conditionalTrigger: '',
      treatmentDay: 1,
      infectionFocus: '',
      ccihStatus: 'REQUESTED',
      ccihOpinion: '',
      renalDoseAssessment: '',
    };
  }

  private emptyProphylaxis(id: number): MedicationTherapyProphylaxisDraft {
    return {
      id,
      intervention: '',
      dosePreparation: '',
      route: 'SC',
      frequency: '24/24H',
      scheduling: 'FIXED',
      conditionalTrigger: '',
      suspensionReason: '',
    };
  }

  private emptyContinuous(id: number): MedicationTherapyContinuousDraft {
    return {
      id,
      medication: '',
      dosePreparation: '',
      route: 'VO',
      frequency: '',
      reconciliationStatus: 'MAINTAINED_HOME',
      scheduling: 'FIXED',
      conditionalTrigger: '',
      suspensionReason: '',
    };
  }

  private emptySymptomatic(id: number): MedicationTherapySymptomaticDraft {
    return {
      id,
      drug: '',
      customDrug: '',
      dosePreparation: '',
      route: 'EV',
      frequency: '',
      scheduling: 'PRN',
      trigger: '',
      minimumInterval: '',
    };
  }

  private invalidatePreview(): void {
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  private emitSelection(response: MedicationTherapyResponse | null): void {
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
    return 'NÃO FOI POSSÍVEL VALIDAR A TERAPIA MEDICAMENTOSA.';
  }
}
