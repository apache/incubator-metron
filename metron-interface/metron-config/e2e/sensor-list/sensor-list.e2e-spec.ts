/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { SensorListPage } from './sensor-list.po';
import {LoginPage} from '../login/login.po';

describe('Sensor List', function() {
  let page: SensorListPage = new SensorListPage();
  let loginPage = new LoginPage();
  let ASCENDING_CSS = 'fa fa-sort-asc';
  let DESCENDING_CSS = 'fa fa-sort-desc';
  let defaultActionState = [
    { classNames: 'fa-circle-o-notch', displayed: false },
    { classNames: 'fa-stop', displayed: false },
    { classNames: 'fa-ban', displayed: false },
    { classNames: 'fa-play', displayed: true },
    { classNames: 'fa-check-circle-o', displayed: false },
    { classNames: 'fa-pencil', displayed: true },
    { classNames: 'fa-trash-o', displayed: true }
  ];
  let runningActionstate = [
    { classNames: 'fa-circle-o-notch', displayed: false},
    { classNames: 'fa-stop', displayed: true},
    { classNames: 'fa-ban', displayed: true},
    { classNames: 'fa-play', displayed: false},
    { classNames: 'fa-check-circle-o', displayed: false},
    { classNames: 'fa-pencil', displayed: true},
    { classNames: 'fa-trash-o', displayed: true}
  ];
  beforeAll(() => {
    loginPage.login();
  });

  afterAll(() => {
    loginPage.logout();
  });

  it('should have title defined', () => {
    expect(page.getTitle()).toEqual('Sensors (8)');
  });

  it('should have add button', () => {
    expect(page.getAddButton()).toEqual(true);
  });

  it('should have all the default parsers', () => {
    expect(page.getParserCount()).toEqual(8);
  });

  it('should have all the table headers', () => {
    expect(page.getTableColumnNames()).toEqual([ 'Name', 'Parser', 'Status', 'Latency', 'Throughput', 'Last Updated', 'Last Editor' ]);
  });

  it('should sort table headers', () => {
    page.toggleSort('Name');
    expect(page.getSortOrder('Name')).toEqual(ASCENDING_CSS);
    expect(page.getColumnValues(0)).toEqual(['asa', 'bro', 'jsonMap', 'jsonMapQuery', 'snort', 'squid', 'websphere', 'yaf']);

    page.toggleSort('Name');
    expect(page.getSortOrder('Name')).toEqual(DESCENDING_CSS);
    expect(page.getColumnValues(0)).toEqual(['yaf', 'websphere', 'squid', 'snort', 'jsonMapQuery', 'jsonMap', 'bro', 'asa']);

    page.toggleSort('Parser');
    expect(page.getSortOrder('Parser')).toEqual(ASCENDING_CSS);
    expect(page.getColumnValues(1)).toEqual(['Asa', 'Bro', 'Grok', 'Grok', 'GrokWebSphere', 'JSONMap', 'JSONMap', 'Snort']);

    page.toggleSort('Parser');
    expect(page.getSortOrder('Parser')).toEqual(DESCENDING_CSS);
    expect(page.getColumnValues(1)).toEqual(['Snort', 'JSONMap', 'JSONMap', 'GrokWebSphere', 'Grok', 'Grok', 'Bro', 'Asa']);

    page.toggleSort('Status');
    expect(page.getSortOrder('Status')).toEqual(ASCENDING_CSS);
    expect(page.getColumnValues(2)).toEqual(['Running', 'Running', 'Stopped', 'Stopped', 'Stopped', 'Stopped', 'Stopped', 'Stopped']);

    page.toggleSort('Status');
    expect(page.getSortOrder('Status')).toEqual(DESCENDING_CSS);
    expect(page.getColumnValues(2)).toEqual(['Stopped', 'Stopped', 'Stopped', 'Stopped', 'Stopped', 'Stopped', 'Running', 'Running']);

    page.toggleSort('Latency');
    expect(page.getSortOrder('Latency')).toEqual(ASCENDING_CSS);
    let latencyValues = page.getColumnValues(3);
    latencyValues.then(values => {
      expect(values.slice(0, 6)).toEqual(['-', '-', '-', '-', '-', '-']);
      expect(values.slice(6)).not.toEqual(['-', '-']);
    });

    page.toggleSort('Latency');
    expect(page.getSortOrder('Latency')).toEqual(DESCENDING_CSS);
    latencyValues = page.getColumnValues(3);
    latencyValues.then(values => {
      expect(values.slice(0, 2)).not.toEqual(['-', '-']);
      expect(values.slice(2)).toEqual(['-', '-', '-', '-', '-', '-']);
    });
  });

  it('should select deselect all rows', () => {
    page.toggleSelectAll();
    expect(page.getSelectedRowCount()).toEqual(8);
    page.toggleSelectAll();
    expect(page.getSelectedRowCount()).toEqual(0);
  });

  it('should select deselect individual rows', () => {
    ['websphere', 'jsonMap', 'jsonMapQuery', 'squid', 'asa', 'snort', 'bro', 'yaf'].map(pName => {
      page.toggleRowSelect(pName);
      expect(page.getSelectedRowCount()).toEqual(1);
      page.toggleRowSelect(pName);
      expect(page.getSelectedRowCount()).toEqual(0);
    });
  });

  it('should enable action in dropdown', () => {
    expect(page.getDropdownActionState()).toEqual({ enabled: 0, disabled: 0, displayed: false});

    page.toggleDropdown();
    expect(page.getDropdownActionState()).toEqual({ enabled: 0, disabled: 5, displayed: true});
    page.toggleDropdown();

    page.toggleSelectAll();
    page.toggleDropdown();
    expect(page.getDropdownActionState()).toEqual({ enabled: 5, disabled: 0, displayed: true});
    page.toggleDropdown();
    page.toggleSelectAll();

    ['websphere', 'jsonMap', 'jsonMapQuery', 'squid', 'asa', 'snort', 'bro', 'yaf'].map(pName => {
      page.toggleRowSelect(pName);
      page.toggleDropdown();
      expect(page.getDropdownActionState()).toEqual({ enabled: 5, disabled: 0, displayed: true});
      page.toggleDropdown();
      page.toggleRowSelect(pName);
    });
  });

  it('should have all the actions with default value', () => {
    ['websphere', 'jsonMap', 'jsonMapQuery', 'squid', 'asa', 'yaf'].map(pName => {
      expect(page.getActions(pName)).toEqual(defaultActionState);
    });

    ['snort', 'bro'].map(pName => {
      expect(page.getActions(pName)).toEqual(runningActionstate);
    });
  });

  it('should open the details pane', (done) => {
    ['websphere', 'jsonMap', 'jsonMapQuery', 'squid', 'asa', 'snort', 'bro', 'yaf'].map(pName => {
      expect(page.openDetailsPane(pName)).toEqual('http://node1:4200/sensors(dialog:sensors-readonly/' + pName + ')');
    });
    page.closePane().then(() => { done(); });
  }, 300000);

  it('should open the edit pane', () => {
    ['websphere', 'jsonMap', 'jsonMapQuery', 'squid', 'asa', 'snort', 'bro', 'yaf'].map(pName => {
      expect(page.openEditPaneAndClose(pName)).toEqual('http://node1:4200/sensors(dialog:sensors-config/' + pName + ')');
    });
  });

});
