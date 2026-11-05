import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { Permission } from '../../core/models';

export interface PermissionActionsCellRendererParams extends ICellRendererParams<Permission> {
  onEdit: (permission: Permission) => void;
  onDelete: (permission: Permission) => void;
}

/** AG Grid cell renderer for the Permissions table's "Edit"/"Delete" action column. */
@Component({
  selector: 'app-permission-actions-cell-renderer',
  template: `
    <button type="button" class="edit-btn" (click)="edit()">Edit</button>
    <button type="button" class="delete-btn" (click)="delete()">Delete</button>
  `,
  styles: [
    `
      button {
        border-radius: 6px;
        padding: 0.25rem 0.65rem;
        font-size: 0.8rem;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.12s ease, border-color 0.12s ease;
        margin-right: 0.4rem;
      }
      .edit-btn {
        background: transparent;
        color: #60a5fa;
        border: 1px solid #60a5fa55;
      }
      .edit-btn:hover {
        background: #60a5fa1f;
        border-color: #60a5fa;
      }
      .delete-btn {
        background: transparent;
        color: #f87171;
        border: 1px solid #f8717155;
      }
      .delete-btn:hover {
        background: #ef44441f;
        border-color: #ef4444;
      }
    `,
  ],
})
export class PermissionActionsCellRenderer implements ICellRendererAngularComp {
  private params!: PermissionActionsCellRendererParams;

  agInit(params: PermissionActionsCellRendererParams): void {
    this.params = params;
  }

  refresh(): boolean {
    return false;
  }

  edit(): void {
    this.params.onEdit(this.params.data!);
  }

  delete(): void {
    this.params.onDelete(this.params.data!);
  }
}
