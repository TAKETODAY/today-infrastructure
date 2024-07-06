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

package cn.taketoday.web.handler.function;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.CacheControl;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.mock.MockRequestContext;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class DefaultServerResponseBuilderTests {

  static final ServerResponse.Context EMPTY_CONTEXT = Collections::emptyList;

  @Test
  void status() {
    ServerResponse response = ServerResponse.status(HttpStatus.CREATED).build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.rawStatusCode()).isEqualTo(201);
  }

  @Test
  void from() {
    HttpCookie cookie = new HttpCookie("foo", "bar");
    ServerResponse other = ServerResponse.ok()
            .header("foo", "bar")
            .cookie(cookie)
            .build();
    ServerResponse result = ServerResponse.from(other).build();
    assertThat(result.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.headers().getFirst("foo")).isEqualTo("bar");
    assertThat(result.cookies().getFirst("foo")).isEqualTo(cookie);
  }

  @Test
  void ok() {
    ServerResponse response = ServerResponse.ok().build();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void created() {
    URI location = URI.create("https://example.com");
    ServerResponse response = ServerResponse.created(location).build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.headers().getLocation()).isEqualTo(location);
  }

  @Test
  void accepted() {
    ServerResponse response = ServerResponse.accepted().build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.ACCEPTED);
  }

  @Test
  void noContent() {
    ServerResponse response = ServerResponse.noContent().build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void seeOther() {
    URI location = URI.create("https://example.com");
    ServerResponse response = ServerResponse.seeOther(location).build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SEE_OTHER);
    assertThat(response.headers().getLocation()).isEqualTo(location);
  }

  @Test
  void temporaryRedirect() {
    URI location = URI.create("https://example.com");
    ServerResponse response = ServerResponse.temporaryRedirect(location).build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.TEMPORARY_REDIRECT);
    assertThat(response.headers().getLocation()).isEqualTo(location);
  }

  @Test
  void permanentRedirect() {
    URI location = URI.create("https://example.com");
    ServerResponse response = ServerResponse.permanentRedirect(location).build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
    assertThat(response.headers().getLocation()).isEqualTo(location);
  }

  @Test
  void badRequest() {
    ServerResponse response = ServerResponse.badRequest().build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void notFound() {
    ServerResponse response = ServerResponse.notFound().build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void unprocessableEntity() {
    ServerResponse response = ServerResponse.unprocessableEntity().build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
  }

  @Test
  void allow() {
    ServerResponse response = ServerResponse.ok().allow(HttpMethod.GET).build();
    assertThat(response.headers().getAllow()).isEqualTo(Set.of(HttpMethod.GET));
  }

  @Test
  void contentLength() {
    ServerResponse response = ServerResponse.ok().contentLength(42).build();
    assertThat(response.headers().getContentLength()).isEqualTo(42L);
  }

  @Test
  void contentType() {
    ServerResponse response = ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).build();
    assertThat(response.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void eTag() {
    ServerResponse response = ServerResponse.ok().eTag("foo").build();
    assertThat(response.headers().getETag()).isEqualTo("\"foo\"");
  }

  @Test
  void lastModified() {
    ZonedDateTime now = ZonedDateTime.now();
    ServerResponse response = ServerResponse.ok().lastModified(now).build();
    long expected = now.toInstant().toEpochMilli() / 1000;
    assertThat(response.headers().getLastModified() / 1000).isEqualTo(expected);
  }

  @Test
  void cacheControlTag() {
    ServerResponse response = ServerResponse.ok().cacheControl(CacheControl.noCache()).build();
    assertThat(response.headers().getCacheControl()).isEqualTo("no-cache");
  }

  @Test
  void varyBy() {
    ServerResponse response = ServerResponse.ok().varyBy("foo").build();
    List<String> expected = Collections.singletonList("foo");
    assertThat(response.headers().getVary()).isEqualTo(expected);
  }

  @Test
  void statusCode() {
    HttpStatus statusCode = HttpStatus.ACCEPTED;
    ServerResponse response = ServerResponse.status(statusCode).build();
    assertThat(response.statusCode()).isEqualTo(statusCode);
  }

  @Test
  void headers() {
    HttpHeaders newHeaders = HttpHeaders.forWritable();
    newHeaders.set("foo", "bar");
    ServerResponse response = ServerResponse.ok()
            .headers(headers -> headers.addAll(newHeaders))
            .build();
    assertThat(response.headers()).isEqualTo(newHeaders);

    response = ServerResponse.ok()
            .headers(newHeaders)
            .build();
    assertThat(response.headers()).isEqualTo(newHeaders);
  }

  @Test
  void cookies() {
    MultiValueMap<String, HttpCookie> newCookies = new LinkedMultiValueMap<>();
    newCookies.add("name", new HttpCookie("name", "value"));
    ServerResponse response = ServerResponse.ok()
            .cookies(cookies -> cookies.addAll(newCookies))
            .build();
    assertThat(response.cookies()).isEqualTo(newCookies);

    response = ServerResponse.ok()
            .cookies(newCookies)
            .build();
    assertThat(response.cookies()).isEqualTo(newCookies);

    response = ServerResponse.ok()
            .cookie("name", "value")
            .build();
    assertThat(response.cookies()).isEqualTo(newCookies);
  }

  @Test
  void build() throws Throwable {
    HttpCookie cookie = new HttpCookie("name", "value");
    ServerResponse response = ServerResponse.status(HttpStatus.CREATED)
            .header("MyKey", "MyValue")
            .cookie(cookie)
            .build();

    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();

    Object mav = getObject(response, mockRequest, mockResponse);
    assertThat(mav).isNull();

    assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.CREATED.value());
    assertThat(mockResponse.getHeader("MyKey")).isEqualTo("MyValue");
    assertThat(mockResponse.getCookie("name").getValue()).isEqualTo("value");
  }

  @Nullable
  private static Object getObject(
          ServerResponse response, HttpMockRequestImpl mockRequest, MockHttpResponseImpl mockResponse) throws Throwable {
    MockRequestContext context = new MockRequestContext(null, mockRequest, mockResponse);
    try {
      return response.writeTo(context, EMPTY_CONTEXT);
    }
    finally {
      context.flush();
    }
  }

  @Test
  void notModifiedEtag() throws Throwable {
    String etag = "\"foo\"";
    ServerResponse response = ServerResponse.ok()
            .eTag(etag)
            .body("bar");

    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, etag);

    MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();

    Object mav = getObject(response, mockRequest, mockResponse);
    assertThat(mav).isEqualTo(EntityResponse.NONE_RETURN_VALUE);
    assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
  }

  @Test
  void notModifiedLastModified() throws Throwable {
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime oneMinuteBeforeNow = now.minus(1, ChronoUnit.MINUTES);

    ServerResponse response = ServerResponse.ok()
            .lastModified(oneMinuteBeforeNow)
            .body("bar");

    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    mockRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateTimeFormatter.RFC_1123_DATE_TIME.format(now));

    MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();
    Object mav = getObject(response, mockRequest, mockResponse);
    assertThat(mav).isEqualTo(EntityResponse.NONE_RETURN_VALUE);
    assertThat(mockResponse.getStatus()).isEqualTo(HttpStatus.NOT_MODIFIED.value());
  }

  @Test
  void body() throws Throwable {
    String body = "foo";
    ServerResponse response = ServerResponse.ok().body(body);

    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();
    ServerResponse.Context context = () -> Collections.singletonList(new StringHttpMessageConverter());
    MockRequestContext requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    Object mav = response.writeTo(requestContext, context);

    assertThat(mav).isEqualTo(EntityResponse.NONE_RETURN_VALUE);
    assertThat(mockResponse.getContentAsString()).isEqualTo(body);
  }

  @Test
  void bodyWithParameterizedTypeReference() throws Throwable {
    List<String> body = new ArrayList<>();
    body.add("foo");
    body.add("bar");
    ServerResponse response = ServerResponse.ok().body(body, new ParameterizedTypeReference<List<String>>() { });

    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();
    ServerResponse.Context context = () -> Collections.singletonList(new MappingJackson2HttpMessageConverter());

    MockRequestContext requestContext = new MockRequestContext(null, mockRequest, mockResponse);
    Object mav = response.writeTo(requestContext, context);

    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(mockResponse.getContentAsString()).isEqualTo("[\"foo\",\"bar\"]");
  }

  @Test
  void bodyCompletionStage() throws Throwable {
    String body = "foo";
    CompletionStage<String> completionStage = CompletableFuture.completedFuture(body);
    ServerResponse response = ServerResponse.ok().body(completionStage);

    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();
    mockRequest.setAsyncSupported(true);

    ServerResponse.Context context = () -> Collections.singletonList(new StringHttpMessageConverter());

    MockRequestContext requestContext = new MockRequestContext(null, mockRequest, mockResponse);
    Object mav = response.writeTo(requestContext, context);

    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(mockResponse.getContentAsString()).isEqualTo(body);
  }

  @Test
  void bodyPublisher() throws Throwable {
    String body = "foo";
    Publisher<String> publisher = Mono.just(body);
    ServerResponse response = ServerResponse.ok().body(publisher);

    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();
    mockRequest.setAsyncSupported(true);

    ServerResponse.Context context = () -> Collections.singletonList(new StringHttpMessageConverter());

    MockRequestContext requestContext = new MockRequestContext(null, mockRequest, mockResponse);
    Object mav = response.writeTo(requestContext, context);
    assertThat(mav).isEqualTo(ServerResponse.NONE_RETURN_VALUE);

    assertThat(mockResponse.getContentAsString()).isEqualTo(body);
  }

}
