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
package org.apache.hadoop.gateway.provider.pontus.jwt;

import org.apache.hadoop.gateway.i18n.messages.Message;
import org.apache.hadoop.gateway.i18n.messages.MessageLevel;
import org.apache.hadoop.gateway.i18n.messages.Messages;
import org.apache.hadoop.gateway.i18n.messages.StackTrace;

@Messages(logger="org.apache.hadoop.gateway.provider.pontus.jwt")
public interface JWTMessages {
  @Message( level = MessageLevel.WARN, text = "Failed to validate the audience attribute." )
  void failedToValidateAudience();

  @Message( level = MessageLevel.WARN, text = "Failed to verify the token signature." )
  void failedToVerifyTokenSignature();

  @Message( level = MessageLevel.INFO, text = "Access token has expired; a new one must be acquired." )
  void tokenHasExpired();

  @Message( level = MessageLevel.WARN, text = "Expected Bearer token is missing." )
  void missingBearerToken();

  @Message( level = MessageLevel.INFO, text = "Unable to verify token: {0}" )
  void unableToVerifyToken(@StackTrace( level = MessageLevel.ERROR) Exception e);

  @Message( level = MessageLevel.ERROR, text = "Unable to store token in zookeeper: {0}" )
  void unableToStoreTokenInZk(@StackTrace( level = MessageLevel.ERROR) Exception e);

  @Message( level = MessageLevel.ERROR, text = "Unable to verify token: {0}" )
  void unableToIssueToken(@StackTrace( level = MessageLevel.DEBUG) Exception e);

  @Message( level = MessageLevel.DEBUG, text = "Sending redirect to: {0}" )
  void sendRedirectToLoginURL(String loginURL);

  @Message( level = MessageLevel.ERROR, text = "Required configuration element for authentication provider is missing." )
  void missingAuthenticationProviderUrlConfiguration();

  @Message( level = MessageLevel.DEBUG, text = "{0} Cookie has been found and is being processed." )
  void cookieHasBeenFound(String cookieName);

  @Message( level = MessageLevel.DEBUG, text = "Audience claim has been validated." )
  void jwtAudienceValidated();
}
