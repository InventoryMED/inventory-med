import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProvisionalAdmission } from '../../models';
import { ProvisionalAdmissionsComponent } from './provisional-admissions.component';

describe('ProvisionalAdmissionsComponent', () => {
  let fixture: ComponentFixture<ProvisionalAdmissionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProvisionalAdmissionsComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(ProvisionalAdmissionsComponent);
  });

  it('shows the waiting patient and emits the allocation and exam actions', () => {
    const admission: ProvisionalAdmission = {
      id: 'admission-1',
      admittedAt: '2026-10-13T12:00:00Z',
      careUnitId: 'unit-1',
      careUnitName: 'CLÍNICA MÉDICA',
      origin: 'PRONTO SOCORRO / TRIAGEM',
      patient: {
        id: 'patient-1',
        admissionId: 'admission-1',
        name: 'MARIA DAS DORES',
        admissionAt: '2026-10-13T12:00:00Z',
        prescriptions: [],
      },
    };
    fixture.componentRef.setInput('admissions', [admission]);
    const emitted: ProvisionalAdmission[] = [];
    const examEmitted: ProvisionalAdmission[] = [];
    fixture.componentInstance.allocate.subscribe((value) => emitted.push(value));
    fixture.componentInstance.exams.subscribe((value) => examEmitted.push(value));
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('MARIA DAS DORES');
    expect(element.querySelector('.patient-detail')).toBeNull();
    element.querySelector<HTMLButtonElement>('.patient-summary')?.click();
    fixture.detectChanges();
    expect(element.textContent?.toLocaleUpperCase('pt-BR')).toContain('SEM LEITO ALOCADO');
    const allocateButton = Array.from(element.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('ALOCAR'),
    );
    allocateButton?.click();
    expect(emitted).toEqual([admission]);
    const examsButton = Array.from(element.querySelectorAll('button')).find((button) =>
      button.textContent?.includes('EXAMES'),
    );
    examsButton?.click();
    expect(examEmitted).toEqual([admission]);
  });

  it('keeps a single compact patient detail open and filters the waiting list', () => {
    const admissions: ProvisionalAdmission[] = [
      {
        id: 'admission-1',
        admittedAt: '2026-10-13T12:00:00Z',
        careUnitId: 'unit-1',
        careUnitName: 'CLÍNICA MÉDICA',
        patient: {
          id: 'patient-1',
          admissionId: 'admission-1',
          name: 'MARIA DAS DORES',
          admissionAt: '2026-10-13T12:00:00Z',
          prescriptions: [],
        },
      },
      {
        id: 'admission-2',
        admittedAt: '2026-10-13T13:00:00Z',
        careUnitId: 'unit-2',
        careUnitName: 'OBSERVAÇÃO',
        patient: {
          id: 'patient-2',
          admissionId: 'admission-2',
          name: 'JOÃO PEREIRA',
          admissionAt: '2026-10-13T13:00:00Z',
          prescriptions: [],
        },
      },
    ];
    fixture.componentRef.setInput('admissions', admissions);
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const patientButtons = element.querySelectorAll<HTMLButtonElement>('.patient-summary');
    patientButtons[0].click();
    patientButtons[1].click();
    fixture.detectChanges();
    expect(element.querySelectorAll('.patient-detail')).toHaveLength(1);
    expect(element.querySelector('.waiting-entry--selected')?.textContent).toContain(
      'JOÃO PEREIRA',
    );

    const search = element.querySelector<HTMLInputElement>('.waiting-search input');
    if (!search) throw new Error('Campo de busca não renderizado.');
    search.value = 'Maria';
    search.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(element.querySelectorAll('.waiting-entry')).toHaveLength(1);
    expect(element.textContent).toContain('MARIA DAS DORES');
  });
});
