import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnChanges, signal, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ClinicalFieldType,
  ClinicalFormField,
  ClinicalFormKind,
  ClinicalFormOption,
  ClinicalFormSection,
  ClinicalFormTemplate,
} from '../medical/medical.models';
import { AdministrationHospital } from './administration.models';
import { AdministrationService } from './administration.service';

@Component({
  selector: 'app-form-template-administration',
  imports: [CommonModule, FormsModule],
  templateUrl: './form-template-administration.component.html',
  styleUrl: './form-template-administration.component.scss',
})
export class FormTemplateAdministrationComponent implements OnChanges {
  @Input({ required: true }) hospitals: AdministrationHospital[] = [];

  private readonly administration = inject(AdministrationService);
  protected readonly templates = signal<ClinicalFormTemplate[]>([]);
  protected readonly selected = signal<ClinicalFormTemplate | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly feedback = signal<string | null>(null);
  protected readonly fieldTypes: Array<{ value: ClinicalFieldType; label: string }> = [
    { value: 'SHORT_TEXT', label: 'TEXTO CURTO' },
    { value: 'LONG_TEXT', label: 'TEXTO LONGO' },
    { value: 'INTEGER', label: 'NÚMERO INTEIRO' },
    { value: 'DECIMAL', label: 'NÚMERO DECIMAL' },
    { value: 'DATE', label: 'DATA' },
    { value: 'TIME', label: 'HORA' },
    { value: 'SINGLE_SELECT', label: 'SELEÇÃO ÚNICA' },
    { value: 'MULTI_SELECT', label: 'SELEÇÃO MÚLTIPLA' },
    { value: 'BOOLEAN', label: 'SIM / NÃO' },
    { value: 'MEDICATION_LINE', label: 'LINHA DE MEDICAÇÃO' },
    { value: 'CLINICAL_TABLE', label: 'TABELA CLÍNICA' },
  ];

  protected hospitalId = '';
  protected newTemplateName = '';
  protected newTemplateKind: ClinicalFormKind = 'PRESCRIPTION';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['hospitals'] && !this.hospitalId) {
      this.hospitalId = this.hospitals.find((hospital) => hospital.status === 'ACTIVE')?.id ?? '';
      if (this.hospitalId) void this.load();
    }
  }

  protected async load(): Promise<void> {
    if (!this.hospitalId) return;
    this.loading.set(true);
    this.clearMessages();
    try {
      const templates = await this.administration.formTemplates(this.hospitalId);
      this.templates.set(templates);
      this.selected.set(templates[0] ? structuredClone(templates[0]) : null);
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected choose(templateId: string): void {
    const template = this.templates().find((item) => item.templateId === templateId);
    this.selected.set(template ? structuredClone(template) : null);
    this.clearMessages();
  }

  protected async createTemplate(): Promise<void> {
    if (!this.newTemplateName.trim()) return;
    await this.change(async () => {
      const created = await this.administration.createFormTemplate(this.hospitalId, {
        name: this.newTemplateName,
        kind: this.newTemplateKind,
      });
      this.newTemplateName = '';
      await this.load();
      this.choose(created.templateId);
    }, 'MODELO CRIADO. ADICIONE AS SEÇÕES E OS CAMPOS.');
  }

  protected addSection(): void {
    const template = this.selected();
    if (!template || template.status !== 'DRAFT') return;
    const number = template.sections.length + 1;
    template.sections.push({
      id: crypto.randomUUID(),
      key: `SECAO_${number}`,
      title: `NOVA SEÇÃO ${number}`,
      displayOrder: number * 10,
      active: true,
      fields: [],
    });
  }

  protected removeSection(index: number): void {
    this.selected()?.sections.splice(index, 1);
    this.normalizeOrders(this.selected()?.sections ?? []);
  }

  protected addField(section: ClinicalFormSection): void {
    const number = section.fields.length + 1;
    section.fields.push({
      id: crypto.randomUUID(),
      key: `CAMPO_${number}`,
      label: `NOVO CAMPO ${number}`,
      type: 'SHORT_TEXT',
      required: false,
      displayOrder: number * 10,
      active: true,
      placeholder: null,
      maxLength: 500,
      options: [],
    });
  }

  protected removeField(section: ClinicalFormSection, index: number): void {
    section.fields.splice(index, 1);
    this.normalizeOrders(section.fields);
  }

  protected addOption(field: ClinicalFormField): void {
    const number = field.options.length + 1;
    field.options.push({
      id: crypto.randomUUID(),
      value: `OPCAO_${number}`,
      label: `OPÇÃO ${number}`,
      displayOrder: number * 10,
      active: true,
    });
  }

  protected removeOption(field: ClinicalFormField, index: number): void {
    field.options.splice(index, 1);
    this.normalizeOrders(field.options);
  }

  protected move<T extends { displayOrder: number }>(
    items: T[],
    index: number,
    offset: number,
  ): void {
    const target = index + offset;
    if (target < 0 || target >= items.length) return;
    [items[index], items[target]] = [items[target], items[index]];
    this.normalizeOrders(items);
  }

  protected selectionField(field: ClinicalFormField): boolean {
    return field.type === 'SINGLE_SELECT' || field.type === 'MULTI_SELECT';
  }

  protected async saveDraft(): Promise<void> {
    const template = this.selected();
    if (!template || template.status !== 'DRAFT') return;
    if (!template.sections.length || template.sections.some((section) => !section.fields.length)) {
      this.error.set('CADA MODELO PRECISA TER AO MENOS UMA SEÇÃO E UM CAMPO.');
      return;
    }
    await this.change(async () => {
      const saved = await this.administration.saveFormTemplateDraft(
        this.hospitalId,
        template.templateId,
        {
          name: template.name,
          sections: template.sections.map((section) => ({
            key: section.key,
            title: section.title,
            displayOrder: section.displayOrder,
            active: section.active,
            fields: section.fields.map((field) => ({
              key: field.key,
              label: field.label,
              type: field.type,
              required: field.required,
              displayOrder: field.displayOrder,
              active: field.active,
              placeholder: field.placeholder || null,
              maxLength: field.maxLength || null,
              options: this.selectionField(field)
                ? field.options.map((option) => ({
                    value: option.value,
                    label: option.label,
                    displayOrder: option.displayOrder,
                    active: option.active,
                  }))
                : [],
            })),
          })),
        },
      );
      await this.reloadSelected(saved.templateId);
    }, 'RASCUNHO SALVO.');
  }

  protected async publish(): Promise<void> {
    const template = this.selected();
    if (!template || template.status !== 'DRAFT') return;
    await this.change(async () => {
      const published = await this.administration.publishFormTemplate(
        this.hospitalId,
        template.templateId,
      );
      await this.reloadSelected(published.templateId);
    }, 'VERSÃO PUBLICADA PARA O HOSPITAL.');
  }

  protected async createVersion(): Promise<void> {
    const template = this.selected();
    if (!template) return;
    await this.change(async () => {
      const draft = await this.administration.createFormTemplateVersion(
        this.hospitalId,
        template.templateId,
      );
      await this.reloadSelected(draft.templateId);
    }, 'NOVA VERSÃO CRIADA EM RASCUNHO.');
  }

  protected async toggleTemplateActive(): Promise<void> {
    const template = this.selected();
    if (!template) return;
    await this.change(
      async () => {
        const updated = await this.administration.setFormTemplateActive(
          this.hospitalId,
          template.templateId,
          !template.active,
        );
        await this.reloadSelected(updated.templateId);
      },
      template.active ? 'MODELO DESATIVADO.' : 'MODELO ATIVADO.',
    );
  }

  private async reloadSelected(templateId: string): Promise<void> {
    const templates = await this.administration.formTemplates(this.hospitalId);
    this.templates.set(templates);
    this.choose(templateId);
  }

  private normalizeOrders<T extends { displayOrder: number }>(items: T[]): void {
    items.forEach((item, index) => (item.displayOrder = (index + 1) * 10));
  }

  private async change(operation: () => Promise<void>, feedback: string): Promise<void> {
    this.saving.set(true);
    this.clearMessages();
    try {
      await operation();
      this.feedback.set(feedback);
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.saving.set(false);
    }
  }

  private clearMessages(): void {
    this.error.set(null);
    this.feedback.set(null);
  }

  private errorMessage(error: unknown): string {
    if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
      return error.error.message.toLocaleUpperCase('pt-BR');
    }
    return 'NÃO FOI POSSÍVEL CONCLUIR A OPERAÇÃO.';
  }
}
