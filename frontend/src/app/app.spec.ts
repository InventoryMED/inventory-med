import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { vi } from 'vitest';
import { App } from './app';
import { AuthService } from './auth/auth.service';
import { INITIAL_HOSPITALS } from './mock-data';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        {
          provide: AuthService,
          useValue: {
            user: signal(null),
            hospitals: signal([]),
            selectedHospitalId: signal(null),
            selectedHospital: signal(null),
            isAuthenticated: signal(false),
            validateSession: vi.fn().mockResolvedValue(false),
            login: vi.fn(),
            selectHospital: vi.fn(),
            logout: vi.fn().mockResolvedValue(undefined),
          },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the login experience', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.brand-logo img')?.getAttribute('src')).toBe(
      'inventory-med-logo.png',
    );
    expect(compiled.querySelector('h1')?.textContent).toContain('Leitos organizados');
    expect(compiled.querySelector('button[type="submit"]')?.textContent).toContain('ENTRAR');
  });

  it('should start with the requested hospitals and no registered patients', () => {
    expect(INITIAL_HOSPITALS.map((hospital) => hospital.name)).toEqual([
      'UPA DE JOÃO PINHEIRO',
      'HOSPITAL DE JOÃO PINHEIRO',
    ]);

    const beds = INITIAL_HOSPITALS.flatMap((hospital) =>
      hospital.rooms.flatMap((room) => room.beds),
    );
    expect(beds.some((bed) => bed.status === 'OCCUPIED' || bed.patient)).toBe(false);
  });

  it('should initialize the structured medication sections', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as any;

    app.startBlankPrescription();

    expect(app.medicationGroups.map((group: any) => group.title)).toEqual([
      'ANALGESIA',
      'SINTOMÁTICOS',
      'PROFILAXIA',
      'ATB',
      'MEDICAÇÕES DE USO CONTÍNUO',
      'DEMAIS MEDICAMENTOS',
    ]);
    const prophylaxis = app.medicationGroups.find((group: any) => group.id === 'PROPHYLAXIS');
    expect(prophylaxis.rows[0].description).toContain('OMEPRAZOL');
    expect(app.vitalSignRows[1].guidance).toContain('GH 50% 40ML EV');
    expect(app.observationRows).toEqual(['']);
    expect(app.abnormalityRows).toEqual(['']);
  });

  it('should fill a medication row from a preset and allow another row', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as any;
    app.startBlankPrescription();
    const symptomatics = app.medicationGroups.find((group: any) => group.id === 'SYMPTOMATICS');

    app.selectMedicationPreset(symptomatics, 'ONDANSETRONA 1 AMPOLA + 100ML DE SF 0,9%');
    app.addMedicationRow(symptomatics);

    expect(symptomatics.rows[0]).toMatchObject({
      route: 'EV',
      frequency: '8/8HR',
      scheduling: 'SN',
    });
    expect(symptomatics.rows).toHaveLength(2);
    expect(app.printOrderRows()).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          section: 'SINTOMÁTICOS',
          description: 'ONDANSETRONA 1 AMPOLA + 100ML DE SF 0,9%',
        }),
      ]),
    );
    expect(app.printHours).toHaveLength(24);
  });

  it('should admit a patient without opening the prescription automatically', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as any;
    const store = app.store;
    store.resetDemo();
    store.selectHospital(INITIAL_HOSPITALS[0].id);
    const bed = store.activeHospital().rooms[0].beds[0];
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

    app.selectedBedId.set(bed.id);
    app.patientName = 'Paciente demonstração';
    app.admitPatient();

    expect(openSpy).not.toHaveBeenCalled();
    expect(store.findBed(bed.id)).toMatchObject({
      status: 'OCCUPIED',
      patient: { name: 'PACIENTE DEMONSTRAÇÃO' },
    });
    openSpy.mockRestore();
  });

  it('should transfer and discharge an admitted patient', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as any;
    const store = app.store;
    store.resetDemo();
    store.selectHospital(INITIAL_HOSPITALS[0].id);
    const [sourceBed, targetBed] = store.activeHospital().rooms[0].beds;

    store.admitPatient(sourceBed.id, { name: 'Paciente demonstração' });
    expect(store.transferPatient(sourceBed.id, targetBed.id)).toBe(true);
    expect(store.findBed(sourceBed.id).status).toBe('AVAILABLE');
    expect(store.findBed(targetBed.id).patient.name).toBe('PACIENTE DEMONSTRAÇÃO');

    expect(store.dischargePatient(targetBed.id, 'ALTA MELHORA')).toBe('PACIENTE DEMONSTRAÇÃO');
    expect(store.findBed(targetBed.id)).toMatchObject({
      status: 'AVAILABLE',
      lastDischarge: {
        patientName: 'PACIENTE DEMONSTRAÇÃO',
        reason: 'ALTA MELHORA',
      },
    });
  });

  it('should open and initialize a blank medical evolution', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance as any;
    const store = app.store;
    store.resetDemo();
    store.selectHospital(INITIAL_HOSPITALS[0].id);
    const bed = store.activeHospital().rooms[0].beds[0];
    store.admitPatient(bed.id, { name: 'Paciente evolução' });
    const occupiedBed = store.findBed(bed.id);
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null);

    app.startEvolution(occupiedBed);

    expect(openSpy).toHaveBeenCalledWith(
      expect.stringContaining('flow=evolution'),
      '_blank',
      'noopener',
    );

    app.prepareEvolution(occupiedBed.patient);
    expect(app.evolutionAdmission).toBe('');
    expect(app.evolutionText).toBe('');
    expect(app.evolutionPosition).toBe('');
    expect(app.evolutionChiefComplaints).toEqual([]);
    expect(app.evolutionFoodAcceptance).toBe('');
    expect(app.evolutionSleepPattern).toBe('');
    expect(app.evolutionVitalSaturation).toBe('');
    expect(app.evolutionGeneralState).toBe('');
    expect(app.evolutionNeurological).toBe('');
    expect(app.evolutionUpperLimbPerfusion).toBe('');
    expect(app.evolutionComplementaryNotes).toBe('');
    expect(app.evolutionConduct).toBe('');
    expect(app.evolutionAntibioticCurrent).toBe('');
    expect(app.evolutionAntibioticPrevious).toBe('');
    expect(app.evolutionExamRows).toHaveLength(8);
    expect(app.evolutionExamDates).toEqual(['', '', '', '']);
    expect(app.formatEvolutionConduct('MANTER HIDRATAÇÃO\n- SOLICITAR EXAMES')).toBe(
      '- MANTER HIDRATAÇÃO\n- SOLICITAR EXAMES',
    );
    app.toggleEvolutionSelection(app.evolutionChiefComplaints, 'DOR', true);
    app.evolutionPainLocation = 'ABDOME';
    app.evolutionPainIntensity = '7';
    expect(app.formattedEvolutionChiefComplaints()).toBe('DOR EM ABDOME (INTENSIDADE 7/10)');

    app.selectedBedId.set(occupiedBed.id);
    app.screen.set('evolution');
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.evolution-form-card h1')?.textContent).toContain(
      'Evolução médica',
    );
    expect(compiled.querySelector('.evolution-print-sheet')).toBeTruthy();
    openSpy.mockRestore();
  });
});
