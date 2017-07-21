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
import org.apache.hadoop.gateway.dispatch.DefaultDispatch;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This specialized dispatch provides Hive specific features to the
 * default HttpClientDispatch.
 */
public class PontusDispatch extends DefaultDispatch {
  private boolean basicAuthPreemptive = false;

  @Override
  public void init() {
    super.init();
    getOutboundRequestExcludeHeaders().remove("Authorization");
//    getOutboundRequestExcludeHeaders().remove("Content-Length");
  }

  protected void addCredentialsToRequest(HttpUriRequest request) {
    if( isBasicAuthPreemptive() ) {
      PontusDispatchUtils.addCredentialsToRequest(request);
    }
  }

  @Configure
  public void setBasicAuthPreemptive( @Default("false") boolean basicAuthPreemptive ) {
    this.basicAuthPreemptive = basicAuthPreemptive;
  }

  public boolean isBasicAuthPreemptive() {
    return basicAuthPreemptive;
  }

public void addJWT(HttpServletRequest request, HttpEntityEnclosingRequest method){
    String jwt = (String) request.getAttribute("JWT");
    if (jwt != null){
      Header[] auth = method.getHeaders("authorization");
      for (int i = 0, ilen = auth.length; i < ilen; i++){
        method.removeHeader(auth[i]);
      }

      method.addHeader("Authorization", "JWT "+ jwt);
    }
  }

  public void addJWT(HttpServletRequest request, HttpOptions method){
    String jwt = (String) request.getAttribute("JWT");
    if (jwt != null){
      Header[] auth = method.getHeaders("authorization");
      for (int i = 0, ilen = auth.length; i < ilen; i++){
        method.removeHeader(auth[i]);
      }

      method.addHeader("Authorization", "JWT "+ jwt);
    }
  }
  public void addJWT(HttpServletRequest request, HttpDelete method){
    String jwt = (String) request.getAttribute("JWT");
    if (jwt != null){
      Header[] auth = method.getHeaders("authorization");
      for (int i = 0, ilen = auth.length; i < ilen; i++){
        method.removeHeader(auth[i]);
      }

      method.addHeader("Authorization", "JWT "+ jwt);
    }
  }

  @Override
  public void doGet(URI url, HttpServletRequest request, HttpServletResponse response)
    throws IOException, URISyntaxException {
    HttpGetWithBody method = new HttpGetWithBody(url);
    // https://issues.apache.org/jira/browse/KNOX-107 - Service URLs not rewritten for WebHDFS GET redirects
    // This is now taken care of in DefaultHttpClientFactory.createHttpClient
    // and setting params here causes configuration setup there to be ignored there.
    // method.getParams().setBooleanParameter("http.protocol.handle-redirects", false);
    copyRequestHeaderFields(method, request);

    addJWT(request,method);
    HttpEntity entity = createRequestEntity(request);
    method.setEntity(entity);

    executeRequest(method, request, response);
  }


  @Override
  public void doOptions(URI url, HttpServletRequest request, HttpServletResponse response)
    throws IOException, URISyntaxException {
    HttpOptions method = new HttpOptions(url);
    addJWT(request,method);
    executeRequest(method, request, response);
  }

  @Override
  public void doPut(URI url, HttpServletRequest request, HttpServletResponse response)
    throws IOException, URISyntaxException {
    HttpPut method = new HttpPut(url);
    HttpEntity entity = createRequestEntity(request);
    method.setEntity(entity);
    copyRequestHeaderFields(method, request);
    addJWT(request,method);

    executeRequest(method, request, response);
  }

  @Override
  public void doPost(URI url, HttpServletRequest request, HttpServletResponse response)
    throws IOException, URISyntaxException {
    HttpPost method = new HttpPost(url);
    HttpEntity entity = createRequestEntity(request);
    method.setEntity(entity);
    copyRequestHeaderFields(method, request);
    addJWT(request,method);

    executeRequest(method, request, response);
  }

  @Override
  public void doDelete(URI url, HttpServletRequest request, HttpServletResponse response)
    throws IOException, URISyntaxException {
    HttpDelete method = new HttpDelete(url);
    copyRequestHeaderFields(method, request);
    addJWT(request,method);

    executeRequest(method, request, response);
  }



}

