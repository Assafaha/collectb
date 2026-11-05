import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { Permission } from '../../core/models';

export interface GrantCheckboxCellRendererParams extends ICellRendererParams<Permission & { granted: boolean }> {
  onToggle: (permission: Permission, checked: boolean) => void;
}

/** AG Grid cell renderer for the Role permissions table's grant/revoke checkbox column - toggling
 *  it immediately grants or revokes that permission on the currently selected role. */
@Component({
  selector: 'app-grant-checkbox-cell-renderer',
  template: `<input
    type="checkbox"
    [attr.aria-label]="'Grant ' + params.data!.permissionKey"
    [checked]="params.data!.granted"
    (change)="toggle($any($event.target).checked)"
  />`,
})
export class GrantCheckboxCellRenderer implements ICellRendererAngularComp {
  params!: GrantCheckboxCellRendererParams;

  agInit(params: GrantCheckboxCellRendererParams): void {
    this.params = params;
  }

  refresh(): boolean {
    return false;
  }

  toggle(checked: boolean): void {
    this.params.onToggle(this.params.data!, checked);
  }
}
