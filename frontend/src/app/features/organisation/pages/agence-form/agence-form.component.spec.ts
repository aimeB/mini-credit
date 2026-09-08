import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { AgenceFormComponent } from './agence-form.component';
import { AgenceService } from '../../../employes/services/agence.service';

describe('AgenceFormComponent — Validation et sécurité des champs', () => {
  let component: AgenceFormComponent;
  const agenceServiceMock = {
    create: jasmine.createSpy('create').and.returnValue(of({ id: 1, codeAgence: 'KIN001', nomAgence: 'Test', actif: true })),
    update: jasmine.createSpy('update').and.returnValue(of({ id: 1, codeAgence: 'KIN001', nomAgence: 'Test', actif: true })),
    getById: jasmine.createSpy('getById').and.returnValue(of({ id: 1, codeAgence: 'KIN001', nomAgence: 'Test', actif: true }))
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AgenceFormComponent, ReactiveFormsModule, RouterTestingModule],
      providers: [{ provide: AgenceService, useValue: agenceServiceMock }]
    }).compileComponents();

    const fixture = TestBed.createComponent(AgenceFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    agenceServiceMock.create.calls.reset();
    agenceServiceMock.update.calls.reset();
  });

  // ──────────────────────────────────────────────────────────────────────
  // Helpers
  // ──────────────────────────────────────────────────────────────────────

  function fillValidForm(): void {
    component.form.setValue({
      codeAgence:  'KIN001',
      nomAgence:   'Agence Centrale',
      ville:       'Kinshasa',
      commune:     'Gombe',
      quartier:    'Centre-Ville',
      adresse:     'Avenue de l\'Equateur 12',
      reference:   '',
      telephone:   '',
      actif:       true
    });
  }

  // ──────────────────────────────────────────────────────────────────────
  // agenceForm_shouldRequireVille
  // ──────────────────────────────────────────────────────────────────────

  it('agenceForm_shouldRequireVille — formulaire invalide si ville vide', () => {
    fillValidForm();
    component.form.get('ville')!.setValue('');
    expect(component.form.get('ville')!.valid).toBeFalse();
    expect(component.form.get('ville')!.hasError('required')).toBeTrue();
  });

  it('agenceForm_shouldRequireVille — formulaire valide si ville renseignée', () => {
    fillValidForm();
    expect(component.form.get('ville')!.valid).toBeTrue();
  });

  // ──────────────────────────────────────────────────────────────────────
  // agenceForm_shouldRequireCommune
  // ──────────────────────────────────────────────────────────────────────

  it('agenceForm_shouldRequireCommune — formulaire invalide si commune vide', () => {
    fillValidForm();
    component.form.get('commune')!.setValue('');
    expect(component.form.get('commune')!.valid).toBeFalse();
    expect(component.form.get('commune')!.hasError('required')).toBeTrue();
  });

  it('agenceForm_shouldRequireCommune — formulaire valide si commune renseignée', () => {
    fillValidForm();
    expect(component.form.get('commune')!.valid).toBeTrue();
  });

  // ──────────────────────────────────────────────────────────────────────
  // agenceForm_shouldRequireQuartier
  // ──────────────────────────────────────────────────────────────────────

  it('agenceForm_shouldRequireQuartier — quartier requis en creation', () => {
    fillValidForm();
    component.form.get('quartier')!.setValue('');
    expect(component.form.get('quartier')!.valid).toBeFalse();
    expect(component.form.get('quartier')!.hasError('required')).toBeTrue();
  });

  // ──────────────────────────────────────────────────────────────────────
  // agenceForm_shouldRequireAdresse
  // ──────────────────────────────────────────────────────────────────────

  it('agenceForm_shouldRequireAdresse — adresse requise en creation', () => {
    fillValidForm();
    component.form.get('adresse')!.setValue('');
    expect(component.form.get('adresse')!.valid).toBeFalse();
    expect(component.form.get('adresse')!.hasError('required')).toBeTrue();
  });

  // ──────────────────────────────────────────────────────────────────────
  // agenceForm_shouldNotDisplayEmail
  // ──────────────────────────────────────────────────────────────────────

  it('agenceForm_shouldNotDisplayEmail — le FormGroup ne doit pas avoir de contrôle email', () => {
    expect(component.form.contains('email')).toBeFalse();
  });

  it('agenceForm_shouldNotDisplayEmail — référence est un champ optionnel (null accepté)', () => {
    fillValidForm();
    component.form.get('reference')!.setValue('');
    // Le formulaire reste valide même avec référence vide
    expect(component.form.get('reference')!.valid).toBeTrue();
  });

  // ──────────────────────────────────────────────────────────────────────
  // Formulaire complet valide
  // ──────────────────────────────────────────────────────────────────────

  it('formulaire complet avec tous les champs obligatoires — doit être valide', () => {
    fillValidForm();
    expect(component.form.valid).toBeTrue();
  });

  it('formulaire sans code agence — doit être invalide', () => {
    fillValidForm();
    component.form.get('codeAgence')!.setValue('');
    expect(component.form.valid).toBeFalse();
  });

  it('payloadAgenceEnMajuscules — création trim et transforme nomAgence', () => {
    fillValidForm();
    component.form.get('nomAgence')!.setValue('  sacombi  ');

    component.enregistrer();

    expect(agenceServiceMock.create).toHaveBeenCalled();
    expect(agenceServiceMock.create.calls.mostRecent().args[0].nomAgence).toBe('SACOMBI');
  });
});
