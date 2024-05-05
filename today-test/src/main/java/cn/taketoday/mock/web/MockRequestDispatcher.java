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

package cn.taketoday.mock.web;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.mock.api.RequestDispatcher;
import cn.taketoday.mock.api.http.HttpMockResponseWrapper;

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
