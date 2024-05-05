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

package cn.taketoday.test.web.mock.request;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.mock.web.MockHttpSession;
import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.web.RedirectModel;
import cn.taketoday.mock.api.http.Cookie;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.util.UriComponentsBuilder;

import static cn.taketoday.http.HttpMethod.GET;
import static cn.taketoday.http.HttpMethod.POST;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for building a {@link HttpMockRequestImpl} with
 * {@link MockHttpRequestBuilder}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class MockHttpRequestBuilderTests {

  private final MockContextImpl mockContext = new MockContextImpl();

  private MockHttpRequestBuilder builder = new MockHttpRequestBuilder(GET, "/foo/bar");

  @Test
  void method() {
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getMethod()).isEqualTo("GET");
  }

  @Test
  void uri() {
    String uri = "https://java.sun.com:8080/javase/6/docs/api/java/util/BitSet.html?foo=bar#and(java.util.BitSet)";
    this.builder = new MockHttpRequestBuilder(GET, uri);
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getScheme()).isEqualTo("https");
    assertThat(request.getQueryString()).isEqualTo("foo=bar");
    assertThat(request.getServerName()).isEqualTo("java.sun.com");
    assertThat(request.getServerPort()).isEqualTo(8080);
    assertThat(request.getRequestURI()).isEqualTo("/javase/6/docs/api/java/util/BitSet.html");
    assertThat(request.getRequestURL().toString())
            .isEqualTo("https://java.sun.com:8080/javase/6/docs/api/java/util/BitSet.html");
  }

  @Test
  void requestUriWithEncoding() {
    this.builder = new MockHttpRequestBuilder(GET, "/foo bar");
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getRequestURI()).isEqualTo("/foo%20bar");
  }

  @Test
    // SPR-13435
  void requestUriWithDoubleSlashes() throws URISyntaxException {
    this.builder = new MockHttpRequestBuilder(GET, URI.create("/test//currentlyValid/0"));
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getRequestURI()).isEqualTo("/test//currentlyValid/0");
  }

  @Test
    // gh-24556
  void requestUriWithoutScheme() {
    assertThatIllegalArgumentException().isThrownBy(() -> MockMvcRequestBuilders.get("localhost:8080/path"))
            .withMessage("'url' should start with a path or be a complete HTTP URL: localhost:8080/path");
  }

  @Test
    // gh-28823, gh-29933
  void emptyPath() {
    this.builder = new MockHttpRequestBuilder(GET, "");
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getRequestURI()).isEqualTo("/");
    assertThat(request.getPathInfo()).isEqualTo("/");
  }

  @Test
    // SPR-16453
  void pathInfoIsDecoded() {
    this.builder = new MockHttpRequestBuilder(GET, "/travel/hotels 42");
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getPathInfo()).isEqualTo("/travel/hotels 42");
  }

  @Test
  void contextPathMockPathInvalid() {
    testContextPathServletPathInvalid("Request URI [/foo/bar] does not start with context path [/Foo]");
    testContextPathServletPathInvalid("Context path must start with a '/'");
    testContextPathServletPathInvalid("Context path must not end with a '/'");

    testContextPathServletPathInvalid("Invalid servlet path [/Bar] for request URI [/foo/bar]");
    testContextPathServletPathInvalid("Mock path must start with a '/'");
    testContextPathServletPathInvalid("Mock path must not end with a '/'");
  }

  private void testContextPathServletPathInvalid(String message) {
    try {
      this.builder.buildRequest(this.mockContext);
    }
    catch (IllegalArgumentException ex) {
      assertThat(ex.getMessage()).isEqualTo(message);
    }
  }

  @Test
  void requestUriAndFragment() {
    this.builder = new MockHttpRequestBuilder(GET, "/foo#bar");
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getRequestURI()).isEqualTo("/foo");
  }

  @Test
  void requestParameter() {
    this.builder.param("foo", "bar", "baz");

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    Map<String, String[]> parameterMap = request.getParameterMap();

    assertThat(parameterMap.get("foo")).containsExactly("bar", "baz");
  }

  @Test
  void requestParameterFromQuery() {
    this.builder = new MockHttpRequestBuilder(GET, "/?foo=bar&foo=baz");

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    Map<String, String[]> parameterMap = request.getParameterMap();

    assertThat(parameterMap.get("foo")).containsExactly("bar", "baz");
    assertThat(request.getQueryString()).isEqualTo("foo=bar&foo=baz");
  }

  @Test
  void requestParameterFromQueryList() {
    this.builder = new MockHttpRequestBuilder(GET, "/?foo[0]=bar&foo[1]=baz");

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getQueryString()).isEqualTo("foo%5B0%5D=bar&foo%5B1%5D=baz");
    assertThat(request.getParameter("foo[0]")).isEqualTo("bar");
    assertThat(request.getParameter("foo[1]")).isEqualTo("baz");
  }

  @Test
  void queryParameter() {
    this.builder = new MockHttpRequestBuilder(GET, "/");
    this.builder.queryParam("foo", "bar");
    this.builder.queryParam("foo", "baz");

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getParameterMap().get("foo")).containsExactly("bar", "baz");
    assertThat(request.getQueryString()).isEqualTo("foo=bar&foo=baz");
  }

  @Test
  void queryParameterMap() {
    this.builder = new MockHttpRequestBuilder(GET, "/");
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    List<String> values = new ArrayList<>();
    values.add("bar");
    values.add("baz");
    queryParams.put("foo", values);
    this.builder.queryParams(queryParams);

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getParameterMap().get("foo")).containsExactly("bar", "baz");
    assertThat(request.getQueryString()).isEqualTo("foo=bar&foo=baz");
  }

  @Test
  void queryParameterList() {
    this.builder = new MockHttpRequestBuilder(GET, "/");
    this.builder.queryParam("foo[0]", "bar");
    this.builder.queryParam("foo[1]", "baz");

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getQueryString()).isEqualTo("foo%5B0%5D=bar&foo%5B1%5D=baz");
    assertThat(request.getParameter("foo[0]")).isEqualTo("bar");
    assertThat(request.getParameter("foo[1]")).isEqualTo("baz");
  }

  @Test
  void requestParameterFromQueryWithEncoding() {
    this.builder = new MockHttpRequestBuilder(GET, "/?foo={value}", "bar=baz");

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getQueryString()).isEqualTo("foo=bar%3Dbaz");
    assertThat(request.getParameter("foo")).isEqualTo("bar=baz");
  }

  @Test
    // SPR-11043
  void requestParameterFromQueryNull() {
    this.builder = new MockHttpRequestBuilder(GET, "/?foo");

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    Map<String, String[]> parameterMap = request.getParameterMap();

    assertThat(parameterMap.get("foo")).containsExactly((String) null);
    assertThat(request.getQueryString()).isEqualTo("foo");
  }

  @Test
    // SPR-13801
  void requestParameterFromMultiValueMap() throws Exception {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("foo", "bar");
    params.add("foo", "baz");
    this.builder = new MockHttpRequestBuilder(POST, "/foo");
    this.builder.params(params);

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getParameterMap().get("foo")).containsExactly("bar", "baz");
  }

  @Test
  void requestParameterFromRequestBodyFormData() throws Exception {
    String contentType = "application/x-www-form-urlencoded;charset=UTF-8";
    String body = "name+1=value+1&name+2=value+A&name+2=value+B&name+3";

    HttpMockRequestImpl request = new MockHttpRequestBuilder(POST, "/foo")
            .contentType(contentType).content(body.getBytes(UTF_8))
            .buildRequest(this.mockContext);

    assertThat(request.getParameterMap().get("name 1")).containsExactly("value 1");
    assertThat(request.getParameterMap().get("name 2")).containsExactly("value A", "value B");
    assertThat(request.getParameterMap().get("name 3")).containsExactly((String) null);
  }

  @Test
  void acceptHeader() {
    this.builder.accept(MediaType.TEXT_HTML, MediaType.APPLICATION_XML);

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    List<String> accept = Collections.list(request.getHeaders("Accept"));
    List<MediaType> result = MediaType.parseMediaTypes(accept.get(0));

    assertThat(accept).hasSize(1);
    assertThat(result.get(0).toString()).isEqualTo("text/html");
    assertThat(result.get(1).toString()).isEqualTo("application/xml");
  }

  @Test
    // gh-2079
  void acceptHeaderWithInvalidValues() {
    this.builder.accept("any", "any2");
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    assertThat(request.getHeader("Accept")).isEqualTo("any, any2");
  }

  @Test
  void contentType() {
    this.builder.contentType(MediaType.TEXT_HTML);

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    String contentType = request.getContentType();
    List<String> contentTypes = Collections.list(request.getHeaders("Content-Type"));

    assertThat(contentType).isEqualTo("text/html");
    assertThat(contentTypes).hasSize(1);
    assertThat(contentTypes.get(0)).isEqualTo("text/html");
  }

  @Test
  void contentTypeViaString() {
    this.builder.contentType("text/html");

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    String contentType = request.getContentType();
    List<String> contentTypes = Collections.list(request.getHeaders("Content-Type"));

    assertThat(contentType).isEqualTo("text/html");
    assertThat(contentTypes).hasSize(1);
    assertThat(contentTypes.get(0)).isEqualTo("text/html");
  }

  @Test
    // gh-2079
  void contentTypeWithInvalidValue() {
    this.builder.contentType("any");
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    assertThat(request.getContentType()).isEqualTo("any");
  }

  @Test
    // SPR-11308
  void contentTypeViaHeader() {
    this.builder.header("Content-Type", MediaType.TEXT_HTML_VALUE);
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    String contentType = request.getContentType();

    assertThat(contentType).isEqualTo("text/html");
  }

  @Test
    // gh-2079
  void contentTypeViaHeaderWithInvalidValue() {
    this.builder.header("Content-Type", "yaml");
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    assertThat(request.getContentType()).isEqualTo("yaml");
  }

  @Test
    // SPR-11308
  void contentTypeViaMultipleHeaderValues() {
    this.builder.header("Content-Type", MediaType.TEXT_HTML_VALUE, MediaType.ALL_VALUE);
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getContentType()).isEqualTo("text/html");
  }

  @Test
  void body() throws IOException {
    byte[] body = "Hello World".getBytes(UTF_8);
    this.builder.content(body);

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    byte[] result = FileCopyUtils.copyToByteArray(request.getInputStream());

    assertThat(result).isEqualTo(body);
    assertThat(request.getContentLength()).isEqualTo(body.length);
  }

  @Test
  void header() {
    this.builder.header("foo", "bar", "baz");

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    List<String> headers = Collections.list(request.getHeaders("foo"));

    assertThat(headers).hasSize(2);
    assertThat(headers.get(0)).isEqualTo("bar");
    assertThat(headers.get(1)).isEqualTo("baz");
  }

  @Test
  void headers() {
    HttpHeaders httpHeaders = HttpHeaders.forWritable();
    httpHeaders.setContentType(MediaType.APPLICATION_JSON);
    httpHeaders.put("foo", Arrays.asList("bar", "baz"));
    this.builder.headers(httpHeaders);

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    List<String> headers = Collections.list(request.getHeaders("foo"));

    assertThat(headers).hasSize(2);
    assertThat(headers.get(0)).isEqualTo("bar");
    assertThat(headers.get(1)).isEqualTo("baz");
    assertThat(request.getHeader("Content-Type")).isEqualTo(MediaType.APPLICATION_JSON.toString());
  }

  @Test
  void cookie() {
    Cookie cookie1 = new Cookie("foo", "bar");
    Cookie cookie2 = new Cookie("baz", "qux");
    this.builder.cookie(cookie1, cookie2);

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    Cookie[] cookies = request.getCookies();

    assertThat(cookies).hasSize(2);
    assertThat(cookies[0].getName()).isEqualTo("foo");
    assertThat(cookies[0].getValue()).isEqualTo("bar");
    assertThat(cookies[1].getName()).isEqualTo("baz");
    assertThat(cookies[1].getValue()).isEqualTo("qux");
  }

  @Test
  void noCookies() {
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    assertThat(request.getCookies()).isNull();
  }

  @Test
  void locale() {
    Locale locale = new Locale("nl", "nl");
    this.builder.locale(locale);

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getLocale()).isEqualTo(locale);
  }

  @Test
  void characterEncoding() {
    String encoding = "UTF-8";

    this.builder.characterEncoding(encoding);
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);
    assertThat(request.getCharacterEncoding()).isEqualTo(encoding);

    this.builder.characterEncoding(ISO_8859_1);
    request = this.builder.buildRequest(this.mockContext);
    assertThat(request.getCharacterEncoding()).isEqualTo(ISO_8859_1.name());
  }

  @Test
  void requestAttribute() {
    this.builder.requestAttr("foo", "bar");
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getAttribute("foo")).isEqualTo("bar");
  }

  @Test
  void sessionAttribute() {
    this.builder.sessionAttr("foo", "bar");
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getSession().getAttribute("foo")).isEqualTo("bar");
  }

  @Test
  void sessionAttributes() {
    Map<String, Object> map = new HashMap<>();
    map.put("foo", "bar");
    this.builder.sessionAttrs(map);

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getSession().getAttribute("foo")).isEqualTo("bar");
  }

  @Test
  void session() {
    MockHttpSession session = new MockHttpSession(this.mockContext);
    session.setAttribute("foo", "bar");
    this.builder.session(session);
    this.builder.sessionAttr("baz", "qux");

    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getSession()).isEqualTo(session);
    assertThat(request.getSession().getAttribute("foo")).isEqualTo("bar");
    assertThat(request.getSession().getAttribute("baz")).isEqualTo("qux");
  }

  @Test
  void flashAttribute() {
    this.builder.flashAttr("foo", "bar");
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    RedirectModel flashMap =
            new MockRequestContext(null, request, new MockHttpResponseImpl()).getInputRedirectModel();

    assertThat(flashMap.get("foo")).isEqualTo("bar");
  }

  @Test
  void principal() {
    User user = new User();
    this.builder.principal(user);
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getUserPrincipal()).isEqualTo(user);
  }

  @Test
  void remoteAddress() {
    String ip = "10.0.0.1";
    this.builder.remoteAddress(ip);
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getRemoteAddr()).isEqualTo(ip);
  }

  @Test
    // SPR-12945
  void mergeInvokesDefaultRequestPostProcessorFirst() {
    final String ATTR = "ATTR";
    final String EXPECTED = "override";

    MockHttpRequestBuilder defaultBuilder =
            new MockHttpRequestBuilder(GET, "/foo/bar")
                    .with(requestAttr(ATTR).value("default"))
                    .with(requestAttr(ATTR).value(EXPECTED));

    builder.merge(defaultBuilder);

    HttpMockRequestImpl request = builder.buildRequest(mockContext);
    request = builder.postProcessRequest(request);

    assertThat(request.getAttribute(ATTR)).isEqualTo(EXPECTED);
  }

  @Test
    // SPR-13719
  void arbitraryMethod() {
    String httpMethod = "REPort";
    URI url = UriComponentsBuilder.fromPath("/foo/{bar}").buildAndExpand(42).toUri();
    this.builder = new MockHttpRequestBuilder(httpMethod, url);
    HttpMockRequestImpl request = this.builder.buildRequest(this.mockContext);

    assertThat(request.getMethod()).isEqualTo(httpMethod);
    assertThat(request.getPathInfo()).isEqualTo("/foo/42");
  }

  private static RequestAttributePostProcessor requestAttr(String attrName) {
    return new RequestAttributePostProcessor().attr(attrName);
  }

  private final class User implements Principal {

    @Override
    public String getName() {
      return "Foo";
    }
  }

  private static class RequestAttributePostProcessor implements RequestPostProcessor {

    String attr;

    String value;

    public RequestAttributePostProcessor attr(String attr) {
      this.attr = attr;
      return this;
    }

    public RequestAttributePostProcessor value(String value) {
      this.value = value;
      return this;
    }

    @Override
    public HttpMockRequestImpl postProcessRequest(HttpMockRequestImpl request) {
      request.setAttribute(attr, value);
      return request;
    }
  }

}
