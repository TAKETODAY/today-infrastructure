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

package infra.web.handler.function;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import infra.core.ParameterizedTypeReference;
import infra.http.CacheControl;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class DefaultEntityResponseBuilderTests {

  static final ServerResponse.Context EMPTY_CONTEXT = Collections::emptyList;

  @Test
  void fromObject() {
    String body = "foo";
    EntityResponse<String> response = EntityResponse.fromObject(body).build();
    assertThat(response.entity()).isSameAs(body);
  }

  @Test
  void fromObjectTypeReference() {
    String body = "foo";
    var response = EntityResponse.fromObject(body, new ParameterizedTypeReference<String>() { })
            .build();

    assertThat(response.entity()).isSameAs(body);
  }

  @Test
  @SuppressWarnings("deprecation")
  void status() {
    String body = "foo";
    EntityResponse<String> result =
            EntityResponse.fromObject(body).status(HttpStatus.CREATED).build();

    assertThat(result.statusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(result.rawStatusCode()).isEqualTo(201);
  }

  @Test
  void allow() {
    String body = "foo";
    EntityResponse<String> result =
            EntityResponse.fromObject(body).allow(HttpMethod.GET).build();
    Set<HttpMethod> expected = Set.of(HttpMethod.GET);
    assertThat(result.headers().getAllow()).isEqualTo(expected);
  }

  @Test
  void contentLength() {
    String body = "foo";
    EntityResponse<String> result = EntityResponse.fromObject(body).contentLength(42).build();
    assertThat(result.headers().getContentLength()).isEqualTo(42);
  }

  @Test
  void contentType() {
    String body = "foo";
    EntityResponse<String>
            result =
            EntityResponse.fromObject(body).contentType(MediaType.APPLICATION_JSON).build();

    assertThat(result.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void etag() {
    String body = "foo";
    EntityResponse<String> result = EntityResponse.fromObject(body).eTag("foo").build();

    assertThat(result.headers().getETag()).isEqualTo("\"foo\"");
  }

  @Test
  void lastModified() {
    ZonedDateTime now = ZonedDateTime.now();
    String body = "foo";
    EntityResponse<String> result = EntityResponse.fromObject(body).lastModified(now).build();
    long expected = now.toInstant().toEpochMilli() / 1000;
    assertThat(result.headers().getLastModified() / 1000).isEqualTo(expected);
  }

  @Test
  void cacheControlTag() {
    String body = "foo";
    EntityResponse<String> result =
            EntityResponse.fromObject(body).cacheControl(CacheControl.noCache()).build();
    assertThat(result.headers().getCacheControl()).isEqualTo("no-cache");
  }

  @Test
  void varyBy() {
    String body = "foo";
    EntityResponse<String> result = EntityResponse.fromObject(body).varyBy("foo").build();
    List<String> expected = Collections.singletonList("foo");
    assertThat(result.headers().getVary()).isEqualTo(expected);
  }

  @Test
  void header() {
    String body = "foo";
    EntityResponse<String> result = EntityResponse.fromObject(body).header("foo", "bar").build();
    assertThat(result.headers().getFirst("foo")).isEqualTo("bar");
  }

  @Test
  void headers() {
    String body = "foo";
    HttpHeaders headers = HttpHeaders.forWritable();
    headers.setOrRemove("foo", "bar");
    EntityResponse<String> result = EntityResponse.fromObject(body)
            .headers(h -> h.addAll(headers))
            .build();
    assertThat(result.headers()).isEqualTo(headers);
  }

  @Test
  void cookie() {
    HttpCookie cookie = new HttpCookie("name", "value");
    EntityResponse<String> result =
            EntityResponse.fromObject("foo").cookie(cookie)
                    .build();
    assertThat(result.cookies().get("name").contains(cookie)).isTrue();
  }

  @Test
  void cookies() {
    MultiValueMap<String, HttpCookie> newCookies = new LinkedMultiValueMap<>();
    newCookies.add("name", new HttpCookie("name", "value"));
    EntityResponse<String> result =
            EntityResponse.fromObject("foo").cookies(cookies -> cookies.addAll(newCookies))
                    .build();
    assertThat(result.cookies()).isEqualTo(newCookies);
  }

  @Test
  void notModifiedEtag() throws Throwable {
    String etag = "\"foo\"";
    EntityResponse<String> entityResponse = EntityResponse.fromObject("bar")
            .eTag(etag)
            .build();

    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);

    MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();

    MockRequestContext context = new MockRequestContext(null, mockRequest, mockResponse);

    Object mav = entityResponse.writeTo(context, EMPTY_CONTEXT);
    assertThat(mav).isEqualTo(EntityResponse.NONE_RETURN_VALUE);

    assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
  }

  @Test
  void notModifiedLastModified() throws Throwable {
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime oneMinuteBeforeNow = now.minus(1, ChronoUnit.MINUTES);

    EntityResponse<String> entityResponse = EntityResponse.fromObject("bar")
            .lastModified(oneMinuteBeforeNow)
            .build();

    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    mockRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateTimeFormatter.RFC_1123_DATE_TIME.format(now));

    MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();
    MockRequestContext context = new MockRequestContext(null, mockRequest, mockResponse);

    Object mav = entityResponse.writeTo(context, EMPTY_CONTEXT);
    assertThat(mav).isEqualTo(EntityResponse.NONE_RETURN_VALUE);

    assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
  }

}
