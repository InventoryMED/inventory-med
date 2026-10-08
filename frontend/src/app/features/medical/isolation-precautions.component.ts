import { CommonModule } from '@angular/common';
import { Component, DestroyRef, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import {
  IsolationPrecautionCatalog,
  IsolationPrecautionDraft,
  IsolationPrecautionItemDraft,
  IsolationPrecautionResponse,
  IsolationPrecautionSelection,
} from './medical.models';
import { MedicalService } from './medical.service';

interface IsolationPrecautionItemControls {
  id: FormControl<number>;
  precautionType: FormControl<string>;
  reasonPathogen: FormControl<string>;
  durationReview: FormControl<string>;
  scheduling: FormControl<string>;
}

type IsolationPrecautionItemForm = FormGroup<IsolationPrecautionItemControls>;

@Component({
  selector: 'app-isolation-precautions',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './isolation-precautions.component.html',
  styleUrl: './isolation-precautions.component.scss',
})
export class IsolationPrecautionsComponent implements OnInit {
  @Output() readonly selectionChange = new EventEmitter<IsolationPrecautionSelection>();

  private readonly medical = inject(MedicalService);
  private readonly destroyRef = inject(DestroyRef);
  private nextItemId = 1;

  protected readonly catalog = signal<IsolationPrecautionCatalog | null>(null);
  readonly preview = signal<IsolationPrecautionResponse | null>(null);
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly selectedTemplate = new FormControl('', { nonNullable: true });
  readonly items = new FormArray<IsolationPrecautionItemForm>([]);

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
    return this.items.controls.some((item) => Boolean(item.controls.precautionType.value));
  }

  reset(): void {
    this.items.clear();
    this.selectedTemplate.setValue('', { emitEvent: false });
    this.nextItemId = 1;
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  applyTemplate(code: string): void {
    this.selectedTemplate.setValue(code);
    const template = this.catalog()?.templates.find((item) => item.code === code);
    if (!template) return;
    this.items.clear();
    template.items.forEach((item) => this.items.push(this.createItemForm(item)));
    this.nextItemId = template.items.length + 1;
  }

  protected addItem(): void {
    if (this.items.length >= 8) return;
    this.items.push(this.createItemForm());
  }

  protected removeItem(index: number): void {
    this.items.removeAt(index);
  }

  protected async generatePreview(): Promise<boolean> {
    if (!this.hasSelection()) {
      this.error.set('SELECIONE AO MENOS UMA PRECAUÇÃO OU ISOLAMENTO.');
      return false;
    }
    this.busy.set(true);
    this.error.set(null);
    try {
      const result = await this.medical.previewIsolationPrecautions(this.draft());
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
      this.catalog.set(await this.medical.isolationPrecautionCatalog());
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.busy.set(false);
    }
  }

  private createItemForm(item?: IsolationPrecautionItemDraft): IsolationPrecautionItemForm {
    const value = item ?? this.emptyItem(this.nextItemId++);
    return new FormGroup<IsolationPrecautionItemControls>({
      id: new FormControl(value.id, { nonNullable: true }),
      precautionType: new FormControl(value.precautionType, { nonNullable: true }),
      reasonPathogen: new FormControl(value.reasonPathogen, { nonNullable: true }),
      durationReview: new FormControl(value.durationReview, { nonNullable: true }),
      scheduling: new FormControl(value.scheduling, { nonNullable: true }),
    });
  }

  private draft(): IsolationPrecautionDraft {
    return {
      selectedTemplate: this.selectedTemplate.value,
      items: this.items.controls
        .filter((group) => Boolean(group.controls.precautionType.value))
        .map((group) => group.getRawValue()),
    };
  }

  private emptyItem(id: number): IsolationPrecautionItemDraft {
    return {
      id,
      precautionType: '',
      reasonPathogen: '',
      durationReview: '',
      scheduling: '',
    };
  }

  private invalidatePreview(): void {
    this.preview.set(null);
    this.error.set(null);
    this.emitSelection(null);
  }

  private emitSelection(response: IsolationPrecautionResponse | null): void {
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
    return 'NÃO FOI POSSÍVEL VALIDAR AS PRECAUÇÕES E O ISOLAMENTO.';
  }
}
