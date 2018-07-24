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
import {Injectable, EventEmitter}     from '@angular/core';
import {Http, Headers, RequestOptions, Response} from '@angular/http';
import {Router} from '@angular/router';
import {Observable}     from 'rxjs/Observable';
import { GlobalConfigService } from './global-config.service';
import { DataSource } from './data-source';

@Injectable()
export class AuthenticationService {

  private currentUser: string;
  userUrl = '/api/v1/user';
  logoutUrl = '/logout/originalUrl={0}';
  defaultHeaders = {'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest'};
  onLoginEvent: EventEmitter<boolean> = new EventEmitter<boolean>();

  constructor(private http: Http,
              private router: Router,
              private globalConfigService: GlobalConfigService,
              private dataSource: DataSource) {
    this.init();
  }

  public init() {
      this.getCurrentUser(new RequestOptions({headers: new Headers(this.defaultHeaders)})).subscribe((response: Response) => {
        this.currentUser = response.text();
        if (this.currentUser) {
          this.onLoginEvent.emit(true);
          this.dataSource.getDefaultAlertTableColumnNames();
        }
      }, error => {
        this.onLoginEvent.emit(false);
        this.currentUser = null;
      });
  }

  public getCurrentUser(options: RequestOptions): Observable<Response> {
    return this.http.get(this.userUrl, options);
  }

  public getCurrentUserName(): string {
    return this.currentUser;
  }

  public isAuthenticated(): boolean {
    return this.currentUser != null;
  }

  public logout() {
    // clear the authentication cookie
    browser.tabs.query({active: true, currentWindow: true}).then((tabs) => {
      browser.cookies.remove({ url: tabs[0].url, name: 'hadoop-jwt' });
    }).then(() => {
      // redirect to the logout endpoint
      location.replace(this.logoutUrl.format(window.location))
    });
  }
}
