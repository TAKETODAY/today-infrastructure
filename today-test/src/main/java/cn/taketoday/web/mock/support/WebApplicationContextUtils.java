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

package cn.taketoday.web.mock.support;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.env.PropertySource.StubPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockRequest;
import cn.taketoday.mock.api.MockResponse;
import cn.taketoday.mock.api.http.HttpSession;
import cn.taketoday.web.mock.ConfigurableWebApplicationContext;
import cn.taketoday.web.mock.MockConfigPropertySource;
import cn.taketoday.web.mock.MockContextPropertySource;
import cn.taketoday.web.mock.MockUtils;
import cn.taketoday.web.mock.WebApplicationContext;

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
   * from a single {@code DispatcherServlet} in the current web application).
   * <p>Note that {@code DispatcherServlet}'s exposure of its context can be
   * controlled through its {@code publishContext} property, which is {@code true}
   * by default but can be selectively switched to only publish a single context
   * despite multiple {@code DispatcherServlet} registrations in the web app.
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
                    "DispatcherServlet registered with publishContext=true?");
          }
          wac = (WebApplicationContext) attrValue;
        }
      }
    }
    return wac;
  }

  /**
   * Register web-specific scopes ("request", "session", "globalSession")
   * with the given BeanFactory, as used by the WebServletApplicationContext.
   *
   * @param beanFactory the BeanFactory to configure
   */
  public static void registerWebApplicationScopes(ConfigurableBeanFactory beanFactory) {
    registerWebApplicationScopes(beanFactory, null);
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

    beanFactory.registerResolvableDependency(HttpSession.class, new SessionObjectSupplier());
    beanFactory.registerResolvableDependency(MockRequest.class, new RequestObjectSupplier());
    beanFactory.registerResolvableDependency(MockResponse.class, new ResponseObjectSupplier());

  }

  /**
   * Register web-specific environment beans ("contextParameters", "contextAttributes")
   * with the given BeanFactory, as used by the WebServletApplicationContext.
   *
   * @param bf the BeanFactory to configure
   * @param sc the MockContext that we're running within
   */
  public static void registerEnvironmentBeans(ConfigurableBeanFactory bf, @Nullable MockContext sc) {
    registerEnvironmentBeans(bf, sc, null);
  }

  /**
   * Register web-specific environment beans ("contextParameters", "contextAttributes")
   * with the given BeanFactory, as used by the WebServletApplicationContext.
   *
   * @param bf the BeanFactory to configure
   * @param mockContext the MockContext that we're running within
   * @param mockConfig the ServletConfig
   */
  public static void registerEnvironmentBeans(ConfigurableBeanFactory bf,
          @Nullable MockContext mockContext, @Nullable MockConfig mockConfig) {

    if (mockContext != null && !bf.containsBean(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME)) {
      bf.registerSingleton(WebApplicationContext.SERVLET_CONTEXT_BEAN_NAME, mockContext);
    }

    if (mockConfig != null && !bf.containsBean(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME)) {
      bf.registerSingleton(ConfigurableWebApplicationContext.SERVLET_CONFIG_BEAN_NAME, mockConfig);
    }

    if (!bf.containsBean(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME)) {
      HashMap<String, String> parameterMap = new HashMap<>();
      if (mockContext != null) {
        Enumeration<String> paramNameEnum = mockContext.getInitParameterNames();
        while (paramNameEnum.hasMoreElements()) {
          String paramName = paramNameEnum.nextElement();
          parameterMap.put(paramName, mockContext.getInitParameter(paramName));
        }
      }
      if (mockConfig != null) {
        Enumeration<String> paramNameEnum = mockConfig.getInitParameterNames();
        while (paramNameEnum.hasMoreElements()) {
          String paramName = paramNameEnum.nextElement();
          parameterMap.put(paramName, mockConfig.getInitParameter(paramName));
        }
      }
      bf.registerSingleton(WebApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME,
              Collections.unmodifiableMap(parameterMap));
    }

    if (!bf.containsBean(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME)) {
      HashMap<String, Object> attributeMap = new HashMap<>();
      if (mockContext != null) {
        Enumeration<String> attrNameEnum = mockContext.getAttributeNames();
        while (attrNameEnum.hasMoreElements()) {
          String attrName = attrNameEnum.nextElement();
          attributeMap.put(attrName, mockContext.getAttribute(attrName));
        }
      }
      bf.registerSingleton(WebApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME,
              Collections.unmodifiableMap(attributeMap));
    }
  }

  /**
   * Convenient variant of {@link #initServletPropertySources(PropertySources,
   * MockContext, MockConfig)} that always provides {@code null} for the
   * {@link MockConfig} parameter.
   *
   * @see #initServletPropertySources(PropertySources, MockContext, MockConfig)
   */
  public static void initServletPropertySources(PropertySources propertySources, MockContext mockContext) {
    initServletPropertySources(propertySources, mockContext, null);
  }

  /**
   * Replace {@code Servlet}-based {@link StubPropertySource stub property sources} with
   * actual instances populated with the given {@code mockContext} and
   * {@code servletConfig} objects.
   * <p>This method is idempotent with respect to the fact it may be called any number
   * of times but will perform replacement of stub property sources with their
   * corresponding actual property sources once and only once.
   *
   * @param sources the {@link PropertySources} to initialize (must not
   * be {@code null})
   * @param mockContext the current {@link MockContext} (ignored if {@code null}
   * or if the {@link StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME
   * servlet context property source} has already been initialized)
   * @param mockConfig the current {@link MockConfig} (ignored if {@code null}
   * or if the {@link StandardServletEnvironment#SERVLET_CONFIG_PROPERTY_SOURCE_NAME
   * servlet config property source} has already been initialized)
   * @see StubPropertySource
   * @see cn.taketoday.core.env.ConfigurableEnvironment#getPropertySources()
   */
  public static void initServletPropertySources(PropertySources sources,
          @Nullable MockContext mockContext, @Nullable MockConfig mockConfig) {
    Assert.notNull(sources, "'propertySources' is required");

    String name = StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME;
    if (mockContext != null && sources.get(name) instanceof StubPropertySource) {
      sources.replace(name, new MockContextPropertySource(name, mockContext));
    }
    name = StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME;
    if (mockConfig != null && sources.get(name) instanceof StubPropertySource) {
      sources.replace(name, new MockConfigPropertySource(name, mockConfig));
    }
  }

  /**
   * Factory that exposes the current request object on demand.
   */
  @SuppressWarnings("serial")
  private static class RequestObjectSupplier implements Supplier<MockRequest>, Serializable {

    @Override
    public MockRequest get() {
      return MockUtils.getServletRequest(RequestContextHolder.get());
    }

    @Override
    public String toString() {
      return "Current HttpServletRequest";
    }

  }

  /**
   * Factory that exposes the current response object on demand.
   */
  @SuppressWarnings("serial")
  private static class ResponseObjectSupplier implements Supplier<MockResponse>, Serializable {

    @Override
    public MockResponse get() {
      return MockUtils.getServletResponse(RequestContextHolder.get());
    }

    @Override
    public String toString() {
      return "Current HttpServletResponse";
    }
  }

  /**
   * Factory that exposes the current session object on demand.
   */
  @SuppressWarnings("serial")
  private static class SessionObjectSupplier implements Supplier<HttpSession>, Serializable {

    @Override
    public HttpSession get() {
      return MockUtils.getHttpSession(RequestContextHolder.get());
    }

    @Override
    public String toString() {
      return "Current HttpSession";
    }

  }
}
