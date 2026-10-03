import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, Input, OnChanges, signal, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AdministrationBed,
  AdministrationCareUnit,
  AdministrationHospital,
  AdministrationRoom,
  AdministrationStructure,
} from './administration.models';
import { AdministrationService } from './administration.service';

@Component({
  selector: 'app-structure-administration',
  imports: [CommonModule, FormsModule],
  templateUrl: './structure-administration.component.html',
  styleUrl: './structure-administration.component.scss',
})
export class StructureAdministrationComponent implements OnChanges {
  @Input({ required: true }) hospitals: AdministrationHospital[] = [];

  private readonly administration = inject(AdministrationService);
  protected readonly structure = signal<AdministrationStructure>({ careUnits: [] });
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly feedback = signal<string | null>(null);

  protected hospitalId = '';
  protected newUnit = { name: '', code: '', displayOrder: 0, active: true };
  protected newRoom = {
    careUnitId: '',
    name: '',
    code: '',
    floorName: '',
    displayOrder: 0,
    active: true,
  };
  protected newBed = {
    roomId: '',
    code: '',
    status: 'AVAILABLE',
    displayOrder: 0,
    active: true,
  };

  protected get rooms(): AdministrationRoom[] {
    return this.structure().careUnits.flatMap((unit) => unit.rooms);
  }

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
      this.structure.set(await this.administration.structure(this.hospitalId));
    } catch (error) {
      this.error.set(this.errorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected async createUnit(): Promise<void> {
    if (!this.newUnit.name.trim() || !this.newUnit.code.trim()) return;
    await this.change(
      () => this.administration.createCareUnit(this.hospitalId, this.newUnit),
      'UNIDADE DE INTERNAÇÃO CRIADA.',
    );
    this.newUnit = { name: '', code: '', displayOrder: 0, active: true };
  }

  protected async saveUnit(unit: AdministrationCareUnit): Promise<void> {
    await this.change(
      () =>
        this.administration.updateCareUnit(this.hospitalId, unit.id, {
          name: unit.name,
          code: unit.code,
          displayOrder: unit.displayOrder,
          active: unit.active,
        }),
      'UNIDADE ATUALIZADA.',
    );
  }

  protected async createRoom(): Promise<void> {
    if (!this.newRoom.careUnitId || !this.newRoom.name.trim() || !this.newRoom.code.trim()) return;
    await this.change(
      () => this.administration.createRoom(this.hospitalId, this.newRoom),
      'QUARTO CRIADO.',
    );
    this.newRoom = {
      careUnitId: '',
      name: '',
      code: '',
      floorName: '',
      displayOrder: 0,
      active: true,
    };
  }

  protected async saveRoom(room: AdministrationRoom): Promise<void> {
    await this.change(
      () =>
        this.administration.updateRoom(this.hospitalId, room.id, {
          careUnitId: room.careUnitId,
          name: room.name,
          code: room.code,
          floorName: room.floorName,
          displayOrder: room.displayOrder,
          active: room.active,
        }),
      'QUARTO ATUALIZADO.',
    );
  }

  protected async createBed(): Promise<void> {
    if (!this.newBed.roomId || !this.newBed.code.trim()) return;
    await this.change(
      () => this.administration.createBed(this.hospitalId, this.newBed),
      'LEITO CRIADO.',
    );
    this.newBed = {
      roomId: '',
      code: '',
      status: 'AVAILABLE',
      displayOrder: 0,
      active: true,
    };
  }

  protected async saveBed(bed: AdministrationBed): Promise<void> {
    await this.change(
      () =>
        this.administration.updateBed(this.hospitalId, bed.id, {
          roomId: bed.roomId,
          code: bed.code,
          status: bed.status,
          displayOrder: bed.displayOrder,
          active: bed.active,
        }),
      'LEITO ATUALIZADO.',
    );
  }

  private async change(operation: () => Promise<unknown>, feedback: string): Promise<void> {
    this.saving.set(true);
    this.clearMessages();
    try {
      await operation();
      await this.load();
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
