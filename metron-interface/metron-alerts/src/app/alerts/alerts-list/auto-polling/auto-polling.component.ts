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
import { Component, Output } from '@angular/core';
import { SearchResponse } from 'app/model/search-response';
import { EventEmitter } from '@angular/core';
import { AutoPollingService } from './auto-polling.service';

@Component({
  selector: 'app-auto-polling',
  templateUrl: './auto-polling.component.html',
  styleUrls: ['./auto-polling.component.scss']
})
export class AutoPollingComponent {
  @Output() onRefresh = new EventEmitter<SearchResponse>();

  showNotification = false;

  constructor(public autoPollingSvc: AutoPollingService) {}

  onToggle() {
    if (!this.autoPollingSvc.getIsPollingActive()) {
      this.autoPollingSvc.start();
    } else {
      this.autoPollingSvc.stop();
    }
  }
}
