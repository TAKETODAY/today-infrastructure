/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.socket.tomcat;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.websocket.server.HandshakeRequest;

import cn.taketoday.http.HttpHeaders;

/**
 * @author TODAY 2021/5/5 22:51
 * @since 3.0.1
 */
public class TomcatHandshakeRequest implements HandshakeRequest {

  private final URI requestUri;
  private final String queryString;
  private final Principal userPrincipal;
  private final Map<String, List<String>> headers;

  private volatile HttpServletRequest request;
  private final Object httpSession;

  public TomcatHandshakeRequest(HttpServletRequest request, HttpHeaders requestHeaders) {
    this.request = request;
    this.headers = requestHeaders;
    this.queryString = request.getQueryString();
    this.userPrincipal = request.getUserPrincipal();
    this.requestUri = buildRequestUri(request);
    this.httpSession = request.getSession(false);
  }

  @Override
  public URI getRequestURI() {
    return requestUri;
  }

  @Override
  public Map<String, List<String>> getParameterMap() {
    return Collections.emptyMap();
  }

  @Override
  public String getQueryString() {
    return queryString;
  }

  @Override
  public Principal getUserPrincipal() {
    return userPrincipal;
  }

  @Override
  public Map<String, List<String>> getHeaders() {
    return headers;
  }

  @Override
  public boolean isUserInRole(String role) {
    if (request == null) {
      throw new IllegalStateException();
    }
    return request.isUserInRole(role);
  }

  @Override
  public Object getHttpSession() {
    // TODO
    return httpSession;
  }

  /**
   * Called when the HandshakeRequest is no longer required. Since an instance
   * of this class retains a reference to the current HttpServletRequest that
   * reference needs to be cleared as the HttpServletRequest may be reused.
   *
   * There is no reason for instances of this class to be accessed once the
   * handshake has been completed.
   */
  protected void finished() {
    request = null;
  }

  /*
   * See RequestUtil.getRequestURL()
   */
  private static URI buildRequestUri(HttpServletRequest req) {
    StringBuilder uri = new StringBuilder();
    String scheme = req.getScheme();
    int port = req.getServerPort();
    if (port < 0) {
      // Work around java.net.URL bug
      port = 80;
    }

    if ("http".equals(scheme)) {
      uri.append("ws");
    }
    else if ("https".equals(scheme)) {
      uri.append("wss");
    }
    else {
      // Should never happen
      throw new IllegalArgumentException("The scheme [" + scheme + "] in the request is not recognised");
    }

    uri.append("://");
    uri.append(req.getServerName());

    if ((scheme.equals("http") && (port != 80))
            || (scheme.equals("https") && (port != 443))) {
      uri.append(':');
      uri.append(port);
    }

    uri.append(req.getRequestURI());

    if (req.getQueryString() != null) {
      uri.append("?");
      uri.append(req.getQueryString());
    }

    try {
      return new URI(uri.toString());
    }
    catch (URISyntaxException e) {
      // Should never happen
      throw new IllegalArgumentException("The string [" + uri + "] cannot be used to construct a valid URI", e);
    }
  }
}
