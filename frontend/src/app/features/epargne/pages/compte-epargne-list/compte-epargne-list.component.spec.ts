import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { CompteEpargneListComponent } from './compte-epargne-list.component';

describe('CompteEpargneListComponent', () => {
  let component: CompteEpargneListComponent;
  let fixture: ComponentFixture<CompteEpargneListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompteEpargneListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CompteEpargneListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
