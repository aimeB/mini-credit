import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';

@Component({
  selector: 'app-form-field',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="form-group">
      <label [for]="fieldId" class="label-required">{{ label }}</label>
      <input
        [id]="fieldId"
        [type]="type"
        [formControl]="control"
        [class.input-error]="control.invalid && control.touched"
        class="input-field"
        [placeholder]="placeholder"
      />
      <span class="text-danger text-sm mt-1 block" *ngIf="control.invalid && control.touched">{{ errorMessage }}</span>
    </div>
  `,
  styles: []
})
export class FormFieldComponent {
  @Input() label = '';
  @Input() type = 'text';
  @Input() placeholder = '';
  @Input() fieldId = '';
  @Input() control!: FormControl<any>;
  @Input() errorMessage = 'Ce champ est obligatoire';
}
