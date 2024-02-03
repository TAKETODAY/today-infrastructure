/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.http.client.reactive;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.reactive.client.ReactiveResponse;

import java.net.HttpCookie;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.support.JettyHeadersAdapter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Flux;

/**
 * {@link ClientHttpResponse} implementation for the Jetty ReactiveStreams HTTP client.
 *
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">
 * Jetty ReactiveStreams HttpClient</a>
 * @since 4.0
 */
class JettyClientHttpResponse extends AbstractClientHttpResponse {

  private static final Pattern SAME_SITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");

  public JettyClientHttpResponse(ReactiveResponse reactiveResponse, Flux<DataBuffer> content) {

    super(HttpStatusCode.valueOf(reactiveResponse.getStatus()),
            adaptHeaders(reactiveResponse),
            adaptCookies(reactiveResponse),
            content);
  }

  private static HttpHeaders adaptHeaders(ReactiveResponse response) {
    MultiValueMap<String, String> headers = new JettyHeadersAdapter(response.getHeaders());
    return HttpHeaders.readOnlyHttpHeaders(headers);
  }

  private static MultiValueMap<String, ResponseCookie> adaptCookies(ReactiveResponse response) {
    var result = MultiValueMap.<String, ResponseCookie>forLinkedHashMap();
    List<HttpField> cookieHeaders = response.getHeaders().getFields(HttpHeaders.SET_COOKIE);
    if (cookieHeaders != null) {
      for (HttpField header : cookieHeaders) {
        List<HttpCookie> httpCookies = HttpCookie.parse(header.getValue());
        for (HttpCookie cookie : httpCookies) {
          result.add(cookie.getName(),
                  ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
                          .domain(cookie.getDomain())
                          .path(cookie.getPath())
                          .maxAge(cookie.getMaxAge())
                          .secure(cookie.getSecure())
                          .httpOnly(cookie.isHttpOnly())
                          .sameSite(parseSameSite(header.getValue()))
                          .build());
        }
      }
    }
    return MultiValueMap.forUnmodifiable(result);
  }

  @Nullable
  private static String parseSameSite(String headerValue) {
    Matcher matcher = SAME_SITE_PATTERN.matcher(headerValue);
    return (matcher.matches() ? matcher.group(1) : null);
  }

}
