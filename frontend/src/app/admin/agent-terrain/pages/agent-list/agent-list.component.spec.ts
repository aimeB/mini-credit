import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

import { AgentListComponent } from './agent-list.component';
import { AdminAgentTerrainService } from '../../services/admin-agent-terrain.service';

describe('AgentListComponent', () => {
  let component: AgentListComponent;
  let fixture: ComponentFixture<AgentListComponent>;
  let serviceSpy: jasmine.SpyObj<AdminAgentTerrainService>;

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj<AdminAgentTerrainService>('AdminAgentTerrainService', ['getAll', 'delete']);
    serviceSpy.getAll.and.returnValue(of([]));
    serviceSpy.delete.and.returnValue(of(void 0));

    await TestBed.configureTestingModule({
      imports: [AgentListComponent],
      providers: [
        provideRouter([]),
        { provide: AdminAgentTerrainService, useValue: serviceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AgentListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('affiche la guidance lot 7', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Gestion des agents terrain');
  });
});
