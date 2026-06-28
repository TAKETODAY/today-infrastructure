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

package infra.web.mock.support;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.function.Supplier;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.lang.Assert;
import infra.mock.api.MockContext;
import infra.mock.api.MockRequest;
import infra.mock.api.MockResponse;
import infra.session.Session;
import infra.web.RequestContextHolder;
import infra.web.RequestContextUtils;
import infra.web.mock.MockUtils;
import infra.web.mock.WebApplicationContext;

/**
 * Convenience methods for retrieving the root {@link WebApplicationContext} for
 * a given {@link MockContext}. This is useful for programmatically accessing
 * a application context from within custom web views or MVC actions.
 *
 * <p>Note that there are more convenient ways of accessing the root context for
 * many web frameworks, either part of Infra or available as an external library.
 * This helper class is just the most generic way to access the root context.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 17:12
 */
public class WebApplicationContextUtils {

  /**
   * Find the root {@code WebServletApplicationContext} for this web app, typically
   * loaded via {@link ContextLoaderListener}.
   * <p>Will rethrow an exception that happened on root context startup,
   * to differentiate between a failed context startup and no context at all.
   *
   * @param sc the MockContext to find the web application context for
   * @return the root WebServletApplicationContext for this web app
   * @throws IllegalStateException if the root WebServletApplicationContext could not be found
   * @see WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
   */
  public static WebApplicationContext getRequiredWebApplicationContext(MockContext sc) throws IllegalStateException {
    WebApplicationContext wac = getWebApplicationContext(sc);
    if (wac == null) {
      throw new IllegalStateException("No WebServletApplicationContext found: no ContextLoaderListener registered?");
    }
    return wac;
  }

  /**
   * Find the root {@code WebServletApplicationContext} for this web app,
   * <p>Will rethrow an exception that happened on root context startup,
   * to differentiate between a failed context startup and no context at all.
   *
   * @param sc the MockContext to find the web application context for
   * @return the root WebServletApplicationContext for this web app, or {@code null} if none
   * @see WebApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
   */
  @Nullable
  public static WebApplicationContext getWebApplicationContext(MockContext sc) {
    return getWebApplicationContext(sc, WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
  }

  /**
   * Find a custom {@code WebServletApplicationContext} for this web app.
   *
   * @param sc the MockContext to find the web application context for
   * @param attrName the name of the MockContext attribute to look for
   * @return the desired WebServletApplicationContext for this web app, or {@code null} if none
   */
  @Nullable
  public static WebApplicationContext getWebApplicationContext(MockContext sc, String attrName) {
    Assert.notNull(sc, "MockContext is required");
    Object attr = sc.getAttribute(attrName);
    if (attr instanceof WebApplicationContext) {
      return (WebApplicationContext) attr;
    }
    if (attr == null) {
      return null;
    }
    if (attr instanceof RuntimeException) {
      throw (RuntimeException) attr;
    }
    if (attr instanceof Error) {
      throw (Error) attr;
    }
    if (attr instanceof Exception) {
      throw new IllegalStateException((Exception) attr);
    }
    throw new IllegalStateException("Context attribute is not of type WebServletApplicationContext: " + attr);
  }

  /**
   * Find a unique {@code WebServletApplicationContext} for this web app: either the
   * root web app context (preferred) or a unique {@code WebServletApplicationContext}
   * among the registered {@code MockContext} attributes (typically coming
   * from a single {@code DispatcherHandler} in the current web application).
   * <p>Note that {@code DispatcherHandler}'s exposure of its context can be
   * controlled through its {@code publishContext} property, which is {@code true}
   * by default but can be selectively switched to only publish a single context
   * despite multiple {@code DispatcherHandler} registrations in the web app.
   *
   * @param sc the MockContext to find the web application context for
   * @return the desired WebServletApplicationContext for this web app, or {@code null} if none
   * @see #getWebApplicationContext(MockContext)
   * @see MockContext#getAttributeNames()
   */
  @Nullable
  public static WebApplicationContext findWebApplicationContext(MockContext sc) {
    WebApplicationContext wac = getWebApplicationContext(sc);
    if (wac == null) {
      Enumeration<String> attrNames = sc.getAttributeNames();
      while (attrNames.hasMoreElements()) {
        String attrName = attrNames.nextElement();
        Object attrValue = sc.getAttribute(attrName);
        if (attrValue instanceof WebApplicationContext) {
          if (wac != null) {
            throw new IllegalStateException("No unique WebServletApplicationContext found: more than one " +
                    "DispatcherHandler registered with publishContext=true?");
          }
          wac = (WebApplicationContext) attrValue;
        }
      }
    }
    return wac;
  }

  /**
   * Register web-specific scopes ("request", "session")
   * with the given BeanFactory, as used by the WebServletApplicationContext.
   *
   * @param beanFactory the BeanFactory to configure
   * @param sc the MockContext that we're running within
   */
  public static void registerWebApplicationScopes(
          ConfigurableBeanFactory beanFactory, @Nullable MockContext sc) {
    RequestContextUtils.registerScopes(beanFactory);

    if (sc != null) {
      beanFactory.registerResolvableDependency(MockContext.class, sc);
    }

    beanFactory.registerResolvableDependency(Session.class, new SessionObjectSupplier());
    beanFactory.registerResolvableDependency(MockRequest.class, new RequestObjectSupplier());
    beanFactory.registerResolvableDependency(MockResponse.class, new ResponseObjectSupplier());

  }

  /**
   * Factory that exposes the current request object on demand.
   */
  @SuppressWarnings("serial")
  private static class RequestObjectSupplier implements Supplier<MockRequest>, Serializable {

    @Override
    public MockRequest get() {
      return MockUtils.getMockRequest(RequestContextHolder.current());
    }

    @Override
    public String toString() {
      return "Current HttpMockRequest";
    }

  }

  /**
   * Factory that exposes the current response object on demand.
   */
  @SuppressWarnings("serial")
  private static class ResponseObjectSupplier implements Supplier<MockResponse>, Serializable {

    @Override
    public MockResponse get() {
      return MockUtils.getMockResponse(RequestContextHolder.current());
    }

    @Override
    public String toString() {
      return "Current HttpMockResponse";
    }
  }

  /**
   * Factory that exposes the current session object on demand.
   */
  @SuppressWarnings("serial")
  private static class SessionObjectSupplier implements Supplier<Session>, Serializable {

    @Override
    public Session get() {
      return RequestContextHolder.required().getSession();
    }

    @Override
    public String toString() {
      return "Current HttpSession";
    }

  }
}
