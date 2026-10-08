import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  CriticalCareCatalog,
  CriticalCareDraft,
  CriticalCareEmergencyDraft,
  CriticalCareResponse,
  CriticalCareSedationDraft,
  CriticalCareSelection,
  CriticalCareVasoactiveDraft,
} from './medical.models';
import { MedicalService } from './medical.service';

interface VasoactiveControls {
  id: FormControl<number>;
  drug: FormControl<string>;
  dilution: FormControl<string>;
  finalConcentration: FormControl<string>;
  initialRate: FormControl<number | null>;
  rateUnit: FormControl<string>;
  vascularAccess: FormControl<string>;
  bloodPressureMonitoring: FormControl<string>;
  therapeuticGoal: FormControl<string>;
  scheduling: FormControl<string>;
}

interface SedationControls {
  id: FormControl<number>;
  drug: FormControl<string>;
  preparation: FormControl<string>;
  administrationMode: FormControl<string>;
  rateDoseValue: FormControl<number | null>;
  rateDoseUnit: FormControl<string>;
  sedationTarget: FormControl<string>;
  ventilatoryStatus: FormControl<string>;
  route: FormControl<string>;
  scheduling: FormControl<string>;
}

interface EmergencyControls {
  id: FormControl<number>;
  drug: FormControl<string>;
  doseAdministration: FormControl<string>;
  route: FormControl<string>;
  emergencyIndication: FormControl<string>;
  scheduling: FormControl<string>;
}

type VasoactiveForm = FormGroup<VasoactiveControls>;
type SedationForm = FormGroup<SedationControls>;
type EmergencyForm = FormGroup<EmergencyControls>;

@Component({
  selector: 'app-critical-care-prescription',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './critical-care-prescription.component.html',
  styleUrl: './critical-care-prescription.component.scss',
})
export class CriticalCarePrescriptionComponent implements OnInit {
  @Output() readonly selectionChange = new EventEmitter<CriticalCareSelection>();

  private readonly medical = inject(MedicalService);
  private readonly destroyRef = inject(DestroyRef);
  private nextVasoactiveId = 1;
  private nextSedationId = 1;
  private nextEmergencyId = 1;

  protected readonly catalog = signal<CriticalCareCatalog | null>(null);
  readonly preview = signal<CriticalCareResponse | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  readonly clinicalContext = new FormControl('', { nonNullable: true });
  readonly vasoactiveDrugs = new FormArray<VasoactiveForm>([]);
  readonly sedationAnalgesiaBnm = new FormArray<SedationForm>([]);
  readonly emergencyMedications = new FormArray<EmergencyForm>([]);

  ngOnInit(): void {
    this.clinicalContext.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.vasoactiveDrugs.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.sedationAnalgesiaBnm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    this.emergencyMedications.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.invalidatePreview());
    void this.loadCatalog();
  }

  hasSelection(): boolean {
    return (
      this.vasoactiveDrugs.length > 0 ||
      this.sedationAnalgesiaBnm.length > 0 ||
      this.emergencyMedications.length > 0
    );
  }

  async validateAndPreview(): Promise<boolean> {
    if (!this.hasSelection()) return true;
    return this.generatePreview();
  }

  reset(): void {
    this.clinicalContext.setValue('', { emitEvent: false });
    this.vasoactiveDrugs.clear({ emitEvent: false });
    this.sedationAnalgesiaBnm.clear({ emitEvent: false });
    this.emergencyMedications.clear({ emitEvent: false });
    this.nextVasoactiveId = 1;
    this.nextSedationId = 1;
    this.nextEmergencyId = 1;
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  addVasoactive(): void {
    if (this.vasoactiveDrugs.length < 8) this.vasoactiveDrugs.push(this.createVasoactiveForm());
  }

  addSedation(): void {
    if (this.sedationAnalgesiaBnm.length < 8)
      this.sedationAnalgesiaBnm.push(this.createSedationForm());
  }

  addEmergency(): void {
    if (this.emergencyMedications.length < 8)
      this.emergencyMedications.push(this.createEmergencyForm());
  }

  protected removeVasoactive(index: number): void {
    this.vasoactiveDrugs.removeAt(index);
  }

  protected removeSedation(index: number): void {
    this.sedationAnalgesiaBnm.removeAt(index);
  }

  protected removeEmergency(index: number): void {
    this.emergencyMedications.removeAt(index);
  }

  protected selectVasoactive(group: VasoactiveForm, drugCode: string): void {
    group.controls.drug.setValue(drugCode);
    const drug = this.catalog()?.vasoactiveDrugs.find((item) => item.code === drugCode);
    if (drug?.centralAccessRequired) group.controls.vascularAccess.setValue('CVC');
  }

  protected selectSedationMode(group: SedationForm, mode: string): void {
    group.controls.administrationMode.setValue(mode);
    group.controls.route.setValue(mode === 'CONTINUOUS_BIC' ? 'EV_BIC' : 'EV');
    group.controls.scheduling.setValue(mode === 'CONTINUOUS_BIC' ? 'CONTINUOUS' : 'SINGLE_DOSE');
  }

  protected selectSedationDrug(group: SedationForm, drugCode: string): void {
    group.controls.drug.setValue(drugCode);
    const allowedModes = this.allowedSedationModes(drugCode);
    const mode = allowedModes.includes(group.controls.administrationMode.value)
      ? group.controls.administrationMode.value
      : (allowedModes[0] ?? '');
    this.selectSedationMode(group, mode);
  }

  protected allowedSedationModes(drugCode: string): string[] {
    return this.catalog()?.sedationDrugs.find((item) => item.code === drugCode)?.allowedModes ?? [];
  }

  protected async generatePreview(): Promise<boolean> {
    if (!this.hasSelection()) {
      this.error.set('SELECIONE AO MENOS UM ITEM DE CUIDADOS CRÍTICOS.');
      return false;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await this.medical.previewCriticalCare(this.draft());
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
      this.catalog.set(await this.medical.criticalCareCatalog());
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private createVasoactiveForm(value?: CriticalCareVasoactiveDraft): VasoactiveForm {
    const item = value ?? this.emptyVasoactive(this.nextVasoactiveId++);
    return new FormGroup<VasoactiveControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      drug: new FormControl(item.drug, { nonNullable: true }),
      dilution: new FormControl(item.dilution, { nonNullable: true }),
      finalConcentration: new FormControl(item.finalConcentration, { nonNullable: true }),
      initialRate: new FormControl(item.initialRate),
      rateUnit: new FormControl(item.rateUnit, { nonNullable: true }),
      vascularAccess: new FormControl(item.vascularAccess, { nonNullable: true }),
      bloodPressureMonitoring: new FormControl(item.bloodPressureMonitoring, { nonNullable: true }),
      therapeuticGoal: new FormControl(item.therapeuticGoal, { nonNullable: true }),
      scheduling: new FormControl(item.scheduling, { nonNullable: true }),
    });
  }

  private createSedationForm(value?: CriticalCareSedationDraft): SedationForm {
    const item = value ?? this.emptySedation(this.nextSedationId++);
    return new FormGroup<SedationControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      drug: new FormControl(item.drug, { nonNullable: true }),
      preparation: new FormControl(item.preparation, { nonNullable: true }),
      administrationMode: new FormControl(item.administrationMode, { nonNullable: true }),
      rateDoseValue: new FormControl(item.rateDoseValue),
      rateDoseUnit: new FormControl(item.rateDoseUnit, { nonNullable: true }),
      sedationTarget: new FormControl(item.sedationTarget, { nonNullable: true }),
      ventilatoryStatus: new FormControl(item.ventilatoryStatus, { nonNullable: true }),
      route: new FormControl(item.route, { nonNullable: true }),
      scheduling: new FormControl(item.scheduling, { nonNullable: true }),
    });
  }

  private createEmergencyForm(value?: CriticalCareEmergencyDraft): EmergencyForm {
    const item = value ?? this.emptyEmergency(this.nextEmergencyId++);
    return new FormGroup<EmergencyControls>({
      id: new FormControl(item.id, { nonNullable: true }),
      drug: new FormControl(item.drug, { nonNullable: true }),
      doseAdministration: new FormControl(item.doseAdministration, { nonNullable: true }),
      route: new FormControl(item.route, { nonNullable: true }),
      emergencyIndication: new FormControl(item.emergencyIndication, { nonNullable: true }),
      scheduling: new FormControl(item.scheduling, { nonNullable: true }),
    });
  }

  private draft(): CriticalCareDraft {
    return {
      clinicalContext: this.clinicalContext.value,
      vasoactiveDrugs: this.vasoactiveDrugs.getRawValue(),
      sedationAnalgesiaBnm: this.sedationAnalgesiaBnm.getRawValue(),
      emergencyMedications: this.emergencyMedications.getRawValue(),
    };
  }

  private emptyVasoactive(id: number): CriticalCareVasoactiveDraft {
    return {
      id,
      drug: '',
      dilution: '',
      finalConcentration: '',
      initialRate: null,
      rateUnit: 'ML_H',
      vascularAccess: 'CVC',
      bloodPressureMonitoring: 'INVASIVE_ARTERIAL',
      therapeuticGoal: 'TITULAR PARA MANTER PAM ≥ 65 MMHG',
      scheduling: 'CONTINUOUS',
    };
  }

  private emptySedation(id: number): CriticalCareSedationDraft {
    return {
      id,
      drug: '',
      preparation: '',
      administrationMode: 'CONTINUOUS_BIC',
      rateDoseValue: null,
      rateDoseUnit: 'ML_H',
      sedationTarget: 'RASS_MINUS_4',
      ventilatoryStatus: 'VMI_TOT',
      route: 'EV_BIC',
      scheduling: 'CONTINUOUS',
    };
  }

  private emptyEmergency(id: number): CriticalCareEmergencyDraft {
    return {
      id,
      drug: '',
      doseAdministration: '',
      route: 'EV',
      emergencyIndication: '',
      scheduling: 'NOW',
    };
  }

  private invalidatePreview(): void {
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  private emitSelection(response: CriticalCareResponse | null): void {
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
    return 'NÃO FOI POSSÍVEL VALIDAR OS CUIDADOS CRÍTICOS.';
  }
}
