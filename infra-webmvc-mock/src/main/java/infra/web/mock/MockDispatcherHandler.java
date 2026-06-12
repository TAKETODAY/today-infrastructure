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

package infra.web.mock;

import java.io.Serial;
import java.io.Serializable;

import infra.context.ApplicationContext;
import infra.lang.Assert;
import infra.mock.api.DispatcherType;
import infra.mock.api.MockConfig;
import infra.mock.api.MockContext;
import infra.mock.api.MockException;
import infra.mock.api.MockHandler;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.api.http.HttpMockResponse;
import infra.web.DispatcherHandler;
import infra.web.RequestContext;
import infra.web.RequestContextHolder;
import infra.web.async.WebAsyncManager;

/**
 * Central dispatcher for HTTP request handlers/controllers in Servlet
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2.0 2018-06-25 19:47:14
 */
@SuppressWarnings("NullAway")
public class MockDispatcherHandler extends DispatcherHandler implements MockHandler, Serializable {

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
  public static final String CONTEXT_PREFIX = MockDispatcherHandler.class.getName() + ".CONTEXT.";

  private transient MockConfig mockConfig;

  /** Should we publish the context as a MockContext attribute?. */
  private boolean publishContext = true;

  public MockDispatcherHandler(ApplicationContext context) {
    super(context);
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
  public void init(MockConfig mockConfig) {
    this.mockConfig = mockConfig;
    String servletName = mockConfig.getMockName();
    mockConfig.getMockContext().log("Initializing Infra %s '%s'".formatted(getClass().getSimpleName(), servletName));
    log.info("Initializing Servlet '{}'", servletName);

    start();
  }

  @Override
  protected void onRefresh(ApplicationContext context) {
    super.onRefresh(context);
    if (publishContext) {
      // Publish the context as a servlet context attribute.
      String attrName = getMockContextAttributeName();
      getMockContext().setAttribute(attrName, getApplicationContext());
    }
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
   * Return the MockContext attribute name for this servlet's WebApplicationContext.
   * <p>The default implementation returns
   * {@code SERVLET_CONTEXT_PREFIX + servlet name}.
   *
   * @see #CONTEXT_PREFIX
   * @see #getName
   */
  public String getMockContextAttributeName() {
    return CONTEXT_PREFIX + getName();
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

    RequestContext context = RequestContextHolder.current();

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
    Assert.state(mockConfig != null, "DispatcherHandler has not been initialized");
    return mockConfig;
  }

  /**
   * Returns the name of this servlet instance. See {@link MockConfig#getMockName}.
   *
   * @return the name of this servlet instance
   */
  public String getName() {
    return getMockConfig().getMockName();
  }

  @Override
  protected void logInfo(String msg) {
    super.logInfo(msg);
    getMockContext().log(msg);
  }

}
