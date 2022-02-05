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

package cn.taketoday.web.util;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;
import java.util.StringTokenizer;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.server.ServletServerHttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Miscellaneous utilities for web applications.
 * <p>Used by various framework classes.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
public abstract class WebUtils {
  public static final String ERROR_EXCEPTION_ATTRIBUTE = HandlerExceptionHandler.class.getName() + "-context-throwable";

  /**
   * Prefix of the charset clause in a content type String: ";charset=".
   */
  public static final String CONTENT_TYPE_CHARSET_PREFIX = ";charset=";

  /**
   * Retrieve the first cookie with the given name. Note that multiple
   * cookies can have the same name but different paths or domains.
   *
   * @param request current servlet request
   * @param name cookie name
   * @return the first cookie with the given name, or {@code null} if none is found
   */
  @Nullable
  public static HttpCookie getCookie(RequestContext request, String name) {
    Assert.notNull(request, "Request must not be null");
    HttpCookie[] cookies = request.getCookies();
    for (HttpCookie cookie : cookies) {
      if (name.equals(cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  /**
   * Parse the given string with matrix variables. An example string would look
   * like this {@code "q1=a;q1=b;q2=a,b,c"}. The resulting map would contain
   * keys {@code "q1"} and {@code "q2"} with values {@code ["a","b"]} and
   * {@code ["a","b","c"]} respectively.
   *
   * @param matrixVariables the unparsed matrix variables string
   * @return a map with matrix variable names and values (never {@code null})
   */
  public static MultiValueMap<String, String> parseMatrixVariables(String matrixVariables) {
    MultiValueMap<String, String> result = MultiValueMap.fromLinkedHashMap();
    if (!StringUtils.hasText(matrixVariables)) {
      return result;
    }
    StringTokenizer pairs = new StringTokenizer(matrixVariables, ";");
    while (pairs.hasMoreTokens()) {
      String pair = pairs.nextToken();
      int index = pair.indexOf('=');
      if (index != -1) {
        String name = pair.substring(0, index);
        if (name.equalsIgnoreCase("jsessionid")) {
          continue;
        }
        String rawValue = pair.substring(index + 1);
        for (String value : StringUtils.commaDelimitedListToStringArray(rawValue)) {
          result.add(name, value);
        }
      }
      else {
        result.add(pair, "");
      }
    }
    return result;
  }

  /**
   * Check the given request origin against a list of allowed origins.
   * A list containing "*" means that all origins are allowed.
   * An empty list means only same origin is allowed.
   *
   * <p><strong>Note:</strong> this method ignores
   * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
   * client-originated address. Consider using the {@code ForwardedHeaderFilter}
   * to extract and use, or to discard such headers.
   *
   * @return {@code true} if the request origin is valid, {@code false} otherwise
   * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
   */
  public static boolean isValidOrigin(HttpRequest request, Collection<String> allowedOrigins) {
    Assert.notNull(request, "Request must not be null");
    Assert.notNull(allowedOrigins, "Allowed origins must not be null");

    String origin = request.getHeaders().getOrigin();
    if (origin == null || allowedOrigins.contains("*")) {
      return true;
    }
    else if (CollectionUtils.isEmpty(allowedOrigins)) {
      return isSameOrigin(request);
    }
    else {
      return allowedOrigins.contains(origin);
    }
  }

  /**
   * Check if the request is a same-origin one, based on {@code Origin}, {@code Host},
   * {@code Forwarded}, {@code X-Forwarded-Proto}, {@code X-Forwarded-Host} and
   * {@code X-Forwarded-Port} headers.
   *
   * <p><strong>Note:</strong> this method ignores
   * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
   * client-originated address. Consider using the {@code ForwardedHeaderFilter}
   * to extract and use, or to discard such headers.
   *
   * @return {@code true} if the request is a same-origin one, {@code false} in case
   * of cross-origin request
   */
  public static boolean isSameOrigin(HttpRequest request) {
    HttpHeaders headers = request.getHeaders();
    String origin = headers.getOrigin();
    if (origin == null) {
      return true;
    }

    String scheme;
    String host;
    int port;

    if (request instanceof ServletRequestContext servletContext) {
      HttpServletRequest servletRequest = ServletUtils.getServletRequest(servletContext);
      scheme = servletRequest.getScheme();
      host = servletRequest.getServerName();
      port = servletRequest.getServerPort();
    }
    else if (request instanceof ServletServerHttpRequest servletServerHttpRequest) {
      // Build more efficiently if we can: we only need scheme, host, port for origin comparison
      HttpServletRequest servletRequest = servletServerHttpRequest.getServletRequest();
      scheme = servletRequest.getScheme();
      host = servletRequest.getServerName();
      port = servletRequest.getServerPort();
    }
    else {
      URI uri = request.getURI();
      scheme = uri.getScheme();
      host = uri.getHost();
      port = uri.getPort();
    }

    UriComponents originUrl = UriComponentsBuilder.fromOriginHeader(origin).build();
    return Objects.equals(scheme, originUrl.getScheme())
            && Objects.equals(host, originUrl.getHost())
            && getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort());
  }

  private static int getPort(@Nullable String scheme, int port) {
    if (port == -1) {
      if ("http".equals(scheme) || "ws".equals(scheme)) {
        port = 80;
      }
      else if ("https".equals(scheme) || "wss".equals(scheme)) {
        port = 443;
      }
    }
    return port;
  }

}
