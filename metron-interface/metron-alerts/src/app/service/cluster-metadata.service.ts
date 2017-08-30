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
import {Injectable} from '@angular/core';
import {Headers, RequestOptions} from '@angular/http';
import {Observable} from 'rxjs/Rx';
import {Http} from '@angular/http';

import {INDEXES} from '../utils/constants';
import {MetronRestApiUtils} from '../utils/metron-rest-api-utils';
import {ColumnMetadata} from '../model/column-metadata';
import {DataSource} from './data-source';
import {HttpUtil} from '../utils/httpUtil';

@Injectable()
export class ClusterMetaDataService {
  defaultHeaders: {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};
  
  constructor(private http: Http,
              private dataSource: DataSource) {
  }

  getDefaultColumns(): Observable<ColumnMetadata[]> {
    return this.dataSource.getDefaultAlertTableColumnNames();
  }

  getColumnMetaData(): Observable<ColumnMetadata[]> {
    let url = '/api/v1/search/column/metadata';
    return this.http.post(url, INDEXES, new RequestOptions({headers: new Headers(this.defaultHeaders)}))
    .map(HttpUtil.extractData)
    .map(MetronRestApiUtils.extractColumnNameDataFromRestApi)
    .catch(HttpUtil.handleError);
  }
}
