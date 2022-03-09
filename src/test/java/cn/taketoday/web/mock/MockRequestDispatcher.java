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

package cn.taketoday.web.mock;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

/**
 * Mock implementation of the {@link jakarta.servlet.RequestDispatcher} interface.
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
   * @param resource
   *         the server resource to dispatch to, located at a
   *         particular path or given by a particular name
   */
  public MockRequestDispatcher(String resource) {
    Assert.notNull(resource, "Resource must not be null");
    this.resource = resource;
  }

  @Override
  public void forward(ServletRequest request, ServletResponse response) {
    Assert.notNull(request, "Request must not be null");
    Assert.notNull(response, "Response must not be null");
    Assert.state(!response.isCommitted(), "Cannot perform forward - response is already committed");
    getMockHttpServletResponse(response).setForwardedUrl(this.resource);
    if (logger.isDebugEnabled()) {
      logger.debug("MockRequestDispatcher: forwarding to [" + this.resource + "]");
    }
  }

  @Override
  public void include(ServletRequest request, ServletResponse response) {
    Assert.notNull(request, "Request must not be null");
    Assert.notNull(response, "Response must not be null");
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
