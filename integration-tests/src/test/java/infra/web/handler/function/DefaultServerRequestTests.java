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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRange;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.http.converter.json.MappingJackson2HttpMessageConverter;
import infra.mock.api.http.Cookie;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.mock.web.MockPart;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.validation.BindException;
import infra.web.HttpMediaTypeNotSupportedException;
import infra.web.mock.MockRequestContext;
import infra.web.multipart.Multipart;
import infra.web.view.PathPatternsTestUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 * @since 4.0
 */
class DefaultServerRequestTests {

  private final List<HttpMessageConverter<?>> messageConverters = Collections.singletonList(
          new StringHttpMessageConverter());

  @Test
  void method() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("HEAD", "/", true);
    MockRequestContext context = new MockRequestContext(null, servletRequest, null);

    DefaultServerRequest request = new DefaultServerRequest(context, this.messageConverters);

    assertThat(request.method()).isEqualTo(HttpMethod.HEAD);
  }

  @Test
  void uri() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setServerName("example.com");
    servletRequest.setScheme("https");
    servletRequest.setServerPort(443);

    MockRequestContext context = new MockRequestContext(null, servletRequest, null);

    DefaultServerRequest request =
            new DefaultServerRequest(context, this.messageConverters);

    assertThat(request.uri()).isEqualTo(URI.create("https://example.com/"));
  }

  @Test
  void uriBuilder() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/path", true);
    servletRequest.setQueryString("a=1");
    MockRequestContext context = new MockRequestContext(null, servletRequest, null);
    DefaultServerRequest request =
            new DefaultServerRequest(context, this.messageConverters);

    URI result = request.uriBuilder().build();
    assertThat(result.getScheme()).isEqualTo("http");
    assertThat(result.getHost()).isEqualTo("localhost");
    assertThat(result.getPort()).isEqualTo(-1);
    assertThat(result.getPath()).isEqualTo("/path");
    assertThat(result.getQuery()).isEqualTo("a=1");
  }

  @Test
  void attribute() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setAttribute("foo", "bar");

    DefaultServerRequest request = getRequest(servletRequest);

    assertThat(request.attribute("foo")).isEqualTo("bar");
  }

  @Test
  void attributes() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    MockRequestContext context = new MockRequestContext(null, servletRequest, null);
    context.setAttribute("foo", "bar");
    context.setAttribute("baz", "qux");

    DefaultServerRequest request = new DefaultServerRequest(context, this.messageConverters);

    Map<String, Object> attributesMap = request.attributes();
    assertThat(attributesMap).isNotEmpty();
    assertThat(attributesMap).containsEntry("foo", "bar");
    assertThat(attributesMap).containsEntry("baz", "qux");
    assertThat(attributesMap).doesNotContainEntry("foo", "blah");

    Set<Map.Entry<String, Object>> entrySet = attributesMap.entrySet();
    assertThat(entrySet).isNotEmpty();
    assertThat(entrySet).hasSize(attributesMap.size());
    assertThat(entrySet).contains(Map.entry("foo", "bar"));
    assertThat(entrySet).contains(Map.entry("baz", "qux"));
    assertThat(entrySet).doesNotContain(Map.entry("foo", "blah"));

    assertThat(entrySet.iterator()).toIterable().contains(Map.entry("foo", "bar"), Map.entry("baz", "qux"));
    Iterator<String> attributes = servletRequest.getAttributeNames().asIterator();
    Iterator<Map.Entry<String, Object>> entrySetIterator = entrySet.iterator();
    while (attributes.hasNext()) {
      attributes.next();
      assertThat(entrySetIterator).hasNext();
      entrySetIterator.next();
    }
    assertThat(entrySetIterator).isExhausted();

    attributesMap.clear();
    assertThat(attributesMap).isEmpty();
    assertThat(attributesMap).hasSize(0);
  }

  private DefaultServerRequest getRequest(HttpMockRequestImpl servletRequest) {
    MockRequestContext context = new MockRequestContext(null, servletRequest, new MockHttpResponseImpl());
    return new DefaultServerRequest(context, this.messageConverters);
  }

  @Test
  void params() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setParameter("foo", "bar");

    DefaultServerRequest request =
            getRequest(servletRequest);

    assertThat(request.param("foo")).isEqualTo(Optional.of("bar"));
  }

  @Test
  void multipartData() throws Exception {
    MockPart formPart = new MockPart("form", "foo".getBytes(UTF_8));
    MockPart filePart = new MockPart("file", "foo.txt", "foo".getBytes(UTF_8));

    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("POST", "/", true);
    servletRequest.addPart(formPart);
    servletRequest.addPart(filePart);

    DefaultServerRequest request =
            getRequest(servletRequest);

    MultiValueMap<String, Multipart> result = request.multipartData();

    assertThat(result).hasSize(2);
//    assertThat(result.get("form")).hasSize(1).containsExactly(formPart);
//    assertThat(result.get("file")).containsExactly(filePart);

    assertThat(result.get("form")).hasSize(1);
    assertThat(result.get("file")).hasSize(1);
  }

  @Test
  void emptyQueryParam() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setParameter("foo", "");

    DefaultServerRequest request =
            getRequest(servletRequest);

    assertThat(request.param("foo")).isEqualTo(Optional.of(""));
  }

  @Test
  void absentQueryParam() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setParameter("foo", "");

    DefaultServerRequest request = getRequest(servletRequest);

    assertThat(request.param("bar")).isEqualTo(Optional.empty());
  }

  @Test
  void pathVariable() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
    servletRequest
            .setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

    DefaultServerRequest request = getRequest(servletRequest);

    assertThat(request.pathVariable("foo")).isEqualTo("bar");
  }

  @Test
  void pathVariableNotFound() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
    servletRequest
            .setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

    DefaultServerRequest request = getRequest(servletRequest);

    assertThatIllegalArgumentException().isThrownBy(() ->
            request.pathVariable("baz"));
  }

  @Test
  void pathVariables() {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
    servletRequest
            .setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

    DefaultServerRequest request = getRequest(servletRequest);

    assertThat(request.pathVariables()).isEqualTo(pathVariables);
  }

  @Test
  void header() {
    HttpHeaders httpHeaders = HttpHeaders.forWritable();
    List<MediaType> accept =
            Collections.singletonList(MediaType.APPLICATION_JSON);
    httpHeaders.setAccept(accept);
    List<Charset> acceptCharset = Collections.singletonList(UTF_8);
    httpHeaders.setAcceptCharset(acceptCharset);
    long contentLength = 42L;
    httpHeaders.setContentLength(contentLength);
    MediaType contentType = MediaType.TEXT_PLAIN;
    httpHeaders.setContentType(contentType);
    InetSocketAddress host = InetSocketAddress.createUnresolved("localhost", 80);
    httpHeaders.setHost(host);
    List<HttpRange> range = Collections.singletonList(HttpRange.createByteRange(0, 42));
    httpHeaders.setRange(range);

    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    httpHeaders.forEach(servletRequest::addHeader);
    servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);

    DefaultServerRequest request = getRequest(servletRequest);

    ServerRequest.Headers headers = request.headers();
    assertThat(headers.accept()).isEqualTo(accept);
    assertThat(headers.acceptCharset()).isEqualTo(acceptCharset);
    assertThat(headers.contentLength()).isEqualTo(OptionalLong.of(contentLength));
    assertThat(headers.contentType()).isEqualTo(Optional.of(contentType));
    assertThat(headers.header(HttpHeaders.CONTENT_TYPE)).containsExactly(MediaType.TEXT_PLAIN_VALUE);
    assertThat(headers.firstHeader(HttpHeaders.CONTENT_TYPE)).isEqualTo(MediaType.TEXT_PLAIN_VALUE);
    assertThat(headers.asHttpHeaders()).isEqualTo(httpHeaders);
  }

  @Test
  void cookies() {
    Cookie cookie = new Cookie("foo", "bar");

    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setCookies(cookie);

    DefaultServerRequest request = getRequest(servletRequest);

    MultiValueMap<String, HttpCookie> expected = new LinkedMultiValueMap<>();
    expected.add("foo", new HttpCookie(cookie.getName(), cookie.getValue()));

    assertThat(request.cookies()).isEqualTo(expected);

  }

  @Test
  void bodyClass() throws Exception {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
    servletRequest.setContent("foo".getBytes(UTF_8));

    DefaultServerRequest request = getRequest(servletRequest);

    String result = request.body(String.class);
    assertThat(result).isEqualTo("foo");
  }

  @Test
  void bodyParameterizedTypeReference() throws Exception {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
    servletRequest.setContent("[\"foo\",\"bar\"]".getBytes(UTF_8));

    MockRequestContext context = new MockRequestContext(null, servletRequest, null);

    DefaultServerRequest request = new DefaultServerRequest(context,
            Collections.singletonList(new MappingJackson2HttpMessageConverter()));

    List<String> result = request.body(new ParameterizedTypeReference<List<String>>() { });
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0)).isEqualTo("foo");
    assertThat(result.get(1)).isEqualTo("bar");
  }

  @Test
  void bodyUnacceptable() throws Exception {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
    servletRequest.setContent("foo".getBytes(UTF_8));

    MockRequestContext context = new MockRequestContext(null, servletRequest, null);

    DefaultServerRequest request =
            new DefaultServerRequest(context, Collections.emptyList());

    assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class).isThrownBy(() ->
            request.body(String.class));
  }

//  @Test
//  void session() {
//    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
//    MockHttpSession session = new MockHttpSession();
//    servletRequest.setSession(session);
//
//    DefaultServerRequest request = new DefaultServerRequest(servletRequest,
//            this.messageConverters);
//
//    assertThat(request.session()).isEqualTo(session);
//
//  }

  @Test
  void bindToConstructor() throws BindException {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addParameter("foo", "FOO");
    servletRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(servletRequest);

    ConstructorInjection result = request.bind(ConstructorInjection.class);
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isEqualTo("BAR");
  }

  @Test
  void bindToProperties() throws BindException {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addParameter("foo", "FOO");
    servletRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(servletRequest);

    PropertyInjection result = request.bind(PropertyInjection.class);
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isEqualTo("BAR");
  }

  @Test
  void bindToMixed() throws BindException {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addParameter("foo", "FOO");
    servletRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(servletRequest);

    MixedInjection result = request.bind(MixedInjection.class);
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isEqualTo("BAR");
  }

  @Test
  void bindCustomizer() throws BindException {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addParameter("foo", "FOO");
    servletRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(servletRequest);

    PropertyInjection result = request.bind(PropertyInjection.class, dataBinder -> dataBinder.setAllowedFields("foo"));
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isNull();
  }

  @Test
  void bindError() throws BindException {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addParameter("foo", "FOO");

    DefaultServerRequest request = getRequest(servletRequest);

    assertThatExceptionOfType(BindException.class).isThrownBy(() ->
            request.bind(ErrorInjection.class)
    );
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedTimestamp(String method) throws Exception {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, now.toEpochMilli());

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(now, "");

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
    });
  }

  @ParameterizedHttpMethodTest
  void checkModifiedTimestamp(String method) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteAgo.toEpochMilli());

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(now, "");

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedETag(String method) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(eTag);

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
    });
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedETagWithSeparatorChars(String method) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo, Bar\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(eTag);

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
    });
  }

  @ParameterizedHttpMethodTest
  void checkModifiedETag(String method) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String currentETag = "\"Foo\"";
    String oldEtag = "Bar";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(currentETag);

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedUnpaddedETag(String method) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "Foo";
    String paddedEtag = String.format("\"%s\"", eTag);
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, paddedEtag);

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(eTag);

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getETag()).isEqualTo(paddedEtag);
    });
  }

  @ParameterizedHttpMethodTest
  void checkModifiedUnpaddedETag(String method) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String currentETag = "Foo";
    String oldEtag = "Bar";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(currentETag);

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedWildcardIsIgnored(String method) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "*");
    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(eTag);

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedETagAndTimestamp(String method) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo\"";
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, now.toEpochMilli());

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(now, eTag);

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
      assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
    });

  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedETagAndModifiedTimestamp(String method) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo\"";
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteAgo.toEpochMilli());

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(now, eTag);

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
      assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
    });
  }

  @ParameterizedHttpMethodTest
  void checkModifiedETagAndNotModifiedTimestamp(String method) {
    HttpMockRequestImpl servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String currentETag = "\"Foo\"";
    String oldEtag = "\"Bar\"";
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, now.toEpochMilli());

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(now, currentETag);

    assertThat(result).isEmpty();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @ParameterizedTest(name = "[{index}] {0}")
  @ValueSource(strings = { "GET", "HEAD" })
  @interface ParameterizedHttpMethodTest {
  }

  private static final class ConstructorInjection {
    private final String foo;

    private final String bar;

    public ConstructorInjection(String foo, String bar) {
      this.foo = foo;
      this.bar = bar;
    }

    public String getFoo() {
      return this.foo;
    }

    public String getBar() {
      return this.bar;
    }
  }

  private static final class PropertyInjection {
    private String foo;

    private String bar;

    public String getFoo() {
      return this.foo;
    }

    public void setFoo(String foo) {
      this.foo = foo;
    }

    public String getBar() {
      return this.bar;
    }

    public void setBar(String bar) {
      this.bar = bar;
    }
  }

  private static final class MixedInjection {
    private final String foo;

    private String bar;

    public MixedInjection(String foo) {
      this.foo = foo;
    }

    public String getFoo() {
      return this.foo;
    }

    public String getBar() {
      return this.bar;
    }

    public void setBar(String bar) {
      this.bar = bar;
    }
  }

  private static final class ErrorInjection {

    private int foo;

    public int getFoo() {
      return this.foo;
    }

    public void setFoo(int foo) {
      this.foo = foo;
    }
  }

}
