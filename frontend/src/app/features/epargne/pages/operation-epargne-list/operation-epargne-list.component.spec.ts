import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { OperationEpargneListComponent } from './operation-epargne-list.component';

describe('OperationEpargneListComponent', () => {
  let component: OperationEpargneListComponent;
  let fixture: ComponentFixture<OperationEpargneListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OperationEpargneListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OperationEpargneListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
