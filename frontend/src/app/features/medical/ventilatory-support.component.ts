import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  VentilatorySupportCatalog,
  VentilatorySupportDraft,
  VentilatorySupportItemDraft,
  VentilatorySupportResponse,
  VentilatorySupportSelection,
} from './medical.models';
import { MedicalService } from './medical.service';

interface VentilatoryItemControls {
  id: FormControl<number>;
  supportType: FormControl<string>;
  frequency: FormControl<string>;
  scheduling: FormControl<string>;
  lowFlowDevice: FormControl<string>;
  oxygenFlowLitersMinute: FormControl<number | null>;
  fio2Percent: FormControl<number | null>;
  highFlowLitersMinute: FormControl<number | null>;
  temperatureCelsius: FormControl<number | null>;
  highFlowInterfaceSize: FormControl<string>;
  nonInvasiveMode: FormControl<string>;
  ipapCmH2o: FormControl<number | null>;
  epapPeepCmH2o: FormControl<number | null>;
  supportPressureCmH2o: FormControl<number | null>;
  backupRate: FormControl<number | null>;
  nonInvasiveInterface: FormControl<string>;
  sessionHours: FormControl<number | null>;
  invasiveAirway: FormControl<string>;
  airwayDetail: FormControl<string>;
  invasiveMode: FormControl<string>;
  tidalVolumeMl: FormControl<number | null>;
  respiratoryRate: FormControl<number | null>;
  peepCmH2o: FormControl<number | null>;
  inspiratoryFlowLitersMinute: FormControl<number | null>;
  inspiratoryTimeSeconds: FormControl<number | null>;
  pauseSeconds: FormControl<number | null>;
  inspiratoryPressureCmH2o: FormControl<number | null>;
  triggerSensitivity: FormControl<number | null>;
  protectiveGoals: FormControl<string[]>;
}

type VentilatoryItemForm = FormGroup<VentilatoryItemControls>;

@Component({
  selector: 'app-ventilatory-support',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './ventilatory-support.component.html',
  styleUrl: './ventilatory-support.component.scss',
})
export class VentilatorySupportComponent implements OnInit {
  @Output() readonly selectionChange = new EventEmitter<VentilatorySupportSelection>();

  private readonly medical = inject(MedicalService);
  private readonly destroyRef = inject(DestroyRef);
  private nextItemId = 1;

  protected readonly catalog = signal<VentilatorySupportCatalog | null>(null);
  protected readonly preview = signal<VentilatorySupportResponse | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly selectedTemplate = new FormControl('', { nonNullable: true });
  protected readonly items = new FormArray<VentilatoryItemForm>([]);

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
    return this.items.controls.some((item) => Boolean(item.controls.supportType.value));
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
    if (this.items.length >= 4) return;
    this.items.push(this.createItemForm());
  }

  protected removeItem(index: number): void {
    this.items.removeAt(index);
  }

  protected changeSupportType(group: VentilatoryItemForm, supportType: string): void {
    const id = group.controls.id.value;
    group.reset(this.emptyFlatItem(id));
    group.controls.supportType.setValue(supportType);
    if (supportType) {
      group.controls.frequency.setValue('CONTINUOUS');
      group.controls.scheduling.setValue('CONTINUOUS');
    }
  }

  protected changeFrequency(group: VentilatoryItemForm, frequency: string): void {
    group.controls.frequency.setValue(frequency);
    const scheduling = ['PRN', 'PRN_SPO2_92', 'PRN_SPO2_88_COPD'].includes(frequency)
      ? 'PRN'
      : frequency === 'CONTINUOUS'
        ? 'CONTINUOUS'
        : 'FIXED';
    group.controls.scheduling.setValue(scheduling);
    if (frequency !== 'EVERY_12H') group.controls.sessionHours.setValue(null);
  }

  protected changeLowFlowDevice(group: VentilatoryItemForm, device: string): void {
    group.controls.lowFlowDevice.setValue(device);
    group.controls.oxygenFlowLitersMinute.setValue(null);
    group.controls.fio2Percent.setValue(null);
  }

  protected changeNonInvasiveMode(group: VentilatoryItemForm, mode: string): void {
    group.controls.nonInvasiveMode.setValue(mode);
    group.controls.ipapCmH2o.setValue(null);
    group.controls.epapPeepCmH2o.setValue(null);
    group.controls.supportPressureCmH2o.setValue(null);
    group.controls.backupRate.setValue(null);
  }

  protected changeInvasiveMode(group: VentilatoryItemForm, mode: string): void {
    group.controls.invasiveMode.setValue(mode);
    group.controls.tidalVolumeMl.setValue(null);
    group.controls.respiratoryRate.setValue(null);
    group.controls.inspiratoryFlowLitersMinute.setValue(null);
    group.controls.inspiratoryTimeSeconds.setValue(null);
    group.controls.pauseSeconds.setValue(null);
    group.controls.inspiratoryPressureCmH2o.setValue(null);
    group.controls.supportPressureCmH2o.setValue(null);
    group.controls.triggerSensitivity.setValue(null);
  }

  protected toggleGoal(group: VentilatoryItemForm, goal: string, checked: boolean): void {
    const control = group.controls.protectiveGoals;
    control.setValue(
      checked ? [...control.value, goal] : control.value.filter((item) => item !== goal),
    );
  }

  protected goalSelected(group: VentilatoryItemForm, goal: string): boolean {
    return group.controls.protectiveGoals.value.includes(goal);
  }

  protected async generatePreview(): Promise<boolean> {
    if (!this.hasSelection()) {
      this.error.set('SELECIONE AO MENOS UM ITEM DE SUPORTE VENTILATÓRIO.');
      return false;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await this.medical.previewVentilatorySupport(this.draft());
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
      this.catalog.set(await this.medical.ventilatorySupportCatalog());
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private createItemForm(item?: VentilatorySupportItemDraft): VentilatoryItemForm {
    const id = item?.id ?? this.nextItemId++;
    const values = item ? this.flatten(item) : this.emptyFlatItem(id);
    return new FormGroup<VentilatoryItemControls>({
      id: new FormControl(values.id, { nonNullable: true }),
      supportType: new FormControl(values.supportType, { nonNullable: true }),
      frequency: new FormControl(values.frequency, { nonNullable: true }),
      scheduling: new FormControl(values.scheduling, { nonNullable: true }),
      lowFlowDevice: new FormControl(values.lowFlowDevice, { nonNullable: true }),
      oxygenFlowLitersMinute: new FormControl(values.oxygenFlowLitersMinute),
      fio2Percent: new FormControl(values.fio2Percent),
      highFlowLitersMinute: new FormControl(values.highFlowLitersMinute),
      temperatureCelsius: new FormControl(values.temperatureCelsius),
      highFlowInterfaceSize: new FormControl(values.highFlowInterfaceSize, { nonNullable: true }),
      nonInvasiveMode: new FormControl(values.nonInvasiveMode, { nonNullable: true }),
      ipapCmH2o: new FormControl(values.ipapCmH2o),
      epapPeepCmH2o: new FormControl(values.epapPeepCmH2o),
      supportPressureCmH2o: new FormControl(values.supportPressureCmH2o),
      backupRate: new FormControl(values.backupRate),
      nonInvasiveInterface: new FormControl(values.nonInvasiveInterface, { nonNullable: true }),
      sessionHours: new FormControl(values.sessionHours),
      invasiveAirway: new FormControl(values.invasiveAirway, { nonNullable: true }),
      airwayDetail: new FormControl(values.airwayDetail, { nonNullable: true }),
      invasiveMode: new FormControl(values.invasiveMode, { nonNullable: true }),
      tidalVolumeMl: new FormControl(values.tidalVolumeMl),
      respiratoryRate: new FormControl(values.respiratoryRate),
      peepCmH2o: new FormControl(values.peepCmH2o),
      inspiratoryFlowLitersMinute: new FormControl(values.inspiratoryFlowLitersMinute),
      inspiratoryTimeSeconds: new FormControl(values.inspiratoryTimeSeconds),
      pauseSeconds: new FormControl(values.pauseSeconds),
      inspiratoryPressureCmH2o: new FormControl(values.inspiratoryPressureCmH2o),
      triggerSensitivity: new FormControl(values.triggerSensitivity),
      protectiveGoals: new FormControl(values.protectiveGoals, { nonNullable: true }),
    });
  }

  private draft(): VentilatorySupportDraft {
    return {
      selectedTemplate: this.selectedTemplate.value,
      items: this.items.controls
        .filter((group) => Boolean(group.controls.supportType.value))
        .map((group) => this.toDraftItem(group)),
    };
  }

  private toDraftItem(group: VentilatoryItemForm): VentilatorySupportItemDraft {
    const value = group.getRawValue();
    const common = {
      id: value.id,
      supportType: value.supportType,
      frequency: value.frequency,
      scheduling: value.scheduling,
    };
    return {
      ...common,
      lowFlow:
        value.supportType === 'LOW_FLOW'
          ? {
              device: value.lowFlowDevice,
              oxygenFlowLitersMinute: value.oxygenFlowLitersMinute,
              fio2Percent: value.fio2Percent,
            }
          : null,
      highFlow:
        value.supportType === 'HIGH_FLOW'
          ? {
              flowLitersMinute: value.highFlowLitersMinute,
              fio2Percent: value.fio2Percent,
              temperatureCelsius: value.temperatureCelsius,
              interfaceSize: value.highFlowInterfaceSize,
            }
          : null,
      nonInvasive:
        value.supportType === 'NIV'
          ? {
              mode: value.nonInvasiveMode,
              ipapCmH2o: value.ipapCmH2o,
              epapPeepCmH2o: value.epapPeepCmH2o,
              supportPressureCmH2o: value.supportPressureCmH2o,
              fio2Percent: value.fio2Percent,
              backupRate: value.backupRate,
              interfaceType: value.nonInvasiveInterface,
              sessionHours: value.sessionHours,
            }
          : null,
      invasive:
        value.supportType === 'IMV'
          ? {
              airway: value.invasiveAirway,
              airwayDetail: value.airwayDetail,
              mode: value.invasiveMode,
              tidalVolumeMl: value.tidalVolumeMl,
              respiratoryRate: value.respiratoryRate,
              peepCmH2o: value.peepCmH2o,
              fio2Percent: value.fio2Percent,
              inspiratoryFlowLitersMinute: value.inspiratoryFlowLitersMinute,
              inspiratoryTimeSeconds: value.inspiratoryTimeSeconds,
              pauseSeconds: value.pauseSeconds,
              inspiratoryPressureCmH2o: value.inspiratoryPressureCmH2o,
              supportPressureCmH2o: value.supportPressureCmH2o,
              triggerSensitivity: value.triggerSensitivity,
              protectiveGoals: value.protectiveGoals,
            }
          : null,
    };
  }

  private flatten(item: VentilatorySupportItemDraft) {
    const empty = this.emptyFlatItem(item.id);
    return {
      ...empty,
      id: item.id,
      supportType: item.supportType,
      frequency: item.frequency,
      scheduling: item.scheduling,
      lowFlowDevice: item.lowFlow?.device ?? '',
      oxygenFlowLitersMinute: item.lowFlow?.oxygenFlowLitersMinute ?? null,
      fio2Percent:
        item.lowFlow?.fio2Percent ??
        item.highFlow?.fio2Percent ??
        item.nonInvasive?.fio2Percent ??
        item.invasive?.fio2Percent ??
        null,
      highFlowLitersMinute: item.highFlow?.flowLitersMinute ?? null,
      temperatureCelsius: item.highFlow?.temperatureCelsius ?? null,
      highFlowInterfaceSize: item.highFlow?.interfaceSize ?? '',
      nonInvasiveMode: item.nonInvasive?.mode ?? '',
      ipapCmH2o: item.nonInvasive?.ipapCmH2o ?? null,
      epapPeepCmH2o: item.nonInvasive?.epapPeepCmH2o ?? null,
      supportPressureCmH2o:
        item.nonInvasive?.supportPressureCmH2o ?? item.invasive?.supportPressureCmH2o ?? null,
      backupRate: item.nonInvasive?.backupRate ?? null,
      nonInvasiveInterface: item.nonInvasive?.interfaceType ?? '',
      sessionHours: item.nonInvasive?.sessionHours ?? null,
      invasiveAirway: item.invasive?.airway ?? '',
      airwayDetail: item.invasive?.airwayDetail ?? '',
      invasiveMode: item.invasive?.mode ?? '',
      tidalVolumeMl: item.invasive?.tidalVolumeMl ?? null,
      respiratoryRate: item.invasive?.respiratoryRate ?? null,
      peepCmH2o: item.invasive?.peepCmH2o ?? null,
      inspiratoryFlowLitersMinute: item.invasive?.inspiratoryFlowLitersMinute ?? null,
      inspiratoryTimeSeconds: item.invasive?.inspiratoryTimeSeconds ?? null,
      pauseSeconds: item.invasive?.pauseSeconds ?? null,
      inspiratoryPressureCmH2o: item.invasive?.inspiratoryPressureCmH2o ?? null,
      triggerSensitivity: item.invasive?.triggerSensitivity ?? null,
      protectiveGoals: item.invasive?.protectiveGoals ?? [],
    };
  }

  private emptyFlatItem(id: number) {
    return {
      id,
      supportType: '',
      frequency: '',
      scheduling: '',
      lowFlowDevice: '',
      oxygenFlowLitersMinute: null,
      fio2Percent: null,
      highFlowLitersMinute: null,
      temperatureCelsius: null,
      highFlowInterfaceSize: '',
      nonInvasiveMode: '',
      ipapCmH2o: null,
      epapPeepCmH2o: null,
      supportPressureCmH2o: null,
      backupRate: null,
      nonInvasiveInterface: '',
      sessionHours: null,
      invasiveAirway: '',
      airwayDetail: '',
      invasiveMode: '',
      tidalVolumeMl: null,
      respiratoryRate: null,
      peepCmH2o: null,
      inspiratoryFlowLitersMinute: null,
      inspiratoryTimeSeconds: null,
      pauseSeconds: null,
      inspiratoryPressureCmH2o: null,
      triggerSensitivity: null,
      protectiveGoals: [] as string[],
    };
  }

  private invalidatePreview(): void {
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  private emitSelection(response: VentilatorySupportResponse | null): void {
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
    return 'NÃO FOI POSSÍVEL VALIDAR O SUPORTE VENTILATÓRIO.';
  }
}
