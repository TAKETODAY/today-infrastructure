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

package cn.taketoday.web.servlet.filter;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

import static cn.taketoday.http.MediaType.APPLICATION_JSON_VALUE;
import static cn.taketoday.http.MediaType.TEXT_PLAIN_VALUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/1/21 20:33
 */
class ShallowEtagHeaderFilterTests {

  private final ShallowEtagHeaderFilter filter = new ShallowEtagHeaderFilter();

  @Test
  void isEligibleForEtag() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(filter.isEligibleForEtag(request, response, 200, InputStream.nullInputStream())).isTrue();
    assertThat(filter.isEligibleForEtag(request, response, 300, InputStream.nullInputStream())).isFalse();

    request = new MockHttpServletRequest("HEAD", "/hotels");
    assertThat(filter.isEligibleForEtag(request, response, 200, InputStream.nullInputStream())).isFalse();

    request = new MockHttpServletRequest("POST", "/hotels");
    assertThat(filter.isEligibleForEtag(request, response, 200, InputStream.nullInputStream())).isFalse();

    request = new MockHttpServletRequest("POST", "/hotels");
    request.addHeader("Cache-Control", "must-revalidate, no-store");
    assertThat(filter.isEligibleForEtag(request, response, 200, InputStream.nullInputStream())).isFalse();
  }

  @Test
  void filterNoMatch() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    MockHttpServletResponse response = new MockHttpServletResponse();

    byte[] responseBody = "Hello World".getBytes(UTF_8);
    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
      ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
      filterResponse.setContentType(TEXT_PLAIN_VALUE);
      FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
    };
    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).as("Invalid status").isEqualTo(200);
    assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
    assertThat(response.getContentLength()).as("Invalid Content-Length header").isGreaterThan(0);
    assertThat(response.getContentType()).as("Invalid Content-Type header").isEqualTo(TEXT_PLAIN_VALUE);
    assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
  }

  @Test
  void filterNoMatchWeakETag() throws Exception {
    this.filter.setWriteWeakETag(true);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    MockHttpServletResponse response = new MockHttpServletResponse();

    byte[] responseBody = "Hello World".getBytes(UTF_8);
    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
      ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
      filterResponse.setContentType(TEXT_PLAIN_VALUE);
      FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
    };
    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).as("Invalid status").isEqualTo(200);
    assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("W/\"0b10a8db164e0754105b7a99be72e3fe5\"");
    assertThat(response.getContentLength()).as("Invalid Content-Length header").isGreaterThan(0);
    assertThat(response.getContentType()).as("Invalid Content-Type header").isEqualTo(TEXT_PLAIN_VALUE);
    assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
  }

  @Test
  void filterMatch() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
    request.addHeader("If-None-Match", etag);
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
      byte[] responseBody = "Hello World".getBytes(UTF_8);
      filterResponse.setContentLength(responseBody.length);
      filterResponse.setContentType(TEXT_PLAIN_VALUE);
      FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
    };
    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).as("Invalid status").isEqualTo(304);
    assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
    assertThat(response.containsHeader("Content-Length")).as("Response has Content-Length header").isFalse();
    assertThat(response.getContentType()).as("Invalid Content-Type header").isEqualTo(TEXT_PLAIN_VALUE);
    assertThat(response.getContentAsByteArray()).as("Invalid content").isEmpty();
  }

  @Test
  void filterMatchWeakEtag() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
    request.addHeader("If-None-Match", "W/" + etag);
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
      byte[] responseBody = "Hello World".getBytes(UTF_8);
      FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
      filterResponse.setContentLength(responseBody.length);
    };
    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).as("Invalid status").isEqualTo(304);
    assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
    assertThat(response.containsHeader("Content-Length")).as("Response has Content-Length header").isFalse();
    assertThat(response.getContentAsByteArray()).as("Invalid content").isEmpty();
  }

  @Test
  void filterWriter() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    String etag = "\"0b10a8db164e0754105b7a99be72e3fe5\"";
    request.addHeader("If-None-Match", etag);
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
      ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
      String responseBody = "Hello World";
      FileCopyUtils.copy(responseBody, filterResponse.getWriter());
    };
    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).as("Invalid status").isEqualTo(304);
    assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
    assertThat(response.containsHeader("Content-Length")).as("Response has Content-Length header").isFalse();
    assertThat(response.getContentAsByteArray()).as("Invalid content").isEmpty();
  }

  @Test  // SPR-12960
  public void filterWriterWithDisabledCaching() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setContentType(TEXT_PLAIN_VALUE);

    byte[] responseBody = "Hello World".getBytes(UTF_8);
    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
      ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
      filterResponse.setContentType(APPLICATION_JSON_VALUE);
      FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
    };

    ShallowEtagHeaderFilter.disableContentCaching(request);
    this.filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("ETag")).isNull();
    assertThat(response.getContentType()).as("Invalid Content-Type header").isEqualTo(APPLICATION_JSON_VALUE);
    assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
  }

  @Test
  void filterSendError() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    MockHttpServletResponse response = new MockHttpServletResponse();

    byte[] responseBody = "Hello World".getBytes(UTF_8);
    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
      response.setContentLength(100);
      FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
      ((HttpServletResponse) filterResponse).sendError(HttpServletResponse.SC_FORBIDDEN);
    };
    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).as("Invalid status").isEqualTo(403);
    assertThat(response.getHeader("ETag")).as("Invalid ETag").isNull();
    assertThat(response.getContentLength()).as("Invalid Content-Length header").isEqualTo(100);
    assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
  }

  @Test
  void filterSendErrorMessage() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    MockHttpServletResponse response = new MockHttpServletResponse();

    byte[] responseBody = "Hello World".getBytes(UTF_8);
    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
      response.setContentLength(100);
      FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
      ((HttpServletResponse) filterResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "ERROR");
    };
    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).as("Invalid status").isEqualTo(403);
    assertThat(response.getHeader("ETag")).as("Invalid ETag").isNull();
    assertThat(response.getContentLength()).as("Invalid Content-Length header").isEqualTo(100);
    assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
    assertThat(response.getErrorMessage()).as("Invalid error message").isEqualTo("ERROR");
  }

  @Test
  void filterSendRedirect() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    MockHttpServletResponse response = new MockHttpServletResponse();

    byte[] responseBody = "Hello World".getBytes(UTF_8);
    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
      response.setContentLength(100);
      FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
      ((HttpServletResponse) filterResponse).sendRedirect("https://www.google.com");
    };
    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).as("Invalid status").isEqualTo(302);
    assertThat(response.getHeader("ETag")).as("Invalid ETag").isNull();
    assertThat(response.getContentLength()).as("Invalid Content-Length header").isEqualTo(100);
    assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
    assertThat(response.getRedirectedUrl()).as("Invalid redirect URL").isEqualTo("https://www.google.com");
  }

  @Test // SPR-13717
  public void filterFlushResponse() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hotels");
    MockHttpServletResponse response = new MockHttpServletResponse();

    byte[] responseBody = "Hello World".getBytes(UTF_8);
    FilterChain filterChain = (filterRequest, filterResponse) -> {
      assertThat(filterRequest).as("Invalid request passed").isEqualTo(request);
      ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
      FileCopyUtils.copy(responseBody, filterResponse.getOutputStream());
      filterResponse.flushBuffer();
    };
    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).as("Invalid status").isEqualTo(200);
    assertThat(response.getHeader("ETag")).as("Invalid ETag").isEqualTo("\"0b10a8db164e0754105b7a99be72e3fe5\"");
    assertThat(response.getContentLength()).as("Invalid Content-Length header").isGreaterThan(0);
    assertThat(response.getContentAsByteArray()).as("Invalid content").isEqualTo(responseBody);
  }

}