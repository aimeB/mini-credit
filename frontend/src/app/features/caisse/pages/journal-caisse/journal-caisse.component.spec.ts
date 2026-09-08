import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { JournalCaisseComponent } from './journal-caisse.component';

describe('JournalCaisseComponent', () => {
  let component: JournalCaisseComponent;
  let fixture: ComponentFixture<JournalCaisseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JournalCaisseComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(JournalCaisseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
