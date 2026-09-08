import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface Column {
  key: string;
  label: string;
  sortable?: boolean;
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="overflow-x-auto">
      <table class="table-responsive">
        <thead>
          <tr>
            @for (column of columns; track column.key) {
              <th [attr.aria-sort]="sortColumn === column.key ? (sortDir === 'asc' ? 'ascending' : 'descending') : 'none'">
                @if (column.sortable !== false) {
                  <button (click)="sort(column.key)" class="flex items-center hover:text-secondary transition">
                    {{ column.label }}
                    @if (sortColumn === column.key) {
                      {{ sortDir === 'asc' ? '▲' : '▼' }}
                    }
                  </button>
                } @else {
                  {{ column.label }}
                }
              </th>
            }
            @if (hasActions) {
              <th>Actions</th>
            }
          </tr>
        </thead>
        <tbody>
          @for (row of data; track row.id) {
            <tr>
              @for (column of columns; track column.key) {
                <td>{{ getNestedProperty(row, column.key) }}</td>
              }
              @if (hasActions) {
                <td>
                  <ng-content></ng-content>
                </td>
              }
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: []
})
export class DataTableComponent {
  @Input() columns: Column[] = [];
  @Input() data: any[] = [];
  @Input() hasActions = false;
  @Output() sortChange = new EventEmitter<{ column: string; direction: 'asc' | 'desc' }>();

  sortColumn = '';
  sortDir: 'asc' | 'desc' = 'asc';

  sort(column: string) {
    if (this.sortColumn === column) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDir = 'asc';
    }
    this.sortChange.emit({ column: this.sortColumn, direction: this.sortDir });
  }

  getNestedProperty(obj: any, path: string): any {
    return path.split('.').reduce((current, prop) => current?.[prop], obj);
  }
}
