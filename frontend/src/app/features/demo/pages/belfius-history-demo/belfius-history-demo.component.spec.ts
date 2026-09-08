import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BelfiusHistoryDemoComponent } from './belfius-history-demo.component';

describe('BelfiusHistoryDemoComponent', () => {
  let component: BelfiusHistoryDemoComponent;
  let fixture: ComponentFixture<BelfiusHistoryDemoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BelfiusHistoryDemoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BelfiusHistoryDemoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
