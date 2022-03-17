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

package cn.taketoday.web.cors;

import java.util.Objects;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.util.UriComponents;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * Utility class for CORS request handling based on the
 * <a href="https://www.w3.org/TR/cors/">CORS W3C recommendation</a>.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Sebastien Deleuze
 * @since 4.0 2022/2/20 15:24
 */
public abstract class CorsUtils {

  /**
   * Returns {@code true} if the request is a valid CORS one by checking {@code Origin}
   * header presence and ensuring that origins are different.
   */
  public static boolean isCorsRequest(RequestContext request) {
    String origin = request.requestHeaders().getOrigin();
    if (origin == null) {
      return false;
    }
    UriComponents originUrl = UriComponentsBuilder.fromOriginHeader(origin).build();
    String scheme = request.getScheme();
    String host = request.getServerName();
    int port = request.getServerPort();

    return !(Objects.equals(scheme, originUrl.getScheme())
            && Objects.equals(host, originUrl.getHost())
            && getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort()));

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

  /**
   * Returns {@code true} if the request is a valid CORS pre-flight one. To be
   * used in combination with {@link #isCorsRequest(RequestContext)} since regular
   * CORS checks are not invoked here for performance reasons.
   */
  public static boolean isPreFlightRequest(RequestContext request) {
    if (HttpMethod.OPTIONS == request.getMethod()) {
      HttpHeaders requestHeaders = request.requestHeaders();
      return requestHeaders.containsKey(HttpHeaders.ORIGIN)
              && requestHeaders.containsKey(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    }
    return false;
  }
}
