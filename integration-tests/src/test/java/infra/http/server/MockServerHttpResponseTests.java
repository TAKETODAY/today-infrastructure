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

package infra.http.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.HttpStatus;
import infra.http.MediaType;
import infra.mock.web.MockHttpResponseImpl;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class MockServerHttpResponseTests {

  private MockServerHttpResponse response;

  private MockHttpResponseImpl mockResponse;

  @BeforeEach
  void create() {
    mockResponse = new MockHttpResponseImpl();
    response = new MockServerHttpResponse(mockResponse);
  }

  @Test
  void setStatusCode() {
    response.setStatusCode(HttpStatus.NOT_FOUND);
    assertThat(mockResponse.getStatus()).as("Invalid status code").isEqualTo(404);
  }

  @Test
  void getHeaders() {
    HttpHeaders headers = response.getHeaders();
    String headerName = "MyHeader";
    String headerValue1 = "value1";
    headers.add(headerName, headerValue1);
    String headerValue2 = "value2";
    headers.add(headerName, headerValue2);
    headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));

    response.close();
    assertThat(mockResponse.getHeaderNames().contains(headerName)).as("Header not set").isTrue();
    List<String> headerValues = mockResponse.getHeaders(headerName);
    assertThat(headerValues.contains(headerValue1)).as("Header not set").isTrue();
    assertThat(headerValues.contains(headerValue2)).as("Header not set").isTrue();
    assertThat(mockResponse.getHeader("Content-Type")).as("Invalid Content-Type").isEqualTo("text/plain;charset=UTF-8");
    assertThat(mockResponse.getContentType()).as("Invalid Content-Type").isEqualTo("text/plain;charset=UTF-8");
    assertThat(mockResponse.getCharacterEncoding()).as("Invalid Content-Type").isEqualTo("UTF-8");
  }

  @Test
  void getHeadersWithNoContentType() {
    this.response = new MockServerHttpResponse(this.mockResponse);
    assertThat(this.response.getHeaders().get(HttpHeaders.CONTENT_TYPE)).isNull();
  }

  @Test
  void getHeadersWithContentType() {
    this.mockResponse.setContentType(MediaType.TEXT_PLAIN_VALUE);
    this.response = new MockServerHttpResponse(this.mockResponse);
    assertThat(this.response.getHeaders().get(HttpHeaders.CONTENT_TYPE)).containsExactly(MediaType.TEXT_PLAIN_VALUE);
  }

  @Test
  void preExistingHeadersFromHttpMockResponse() {
    String headerName = "Access-Control-Allow-Origin";
    String headerValue = "localhost:8080";

    this.mockResponse.addHeader(headerName, headerValue);
    this.mockResponse.setContentType("text/csv");
    this.response = new MockServerHttpResponse(this.mockResponse);

    assertThat(this.response.getHeaders().getFirst(headerName)).isEqualTo(headerValue);
    assertThat(this.response.getHeaders().get(headerName)).containsExactly(headerValue);
    assertThat(this.response.getHeaders()).containsKey(headerName);
    assertThat(this.response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(headerValue);
  }

  @Test
    // gh-25490
  void preExistingContentTypeIsOverriddenImmediately() {
    this.mockResponse.setContentType("text/csv");
    this.response = new MockServerHttpResponse(this.mockResponse);
    this.response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
  }

  @Test
  void getBody() throws Exception {
    byte[] content = "Hello World".getBytes(StandardCharsets.UTF_8);
    FileCopyUtils.copy(content, response.getBody());

    assertThat(mockResponse.getContentAsByteArray()).as("Invalid content written").isEqualTo(content);
  }

}
