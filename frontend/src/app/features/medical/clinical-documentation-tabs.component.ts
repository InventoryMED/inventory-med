import { Component, EventEmitter, Input, Output } from '@angular/core';

export type ClinicalDocumentationTab = 'PROCEDURE' | 'AIH';

@Component({
  selector: 'app-clinical-documentation-tabs',
  templateUrl: './clinical-documentation-tabs.component.html',
  styleUrl: './clinical-documentation-tabs.component.scss',
})
export class ClinicalDocumentationTabsComponent {
  @Input({ required: true }) activeTab: ClinicalDocumentationTab = 'PROCEDURE';
  @Output() readonly tabChange = new EventEmitter<ClinicalDocumentationTab>();
}
