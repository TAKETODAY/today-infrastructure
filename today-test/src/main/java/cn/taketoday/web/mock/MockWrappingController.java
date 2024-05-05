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

import java.util.Enumeration;
import java.util.Properties;

import cn.taketoday.beans.factory.BeanNameAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockApi;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.mvc.AbstractController;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.view.ModelAndView;

/**
 * Framework Controller implementation that wraps a servlet instance which it manages
 * internally. Such a wrapped servlet is not known outside of this controller;
 * its entire lifecycle is covered here (in contrast to {@link MockForwardingController}).
 *
 * <p>Useful to invoke an existing servlet via Framework's dispatching infrastructure,
 * for example to apply Framework HandlerInterceptors to its requests.
 *
 * <p>Note that Struts has a special requirement in that it parses {@code web.xml}
 * to find its servlet mapping. Therefore, you need to specify the DispatcherServlet's
 * servlet name as "servletName" on this controller, so that Struts finds the
 * DispatcherServlet's mapping (thinking that it refers to the ActionServlet).
 *
 * <p><b>Example:</b> a DispatcherServlet XML context, forwarding "*.do" to the Struts
 * ActionServlet wrapped by a ServletWrappingController. All such requests will go
 * through the configured HandlerInterceptor chain (e.g. an OpenSessionInViewInterceptor).
 * From the Struts point of view, everything will work as usual.
 *
 * <pre class="code">
 *
 * &lt;bean id="strutsWrappingController" class="cn.taketoday.web.mock.ServletWrappingController"&gt;
 *   &lt;property name="servletClass"&gt;
 *     &lt;value&gt;org.apache.struts.action.ActionServlet&lt;/value&gt;
 *   &lt;/property&gt;
 *   &lt;property name="servletName"&gt;
 *     &lt;value&gt;action&lt;/value&gt;
 *   &lt;/property&gt;
 *   &lt;property name="initParameters"&gt;
 *     &lt;props&gt;
 *       &lt;prop key="config"&gt;/WEB-INF/struts-config.xml&lt;/prop&gt;
 *     &lt;/props&gt;
 *   &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockForwardingController
 * @since 4.0 2022/2/8 17:18
 */
public class MockWrappingController extends AbstractController
        implements BeanNameAware, InitializingBean, DisposableBean, MockContextAware {

  @Nullable
  private Class<? extends MockApi> mockClass;

  @Nullable
  private String mockName;

  private Properties initParameters = new Properties();

  @Nullable
  private String beanName;

  @Nullable
  private MockApi mockApiInstance;

  private MockContext mockContext;

  public MockWrappingController() {
    super(false);
  }

  /**
   * Set the class of the servlet to wrap.
   * Needs to implement {@code cn.taketoday.mock.api.Servlet}.
   *
   * @see MockApi
   */
  public void setServletClass(@Nullable Class<? extends MockApi> servletClass) {
    this.mockClass = servletClass;
  }

  /**
   * Set the name of the servlet to wrap.
   * Default is the bean name of this controller.
   */
  public void setServletName(@Nullable String servletName) {
    this.mockName = servletName;
  }

  /**
   * Specify init parameters for the servlet to wrap,
   * as name-value pairs.
   */
  public void setInitParameters(Properties initParameters) {
    this.initParameters = initParameters;
  }

  @Override
  public void setBeanName(@Nullable String name) {
    this.beanName = name;
  }

  /**
   * Initialize the wrapped Servlet instance.
   *
   * @see MockApi#init(MockConfig)
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    if (this.mockClass == null) {
      throw new IllegalArgumentException("'servletClass' is required");
    }
    if (this.mockName == null) {
      this.mockName = this.beanName;
    }
    this.mockApiInstance = ReflectionUtils.accessibleConstructor(this.mockClass).newInstance();
    this.mockApiInstance.init(new DelegatingMockConfig());
  }

  /**
   * Invoke the wrapped Servlet instance.
   *
   * @see MockApi#service(MockRequest, MockResponse)
   */
  @Override
  protected ModelAndView handleRequestInternal(RequestContext request) throws Exception {
    MockRequestContext nativeContext = WebUtils.getNativeContext(request, MockRequestContext.class);
    Assert.state(nativeContext != null, "Not run in servlet");
    Assert.state(this.mockApiInstance != null, "No Servlet instance");
    this.mockApiInstance.service(nativeContext.getRequest(), nativeContext.getResponse());
    return null;
  }

  /**
   * Destroy the wrapped Servlet instance.
   *
   * @see MockApi#destroy()
   */
  @Override
  public void destroy() {
    if (this.mockApiInstance != null) {
      this.mockApiInstance.destroy();
    }
  }

  @Override
  public void setMockContext(MockContext mockContext) {
    this.mockContext = mockContext;
  }

  public MockContext getMockContext() {
    return mockContext;
  }

  /**
   * Internal implementation of the ServletConfig interface, to be passed
   * to the wrapped servlet. Delegates to ServletWrappingController fields
   * and methods to provide init parameters and other environment info.
   */
  private class DelegatingMockConfig implements MockConfig {

    @Override
    @Nullable
    public String getMockName() {
      return mockName;
    }

    @Override
    @Nullable
    public MockContext getMockContext() {
      return MockWrappingController.this.getMockContext();
    }

    @Override
    public String getInitParameter(String paramName) {
      return initParameters.getProperty(paramName);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Enumeration<String> getInitParameterNames() {
      return (Enumeration) initParameters.keys();
    }
  }

}
