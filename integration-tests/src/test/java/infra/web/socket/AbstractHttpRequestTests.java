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
