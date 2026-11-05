import { Component } from '@angular/core';
import { ICellRendererAngularComp } from 'ag-grid-angular';
import { ICellRendererParams } from 'ag-grid-community';
import { TooltipModule } from 'primeng/tooltip';

/** AG Grid cell renderer shared by Permissions and Role permissions: shows a small info icon
 *  instead of the raw description text, revealing it on hover via a tooltip - keeps those rows
 *  compact since descriptions can be long free text while key/resource/action are always short. */
@Component({
  selector: 'app-description-cell-renderer',
  imports: [TooltipModule],
  template: `@if (description) {
    <i class="pi pi-info-circle" [pTooltip]="description" tooltipPosition="top"></i>
  }`,
})
export class DescriptionCellRenderer implements ICellRendererAngularComp {
  description: string | null = null;

  agInit(params: ICellRendererParams): void {
    this.description = params.value ?? null;
  }

  refresh(): boolean {
    return false;
  }
}
