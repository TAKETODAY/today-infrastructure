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

package cn.taketoday.web.handler.function;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.http.converter.json.MappingJackson2HttpMessageConverter;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.validation.BindException;
import cn.taketoday.web.HttpMediaTypeNotSupportedException;
import cn.taketoday.web.multipart.Multipart;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import cn.taketoday.web.testfixture.servlet.MockPart;
import cn.taketoday.web.view.PathPatternsTestUtils;
import jakarta.servlet.http.Cookie;

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
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("HEAD", "/", true);
    ServletRequestContext context = new ServletRequestContext(null, servletRequest, null);

    DefaultServerRequest request = new DefaultServerRequest(context, this.messageConverters);

    assertThat(request.method()).isEqualTo(HttpMethod.HEAD);
  }

  @Test
  void uri() {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setServerName("example.com");
    servletRequest.setScheme("https");
    servletRequest.setServerPort(443);

    ServletRequestContext context = new ServletRequestContext(null, servletRequest, null);

    DefaultServerRequest request =
            new DefaultServerRequest(context, this.messageConverters);

    assertThat(request.uri()).isEqualTo(URI.create("https://example.com/"));
  }

  @Test
  void uriBuilder() {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/path", true);
    servletRequest.setQueryString("a=1");
    ServletRequestContext context = new ServletRequestContext(null, servletRequest, null);
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
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setAttribute("foo", "bar");

    DefaultServerRequest request = getRequest(servletRequest);

    assertThat(request.attribute("foo")).isEqualTo(Optional.of("bar"));
  }

  private DefaultServerRequest getRequest(MockHttpServletRequest servletRequest) {
    ServletRequestContext context = new ServletRequestContext(null, servletRequest, new MockHttpServletResponse());
    return new DefaultServerRequest(context, this.messageConverters);
  }

  @Test
  void params() {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setParameter("foo", "bar");

    DefaultServerRequest request =
            getRequest(servletRequest);

    assertThat(request.param("foo")).isEqualTo(Optional.of("bar"));
  }

  @Test
  void multipartData() throws Exception {
    MockPart formPart = new MockPart("form", "foo".getBytes(UTF_8));
    MockPart filePart = new MockPart("file", "foo.txt", "foo".getBytes(UTF_8));

    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("POST", "/", true);
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
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setParameter("foo", "");

    DefaultServerRequest request =
            getRequest(servletRequest);

    assertThat(request.param("foo")).isEqualTo(Optional.of(""));
  }

  @Test
  void absentQueryParam() {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setParameter("foo", "");

    DefaultServerRequest request =
            getRequest(servletRequest);

    assertThat(request.param("bar")).isEqualTo(Optional.empty());
  }

  @Test
  void pathVariable() {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
    servletRequest
            .setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

    DefaultServerRequest request = getRequest(servletRequest);

    assertThat(request.pathVariable("foo")).isEqualTo("bar");
  }

  @Test
  void pathVariableNotFound() {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    Map<String, String> pathVariables = Collections.singletonMap("foo", "bar");
    servletRequest
            .setAttribute(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

    DefaultServerRequest request = getRequest(servletRequest);

    assertThatIllegalArgumentException().isThrownBy(() ->
            request.pathVariable("baz"));
  }

  @Test
  void pathVariables() {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
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

    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
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

    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setCookies(cookie);

    DefaultServerRequest request = getRequest(servletRequest);

    MultiValueMap<String, HttpCookie> expected = new LinkedMultiValueMap<>();
    expected.add("foo", new HttpCookie(cookie.getName(), cookie.getValue()));

    assertThat(request.cookies()).isEqualTo(expected);

  }

  @Test
  void bodyClass() throws Exception {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
    servletRequest.setContent("foo".getBytes(UTF_8));

    DefaultServerRequest request = getRequest(servletRequest);

    String result = request.body(String.class);
    assertThat(result).isEqualTo("foo");
  }

  @Test
  void bodyParameterizedTypeReference() throws Exception {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
    servletRequest.setContent("[\"foo\",\"bar\"]".getBytes(UTF_8));

    ServletRequestContext context = new ServletRequestContext(null, servletRequest, null);

    DefaultServerRequest request = new DefaultServerRequest(context,
            Collections.singletonList(new MappingJackson2HttpMessageConverter()));

    List<String> result = request.body(new ParameterizedTypeReference<List<String>>() { });
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0)).isEqualTo("foo");
    assertThat(result.get(1)).isEqualTo("bar");
  }

  @Test
  void bodyUnacceptable() throws Exception {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);
    servletRequest.setContent("foo".getBytes(UTF_8));

    ServletRequestContext context = new ServletRequestContext(null, servletRequest, null);

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
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addParameter("foo", "FOO");
    servletRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(servletRequest);

    ConstructorInjection result = request.bind(ConstructorInjection.class);
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isEqualTo("BAR");
  }

  @Test
  void bindToProperties() throws BindException {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addParameter("foo", "FOO");
    servletRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(servletRequest);

    PropertyInjection result = request.bind(PropertyInjection.class);
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isEqualTo("BAR");
  }

  @Test
  void bindToMixed() throws BindException {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addParameter("foo", "FOO");
    servletRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(servletRequest);

    MixedInjection result = request.bind(MixedInjection.class);
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isEqualTo("BAR");
  }

  @Test
  void bindCustomizer() throws BindException {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addParameter("foo", "FOO");
    servletRequest.addParameter("bar", "BAR");

    DefaultServerRequest request = getRequest(servletRequest);

    PropertyInjection result = request.bind(PropertyInjection.class, dataBinder -> dataBinder.setAllowedFields("foo"));
    assertThat(result.getFoo()).isEqualTo("FOO");
    assertThat(result.getBar()).isNull();
  }

  @Test
  void bindError() throws BindException {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest("GET", "/", true);
    servletRequest.addParameter("foo", "FOO");

    DefaultServerRequest request = getRequest(servletRequest);

    assertThatExceptionOfType(BindException.class).isThrownBy(() ->
            request.bind(ErrorInjection.class)
    );
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedTimestamp(String method) throws Exception {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant oneMinuteAgo = now.minus(1, ChronoUnit.MINUTES);
    servletRequest.addHeader(HttpHeaders.IF_MODIFIED_SINCE, oneMinuteAgo.toEpochMilli());

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(now, "");

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedETag(String method) {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String currentETag = "\"Foo\"";
    String oldEtag = "Bar";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(currentETag);

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedUnpaddedETag(String method) {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String currentETag = "Foo";
    String oldEtag = "Bar";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, oldEtag);

    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(currentETag);

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedWildcardIsIgnored(String method) {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
    String eTag = "\"Foo\"";
    servletRequest.addHeader(HttpHeaders.IF_NONE_MATCH, "*");
    DefaultServerRequest request = getRequest(servletRequest);

    Optional<ServerResponse> result = request.checkNotModified(eTag);

    assertThat(result).isEmpty();
  }

  @ParameterizedHttpMethodTest
  void checkNotModifiedETagAndTimestamp(String method) {
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
    MockHttpServletRequest servletRequest = PathPatternsTestUtils.initRequest(method, "/", true);
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
