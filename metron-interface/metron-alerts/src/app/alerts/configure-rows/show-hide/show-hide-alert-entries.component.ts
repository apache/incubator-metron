import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { QueryBuilder } from 'app/alerts/alerts-list/query-builder';
import { Filter } from 'app/model/filter';

export class ShowHideChanged {
  value: string;
  isHide: boolean;

  constructor(value: string, isHide: boolean) {
    this.value = value;
    this.isHide = isHide;
  }
}

@Component({
  selector: 'app-show-hide-alert-entries',
  template: `
    <app-switch [text]="'HIDE Resolved Alerts'" data-qe-id="hideResolvedAlertsToggle" [selected]="hideResolved"
      (onChange)="onVisibilityChanged('RESOLVE', $event)"> </app-switch>
    <app-switch [text]="'HIDE Dismissed Alerts'" data-qe-id="hideDismissedAlertsToggle" [selected]="hideDismissed"
      (onChange)="onVisibilityChanged('DISMISS', $event)"> </app-switch>
  `,
  styles: ['']
})
export class ShowHideAlertEntriesComponent implements OnInit {

  private readonly FIELD = '-alert_status';
  private readonly RESOLVE = 'RESOLVE';
  private readonly DISMISS = 'DISMISS';

  public readonly HIDE_RESOLVE_STORAGE_KEY = 'hideResolvedAlertItems';
  public readonly HIDE_DISMISS_STORAGE_KEY = 'hideDismissAlertItems';

  private readonly resolveFilter = new Filter(this.FIELD, this.RESOLVE, false);
  private readonly dismissFilter = new Filter(this.FIELD, this.DISMISS, false);

  hideResolved = false;
  hideDismissed = false;

  @Output() changed = new EventEmitter<ShowHideChanged>();

  constructor(private queryBuilder: QueryBuilder) {}

  ngOnInit() {
    this.hideResolved = localStorage.getItem(this.HIDE_RESOLVE_STORAGE_KEY) === 'true';
    this.onVisibilityChanged(this.RESOLVE, this.hideResolved);

    this.hideDismissed = localStorage.getItem(this.HIDE_DISMISS_STORAGE_KEY) === 'true';
    this.onVisibilityChanged(this.DISMISS, this.hideDismissed);
  }

  onVisibilityChanged(alertStatus, isHide) {
    const filterOperation = ((isFilterToAdd) => {
      if (isFilterToAdd) {
        return this.queryBuilder.addOrUpdateFilter.bind(this.queryBuilder);
      } else {
        return this.queryBuilder.removeFilter.bind(this.queryBuilder);
      }
    })(isHide);

    switch (alertStatus) {
      case this.DISMISS:
        filterOperation(this.dismissFilter);
        this.hideDismissed = isHide;
        localStorage.setItem(this.HIDE_DISMISS_STORAGE_KEY, isHide);
        break;
      case this.RESOLVE:
        filterOperation(this.resolveFilter);
        this.hideResolved = isHide;
        localStorage.setItem(this.HIDE_RESOLVE_STORAGE_KEY, isHide);
        break;
    }

    this.changed.emit(new ShowHideChanged(alertStatus, isHide));
  }

}
