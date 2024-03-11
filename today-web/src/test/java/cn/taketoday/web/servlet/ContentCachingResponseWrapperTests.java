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

package cn.taketoday.web.servlet;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import cn.taketoday.http.MediaType;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import static cn.taketoday.http.HttpHeaders.CONTENT_LENGTH;
import static cn.taketoday.http.HttpHeaders.CONTENT_TYPE;
import static cn.taketoday.http.HttpHeaders.TRANSFER_ENCODING;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/11 13:53
 */
class ContentCachingResponseWrapperTests {

  @Test
  void copyBodyToResponse() throws Exception {
    byte[] responseBody = "Hello World".getBytes(UTF_8);
    MockHttpServletResponse response = new MockHttpServletResponse();

    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
    responseWrapper.setStatus(HttpServletResponse.SC_CREATED);
    FileCopyUtils.copy(responseBody, responseWrapper.getOutputStream());
    responseWrapper.copyBodyToResponse();

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_CREATED);
    assertThat(response.getContentLength()).isGreaterThan(0);
    assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
  }

  @Test
  void copyBodyToResponseWithPresetHeaders() throws Exception {
    String PUZZLE = "puzzle";
    String ENIGMA = "enigma";
    String NUMBER = "number";
    String MAGIC = "42";

    byte[] responseBody = "Hello World".getBytes(UTF_8);
    int responseLength = responseBody.length;
    int originalContentLength = 999;
    String contentType = MediaType.APPLICATION_JSON_VALUE;

    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setContentType(contentType);
    response.setContentLength(originalContentLength);
    response.setHeader(PUZZLE, ENIGMA);
    response.setIntHeader(NUMBER, 42);

    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
    responseWrapper.setStatus(HttpServletResponse.SC_CREATED);

    assertThat(responseWrapper.getStatus()).isEqualTo(HttpServletResponse.SC_CREATED);
    assertThat(responseWrapper.getContentSize()).isZero();
    assertThat(responseWrapper.getHeaderNames())
            .containsExactlyInAnyOrder(PUZZLE, NUMBER, CONTENT_TYPE, CONTENT_LENGTH);

    assertHeader(responseWrapper, PUZZLE, ENIGMA);
    assertHeader(responseWrapper, NUMBER, MAGIC);
    assertHeader(responseWrapper, CONTENT_LENGTH, originalContentLength);
    assertContentTypeHeader(responseWrapper, contentType);

    FileCopyUtils.copy(responseBody, responseWrapper.getOutputStream());
    assertThat(responseWrapper.getContentSize()).isEqualTo(responseLength);

    responseWrapper.copyBodyToResponse();

    assertThat(responseWrapper.getStatus()).isEqualTo(HttpServletResponse.SC_CREATED);
    assertThat(responseWrapper.getContentSize()).isZero();
    assertThat(responseWrapper.getHeaderNames())
            .containsExactlyInAnyOrder(PUZZLE, NUMBER, CONTENT_TYPE, CONTENT_LENGTH);

    assertHeader(responseWrapper, PUZZLE, ENIGMA);
    assertHeader(responseWrapper, NUMBER, MAGIC);
    assertHeader(responseWrapper, CONTENT_LENGTH, responseLength);
    assertContentTypeHeader(responseWrapper, contentType);

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_CREATED);
    assertThat(response.getContentLength()).isEqualTo(responseLength);
    assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
    assertThat(response.getHeaderNames())
            .containsExactlyInAnyOrder(PUZZLE, NUMBER, CONTENT_TYPE, CONTENT_LENGTH);

    assertHeader(response, PUZZLE, ENIGMA);
    assertHeader(response, NUMBER, MAGIC);
    assertHeader(response, CONTENT_LENGTH, responseLength);
    assertContentTypeHeader(response, contentType);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("setContentLengthFunctions")
  void copyBodyToResponseWithOverridingContentLength(SetContentLength setContentLength) throws Exception {
    byte[] responseBody = "Hello World".getBytes(UTF_8);
    int responseLength = responseBody.length;
    int originalContentLength = 11;
    int overridingContentLength = 22;

    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setContentLength(originalContentLength);

    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
    responseWrapper.setContentLength(overridingContentLength);

    setContentLength.invoke(responseWrapper, overridingContentLength);

    assertThat(responseWrapper.getContentSize()).isZero();
    assertThat(responseWrapper.getHeaderNames()).containsExactlyInAnyOrder(CONTENT_LENGTH);

    assertHeader(response, CONTENT_LENGTH, originalContentLength);
    assertHeader(responseWrapper, CONTENT_LENGTH, overridingContentLength);

    FileCopyUtils.copy(responseBody, responseWrapper.getOutputStream());
    assertThat(responseWrapper.getContentSize()).isEqualTo(responseLength);

    responseWrapper.copyBodyToResponse();

    assertThat(responseWrapper.getContentSize()).isZero();
    assertThat(responseWrapper.getHeaderNames()).containsExactlyInAnyOrder(CONTENT_LENGTH);

    assertHeader(response, CONTENT_LENGTH, responseLength);
    assertHeader(responseWrapper, CONTENT_LENGTH, responseLength);

    assertThat(response.getContentLength()).isEqualTo(responseLength);
    assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
    assertThat(response.getHeaderNames()).containsExactlyInAnyOrder(CONTENT_LENGTH);
  }

  private static Stream<Named<SetContentLength>> setContentLengthFunctions() {
    return Stream.of(
            named("setContentLength()", HttpServletResponse::setContentLength),
            named("setContentLengthLong()", HttpServletResponse::setContentLengthLong),
            named("setIntHeader()", (response, contentLength) -> response.setIntHeader(CONTENT_LENGTH, contentLength)),
            named("addIntHeader()", (response, contentLength) -> response.addIntHeader(CONTENT_LENGTH, contentLength)),
            named("setHeader()", (response, contentLength) -> response.setHeader(CONTENT_LENGTH, "" + contentLength)),
            named("addHeader()", (response, contentLength) -> response.addHeader(CONTENT_LENGTH, "" + contentLength))
    );
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("setContentTypeFunctions")
  void copyBodyToResponseWithOverridingContentType(SetContentType setContentType) throws Exception {
    byte[] responseBody = "Hello World".getBytes(UTF_8);
    int responseLength = responseBody.length;
    String originalContentType = MediaType.TEXT_PLAIN_VALUE;
    String overridingContentType = MediaType.APPLICATION_JSON_VALUE;

    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setContentType(originalContentType);

    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

    assertContentTypeHeader(response, originalContentType);
    assertContentTypeHeader(responseWrapper, originalContentType);

    setContentType.invoke(responseWrapper, overridingContentType);

    assertThat(responseWrapper.getContentSize()).isZero();
    assertThat(responseWrapper.getHeaderNames()).containsExactlyInAnyOrder(CONTENT_TYPE);

    assertContentTypeHeader(response, overridingContentType);
    assertContentTypeHeader(responseWrapper, overridingContentType);

    FileCopyUtils.copy(responseBody, responseWrapper.getOutputStream());
    assertThat(responseWrapper.getContentSize()).isEqualTo(responseLength);

    responseWrapper.copyBodyToResponse();

    assertThat(responseWrapper.getContentSize()).isZero();
    assertThat(responseWrapper.getHeaderNames()).containsExactlyInAnyOrder(CONTENT_TYPE, CONTENT_LENGTH);

    assertHeader(response, CONTENT_LENGTH, responseLength);
    assertHeader(responseWrapper, CONTENT_LENGTH, responseLength);
    assertContentTypeHeader(response, overridingContentType);
    assertContentTypeHeader(responseWrapper, overridingContentType);

    assertThat(response.getContentLength()).isEqualTo(responseLength);
    assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
    assertThat(response.getHeaderNames()).containsExactlyInAnyOrder(CONTENT_TYPE, CONTENT_LENGTH);
  }

  private static Stream<Named<SetContentType>> setContentTypeFunctions() {
    return Stream.of(
            named("setContentType()", HttpServletResponse::setContentType),
            named("setHeader()", (response, contentType) -> response.setHeader(CONTENT_TYPE, contentType)),
            named("addHeader()", (response, contentType) -> response.addHeader(CONTENT_TYPE, contentType))
    );
  }

  @Test
  void copyBodyToResponseWithTransferEncoding() throws Exception {
    byte[] responseBody = "6\r\nHello 5\r\nWorld0\r\n\r\n".getBytes(UTF_8);
    MockHttpServletResponse response = new MockHttpServletResponse();

    ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
    responseWrapper.setStatus(HttpServletResponse.SC_CREATED);
    responseWrapper.setHeader(TRANSFER_ENCODING, "chunked");
    FileCopyUtils.copy(responseBody, responseWrapper.getOutputStream());
    responseWrapper.copyBodyToResponse();

    assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_CREATED);
    assertHeader(response, TRANSFER_ENCODING, "chunked");
    assertHeader(response, CONTENT_LENGTH, null);
    assertThat(response.getContentAsByteArray()).isEqualTo(responseBody);
  }

  private void assertHeader(HttpServletResponse response, String header, int value) {
    assertHeader(response, header, Integer.toString(value));
  }

  private void assertHeader(HttpServletResponse response, String header, String value) {
    if (value == null) {
      assertThat(response.containsHeader(header)).as(header).isFalse();
      assertThat(response.getHeader(header)).as(header).isNull();
      assertThat(response.getHeaders(header)).as(header).isEmpty();
    }
    else {
      assertThat(response.containsHeader(header)).as(header).isTrue();
      assertThat(response.getHeader(header)).as(header).isEqualTo(value);
      assertThat(response.getHeaders(header)).as(header).containsExactly(value);
    }
  }

  private void assertContentTypeHeader(HttpServletResponse response, String contentType) {
    assertHeader(response, CONTENT_TYPE, contentType);
    assertThat(response.getContentType()).as(CONTENT_TYPE).isEqualTo(contentType);
  }

  @FunctionalInterface
  private interface SetContentLength {
    void invoke(HttpServletResponse response, int contentLength);
  }

  @FunctionalInterface
  private interface SetContentType {
    void invoke(HttpServletResponse response, String contentType);
  }

}