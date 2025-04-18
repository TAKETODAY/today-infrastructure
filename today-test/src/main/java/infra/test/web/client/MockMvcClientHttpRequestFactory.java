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

package infra.test.web.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.client.ClientHttpRequest;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.mock.http.client.MockClientHttpRequest;
import infra.mock.http.client.MockClientHttpResponse;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.web.mock.MockMvc;
import infra.util.StringUtils;

import static infra.test.web.mock.request.MockMvcRequestBuilders.request;

/**
 * A {@link ClientHttpRequestFactory} for requests executed via {@link MockMvc}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MockMvcClientHttpRequestFactory implements ClientHttpRequestFactory {

  private final MockMvc mockMvc;

  public MockMvcClientHttpRequestFactory(MockMvc mockMvc) {
    Assert.notNull(mockMvc, "MockMvc is required");
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
      MockHttpResponseImpl servletResponse = this.mockMvc
              .perform(request(httpMethod, uri).content(requestBody).headers(requestHeaders))
              .andReturn()
              .getResponse();

      HttpStatusCode status = HttpStatusCode.valueOf(servletResponse.getStatus());
      byte[] body = servletResponse.getContentAsByteArray();
      if (body.length == 0) {
        String error = servletResponse.getErrorMessage();
        if (StringUtils.isNotEmpty(error)) {
          // sendError message as default body
          body = error.getBytes(StandardCharsets.UTF_8);
        }
      }

      MockClientHttpResponse clientResponse = new MockClientHttpResponse(body, status);
      clientResponse.getHeaders().putAll(getResponseHeaders(servletResponse));
      return clientResponse;
    }
    catch (Exception ex) {
      byte[] body = ex.toString().getBytes(StandardCharsets.UTF_8);
      return new MockClientHttpResponse(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private HttpHeaders getResponseHeaders(MockHttpResponseImpl response) {
    HttpHeaders headers = HttpHeaders.forWritable();
    for (String name : response.getHeaderNames()) {
      List<String> values = response.getHeaders(name);
      for (String value : values) {
        headers.add(name, value);
      }
    }
    return headers;
  }

}
