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

package infra.web.handler.function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import infra.core.ParameterizedTypeReference;
import infra.http.CacheControl;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.web.mock.MockRequestContext;
import infra.web.view.ModelAndView;
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
    ResponseCookie cookie = ResponseCookie.from("foo", "bar").build();
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
    newHeaders.setOrRemove("foo", "bar");
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
    MultiValueMap<String, ResponseCookie> newCookies = new LinkedMultiValueMap<>();
    newCookies.add("name", ResponseCookie.forSimple("name", "value"));
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
    var cookie = ResponseCookie.forSimple("name", "value");
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
    ServerResponse.Context context = () -> Collections.singletonList(new JacksonJsonHttpMessageConverter());

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

  @Test
  void fromServerResponseCopiesAllProperties() {
    ResponseCookie cookie1 = ResponseCookie.from("cookie1", "value1").build();
    ResponseCookie cookie2 = ResponseCookie.from("cookie2", "value2").build();

    ServerResponse original = ServerResponse.status(HttpStatus.CONFLICT)
            .header("Custom-Header", "custom-value")
            .cookie(cookie1)
            .cookie(cookie2)
            .contentType(MediaType.TEXT_PLAIN)
            .contentLength(100)
            .eTag("etag-value")
            .build();

    ServerResponse copied = ServerResponse.from(original).build();

    assertThat(copied.statusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(copied.headers().getFirst("Custom-Header")).isEqualTo("custom-value");
    assertThat(copied.cookies().get("cookie1")).contains(cookie1);
    assertThat(copied.cookies().get("cookie2")).contains(cookie2);
    assertThat(copied.headers().getContentType()).isEqualTo(MediaType.TEXT_PLAIN);
    assertThat(copied.headers().getContentLength()).isEqualTo(100);
    assertThat(copied.headers().getETag()).isEqualTo("\"etag-value\"");
  }

  @Test
  void allowWithMultipleMethods() {
    ServerResponse response = ServerResponse.ok().allow(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT).build();
    Set<HttpMethod> allowedMethods = response.headers().getAllow();

    assertThat(allowedMethods).containsExactlyInAnyOrder(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT);
  }

  @Test
  void allowWithSet() {
    Set<HttpMethod> methods = new LinkedHashSet<>();
    methods.add(HttpMethod.DELETE);
    methods.add(HttpMethod.PATCH);

    ServerResponse response = ServerResponse.ok().allow(methods).build();
    Set<HttpMethod> allowedMethods = response.headers().getAllow();

    assertThat(allowedMethods).containsExactlyInAnyOrder(HttpMethod.DELETE, HttpMethod.PATCH);
  }

  @Test
  void locationHeader() {
    URI location = URI.create("https://example.com/resource/123");
    ServerResponse response = ServerResponse.created(location).build();

    assertThat(response.headers().getLocation()).isEqualTo(location);
  }

  @Test
  void eTagWithoutQuotes() {
    ServerResponse response = ServerResponse.ok().eTag("foo").build();
    assertThat(response.headers().getETag()).isEqualTo("\"foo\"");
  }

  @Test
  void eTagWithQuotes() {
    ServerResponse response = ServerResponse.ok().eTag("\"foo\"").build();
    assertThat(response.headers().getETag()).isEqualTo("\"foo\"");
  }

  @Test
  void eTagNull() {
    ServerResponse response = ServerResponse.ok().eTag(null).build();
    assertThat(response.headers().getETag()).isNull();
  }

  @Test
  void varyByMultipleHeaders() {
    ServerResponse response = ServerResponse.ok().varyBy("Accept", "Accept-Language").build();
    List<String> varyHeaders = response.headers().getVary();

    assertThat(varyHeaders).containsExactly("Accept", "Accept-Language");
  }

  @Test
  void cookieWithMultipleValues() {
    ServerResponse response = ServerResponse.ok().cookie("preferences", "theme=dark", "lang=en").build();

    List<ResponseCookie> cookies = response.cookies().get("preferences");
    assertThat(cookies).hasSize(2);
    assertThat(cookies.get(0).getValue()).isEqualTo("theme=dark");
    assertThat(cookies.get(1).getValue()).isEqualTo("lang=en");
  }

  @Test
  void cookiesConsumer() {
    ServerResponse response = ServerResponse.ok().cookies(cookies -> {
      cookies.add("session", ResponseCookie.from("session", "abc").build());
      cookies.add("preferences", ResponseCookie.from("preferences", "dark-mode").build());
    }).build();

    assertThat(response.cookies().getFirst("session").getValue()).isEqualTo("abc");
    assertThat(response.cookies().getFirst("preferences").getValue()).isEqualTo("dark-mode");
  }

  @Test
  void cookiesCollection() {
    ResponseCookie cookie1 = ResponseCookie.from("auth", "token123").build();
    ResponseCookie cookie2 = ResponseCookie.from("lang", "zh").build();
    List<ResponseCookie> cookieList = Arrays.asList(cookie1, cookie2);

    ServerResponse response = ServerResponse.ok().cookies(cookieList).build();

    assertThat(response.cookies().getFirst("auth").getValue()).isEqualTo("token123");
    assertThat(response.cookies().getFirst("lang").getValue()).isEqualTo("zh");
  }

  @Test
  void cookiesMap() {
    LinkedMultiValueMap<String, ResponseCookie> cookieMap = new LinkedMultiValueMap<>();
    cookieMap.add("user", ResponseCookie.from("user", "john").build());
    cookieMap.add("theme", ResponseCookie.from("theme", "light").build());

    ServerResponse response = ServerResponse.ok().cookies(cookieMap).build();

    assertThat(response.cookies().getFirst("user").getValue()).isEqualTo("john");
    assertThat(response.cookies().getFirst("theme").getValue()).isEqualTo("light");
  }

  @Test
  void headersConsumer() {
    ServerResponse response = ServerResponse.ok().headers(headers -> {
      headers.add("X-Frame-Options", "DENY");
      headers.add("X-Content-Type-Options", "nosniff");
    }).build();

    assertThat(response.headers().getFirst("X-Frame-Options")).isEqualTo("DENY");
    assertThat(response.headers().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
  }

  @Test
  void headersMap() {
    HttpHeaders headerMap = HttpHeaders.forWritable();
    headerMap.add("Custom-Header-1", "value1");
    headerMap.add("Custom-Header-2", "value2");

    ServerResponse response = ServerResponse.ok().headers(headerMap).build();

    assertThat(response.headers().getFirst("Custom-Header-1")).isEqualTo("value1");
    assertThat(response.headers().getFirst("Custom-Header-2")).isEqualTo("value2");
  }

  @Test
  void buildWithWriteFunction() throws Throwable {
    HttpMockRequestImpl mockRequest = new HttpMockRequestImpl("GET", "https://example.com");
    MockHttpResponseImpl mockResponse = new MockHttpResponseImpl();
    MockRequestContext requestContext = new MockRequestContext(null, mockRequest, mockResponse);

    ServerResponse response = ServerResponse.ok().build(request -> "custom response");
    Object result = response.writeTo(requestContext, DefaultServerResponseBuilderTests.EMPTY_CONTEXT);

    assertThat(result).isEqualTo("custom response");
  }

  @Test
  void renderWithModelAttributes() {
    ServerResponse response = ServerResponse.ok().render("viewName", "attr1", "attr2");

    assertThat(response).isInstanceOf(RenderingResponse.class);
    RenderingResponse renderingResponse = (RenderingResponse) response;
    assertThat(renderingResponse.name()).isEqualTo("viewName");
    assertThat(renderingResponse.model()).containsKey("string");
  }

  @Test
  void renderWithModelMap() {
    Map<String, Object> model = Map.of("key1", "value1", "key2", "value2");
    ServerResponse response = ServerResponse.ok().render("viewName", model);

    assertThat(response).isInstanceOf(RenderingResponse.class);
    RenderingResponse renderingResponse = (RenderingResponse) response;
    assertThat(renderingResponse.name()).isEqualTo("viewName");
    assertThat(renderingResponse.model()).containsEntry("key1", "value1");
    assertThat(renderingResponse.model()).containsEntry("key2", "value2");
  }

  @Test
  void stream() {
    ServerResponse response = ServerResponse.ok().stream(builder -> {
      builder.write("Hello");
      builder.write(" ");
      builder.write("World");
    });

    assertThat(response).isInstanceOf(StreamingServerResponse.class);
  }

  @Test
  void lastModifiedWithInstant() {
    Instant now = Instant.now();
    ServerResponse response = ServerResponse.ok().lastModified(now).build();
    long expected = now.toEpochMilli() / 1000;
    assertThat(response.headers().getLastModified() / 1000).isEqualTo(expected);
  }

  @Test
  void renderWithModelAndView() {
    ModelAndView modelAndView = new ModelAndView("viewName");
    modelAndView.addObject("key", "value");

    ServerResponse response = ServerResponse.ok().render(modelAndView);

    assertThat(response).isInstanceOf(RenderingResponse.class);
    RenderingResponse renderingResponse = (RenderingResponse) response;
    assertThat(renderingResponse.name()).isEqualTo("viewName");
    assertThat(renderingResponse.model()).containsEntry("key", "value");
  }

  @Test
  void buildWithStatusCodeInt() {
    ServerResponse response = ServerResponse.status(418).build();
    assertThat(response.statusCode()).isEqualTo(HttpStatus.valueOf(418));
    assertThat(response.rawStatusCode()).isEqualTo(418);
  }

  @Test
  void headerWithMultipleValues() {
    ServerResponse response = ServerResponse.ok()
            .header("X-Custom", "value1", "value2", "value3")
            .build();

    List<String> headerValues = response.headers().get("X-Custom");
    assertThat(headerValues).containsExactly("value1", "value2", "value3");
  }

  @Test
  void headerRemoval() {
    ServerResponse response = ServerResponse.ok()
            .header("X-Custom") // No values should remove the header
            .build();

    assertThat(response.headers().containsKey("X-Custom")).isFalse();
  }

  @Test
  void cookieWithSingleValue() {
    ServerResponse response = ServerResponse.ok()
            .cookie("sessionId", "abc123")
            .build();

    ResponseCookie cookie = response.cookies().getFirst("sessionId");
    assertThat(cookie.getValue()).isEqualTo("abc123");
  }

  @Test
  void emptyCookiesCollection() {
    ServerResponse response = ServerResponse.ok()
            .cookies((Collection<ResponseCookie>) null)
            .build();

    assertThat(response.cookies().size()).isEqualTo(0);
  }

  @Test
  void emptyCookiesMap() {
    ServerResponse response = ServerResponse.ok()
            .cookies((MultiValueMap<String, ResponseCookie>) null)
            .build();

    assertThat(response.cookies().size()).isEqualTo(0);
  }

  @Test
  void contentTypeAndLengthTogether() {
    ServerResponse response = ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .contentLength(1024)
            .build();

    assertThat(response.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    assertThat(response.headers().getContentLength()).isEqualTo(1024L);
  }

  @Test
  void cacheControlWithMaxAge() {
    CacheControl cacheControl = CacheControl.maxAge(Duration.ofSeconds(3600));
    ServerResponse response = ServerResponse.ok()
            .cacheControl(cacheControl)
            .build();

    assertThat(response.headers().getCacheControl()).contains("max-age=3600");
  }

  @Test
  void multipleVaryHeaders() {
    ServerResponse response = ServerResponse.ok()
            .varyBy("Accept", "Accept-Encoding", "Accept-Language")
            .build();

    List<String> varyHeaders = response.headers().getVary();
    assertThat(varyHeaders).containsExactly("Accept", "Accept-Encoding", "Accept-Language");
  }

}
