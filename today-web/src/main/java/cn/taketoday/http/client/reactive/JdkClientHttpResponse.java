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

import java.net.HttpCookie;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DataBufferUtils;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedCaseInsensitiveMap;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;

/**
 * {@link ClientHttpResponse} for the Java {@link HttpClient}.
 *
 * @author Julien Eyraud
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class JdkClientHttpResponse implements ClientHttpResponse {

  private static final Pattern SAME_SITE_PATTERN = Pattern.compile("(?i).*SameSite=(Strict|Lax|None).*");

  private final HttpResponse<Flow.Publisher<List<ByteBuffer>>> response;

  private final DataBufferFactory bufferFactory;

  private final HttpHeaders headers;

  public JdkClientHttpResponse(
          HttpResponse<Flow.Publisher<List<ByteBuffer>>> response, DataBufferFactory bufferFactory) {

    this.response = response;
    this.bufferFactory = bufferFactory;
    this.headers = adaptHeaders(response);
  }

  private static HttpHeaders adaptHeaders(HttpResponse<Flow.Publisher<List<ByteBuffer>>> response) {
    Map<String, List<String>> rawHeaders = response.headers().map();
    Map<String, List<String>> map = new LinkedCaseInsensitiveMap<>(rawHeaders.size(), Locale.ENGLISH);
    MultiValueMap<String, String> multiValueMap = MultiValueMap.from(map);
    multiValueMap.putAll(rawHeaders);
    return HttpHeaders.readOnlyHttpHeaders(multiValueMap);
  }

  @Override
  public HttpStatusCode getStatusCode() {
    return HttpStatusCode.valueOf(response.statusCode());
  }

  @Override
  public int getRawStatusCode() {
    return response.statusCode();
  }

  @Override
  public HttpHeaders getHeaders() {
    return headers;
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    return response.headers().allValues(HttpHeaders.SET_COOKIE).stream()
            .flatMap(header -> {
              Matcher matcher = SAME_SITE_PATTERN.matcher(header);
              String sameSite = (matcher.matches() ? matcher.group(1) : null);
              return HttpCookie.parse(header).stream().map(cookie -> toResponseCookie(cookie, sameSite));
            })
            .collect(LinkedMultiValueMap::new,
                    (cookies, cookie) -> cookies.add(cookie.getName(), cookie),
                    LinkedMultiValueMap::addAll);
  }

  private ResponseCookie toResponseCookie(HttpCookie cookie, @Nullable String sameSite) {
    return ResponseCookie.from(cookie.getName(), cookie.getValue())
            .domain(cookie.getDomain())
            .httpOnly(cookie.isHttpOnly())
            .maxAge(cookie.getMaxAge())
            .path(cookie.getPath())
            .secure(cookie.getSecure())
            .sameSite(sameSite)
            .build();
  }

  @Override
  public Flux<DataBuffer> getBody() {
    return JdkFlowAdapter.flowPublisherToFlux(response.body())
            .flatMapIterable(Function.identity())
            .map(bufferFactory::wrap)
            .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
  }

}
