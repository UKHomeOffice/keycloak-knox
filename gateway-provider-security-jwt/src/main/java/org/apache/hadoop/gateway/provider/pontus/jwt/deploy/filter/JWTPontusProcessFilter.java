/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.gateway.provider.pontus.jwt.deploy.filter;

//import static uk.gov.homeoffice.pontus.JWTStore.*;

import org.keycloak.adapters.AuthenticatedActionsHandler;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.PreAuthActionsHandler;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.servlet.FilterRequestAuthenticator;
import org.keycloak.adapters.servlet.KeycloakOIDCFilter;
import org.keycloak.adapters.servlet.OIDCFilterSessionStore;
import org.keycloak.adapters.servlet.OIDCServletHttpFacade;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.KeycloakAccount;
import org.keycloak.adapters.spi.UserSessionManagement;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public class JWTPontusProcessFilter extends org.keycloak.adapters.servlet.KeycloakOIDCFilter {
  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {


    //System.err.println("Keycloak OIDC Filter: " + ((HttpServletRequest)req).getRequestURL().toString());
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    if (shouldSkip(request)) {
      chain.doFilter(req, res);
      return;
    }

    OIDCServletHttpFacade facade = new OIDCServletHttpFacade(request, response);
    KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
    if (deployment == null || !deployment.isConfigured()) {
      response.sendError(403);
      return;
    }

    PreAuthActionsHandler preActions = new PreAuthActionsHandler(new UserSessionManagement() {
      @Override
      public void logoutAll() {
        if (idMapper != null) {
          idMapper.clear();
        }
      }

      @Override
      public void logoutHttpSessions(List<String> ids) {
        //System.err.println("**************** logoutHttpSessions");
        for (String id : ids) {
          idMapper.removeSession(id);
        }

      }
    }, deploymentContext, facade);

    if (preActions.handleRequest()) {
      //System.err.println("**************** preActions.handleRequest happened!");
      return;
    }


    nodesRegistrationManagement.tryRegister(deployment);
    OIDCFilterSessionStore tokenStore = new OIDCFilterSessionStore(request, facade, 100000, deployment, idMapper);
    tokenStore.checkCurrentToken();


    FilterRequestAuthenticator authenticator = new FilterRequestAuthenticator(deployment, tokenStore, facade, request, 8443);
    AuthOutcome outcome = authenticator.authenticate();
    if (outcome == AuthOutcome.AUTHENTICATED) {
      if (facade.isEnded()) {
        return;
      }
      AuthenticatedActionsHandler actions = new AuthenticatedActionsHandler(deployment, facade);
      if (actions.handledRequest()) {
        return;
      } else {
//        HttpServletRequestWrapper wrapper = tokenStore.buildWrapper();
        HttpSession session = request.getSession(false);
        KeycloakAccount account = null;
        if (session != null) {
          account = (KeycloakAccount) session.getAttribute(KeycloakAccount.class.getName());
          if (account == null) {
            account = (KeycloakAccount) request.getAttribute(KeycloakAccount.class.getName());
          }
        }
        if (account == null) {
          account = (KeycloakAccount) request.getAttribute(KeycloakAccount.class.getName());
        }
        if (account instanceof OIDCFilterSessionStore.SerializableKeycloakAccount) {
          OIDCFilterSessionStore.SerializableKeycloakAccount acct = (OIDCFilterSessionStore.SerializableKeycloakAccount) account;
          RefreshableKeycloakSecurityContext ctx = acct.getKeycloakSecurityContext();

          request.setAttribute("JWT", ctx.getTokenString());
        }


        chain.doFilter(request, res);
        return;
      }
    }
    AuthChallenge challenge = authenticator.getChallenge();
    if (challenge != null) {
      challenge.challenge(facade);
      return;
    }
    response.sendError(403);

  }

  /**
   * Decides whether this {@link Filter} should skip the given {@link HttpServletRequest} based on the configured {@link KeycloakOIDCFilter#skipPattern}.
   * Patterns are matched against the {@link HttpServletRequest#getRequestURI() requestURI} of a request without the context-path.
   * A request for {@code /myapp/index.html} would be tested with {@code /index.html} against the skip pattern.
   * Skipped requests will not be processed further by {@link KeycloakOIDCFilter} and immediately delegated to the {@link FilterChain}.
   *
   * @param request the request to check
   * @return {@code true} if the request should not be handled,
   *         {@code false} otherwise.
   */
  private boolean shouldSkip(HttpServletRequest request) {

//    if (skipPattern == null) {
//      return false;
//    }
    String auth = request.getHeader("Authorization");
    return auth == null? false: auth.startsWith("JWT ");
//    String requestPath = request.getRequestURI().substring(request.getContextPath().length());
//    return skipPattern.matcher(requestPath).matches();
  }


}
