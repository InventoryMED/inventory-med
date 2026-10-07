import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule } from '@angular/forms';
import {
  MonitoringPrescriptionCatalog,
  MonitoringPrescriptionDraft,
  MonitoringPrescriptionResponse,
  MonitoringPrescriptionSelection,
} from './medical.models';
import { MedicalService } from './medical.service';

@Component({
  selector: 'app-monitoring-prescription',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './monitoring-prescription.component.html',
  styleUrl: './monitoring-prescription.component.scss',
})
export class MonitoringPrescriptionComponent implements OnInit {
  @Output() readonly selectionChange = new EventEmitter<MonitoringPrescriptionSelection>();

  private readonly medical = inject(MedicalService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  protected readonly catalog = signal<MonitoringPrescriptionCatalog | null>(null);
  protected readonly preview = signal<MonitoringPrescriptionResponse | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.formBuilder.group({
    vitalSigns: this.formBuilder.group({
      frequency: [''],
      painScale: [''],
      consciousnessSedationScale: [''],
      fallRiskScale: [''],
    }),
    glucoseMonitoring: this.formBuilder.group({
      frequency: [''],
      hypoglycemiaProtocol: [false],
      slidingScale: [false],
      insulinType: [''],
    }),
    fluidBalanceOutputs: this.formBuilder.group({
      fluidBalance: [''],
      urineOutput: [''],
      drainsTubes: this.formBuilder.control<string[]>([]),
      otherMeasurements: this.formBuilder.control<string[]>([]),
    }),
    invasiveMonitoring: this.formBuilder.group({
      hemodynamic: this.formBuilder.control<string[]>([]),
      neurological: this.formBuilder.control<string[]>([]),
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
      value.vitalSigns.frequency,
      value.vitalSigns.painScale,
      value.vitalSigns.consciousnessSedationScale,
      value.vitalSigns.fallRiskScale,
      value.glucoseMonitoring.frequency,
      value.glucoseMonitoring.hypoglycemiaProtocol,
      value.glucoseMonitoring.slidingScale,
      value.glucoseMonitoring.insulinType,
      value.fluidBalanceOutputs.fluidBalance,
      value.fluidBalanceOutputs.urineOutput,
      ...value.fluidBalanceOutputs.drainsTubes,
      ...value.fluidBalanceOutputs.otherMeasurements,
      ...value.invasiveMonitoring.hemodynamic,
      ...value.invasiveMonitoring.neurological,
    ].some(Boolean);
  }

  reset(): void {
    this.form.reset(this.emptyDraft());
  }

  protected clear(): void {
    this.reset();
  }

  protected toggle(control: FormControl<string[]>, option: string, checked: boolean): void {
    control.setValue(
      checked ? [...control.value, option] : control.value.filter((item) => item !== option),
    );
  }

  protected selected(control: FormControl<string[]>, option: string): boolean {
    return control.value.includes(option);
  }

  protected toggleSlidingScale(checked: boolean): void {
    const group = this.form.controls.glucoseMonitoring.controls;
    group.slidingScale.setValue(checked);
    if (!checked) group.insulinType.setValue('');
  }

  protected async generatePreview(): Promise<boolean> {
    if (!this.hasSelection()) {
      this.error.set('SELECIONE AO MENOS UM ITEM DE MONITORIZAÇÃO.');
      return false;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await this.medical.previewMonitoring(this.draft());
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
      this.catalog.set(await this.medical.monitoringCatalog());
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private emitSelection(response: MonitoringPrescriptionResponse | null): void {
    this.selectionChange.emit({ draft: this.draft(), response });
  }

  private draft(): MonitoringPrescriptionDraft {
    return this.form.getRawValue();
  }

  private emptyDraft(): MonitoringPrescriptionDraft {
    return {
      vitalSigns: {
        frequency: '',
        painScale: '',
        consciousnessSedationScale: '',
        fallRiskScale: '',
      },
      glucoseMonitoring: {
        frequency: '',
        hypoglycemiaProtocol: false,
        slidingScale: false,
        insulinType: '',
      },
      fluidBalanceOutputs: {
        fluidBalance: '',
        urineOutput: '',
        drainsTubes: [],
        otherMeasurements: [],
      },
      invasiveMonitoring: { hemodynamic: [], neurological: [] },
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
    return 'NÃO FOI POSSÍVEL VALIDAR A MONITORIZAÇÃO.';
  }
}
