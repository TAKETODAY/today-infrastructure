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

package infra.web.socket;

import org.junit.jupiter.api.BeforeEach;

import infra.http.server.ServerHttpRequest;
import infra.http.server.ServerHttpResponse;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

/**
 * Base class for tests using {@link ServerHttpRequest} and {@link ServerHttpResponse}.
 *
 * @author Rossen Stoyanchev
 */
public abstract class AbstractHttpRequestTests {

  protected RequestContext request;

  protected HttpMockRequestImpl mockRequest;

  protected MockHttpResponseImpl mockResponse;

  @BeforeEach
  protected void setup() {
    resetRequestAndResponse();
  }

  protected void setRequest(String method, String requestUri) {
    this.mockRequest.setMethod(method);
    this.mockRequest.setRequestURI(requestUri);
    this.request = new MockRequestContext(null, this.mockRequest, mockResponse);
  }

  protected void resetRequestAndResponse() {
    resetResponse();
    resetRequest();
  }

  protected void resetRequest() {
    this.mockRequest = new HttpMockRequestImpl();
    this.mockRequest.setAsyncSupported(true);
    this.request = new MockRequestContext(null, this.mockRequest, mockResponse);
  }

  protected void resetResponse() {
    this.mockResponse = new MockHttpResponseImpl();
  }

}
