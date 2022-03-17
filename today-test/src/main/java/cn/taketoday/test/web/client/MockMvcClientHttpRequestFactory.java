/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.web.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.mock.http.client.MockClientHttpRequest;
import cn.taketoday.mock.http.client.MockClientHttpResponse;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders;
import cn.taketoday.test.web.servlet.MockMvc;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * A {@link ClientHttpRequestFactory} for requests executed via {@link MockMvc}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockMvcClientHttpRequestFactory implements ClientHttpRequestFactory {

  private final MockMvc mockMvc;

  public MockMvcClientHttpRequestFactory(MockMvc mockMvc) {
    Assert.notNull(mockMvc, "MockMvc must not be null");
    this.mockMvc = mockMvc;
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
    return new MockClientHttpRequest(httpMethod, uri) {
      @Override
      public ClientHttpResponse executeInternal() {
        return getClientHttpResponse(httpMethod, uri, getHeaders(), getBodyAsBytes());
      }
    };
  }

  private ClientHttpResponse getClientHttpResponse(
          HttpMethod httpMethod, URI uri, HttpHeaders requestHeaders, byte[] requestBody) {

    try {
      MockHttpServletResponse servletResponse = this.mockMvc
              .perform(MockMvcRequestBuilders.request(httpMethod, uri).content(requestBody).headers(requestHeaders))
              .andReturn()
              .getResponse();

      HttpStatus status = HttpStatus.valueOf(servletResponse.getStatus());
      byte[] body = servletResponse.getContentAsByteArray();
      MockClientHttpResponse clientResponse = new MockClientHttpResponse(body, status);
      clientResponse.getHeaders().putAll(getResponseHeaders(servletResponse));
      return clientResponse;
    }
    catch (Exception ex) {
      byte[] body = ex.toString().getBytes(StandardCharsets.UTF_8);
      return new MockClientHttpResponse(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private HttpHeaders getResponseHeaders(MockHttpServletResponse response) {
    HttpHeaders headers = HttpHeaders.create();
    for (String name : response.getHeaderNames()) {
      List<String> values = response.getHeaders(name);
      for (String value : values) {
        headers.add(name, value);
      }
    }
    return headers;
  }

}
