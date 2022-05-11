/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.session;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * A {@link HeaderSessionIdResolver} that uses a header to resolve the session id.
 * Specifically, this implementation will allow specifying a header name using
 * {@link #HeaderSessionIdResolver(String)}. Convenience factory methods for creating
 * instances that use common header names, such as "X-Auth-Token" and
 * "Authentication-Info", are available as well.
 * <p>
 * When a session is created, the HTTP response will have a response header of the
 * specified name and the value of the session id. For example:
 *
 * <pre>
 * HTTP/1.1 200 OK
 * X-Auth-Token: f81d4fae-7dec-11d0-a765-00a0c91e6bf6
 * </pre>
 *
 * The client should now include the session in each request by specifying the same header
 * in their request. For example:
 *
 * <pre>
 * GET /messages/ HTTP/1.1
 * Host: example.com
 * X-Auth-Token: f81d4fae-7dec-11d0-a765-00a0c91e6bf6
 * </pre>
 *
 * When the session is invalidated, the server will send an HTTP response that has the
 * header name and a blank value. For example:
 *
 * <pre>
 * HTTP/1.1 200 OK
 * X-Auth-Token:
 * </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * 2019-10-03 10:56
 */
public class HeaderSessionIdResolver implements SessionIdResolver {
  public static final String HEADER_X_AUTH_TOKEN = "X-Auth-Token";
  public static final String HEADER_AUTHENTICATION_INFO = "Authentication-Info";

  private final String headerName;

  /**
   * The name of the header to obtain the session id from.
   *
   * @param headerName the name of the header to obtain the session id from.
   */
  public HeaderSessionIdResolver(String headerName) {
    Assert.notNull(headerName, "headerName is required");
    this.headerName = headerName;
  }

  @Nullable
  @Override
  public String retrieveId(RequestContext context) {
    return context.requestHeaders().getFirst(headerName);
  }

  @Override
  public void setId(RequestContext context, String sessionId) {
    Assert.notNull(sessionId, "'sessionId' is required.");
    context.responseHeaders().set(headerName, sessionId);
  }

  @Override
  public void expireSession(RequestContext exchange) {
    setId(exchange, "");
  }

  /**
   * Convenience factory to create {@link HeaderSessionIdResolver} that uses
   * "X-Auth-Token" header.
   *
   * @return the instance configured to use "X-Auth-Token" header
   */
  public static HeaderSessionIdResolver xAuthToken() {
    return new HeaderSessionIdResolver(HEADER_X_AUTH_TOKEN);
  }

  /**
   * Convenience factory to create {@link HeaderSessionIdResolver} that uses
   * "Authentication-Info" header.
   *
   * @return the instance configured to use "Authentication-Info" header
   */
  public static HeaderSessionIdResolver authenticationInfo() {
    return new HeaderSessionIdResolver(HEADER_AUTHENTICATION_INFO);
  }

}
