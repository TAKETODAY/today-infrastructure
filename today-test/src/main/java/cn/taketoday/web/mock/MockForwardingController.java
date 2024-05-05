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

package cn.taketoday.web.mock;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.mvc.AbstractController;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.mock.api.RequestDispatcher;
import cn.taketoday.mock.api.ServletException;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.api.http.HttpMockResponse;

/**
 * Controller implementation that forwards to a named servlet,
 * i.e. the "servlet-name" in web.xml rather than a URL path mapping.
 * A target servlet doesn't even need a "servlet-mapping" in web.xml
 * in the first place: A "servlet" declaration is sufficient.
 *
 * <p>Useful to invoke an existing servlet via Framework's dispatching infrastructure,
 * for example to apply Framework HandlerInterceptors to its requests. This will work
 * even in a minimal Servlet container that does not support Servlet filters.
 *
 * <p><b>Example:</b> web.xml, mapping all "/myservlet" requests to a Framework dispatcher.
 * Also defines a custom "myServlet", but <i>without</i> servlet mapping.
 *
 * <pre class="code">
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;myServlet&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;mypackage.TestServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;myDispatcher&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;cn.taketoday.web.mock.DispatcherServlet&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 *
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;myDispatcher&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/myservlet&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;</pre>
 *
 * <b>Example:</b> myDispatcher-servlet.xml, in turn forwarding "/myservlet" to your
 * servlet (identified by servlet name). All such requests will go through the
 * configured HandlerInterceptor chain (e.g. an OpenSessionInViewInterceptor).
 * From the servlet point of view, everything will work as usual.
 *
 * <pre class="code">
 * &lt;bean id="myServletForwardingController" class="cn.taketoday.web.mock.mvc.ServletForwardingController"&gt;
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
  private String servletName;

  @Nullable
  private String beanName;

  private MockContext mockContext;

  public MockForwardingController() {
    super(false);
  }

  /**
   * Set the name of the servlet to forward to,
   * i.e. the "servlet-name" of the target servlet in web.xml.
   * <p>Default is the bean name of this controller.
   */
  public void setServletName(@Nullable String servletName) {
    this.servletName = servletName;
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
    if (this.servletName == null) {
      this.servletName = name;
    }
  }

  @Nullable
  @Override
  protected ModelAndView handleRequestInternal(RequestContext request) throws Exception {
    MockContext mockContext = getMockContext();
    Assert.state(mockContext != null, "No MockContext");
    RequestDispatcher rd = mockContext.getNamedDispatcher(servletName);
    if (rd == null) {
      throw new ServletException("No servlet with name '%s' defined in web.xml".formatted(servletName));
    }

    HttpMockRequest servletRequest = MockUtils.getServletRequest(request);
    HttpMockResponse servletResponse = MockUtils.getServletResponse(request);

    // If already included, include again, else forward.
    if (useInclude(servletRequest, servletResponse)) {
      rd.include(servletRequest, servletResponse);
      if (logger.isTraceEnabled()) {
        logger.trace("Included servlet [{}] in ServletForwardingController '{}'", servletName, beanName);
      }
    }
    else {
      rd.forward(servletRequest, servletResponse);
      if (logger.isTraceEnabled()) {
        logger.trace("Forwarded to servlet [{}] in ServletForwardingController '{}'", servletName, beanName);
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
   * @see cn.taketoday.mock.api.RequestDispatcher#forward
   * @see cn.taketoday.mock.api.RequestDispatcher#include
   * @see MockResponse#isCommitted
   * @see MockUtils#isIncludeRequest
   */
  protected boolean useInclude(HttpMockRequest request, HttpMockResponse response) {
    return MockUtils.isIncludeRequest(request) || response.isCommitted();
  }

}
