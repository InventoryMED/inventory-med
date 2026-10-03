import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, EventEmitter, inject, OnInit, Output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../auth/auth.service';
import { AdministrationHospital, AdministrationUser } from './administration.models';
import { AdministrationService } from './administration.service';

type AdministrationTab = 'HOSPITALS' | 'USERS';
type UserScope = 'SYSTEM' | 'HOSPITAL';

@Component({
  selector: 'app-administration',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './administration.component.html',
  styleUrl: './administration.component.scss',
})
export class AdministrationComponent implements OnInit {
  @Output() readonly exit = new EventEmitter<void>();

  private readonly administration = inject(AdministrationService);
  private readonly formBuilder = inject(FormBuilder).nonNullable;
  protected readonly auth = inject(AuthService);
  protected readonly activeTab = signal<AdministrationTab>('HOSPITALS');
  protected readonly hospitals = signal<AdministrationHospital[]>([]);
  protected readonly users = signal<AdministrationUser[]>([]);
  protected readonly loading = signal(true);
  protected readonly savingHospital = signal(false);
  protected readonly provisioningHospitalId = signal<string | null>(null);
  protected readonly savingUser = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly feedback = signal<string | null>(null);

  protected readonly activeHospitals = computed(
    () => this.hospitals().filter((hospital) => hospital.status === 'ACTIVE').length,
  );
  protected readonly activeUsers = computed(
    () => this.users().filter((user) => user.active).length,
  );
  protected readonly systemAdministrators = computed(
    () => this.users().filter((user) => user.systemRoles.includes('ADMIN_SISTEMA')).length,
  );

  protected readonly hospitalForm = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(180)]],
    shortName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(40)]],
    city: ['', [Validators.required, Validators.maxLength(120)]],
  });

  protected readonly userForm = this.formBuilder.group({
    fullName: ['', [Validators.required, Validators.maxLength(160)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    initialPassword: [
      '',
      [Validators.required, Validators.minLength(12), Validators.maxLength(128)],
    ],
    scope: ['HOSPITAL' as UserScope, Validators.required],
    hospitalId: [''],
    hospitalRole: ['MEDICO'],
  });

  ngOnInit(): void {
    void this.loadData();
  }

  protected selectTab(tab: AdministrationTab): void {
    this.activeTab.set(tab);
    this.clearMessages();
  }

  protected async createHospital(): Promise<void> {
    if (this.hospitalForm.invalid) {
      this.hospitalForm.markAllAsTouched();
      return;
    }
    this.savingHospital.set(true);
    this.clearMessages();
    try {
      const created = await this.administration.createHospital(this.hospitalForm.getRawValue());
      this.hospitals.update((hospitals) =>
        [...hospitals, created].sort((first, second) => first.name.localeCompare(second.name)),
      );
      this.hospitalForm.reset({ name: '', shortName: '', city: '' });
      this.feedback.set('HOSPITAL CRIADO E BANCO EXCLUSIVO PROVISIONADO.');
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.savingHospital.set(false);
    }
  }

  protected async createUser(): Promise<void> {
    const formValue = this.userForm.getRawValue();
    if (formValue.scope === 'HOSPITAL' && !formValue.hospitalId) {
      this.error.set('SELECIONE O HOSPITAL DO USUÁRIO.');
      return;
    }
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    this.savingUser.set(true);
    this.clearMessages();
    try {
      const created = await this.administration.createUser({
        fullName: formValue.fullName,
        email: formValue.email,
        initialPassword: formValue.initialPassword,
        systemRoles: formValue.scope === 'SYSTEM' ? ['ADMIN_SISTEMA'] : [],
        hospitalAssignments:
          formValue.scope === 'HOSPITAL'
            ? [{ hospitalId: formValue.hospitalId, role: formValue.hospitalRole }]
            : [],
      });
      this.users.update((users) =>
        [...users, created].sort((first, second) => first.name.localeCompare(second.name)),
      );
      this.userForm.reset({
        fullName: '',
        email: '',
        initialPassword: '',
        scope: 'HOSPITAL',
        hospitalId: '',
        hospitalRole: 'MEDICO',
      });
      this.feedback.set('USUÁRIO CRIADO. A TROCA DA SENHA INICIAL SERÁ OBRIGATÓRIA.');
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.savingUser.set(false);
    }
  }

  protected async retryHospitalProvisioning(hospital: AdministrationHospital): Promise<void> {
    this.provisioningHospitalId.set(hospital.id);
    this.clearMessages();
    try {
      const updated = await this.administration.retryHospitalProvisioning(hospital.id);
      this.hospitals.update((hospitals) =>
        hospitals.map((item) => (item.id === updated.id ? updated : item)),
      );
      this.feedback.set('BANCO EXCLUSIVO DO HOSPITAL PROVISIONADO COM SUCESSO.');
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.provisioningHospitalId.set(null);
    }
  }

  protected statusLabel(status: AdministrationHospital['status']): string {
    return {
      ACTIVE: 'ATIVO',
      PROVISIONING: 'PROVISIONANDO',
      SUSPENDED: 'SUSPENSO',
      PROVISIONING_FAILED: 'FALHA NO PROVISIONAMENTO',
    }[status];
  }

  protected roleLabel(role: string): string {
    return (
      {
        ADMIN_SISTEMA: 'ADMINISTRADOR GERAL',
        ADMIN_HOSPITAL: 'ADMINISTRADOR HOSPITALAR',
        RESPONSAVEL_CLINICO: 'RESPONSÁVEL CLÍNICO',
        MEDICO: 'MÉDICO',
        ENFERMAGEM: 'ENFERMAGEM',
        RECEPCAO: 'RECEPÇÃO',
      }[role] ?? role
    );
  }

  protected userAccessLabel(user: AdministrationUser): string {
    const systemRoles = user.systemRoles.map((role) => this.roleLabel(role));
    const hospitalRoles = user.hospitals.map(
      (membership) => `${membership.hospitalName} — ${this.roleLabel(membership.role)}`,
    );
    return [...systemRoles, ...hospitalRoles].join(' • ');
  }

  private async loadData(): Promise<void> {
    this.loading.set(true);
    this.clearMessages();
    try {
      const [hospitals, users] = await Promise.all([
        this.administration.listHospitals(),
        this.administration.listUsers(),
      ]);
      this.hospitals.set(hospitals);
      this.users.set(users);
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
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
