import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkflowGuidanceBannerComponent } from './workflow-guidance-banner.component';
import { WorkflowGuidance } from '../../models/workflow-guidance.model';

describe('WorkflowGuidanceBannerComponent', () => {
  let fixture: ComponentFixture<WorkflowGuidanceBannerComponent>;
  let component: WorkflowGuidanceBannerComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkflowGuidanceBannerComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(WorkflowGuidanceBannerComponent);
    component = fixture.componentInstance;
  });

  it('affiche titre, message, étape courante, prochaine étape, rôle attendu et blocage', () => {
    const guidance: WorkflowGuidance = {
      title: 'Analyse contrôleur requise',
      message: 'Pré-analyse terminée. Le Contrôleur doit maintenant analyser le dossier.',
      currentStep: 'Analyse Contrôleur',
      nextStep: 'Garantie',
      expectedRole: 'Contrôleur',
      expectedAction: 'Analyser le dossier',
      severity: 'warning',
      canCurrentUserAct: false,
      blockedReason: 'Vous ne pouvez pas agir à cette étape. Cette action est réservée au contrôleur.'
    };

    component.guidance = guidance;
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Analyse contrôleur requise');
    expect(text).toContain('Pré-analyse terminée');
    expect(text).toContain('Analyse Contrôleur');
    expect(text).toContain('Garantie');
    expect(text).toContain('Contrôleur');
    expect(text).toContain('Vous ne pouvez pas agir à cette étape');
    expect(text).toContain('réservée au contrôleur');
  });

  it('affiche le message positif quand l’utilisateur peut agir', () => {
    const guidance: WorkflowGuidance = {
      title: 'Décaissement attendu',
      message: 'Crédit approuvé. Le Caissier doit maintenant effectuer le décaissement.',
      currentStep: 'Décaissement Caissier',
      nextStep: 'Remboursement',
      expectedRole: 'Caissier',
      expectedAction: 'Procéder au décaissement',
      severity: 'success',
      canCurrentUserAct: true
    };

    component.guidance = guidance;
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Vous pouvez agir');
    expect(text).toContain('Procéder au décaissement');
    expect(text).not.toContain('Vous ne pouvez pas agir à cette étape');
  });
});
