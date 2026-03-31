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

package infra.test.web.mock.client;

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
import infra.mock.api.http.Cookie;
import infra.mock.http.client.MockClientHttpRequest;
import infra.mock.http.client.MockClientHttpResponse;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.request.MockHttpRequestBuilder;
import infra.util.ObjectUtils;
import infra.util.StringUtils;

import static infra.test.web.mock.request.MockMvcRequestBuilders.request;

/**
 * {@link ClientHttpRequestFactory} for requests executed via {@link MockMvc}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Worsnop
 * @since 5.0
 */
public class MockMvcClientHttpRequestFactory implements ClientHttpRequestFactory {

  private final MockMvc mockMvc;

  /**
   * Constructor with a MockMvc instance to perform requests with.
   */
  public MockMvcClientHttpRequestFactory(MockMvc mockMvc) {
    Assert.notNull(mockMvc, "MockMvc is required");
    this.mockMvc = mockMvc;
  }

  @Override
  public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
    return new MockMvcClientHttpRequest(httpMethod, uri);
  }

  /**
   * {@link ClientHttpRequest} that executes via MockMvc.
   */
  private class MockMvcClientHttpRequest extends MockClientHttpRequest {

    MockMvcClientHttpRequest(HttpMethod httpMethod, URI uri) {
      super(httpMethod, uri);
    }

    @Override
    public ClientHttpResponse executeInternal() {
      try {
        var servletRequestBuilder = request(getMethod(), getURI())
                .headers(getHeaders())
                .content(getBodyAsBytes());

        addCookies(servletRequestBuilder);

        MockHttpResponseImpl servletResponse = MockMvcClientHttpRequestFactory.this.mockMvc
                .perform(servletRequestBuilder)
                .andReturn()
                .getResponse();

        MockClientHttpResponse clientResponse = new MockClientHttpResponse(
                getResponseBody(servletResponse),
                HttpStatusCode.valueOf(servletResponse.getStatus()));

        copyHeaders(servletResponse, clientResponse);

        return clientResponse;
      }
      catch (Exception ex) {
        byte[] body = ex.toString().getBytes(StandardCharsets.UTF_8);
        return new MockClientHttpResponse(body, HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

    private void addCookies(MockHttpRequestBuilder requestBuilder) {
      List<String> values = getHeaders().get(HttpHeaders.COOKIE);
      if (!ObjectUtils.isEmpty(values)) {
        values.stream()
                .flatMap(header -> StringUtils.commaDelimitedListToSet(header).stream())
                .map(value -> {
                  String[] parts = StringUtils.split(value, "=");
                  Assert.isTrue(parts != null && parts.length == 2, "Invalid cookie: '" + value + "'");
                  return new Cookie(parts[0], parts[1]);
                })
                .forEach(requestBuilder::cookie);
      }
    }

    private static byte[] getResponseBody(MockHttpResponseImpl servletResponse) {
      byte[] body = servletResponse.getContentAsByteArray();
      if (body.length == 0) {
        String error = servletResponse.getErrorMessage();
        if (StringUtils.isNotEmpty(error)) {
          body = error.getBytes(StandardCharsets.UTF_8);
        }
      }
      return body;
    }

    private static void copyHeaders(
            MockHttpResponseImpl servletResponse, MockClientHttpResponse clientResponse) {

      servletResponse.getHeaderNames()
              .forEach(name -> servletResponse.getHeaders(name)
                      .forEach(value -> clientResponse.getHeaders().add(name, value)));
    }
  }

}
