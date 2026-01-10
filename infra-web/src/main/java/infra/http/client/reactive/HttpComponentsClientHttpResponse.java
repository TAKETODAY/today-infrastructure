/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.http.client.reactive;

import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieOrigin;
import org.apache.hc.client5.http.cookie.CookieSpec;
import org.apache.hc.client5.http.cookie.MalformedCookieException;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Message;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.List;

import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
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
            new HttpComponentsHeaders(message.getHead()).asReadOnly(),
            adaptCookies(message.getHead(), context),
            Flux.from(message.getBody()).map(dataBufferFactory::wrap));
  }

  private static MultiValueMap<String, ResponseCookie> adaptCookies(HttpResponse response, HttpClientContext context) {
    LinkedMultiValueMap<String, ResponseCookie> result = new LinkedMultiValueMap<>();

    CookieSpec cookieSpec = context.getCookieSpec();
    if (cookieSpec == null) {
      return result;
    }

    CookieOrigin cookieOrigin = context.getCookieOrigin();
    Iterator<Header> itr = response.headerIterator(HttpHeaders.SET_COOKIE);
    while (itr.hasNext()) {
      Header header = itr.next();
      try {
        List<Cookie> cookies = cookieSpec.parse(header, cookieOrigin);
        for (Cookie cookie : cookies) {
          try {
            cookieSpec.validate(cookie, cookieOrigin);
            result.add(cookie.getName(),
                    ResponseCookie.fromClientResponse(cookie.getName(), cookie.getValue())
                            .domain(cookie.getDomain())
                            .path(cookie.getPath())
                            .maxAge(getMaxAgeSeconds(cookie))
                            .secure(cookie.isSecure())
                            .partitioned(cookie.containsAttribute("partitioned"))
                            .httpOnly(cookie.containsAttribute("httponly"))
                            .sameSite(cookie.getAttribute("samesite"))
                            .build());
          }
          catch (final MalformedCookieException ex) {
            // ignore invalid cookie
          }
        }
      }
      catch (final MalformedCookieException ex) {
        // ignore invalid cookie
      }
    }

    return result;
  }

  private static long getMaxAgeSeconds(Cookie cookie) {
    String expiresAttribute = cookie.getAttribute(Cookie.EXPIRES_ATTR);
    String maxAgeAttribute = cookie.getAttribute(Cookie.MAX_AGE_ATTR);
    if (maxAgeAttribute != null) {
      return Long.parseLong(maxAgeAttribute);
    }
    // only consider expires if max-age is not present
    else if (expiresAttribute != null) {
      try {
        ZonedDateTime expiresDate = ZonedDateTime.parse(expiresAttribute, DateTimeFormatter.RFC_1123_DATE_TIME);
        return Duration.between(ZonedDateTime.now(expiresDate.getZone()), expiresDate).toSeconds();
      }
      catch (DateTimeParseException ex) {
        // ignore
      }
    }
    return -1;
  }

}
