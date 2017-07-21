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
package org.apache.hadoop.gateway.pontus;

import org.apache.hadoop.gateway.config.Configure;
import org.apache.hadoop.gateway.config.Default;
import org.apache.hadoop.gateway.ha.dispatch.DefaultHaDispatch;
import org.apache.http.client.methods.HttpUriRequest;


public class PontusHaDispatch extends DefaultHaDispatch {

  private boolean basicAuthPreemptive = false;

  public PontusHaDispatch() {
    setServiceRole("Pontus");
  }

  protected void addCredentialsToRequest(HttpUriRequest request) {
    if ( isBasicAuthPreemptive() ) {
      PontusDispatchUtils.addCredentialsToRequest(request);
    }
  }

  @Configure
  public void setBasicAuthPreemptive(@Default("false")boolean basicAuthPreemptive) {
    this.basicAuthPreemptive = basicAuthPreemptive;
  }

  public boolean isBasicAuthPreemptive() {
    return basicAuthPreemptive;
  }
}
