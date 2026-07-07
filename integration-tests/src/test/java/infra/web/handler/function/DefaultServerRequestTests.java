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
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.validation.BindException;
import infra.web.HttpMediaTypeNotSupportedException;
import infra.web.mock.MockMemoryPart;
import infra.web.mock.MockRequest;
import infra.web.mock.MockRequestContext;
import infra.web.mock.MockResponse;
import infra.web.mock.api.Cookie;
import infra.web.multipart.Part;
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
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("HEAD", "/", true);
    MockRequestContext context = new MockRequestContext(null, mockRequest, null);

    DefaultServerRequest request = new DefaultServerRequest(context, this.messageConverters);

    assertThat(request.method()).isEqualTo(HttpMethod.HEAD);
  }

  @Test
  void uri() {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.setServerName("example.com");
    mockRequest.setScheme("https");
    mockRequest.setServerPort(443);

    MockRequestContext context = new MockRequestContext(null, mockRequest, null);

    DefaultServerRequest request =
            new DefaultServerRequest(context, this.messageConverters);

    assertThat(request.uri()).isEqualTo(URI.create("https://example.com/"));
  }

  @Test
  void uriBuilder() {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/path", true);
    mockRequest.setQueryString("a=1");
    MockRequestContext context = new MockRequestContext(null, mockRequest, null);
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
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.setAttribute("foo", "bar");

    DefaultServerRequest request = getRequest(mockRequest);

    assertThat(request.attribute("foo")).isEqualTo("bar");
  }

  @Test
  void attributes() {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    MockRequestContext context = new MockRequestContext(null, mockRequest, null);
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
    Iterator<String> attributes = mockRequest.getAttributeNames().asIterator();
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

  private DefaultServerRequest getRequest(MockRequest mockRequest) {
    MockRequestContext context = new MockRequestContext(null, mockRequest, new MockResponse());
    return new DefaultServerRequest(context, this.messageConverters);
  }

  @Test
  void params() {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.setParameter("foo", "bar");

    DefaultServerRequest request =
            getRequest(mockRequest);

    assertThat(request.param("foo")).isEqualTo(Optional.of("bar"));
  }

  @Test
  void parts() throws Exception {
    MockMemoryPart formPart = new MockMemoryPart("form", "foo".getBytes(UTF_8));
    MockMemoryPart filePart = new MockMemoryPart("file", "foo.txt", "foo".getBytes(UTF_8));

    MockRequest mockRequest = PathPatternsTestUtils.initRequest("POST", "/", true);
    mockRequest.addPart(formPart);
    mockRequest.addPart(filePart);

    DefaultServerRequest request =
            getRequest(mockRequest);

    MultiValueMap<String, Part> result = request.parts();

    assertThat(result).hasSize(2);
//    assertThat(result.get("form")).hasSize(1).containsExactly(formPart);
//    assertThat(result.get("file")).containsExactly(filePart);

    assertThat(result.get("form")).hasSize(1);
    assertThat(result.get("file")).hasSize(1);
  }

  @Test
  void emptyQueryParam() {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.setParameter("foo", "");

    DefaultServerRequest request =
            getRequest(mockRequest);

    assertThat(request.param("foo")).isEqualTo(Optional.of(""));
  }

  @Test
  void absentQueryParam() {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.setParameter("foo", "");

    DefaultServerRequest request = getRequest(mockRequest);

    assertThat(request.param("bar")).isEqualTo(Optional.empty());
  }

  @Test
  void pathVariable() {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
    mockRequest
            .setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

    DefaultServerRequest request = getRequest(mockRequest);

    assertThat(request.pathVariable("foo")).isEqualTo("bar");
  }

  @Test
  void pathVariableNotFound() {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
    mockRequest
            .setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

    DefaultServerRequest request = getRequest(mockRequest);

    assertThatIllegalArgumentException().isThrownBy(() ->
            request.pathVariable("baz"));
  }

  @Test
  void pathVariables() {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
    mockRequest
            .setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

    DefaultServerRequest request = getRequest(mockRequest);

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

    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    httpHeaders.forEach(mockRequest::addHeader);
    mockRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);

    DefaultServerRequest request = getRequest(mockRequest);

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

    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.setCookies(cookie);

    DefaultServerRequest request = getRequest(mockRequest);

    MultiValueMap<String, HttpCookie> expected = new LinkedMultiValueMap<>();
    expected.add("foo", new HttpCookie(cookie.getName(), cookie.getValue()));

    assertThat(request.cookies()).isEqualTo(expected);

  }

  @Test
  void bodyClass() throws Exception {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
    mockRequest.setContent("foo".getBytes(UTF_8));

    DefaultServerRequest request = getRequest(mockRequest);

    String result = request.body(String.class);
    assertThat(result).isEqualTo("foo");
  }

  @Test
  void bodyParameterizedTypeReference() throws Exception {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
    mockRequest.setContent("[\"foo\",\"bar\"]".getBytes(UTF_8));

    MockRequestContext context = new MockRequestContext(null, mockRequest, null);

    DefaultServerRequest request = new DefaultServerRequest(context,
            Collections.singletonList(new JacksonJsonHttpMessageConverter()));

    List<String> result = request.body(new ParameterizedTypeReference<List<String>>() { });
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0)).isEqualTo("foo");
    assertThat(result.get(1)).isEqualTo("bar");
  }

  @Test
  void bodyUnacceptable() throws Exception {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
    mockRequest.setContent("foo".getBytes(UTF_8));

    MockRequestContext context = new MockRequestContext(null, mockRequest, null);

    DefaultServerRequest request =
            new DefaultServerRequest(context, Collections.emptyList());

    assertThatExceptionOfType(HttpMediaTypeNotSupportedException.class).isThrownBy(() ->
            request.body(String.class));
  }

  @Test
  void bindToConstructor() throws BindException {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.addParameter("foo", "FOO");
    mockRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(mockRequest);

    ConstructorInjection result = request.bind(ConstructorInjection.class);
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isEqualTo("BAR");
  }

  @Test
  void bindToProperties() throws BindException {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.addParameter("foo", "FOO");
    mockRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(mockRequest);

    PropertyInjection result = request.bind(PropertyInjection.class);
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isEqualTo("BAR");
  }

  @Test
  void bindToMixed() throws BindException {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.addParameter("foo", "FOO");
    mockRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(mockRequest);

    MixedInjection result = request.bind(MixedInjection.class);
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isEqualTo("BAR");
  }

  @Test
  void bindCustomizer() throws BindException {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.addParameter("foo", "FOO");
    mockRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(mockRequest);

    PropertyInjection result = request.bind(PropertyInjection.class, dataBinder -> dataBinder.setAllowedFields("foo"));
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isNull();
  }

  @Test
  void bindError() throws BindException {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    mockRequest.addParameter("foo", "FOO");

    DefaultServerRequest request = getRequest(mockRequest);

    assertThatExceptionOfType(BindException.class).isThrownBy(() ->
            request.bind(ErrorInjection.class)
    );
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedTimestamp(String method) throws Exception {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    mockRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, now.toEpochMilli());

    DefaultServerRequest request = getRequest(mockRequest);

    Optional<ServerResponse> result = request.checkNotModified(now, "");

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
    });
  }

  @ParameterizedHttpMethodTest
  void checkModifiedTimestamp(String method) {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
    mockRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteAgo.toEpochMilli());

    DefaultServerRequest request = getRequest(mockRequest);

    Optional<ServerResponse> result = request.checkNotModified(now, "");

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedETag(String method) {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo\"";
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);

    DefaultServerRequest request = getRequest(mockRequest);

    Optional<ServerResponse> result = request.checkNotModified(eTag);

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
    });
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedETagWithSeparatorChars(String method) {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo, Bar\"";
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);

    DefaultServerRequest request = getRequest(mockRequest);

    Optional<ServerResponse> result = request.checkNotModified(eTag);

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
    });
  }

  @ParameterizedHttpMethodTest
  void checkModifiedETag(String method) {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String currentETag = "\"Foo\"";
    String oldEtag = "Bar";
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);

    DefaultServerRequest request = getRequest(mockRequest);

    Optional<ServerResponse> result = request.checkNotModified(currentETag);

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedUnpaddedETag(String method) {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "Foo";
    String paddedEtag = String.format("\"%s\"", eTag);
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, paddedEtag);

    DefaultServerRequest request = getRequest(mockRequest);

    Optional<ServerResponse> result = request.checkNotModified(eTag);

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getETag()).isEqualTo(paddedEtag);
    });
  }

  @ParameterizedHttpMethodTest
  void checkModifiedUnpaddedETag(String method) {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String currentETag = "Foo";
    String oldEtag = "Bar";
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);

    DefaultServerRequest request = getRequest(mockRequest);

    Optional<ServerResponse> result = request.checkNotModified(currentETag);

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedWildcardIsIgnored(String method) {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo\"";
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "*");
    DefaultServerRequest request = getRequest(mockRequest);

    Optional<ServerResponse> result = request.checkNotModified(eTag);

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedETagAndTimestamp(String method) {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo\"";
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);
    mockRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, now.toEpochMilli());

    DefaultServerRequest request = getRequest(mockRequest);

    Optional<ServerResponse> result = request.checkNotModified(now, eTag);

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
      assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
    });

  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedETagAndModifiedTimestamp(String method) {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo\"";
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, eTag);
    mockRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteAgo.toEpochMilli());

    DefaultServerRequest request = getRequest(mockRequest);

    Optional<ServerResponse> result = request.checkNotModified(now, eTag);

    assertThat(result).hasValueSatisfying(serverResponse -> {
      assertThat(serverResponse.statusCode()).isEqualTo(HttpStatus.NOT_MODIFIED);
      assertThat(serverResponse.headers().getETag()).isEqualTo(eTag);
      assertThat(serverResponse.headers().getLastModified()).isEqualTo(now.toEpochMilli());
    });
  }

  @ParameterizedHttpMethodTest
  void checkModifiedETagAndNotModifiedTimestamp(String method) {
    MockRequest mockRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String currentETag = "\"Foo\"";
    String oldEtag = "\"Bar\"";
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    mockRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);
    mockRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, now.toEpochMilli());

    DefaultServerRequest request = getRequest(mockRequest);

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
