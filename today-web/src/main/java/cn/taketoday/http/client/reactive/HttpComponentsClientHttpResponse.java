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

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;

import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.ResponseCookie;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import reactor.core.publisher.Flux;

/**
 * {@link ClientHttpResponse} implementation for the Apache HttpComponents HttpClient 5.x.
 *
 * @author Martin Tarjányi
 * @author Arjen Poutsma
 * @see <a href="https://hc.apache.org/index.html">Apache HttpComponents</a>
 * @since 4.0
 */
class HttpComponentsClientHttpResponse extends AbstractClientHttpResponse {

  public HttpComponentsClientHttpResponse(DataBufferFactory dataBufferFactory,
          Message<HttpResponse, Publisher<ByteBuffer>> message, HttpClientContext context) {

    super(HttpStatusCode.valueOf(message.getHead().getCode()),
            HttpHeaders.readOnlyHttpHeaders(new HttpComponentsHeadersAdapter(message.getHead())),
            adaptCookies(context),
            Flux.from(message.getBody()).map(dataBufferFactory::wrap)
    );
  }

  private static MultiValueMap<String, ResponseCookie> adaptCookies(HttpClientContext context) {
    LinkedMultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();
    context.getCookieStore().getCookies().forEach(cookie ->
            result.add(cookie.getName(),
                    ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
                            .domain(cookie.getDomain())
                            .path(cookie.getPath())
                            .maxAge(getMaxAgeSeconds(cookie))
                            .secure(cookie.isSecure())
                            .httpOnly(cookie.containsAttribute("httponly"))
                            .sameSite(cookie.getAttribute("samesite"))
                            .build()));
    return result;
  }

  private static long getMaxAgeSeconds(Cookie cookie) {
    String maxAgeAttribute = cookie.getAttribute(Cookie.MAX_AGE_ATTR);
    return (maxAgeAttribute != null ? Long.parseLong(maxAgeAttribute) : -1);
  }

}
