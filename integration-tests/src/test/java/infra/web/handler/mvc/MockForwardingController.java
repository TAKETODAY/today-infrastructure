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

package infra.web.handler.mvc;

import infra.beans.factory.BeanNameAware;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.mock.api.MockContext;
import infra.mock.api.MockException;
import infra.mock.api.MockResponse;
import infra.mock.api.RequestDispatcher;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;
import infra.web.RequestContext;
import infra.web.mock.MockContextAware;
import infra.web.mock.MockUtils;
import infra.web.mock.MockWrappingController;
import infra.web.view.ModelAndView;

/**
 * Controller implementation that forwards to a named servlet,
 * i.e. the "servlet-name" in web.xml rather than a URL path mapping.
 * A target servlet doesn't even need a "servlet-mapping" in web.xml
 * in the first place: A "servlet" declaration is sufficient.
 *
 * <p>Useful to invoke an existing servlet via Framework's dispatching infrastructure,
 * for example to apply Framework HandlerInterceptors to its requests. This will work
 * even in a minimal Mock container that does not support Mock filters.
 *
 * <b>Example:</b> myDispatcher-servlet.xml, in turn forwarding "/myservlet" to your
 * servlet (identified by servlet name). All such requests will go through the
 * configured HandlerInterceptor chain (e.g. an OpenSessionInViewInterceptor).
 * From the servlet point of view, everything will work as usual.
 *
 * <pre class="code">
 * &lt;bean id="myServletForwardingController" class="infra.web.mock.mvc.ServletForwardingController"&gt;
 *   &lt;property name="servletName"&gt;&lt;value&gt;myServlet&lt;/value&gt;&lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockWrappingController
 * @since 4.0 2022/2/8 17:18
 */
public class MockForwardingController extends AbstractController implements BeanNameAware, MockContextAware {

  @Nullable
  private String mockName;

  @Nullable
  private String beanName;

  private MockContext mockContext;

  public MockForwardingController() {
    super(false);
  }

  /**
   * Set the name of the servlet to forward to,
   * i.e. the "mock-name" of the target servlet in web.xml.
   * <p>Default is the bean name of this controller.
   */
  public void setMockName(@Nullable String mockName) {
    this.mockName = mockName;
  }

  @Override
  public void setMockContext(MockContext mockContext) {
    this.mockContext = mockContext;
  }

  public MockContext getMockContext() {
    return mockContext;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
    if (this.mockName == null) {
      this.mockName = name;
    }
  }

  @Nullable
  @Override
  protected ModelAndView handleRequestInternal(RequestContext request) throws Exception {
    MockContext mockContext = getMockContext();
    Assert.state(mockContext != null, "No MockContext");
    RequestDispatcher rd = mockContext.getNamedDispatcher(mockName);
    if (rd == null) {
      throw new MockException("No servlet with name '%s' defined in web.xml".formatted(mockName));
    }

    HttpMockRequest servletRequest = MockUtils.getMockRequest(request);
    HttpMockResponse servletResponse = MockUtils.getMockResponse(request);

    // If already included, include again, else forward.
    if (useInclude(servletRequest, servletResponse)) {
      rd.include(servletRequest, servletResponse);
      if (logger.isTraceEnabled()) {
        logger.trace("Included servlet [{}] in MockForwardingController '{}'", mockName, beanName);
      }
    }
    else {
      rd.forward(servletRequest, servletResponse);
      if (logger.isTraceEnabled()) {
        logger.trace("Forwarded to servlet [{}] in MockForwardingController '{}'", mockName, beanName);
      }
    }

    return null;
  }

  /**
   * Determine whether to use RequestDispatcher's {@code include} or {@code forward} method.
   * <p>Performs a check whether an include URI attribute is found in the request,
   * indicating an include request, and whether the response has already been committed.
   * In both cases, an include will be performed, as a forward is not possible anymore.
   *
   * @param request current HTTP request
   * @param response current HTTP response
   * @return {@code true} for include, {@code false} for forward
   * @see RequestDispatcher#forward
   * @see RequestDispatcher#include
   * @see MockResponse#isCommitted
   * @see MockUtils#isIncludeRequest
   */
  protected boolean useInclude(HttpMockRequest request, HttpMockResponse response) {
    return MockUtils.isIncludeRequest(request) || response.isCommitted();
  }

}
