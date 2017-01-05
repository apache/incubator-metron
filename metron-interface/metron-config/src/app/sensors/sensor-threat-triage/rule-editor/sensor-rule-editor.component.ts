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
import {Component, OnInit, Input, EventEmitter, Output, ViewChild} from '@angular/core';

@Component({
  selector: 'metron-config-sensor-rule-editor',
  templateUrl: './sensor-rule-editor.component.html',
  styleUrls: ['./sensor-rule-editor.component.scss']
})

export class SensorRuleEditorComponent {

  @Input() value: string;
  @Input() score: number;

  @Output() onCancelTextEditor: EventEmitter<boolean> = new EventEmitter<boolean>();
  @Output() onSubmitTextEditor: EventEmitter<{}> = new EventEmitter<{}>();
  
  

  @ViewChild('textEditor') vc;

  ngAfterViewChecked() {
    //console.log(this.vc.nativeElement);
    //setTimeout(() => { this.vc.nativeElement.focus() }, 500);
  }

  constructor() { }

  onSave(): void {
    let rule = {};
    rule[this.value] = this.score;
    this.onSubmitTextEditor.emit(rule);
  }

  onCancel(): void {
    this.onCancelTextEditor.emit(true);
  }

}
