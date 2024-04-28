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

package cn.taketoday.web.testfixture.servlet;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.mock.RequestDispatcher;
import cn.taketoday.web.mock.ServletRequest;
import cn.taketoday.web.mock.ServletResponse;
import cn.taketoday.web.mock.http.HttpServletResponseWrapper;

/**
 * Mock implementation of the {@link cn.taketoday.web.mock.RequestDispatcher} interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see MockHttpServletRequest#getRequestDispatcher(String)
 * @since 3.0
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
  public void forward(ServletRequest request, ServletResponse response) {
    Assert.notNull(request, "Request is required");
    Assert.notNull(response, "Response is required");
    Assert.state(!response.isCommitted(), "Cannot perform forward - response is already committed");
    getMockHttpServletResponse(response).setForwardedUrl(this.resource);
    if (logger.isDebugEnabled()) {
      logger.debug("MockRequestDispatcher: forwarding to [" + this.resource + "]");
    }
  }

  @Override
  public void include(ServletRequest request, ServletResponse response) {
    Assert.notNull(request, "Request is required");
    Assert.notNull(response, "Response is required");
    getMockHttpServletResponse(response).addIncludedUrl(this.resource);
    if (logger.isDebugEnabled()) {
      logger.debug("MockRequestDispatcher: including [" + this.resource + "]");
    }
  }

  /**
   * Obtain the underlying {@link MockHttpServletResponse}, unwrapping
   * {@link HttpServletResponseWrapper} decorators if necessary.
   */
  protected MockHttpServletResponse getMockHttpServletResponse(ServletResponse response) {
    if (response instanceof MockHttpServletResponse) {
      return (MockHttpServletResponse) response;
    }
    if (response instanceof HttpServletResponseWrapper) {
      return getMockHttpServletResponse(((HttpServletResponseWrapper) response).getResponse());
    }
    throw new IllegalArgumentException("MockRequestDispatcher requires MockHttpServletResponse");
  }

}
