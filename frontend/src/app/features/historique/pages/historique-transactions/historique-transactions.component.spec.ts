import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HistoriqueTransactionsComponent } from './historique-transactions.component';

describe('HistoriqueTransactionsComponent', () => {
  let component: HistoriqueTransactionsComponent;
  let fixture: ComponentFixture<HistoriqueTransactionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistoriqueTransactionsComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(HistoriqueTransactionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize data on component init', () => {
    expect(component.comptes.length).toBeGreaterThan(0);
    expect(component.transactions.length).toBeGreaterThan(0);
  });

  it('should select a compte', () => {
    const compte = component.comptes[0];
    component.selectCompte(compte);
    expect(component.selectedCompte).toEqual(compte);
  });

  it('should change tab', () => {
    component.changeTab('annexes');
    expect(component.activeTab).toBe('annexes');
  });

  it('should format date correctly', () => {
    const date = new Date('2026-05-18');
    const formatted = component.formatDate(date);
    expect(formatted).toBe('18/05/2026');
  });

  it('should filter transactions by montant', () => {
    component.searchCriteria.montantMin = 25;
    component.applyFilters();
    expect(component.filteredTransactions.every(t => Math.abs(t.montant) >= 25)).toBeTruthy();
  });

  it('should reset filters', () => {
    component.searchCriteria.montantMin = 25;
    component.searchCriteria.dateFin = '2026-05-18';
    component.resetFilters();
    expect(component.searchCriteria.montantMin).toBeNull();
    expect(component.searchCriteria.dateFin).toBeNull();
  });

  it('should format montant with sign', () => {
    expect(component.formatMontant(25.50)).toBe('+25.50');
    expect(component.formatMontant(-25.50)).toBe('-25.50');
  });

  it('should return correct status class', () => {
    expect(component.getStatusClass('COMPLETED')).toBe('status-completed');
    expect(component.getStatusClass('PENDING')).toBe('status-pending');
    expect(component.getStatusClass('FAILED')).toBe('status-failed');
  });
});
