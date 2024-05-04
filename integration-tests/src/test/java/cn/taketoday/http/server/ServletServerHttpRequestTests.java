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

package cn.taketoday.http.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 */
public class ServletServerHttpRequestTests {

  private ServletServerHttpRequest request;

  private MockHttpServletRequest mockRequest;

  @BeforeEach
  void create() {
    mockRequest = new MockHttpServletRequest();
    request = new ServletServerHttpRequest(mockRequest);
  }

  @Test
  void getMethod() {
    mockRequest.setMethod("POST");
    request = new ServletServerHttpRequest(mockRequest);
    assertThat(request.getMethod()).as("Invalid method").isEqualTo(HttpMethod.POST);
  }

  @Test
  void getUriForSimplePath() throws URISyntaxException {
    URI uri = new URI("https://example.com/path");
    mockRequest.setScheme(uri.getScheme());
    mockRequest.setServerName(uri.getHost());
    mockRequest.setServerPort(uri.getPort());
    mockRequest.setRequestURI(uri.getPath());
    mockRequest.setQueryString(uri.getQuery());
    assertThat(request.getURI()).isEqualTo(uri);
  }

  @Test
  void getUriWithQueryString() throws URISyntaxException {
    URI uri = new URI("https://example.com/path?query");
    mockRequest.setScheme(uri.getScheme());
    mockRequest.setServerName(uri.getHost());
    mockRequest.setServerPort(uri.getPort());
    mockRequest.setRequestURI(uri.getPath());
    mockRequest.setQueryString(uri.getQuery());
    assertThat(request.getURI()).isEqualTo(uri);
  }

  @Test
    // SPR-16414
  void getUriWithQueryParam() throws URISyntaxException {
    mockRequest.setScheme("https");
    mockRequest.setServerPort(443);
    mockRequest.setServerName("example.com");
    mockRequest.setRequestURI("/path");
    mockRequest.setQueryString("query=foo");
    assertThat(request.getURI()).isEqualTo(new URI("https://example.com/path?query=foo"));
  }

  @Test
    // SPR-16414
  void getUriWithMalformedQueryParam() throws URISyntaxException {
    mockRequest.setScheme("https");
    mockRequest.setServerPort(443);
    mockRequest.setServerName("example.com");
    mockRequest.setRequestURI("/path");
    mockRequest.setQueryString("query=foo%%x");
    assertThat(request.getURI()).isEqualTo(new URI("https://example.com/path"));
  }

  @Test
    // SPR-13876
  void getUriWithEncoding() throws URISyntaxException {
    URI uri = new URI("https://example.com/%E4%B8%AD%E6%96%87" +
            "?redirect=https%3A%2F%2Fgithub.com%2Fspring-projects%2Fspring-framework");
    mockRequest.setScheme(uri.getScheme());
    mockRequest.setServerName(uri.getHost());
    mockRequest.setServerPort(uri.getPort());
    mockRequest.setRequestURI(uri.getRawPath());
    mockRequest.setQueryString(uri.getRawQuery());
    assertThat(request.getURI()).isEqualTo(uri);
  }

  @Test
  void getHeaders() {
    String headerName = "MyHeader";
    String headerValue1 = "value1";
    String headerValue2 = "value2";
    mockRequest.addHeader(headerName, headerValue1);
    mockRequest.addHeader(headerName, headerValue2);
    mockRequest.setContentType("text/plain");
    mockRequest.setCharacterEncoding("UTF-8");

    HttpHeaders headers = request.getHeaders();
    assertThat(headers).as("No HttpHeaders returned").isNotNull();
    assertThat(headers.containsKey(headerName)).as("Invalid headers returned").isTrue();
    List<String> headerValues = headers.get(headerName);
    assertThat(headerValues.size()).as("Invalid header values returned").isEqualTo(2);
    assertThat(headerValues.contains(headerValue1)).as("Invalid header values returned").isTrue();
    assertThat(headerValues.contains(headerValue2)).as("Invalid header values returned").isTrue();
    assertThat(headers.getContentType()).as("Invalid Content-Type")
            .isEqualTo(new MediaType("text", "plain", StandardCharsets.UTF_8));
  }

  @Test
  void getHeadersWithEmptyContentTypeAndEncoding() {
    String headerName = "MyHeader";
    String headerValue1 = "value1";
    String headerValue2 = "value2";
    mockRequest.addHeader(headerName, headerValue1);
    mockRequest.addHeader(headerName, headerValue2);
    mockRequest.setContentType("");
    mockRequest.setCharacterEncoding("");

    HttpHeaders headers = request.getHeaders();
    assertThat(headers).as("No HttpHeaders returned").isNotNull();
    assertThat(headers.containsKey(headerName)).as("Invalid headers returned").isTrue();
    List<String> headerValues = headers.get(headerName);
    assertThat(headerValues.size()).as("Invalid header values returned").isEqualTo(2);
    assertThat(headerValues.contains(headerValue1)).as("Invalid header values returned").isTrue();
    assertThat(headerValues.contains(headerValue2)).as("Invalid header values returned").isTrue();
    assertThat(headers.getContentType()).isNull();
  }

  @Test
  void getHeadersWithWildcardContentType() {
    mockRequest.setContentType("*/*");
    mockRequest.removeHeader("Content-Type");
    assertThat(request.getHeaders()).as("Invalid content-type should not raise exception").hasSize(0);
  }

  @Test
  void getBody() throws IOException {
    byte[] content = "Hello World".getBytes(StandardCharsets.UTF_8);
    mockRequest.setContent(content);

    byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
    assertThat(result).as("Invalid content returned").isEqualTo(content);
  }

  @Test
  void getFormBody() throws IOException {
    // Charset (SPR-8676)
    mockRequest.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
    mockRequest.setMethod("POST");
    mockRequest.addParameter("name 1", "value 1");
    mockRequest.addParameter("name 2", "value 2+1", "value 2+2");
    mockRequest.addParameter("name 3", (String) null);

    byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
    byte[] content = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3".getBytes(
            StandardCharsets.UTF_8);
    assertThat(result).as("Invalid content returned").isEqualTo(content);
  }

  @Test
  void getEmptyFormBody() throws IOException {
    mockRequest.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
    mockRequest.setMethod("POST");

    byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
    byte[] content = "".getBytes(StandardCharsets.UTF_8);
    assertThat(result).as("Invalid content returned").isEqualTo(content);
  }

  @Test
  void getFormBodyWhenQueryParamsAlsoPresent() throws IOException {
    mockRequest.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
    mockRequest.setMethod("POST");
    mockRequest.setQueryString("q=1");
    mockRequest.addParameter("q", "1");
    mockRequest.setContent("foo=bar".getBytes(StandardCharsets.UTF_8));
    mockRequest.addHeader("Content-Length", 7);

    byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
    byte[] content = "foo=bar".getBytes(StandardCharsets.UTF_8);
    assertThat(result).as("Invalid content returned").isEqualTo(content);
  }

  @Test
  void getFormBodyWhenNotEncodedCharactersPresent() throws IOException {
    mockRequest.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
    mockRequest.setMethod("POST");
    mockRequest.addParameter("name", "Test");
    mockRequest.addParameter("lastName", "Test@er");
    mockRequest.addHeader("Content-Length", 26);

    byte[] result = FileCopyUtils.copyToByteArray(request.getBody());
    assertThat(result).isEqualTo("name=Test&lastName=Test%40er".getBytes(StandardCharsets.UTF_8));
    assertThat(request.getHeaders().getContentLength()).isEqualTo(result.length);
  }
}
