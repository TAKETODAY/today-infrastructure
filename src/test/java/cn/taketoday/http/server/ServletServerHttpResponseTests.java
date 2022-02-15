/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.http.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.http.MediaType;
import cn.taketoday.web.mock.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class ServletServerHttpResponseTests {

  private ServletServerHttpResponse response;

  private MockHttpServletResponse mockResponse;

  @BeforeEach
  void create() {
    mockResponse = new MockHttpServletResponse();
    response = new ServletServerHttpResponse(mockResponse);
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
  void preExistingHeadersFromHttpServletResponse() {
    String headerName = "Access-Control-Allow-Origin";
    String headerValue = "localhost:8080";

    this.mockResponse.addHeader(headerName, headerValue);
    this.mockResponse.setContentType("text/csv");
    this.response = new ServletServerHttpResponse(this.mockResponse);

    assertThat(this.response.getHeaders().getFirst(headerName)).isEqualTo(headerValue);
    assertThat(this.response.getHeaders().get(headerName)).containsExactly(headerValue);
    assertThat(this.response.getHeaders()).containsKey(headerName);
    assertThat(this.response.getHeaders().getAccessControlAllowOrigin()).isEqualTo(headerValue);
  }

  @Test
    // gh-25490
  void preExistingContentTypeIsOverriddenImmediately() {
    this.mockResponse.setContentType("text/csv");
    this.response = new ServletServerHttpResponse(this.mockResponse);
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
