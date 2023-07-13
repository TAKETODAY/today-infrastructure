/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.http.client.reactive;

import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.reactivestreams.Publisher;

import java.net.HttpCookie;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.http.support.JettyHeadersAdapter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Flux;

/**
 * {@link ClientHttpResponse} implementation for the Jetty ReactiveStreams HTTP client.
 *
 * @author Sebastien Deleuze
 * @see <a href="https://github.com/jetty-project/jetty-reactive-httpclient">
 * Jetty ReactiveStreams HttpClient</a>
 * @since 4.0
 */
class JettyClientHttpResponse implements ClientHttpResponse {
  private static final Pattern SAMESITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");

  private final HttpHeaders headers;
  private final Flux<DataBuffer> content;
  private final ReactiveResponse reactiveResponse;

  public JettyClientHttpResponse(ReactiveResponse reactiveResponse, Publisher<DataBuffer> content) {
    this.reactiveResponse = reactiveResponse;
    this.content = Flux.from(content);

    var headers = new JettyHeadersAdapter(reactiveResponse.getHeaders());
    this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
  }

  @Override
  public int getRawStatusCode() {
    return this.reactiveResponse.getStatus();
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    var result = MultiValueMap.<String, ResponseCookie>forLinkedHashMap();
    List<String> cookieHeader = getHeaders().get(HttpHeaders.SET_COOKIE);
    if (cookieHeader != null) {
      for (String header : cookieHeader) {
        List<HttpCookie> httpCookies = HttpCookie.parse(header);
        for (HttpCookie cookie : httpCookies) {
          result.add(cookie.getName(),
                  ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
                          .domain(cookie.getDomain())
                          .path(cookie.getPath())
                          .maxAge(cookie.getMaxAge())
                          .secure(cookie.getSecure())
                          .httpOnly(cookie.isHttpOnly())
                          .sameSite(parseSameSite(header))
                          .build());
        }
      }
    }
    return MultiValueMap.forUnmodifiable(result);
  }

  @Nullable
  private static String parseSameSite(String headerValue) {
    Matcher matcher = SAMESITE_PATTERN.matcher(headerValue);
    return matcher.matches() ? matcher.group(1) : null;
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return this.content;
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

}
