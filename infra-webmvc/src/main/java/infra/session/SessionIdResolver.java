/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.session;

import org.jspecify.annotations.Nullable;

import java.util.List;

import infra.core.Conventions;
import infra.session.config.CookieProperties;
import infra.web.RequestContext;

/**
 * Contract for session id resolution strategies. Allows for session id
 * resolution through the request and for sending the session id or expiring
 * the session through the response.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-10-03 10:56
 */
public interface SessionIdResolver {

  String WRITTEN_SESSION_ID_ATTR = Conventions.getQualifiedAttributeName(
          CookieSessionIdResolver.class, "WRITTEN_SESSION_ID_ATTR");

  String HEADER_X_AUTH_TOKEN = "X-Auth-Token";

  String HEADER_AUTHENTICATION_INFO = "Authentication-Info";

  /**
   * Resolving session id from RequestContext
   * <p>
   * session id including {@link #setSessionId applied session id}
   *
   * @param exchange request context
   * @return session id
   */
  @Nullable
  String getSessionId(RequestContext exchange);

  /**
   * Send the given session id to the client.
   *
   * @param exchange the current context
   * @param sessionId the session id
   */
  void setSessionId(RequestContext exchange, String sessionId);

  /**
   * Instruct the client to end the current session.
   *
   * @param exchange the current exchange
   */
  void expireSession(RequestContext exchange);

  // Static Factory Methods

  /**
   * Convenience factory to create {@link HeaderSessionIdResolver} that uses
   * "X-Auth-Token" header.
   *
   * @return the instance configured to use "X-Auth-Token" header
   */
  static HeaderSessionIdResolver xAuthToken() {
    return forHeader(HEADER_X_AUTH_TOKEN);
  }

  /**
   * Convenience factory to create {@link HeaderSessionIdResolver} that uses
   * "Authentication-Info" header.
   *
   * @return the instance configured to use "Authentication-Info" header
   */
  static HeaderSessionIdResolver authenticationInfo() {
    return forHeader(HEADER_AUTHENTICATION_INFO);
  }

  /**
   * Convenience factory to create {@link HeaderSessionIdResolver} that uses
   * given  header.
   *
   * @return the instance configured to use given header
   */
  static HeaderSessionIdResolver forHeader(String headerName) {
    return new HeaderSessionIdResolver(headerName);
  }

  /**
   * Convenience factory to create {@link RequestParameterSessionIdResolver} that uses
   * given parameter name.
   *
   * @param parameterName request parameter name
   * @return the instance configured to use given parameter name
   */
  static RequestParameterSessionIdResolver forParameter(String parameterName) {
    return new RequestParameterSessionIdResolver(parameterName);
  }

  /**
   * Convenience factory to create {@link CookieSessionIdResolver} that uses
   * given cookie name.
   *
   * @param cookieName cookie name
   * @return the instance configured to use given cookie name
   */
  static CookieSessionIdResolver forCookie(String cookieName) {
    return new CookieSessionIdResolver(cookieName);
  }

  /**
   * Convenience factory to create {@link CookieSessionIdResolver} that uses
   * given cookie name.
   *
   * @param properties cookie config
   * @return the instance configured to use given cookie name
   */
  static CookieSessionIdResolver forCookie(CookieProperties properties) {
    return new CookieSessionIdResolver(properties);
  }

  /**
   * for Composite SessionIdResolver
   */
  static SessionIdResolver forComposite(SessionIdResolver... resolvers) {
    return new CompositeSessionIdResolver(List.of(resolvers));
  }

  /**
   * for Composite SessionIdResolver
   */
  static SessionIdResolver forComposite(List<SessionIdResolver> resolvers) {
    return new CompositeSessionIdResolver(resolvers);
  }

}
