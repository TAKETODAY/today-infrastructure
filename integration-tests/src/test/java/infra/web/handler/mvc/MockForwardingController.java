/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.handler.mvc;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.BeanNameAware;
import infra.lang.Assert;
import infra.mock.api.MockContext;
import infra.web.RequestContext;
import infra.web.mock.MockContextAware;
import infra.web.mock.MockWrappingController;

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

  private @Nullable String mockName;

  private @Nullable String beanName;

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

  @Override
  protected @Nullable Object handleRequestInternal(RequestContext request) throws Exception {
    MockContext mockContext = getMockContext();
    Assert.state(mockContext != null, "No MockContext");

    request.forward(mockName);
    if (logger.isTraceEnabled()) {
      logger.trace("Forwarded to servlet [{}] in MockForwardingController '{}'", mockName, beanName);
    }

    return NONE_RETURN_VALUE;
  }

}
