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

import java.io.Serial;
import java.io.Serializable;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.DispatcherType;
import cn.taketoday.mock.api.MockApi;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockException;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.mock.api.http.HttpMockRequest;
import cn.taketoday.mock.api.http.HttpMockResponse;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.web.DispatcherHandler;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.async.WebAsyncManager;
import cn.taketoday.web.mock.support.StandardMockEnvironment;
import cn.taketoday.web.mock.support.WebApplicationContextUtils;

/**
 * Central dispatcher for HTTP request handlers/controllers in Servlet
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.0 2018-06-25 19:47:14
 */
public class DispatcherServlet extends DispatcherHandler implements MockApi, Serializable {
  /**
   * Prefix for ApplicationContext ids that refer to context path
   */
  public static final String APPLICATION_CONTEXT_ID_PREFIX = ApplicationContext.class.getName() + ":";

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Prefix for the MockContext attribute for the ApplicationContext.
   * The completion is the servlet name.
   */
  public static final String CONTEXT_PREFIX = DispatcherServlet.class.getName() + ".CONTEXT.";

  private transient MockConfig mockConfig;

  /** MockContext attribute to find the WebApplicationContext in. */
  @Nullable
  private String contextAttribute;

  /** Should we publish the context as a MockContext attribute?. */
  private boolean publishContext = true;

  public DispatcherServlet() { }

  /**
   * Create a new {@code InfraHandler} with the given application context. This
   * constructor is useful in Servlet environments where instance-based registration
   * of servlets is possible through the {@link MockContext#addServlet} API.
   * <p>Using this constructor indicates that the following properties / init-params
   * will be ignored:
   * <ul>
   * <li>{@link #setContextClass(Class)} / 'contextClass'</li>
   * <li>{@link #setContextConfigLocation(String)} / 'contextConfigLocation'</li>
   * </ul>
   * <p>The given application context may or may not yet be {@linkplain
   * ConfigurableApplicationContext#refresh() refreshed}. If it (a) is an implementation
   * of {@link ConfigurableApplicationContext} and (b) has <strong>not</strong>
   * already been refreshed (the recommended approach), then the following will occur:
   * <ul>
   * <li>If the given context does not already have a {@linkplain
   * ConfigurableApplicationContext#setParent parent}, the root application context
   * will be set as the parent.</li>
   * <li>If the given context has not already been assigned an {@linkplain
   * ConfigurableApplicationContext#setId id}, one will be assigned to it</li>
   * <li>{@code MockContext} and {@code ServletConfig} objects will be delegated to
   * the application context</li>
   * <li>{@link #postProcessApplicationContext} will be called</li>
   * <li>Any {@link ApplicationContextInitializer ApplicationContextInitializers} specified through the
   * "contextInitializerClasses" init-param or through the {@link
   * #addContextInitializers} property will be applied.</li>
   * <li>{@link ConfigurableApplicationContext#refresh refresh()} will be called</li>
   * </ul>
   * If the context has already been refreshed or does not implement
   * {@code ConfigurableApplicationContext}, none of the above will occur under the
   * assumption that the user has performed these actions (or not) per his or her
   * specific needs.
   *
   * @param context the context to use
   * @see #initApplicationContext
   * @see #configureAndRefreshApplicationContext
   */
  public DispatcherServlet(ApplicationContext context) {
    super(context);
  }

  /**
   * Set the name of the MockContext attribute which should be used to retrieve the
   * {@link WebApplicationContext} that this servlet is supposed to use.
   *
   * @since 4.0
   */
  public void setContextAttribute(@Nullable String contextAttribute) {
    this.contextAttribute = contextAttribute;
  }

  /**
   * Return the name of the MockContext attribute which should be used to retrieve the
   * {@link WebApplicationContext} that this servlet is supposed to use.
   *
   * @since 4.0
   */
  @Nullable
  public String getContextAttribute() {
    return this.contextAttribute;
  }

  /**
   * Set whether to publish this servlet's context as a MockContext attribute,
   * available to all objects in the web container. Default is "true".
   * <p>This is especially handy during testing, although it is debatable whether
   * it's good practice to let other application objects access the context this way.
   *
   * @since 4.0
   */
  public void setPublishContext(boolean publishContext) {
    this.publishContext = publishContext;
  }

  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardMockEnvironment();
  }

  @Override
  public void init(MockConfig mockConfig) {
    this.mockConfig = mockConfig;
    String servletName = mockConfig.getMockName();
    mockConfig.getMockContext().log("Initializing Infra %s '%s'".formatted(getClass().getSimpleName(), servletName));
    log.info("Initializing Servlet '{}'", servletName);

    init();
  }

  @Override
  protected void afterApplicationContextInit() {
    if (publishContext) {
      // Publish the context as a servlet context attribute.
      String attrName = getMockContextAttributeName();
      getMockContext().setAttribute(attrName, getApplicationContext());
    }
  }

  @Override
  protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
    super.postProcessApplicationContext(context);

    if (context instanceof ConfigurableWebApplicationContext wac) {
      wac.setMockContext(getMockContext());
      wac.setMockConfig(getMockConfig());
    }

    // The wac environment's #initPropertySources will be called in any case when the context
    // is refreshed; do it eagerly here to ensure servlet property sources are in place for
    // use in any post-processing or initialization that occurs below prior to #refresh
    ConfigurableEnvironment env = context.getEnvironment();
    if (env instanceof ConfigurableWebEnvironment cwe) {
      cwe.initPropertySources(getMockContext(), getMockConfig());
    }
  }

  @Override
  protected void applyDefaultContextId(ConfigurableApplicationContext context) {
    // Generate default id...
    context.setId(APPLICATION_CONTEXT_ID_PREFIX +
            ObjectUtils.getDisplayString(getMockContext()) + '/' + getServletName());
  }

  @Override
  protected ApplicationContext getRootApplicationContext() {
    return WebApplicationContextUtils.getWebApplicationContext(getMockContext());
  }

  /**
   * Returns a reference to the {@link MockContext} in which this
   * servlet is running. See {@link MockConfig#getMockContext}.
   *
   * <p>
   * This method is supplied for convenience. It gets the context
   * from the servlet's <code>ServletConfig</code> object.
   *
   * @return MockContext the <code>MockContext</code>
   * object passed to this servlet by the <code>init</code> method
   */
  public MockContext getMockContext() {
    return getMockConfig().getMockContext();
  }

  /**
   * Retrieve a {@code ApplicationContext} from the {@code MockContext}
   * attribute with the {@link #setContextAttribute configured name}. The
   * {@code ApplicationContext} must have already been loaded and stored in the
   * {@code MockContext} before this servlet gets initialized (or invoked).
   * <p>Subclasses may override this method to provide a different
   * {@code ApplicationContext} retrieval strategy.
   *
   * @return the ApplicationContext for this servlet, or {@code null} if not found
   * @see #getContextAttribute()
   */
  @Override
  @Nullable
  protected ApplicationContext findApplicationContext() {
    String attrName = getContextAttribute();
    if (attrName == null) {
      return null;
    }
    WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getMockContext(), attrName);
    if (wac == null) {
      throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
    }
    return null;
  }

  /**
   * Return the MockContext attribute name for this servlet's WebApplicationContext.
   * <p>The default implementation returns
   * {@code SERVLET_CONTEXT_PREFIX + servlet name}.
   *
   * @see #CONTEXT_PREFIX
   * @see #getServletName
   */
  public String getMockContextAttributeName() {
    return CONTEXT_PREFIX + getServletName();
  }

  @Override
  public void service(MockRequest request, MockResponse response) throws MockException {
    if (request.getDispatcherType() == DispatcherType.ASYNC) {
      // send async results
      Object concurrentResult = request.getAttribute(WebAsyncManager.WEB_ASYNC_RESULT_ATTRIBUTE);
      RequestContext context = (RequestContext) request.getAttribute(WebAsyncManager.WEB_ASYNC_REQUEST_ATTRIBUTE);
      Object httpRequestHandler = WebAsyncManager.findHttpRequestHandler(context);

      try {
        handleConcurrentResult(context, httpRequestHandler, concurrentResult);
      }
      catch (final Throwable e) {
        throw new MockException("Async processing failed: " + e, e);
      }
      return;
    }

    RequestContext context = RequestContextHolder.get();

    boolean reset = false;
    if (context == null) {
      context = new MockRequestContext(getApplicationContext(),
              (HttpMockRequest) request, (HttpMockResponse) response, this);
      RequestContextHolder.set(context);
      reset = true;
    }

    try {
      handleRequest(context);
    }
    catch (final Throwable e) {
      throw new MockException("Handler processing failed: " + e, e);
    }
    finally {
      if (reset) {
        RequestContextHolder.cleanup();
      }
    }
  }

  @Override
  public MockConfig getMockConfig() {
    Assert.state(mockConfig != null, "DispatcherServlet has not been initialized");
    return mockConfig;
  }

  @Override
  public final String getMockInfo() {
    return "DispatcherServlet, Copyright © TODAY & 2017 - 2024 All Rights Reserved";
  }

  /**
   * Returns the name of this servlet instance. See {@link MockConfig#getMockName}.
   *
   * @return the name of this servlet instance
   */
  public String getServletName() {
    return getMockConfig().getMockName();
  }

  @Override
  protected void logInfo(String msg) {
    super.logInfo(msg);
    getMockContext().log(msg);
  }

}
