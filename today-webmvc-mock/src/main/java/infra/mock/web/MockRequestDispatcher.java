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

package infra.mock.web;

import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.mock.api.RequestDispatcher;
import infra.mock.api.http.HttpMockResponseWrapper;

/**
 * Mock implementation of the {@link RequestDispatcher} interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see HttpMockRequestImpl#getRequestDispatcher(String)
 * @since 4.0
 */
public class MockRequestDispatcher implements RequestDispatcher {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final String resource;

  /**
   * Create a new MockRequestDispatcher for the given resource.
   *
   * @param resource the server resource to dispatch to, located at a
   * particular path or given by a particular name
   */
  public MockRequestDispatcher(String resource) {
    Assert.notNull(resource, "Resource is required");
    this.resource = resource;
  }

  @Override
  public void forward(MockRequest request, MockResponse response) {
    Assert.notNull(request, "Request is required");
    Assert.notNull(response, "Response is required");
    Assert.state(!response.isCommitted(), "Cannot perform forward - response is already committed");
    getMockHttpResponse(response).setForwardedUrl(this.resource);
    if (logger.isDebugEnabled()) {
      logger.debug("MockRequestDispatcher: forwarding to [" + this.resource + "]");
    }
  }

  @Override
  public void include(MockRequest request, MockResponse response) {
    Assert.notNull(request, "Request is required");
    Assert.notNull(response, "Response is required");
    getMockHttpResponse(response).addIncludedUrl(this.resource);
    if (logger.isDebugEnabled()) {
      logger.debug("MockRequestDispatcher: including [" + this.resource + "]");
    }
  }

  /**
   * Obtain the underlying {@link MockHttpResponseImpl}, unwrapping
   * {@link HttpMockResponseWrapper} decorators if necessary.
   */
  protected MockHttpResponseImpl getMockHttpResponse(MockResponse response) {
    if (response instanceof MockHttpResponseImpl) {
      return (MockHttpResponseImpl) response;
    }
    if (response instanceof HttpMockResponseWrapper) {
      return getMockHttpResponse(((HttpMockResponseWrapper) response).getResponse());
    }
    throw new IllegalArgumentException("MockRequestDispatcher requires MockHttpServletResponse");
  }

}
