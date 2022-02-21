/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.context.support;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.core.env.PropertySource.StubPropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.context.ConfigurableWebServletApplicationContext;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.servlet.WebServletApplicationContext;
import cn.taketoday.web.session.DefaultWebSessionManager;
import cn.taketoday.web.session.WebSessionManager;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 17:12
 */
public class WebApplicationContextUtils {

  /**
   * Find the root {@code WebApplicationContext} for this web app, typically
   * loaded via {@link cn.taketoday.web.context.ContextLoaderListener}.
   * <p>Will rethrow an exception that happened on root context startup,
   * to differentiate between a failed context startup and no context at all.
   *
   * @param sc the ServletContext to find the web application context for
   * @return the root WebApplicationContext for this web app
   * @throws IllegalStateException if the root WebApplicationContext could not be found
   * @see cn.taketoday.web.servlet.WebServletApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
   */
  public static WebApplicationContext getRequiredWebApplicationContext(ServletContext sc) throws IllegalStateException {
    WebApplicationContext wac = getWebApplicationContext(sc);
    if (wac == null) {
      throw new IllegalStateException("No WebApplicationContext found: no ContextLoaderListener registered?");
    }
    return wac;
  }

  /**
   * Find the root {@code WebApplicationContext} for this web app, typically
   * loaded via {@link cn.taketoday.web.context.ContextLoaderListener}.
   * <p>Will rethrow an exception that happened on root context startup,
   * to differentiate between a failed context startup and no context at all.
   *
   * @param sc the ServletContext to find the web application context for
   * @return the root WebApplicationContext for this web app, or {@code null} if none
   * @see cn.taketoday.web.servlet.WebServletApplicationContext#ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE
   */
  @Nullable
  public static WebApplicationContext getWebApplicationContext(ServletContext sc) {
    return getWebApplicationContext(sc, WebServletApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
  }

  /**
   * Find a custom {@code WebApplicationContext} for this web app.
   *
   * @param sc the ServletContext to find the web application context for
   * @param attrName the name of the ServletContext attribute to look for
   * @return the desired WebApplicationContext for this web app, or {@code null} if none
   */
  @Nullable
  public static WebApplicationContext getWebApplicationContext(ServletContext sc, String attrName) {
    Assert.notNull(sc, "ServletContext must not be null");
    Object attr = sc.getAttribute(attrName);
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
    if (!(attr instanceof WebApplicationContext)) {
      throw new IllegalStateException("Context attribute is not of type WebApplicationContext: " + attr);
    }
    return (WebApplicationContext) attr;
  }

  /**
   * Find a unique {@code WebApplicationContext} for this web app: either the
   * root web app context (preferred) or a unique {@code WebApplicationContext}
   * among the registered {@code ServletContext} attributes (typically coming
   * from a single {@code DispatcherServlet} in the current web application).
   * <p>Note that {@code DispatcherServlet}'s exposure of its context can be
   * controlled through its {@code publishContext} property, which is {@code true}
   * by default but can be selectively switched to only publish a single context
   * despite multiple {@code DispatcherServlet} registrations in the web app.
   *
   * @param sc the ServletContext to find the web application context for
   * @return the desired WebApplicationContext for this web app, or {@code null} if none
   * @see #getWebApplicationContext(ServletContext)
   * @see ServletContext#getAttributeNames()
   */
  @Nullable
  public static WebApplicationContext findWebApplicationContext(ServletContext sc) {
    WebApplicationContext wac = getWebApplicationContext(sc);
    if (wac == null) {
      Enumeration<String> attrNames = sc.getAttributeNames();
      while (attrNames.hasMoreElements()) {
        String attrName = attrNames.nextElement();
        Object attrValue = sc.getAttribute(attrName);
        if (attrValue instanceof WebApplicationContext) {
          if (wac != null) {
            throw new IllegalStateException("No unique WebApplicationContext found: more than one " +
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
   * with the given BeanFactory, as used by the WebApplicationContext.
   *
   * @param beanFactory the BeanFactory to configure
   */
  public static void registerWebApplicationScopes(ConfigurableBeanFactory beanFactory) {
    registerWebApplicationScopes(beanFactory, null);
  }

  /**
   * Register web-specific scopes ("request", "session", "globalSession", "application")
   * with the given BeanFactory, as used by the WebApplicationContext.
   *
   * @param beanFactory the BeanFactory to configure
   * @param sc the ServletContext that we're running within
   */
  public static void registerWebApplicationScopes(
          ConfigurableBeanFactory beanFactory, @Nullable ServletContext sc) {

    WebSessionManager sessionManager = beanFactory.getBean(WebSessionManager.BEAN_NAME, WebSessionManager.class);
    if (sessionManager == null) {
      sessionManager = new DefaultWebSessionManager();
    }
    beanFactory.registerScope(WebApplicationContext.SCOPE_REQUEST, new RequestScope());
    beanFactory.registerScope(WebApplicationContext.SCOPE_SESSION, new SessionScope(sessionManager));
    if (sc != null) {
      ServletContextScope appScope = new ServletContextScope(sc);
      beanFactory.registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
      // Register as ServletContext attribute, for ContextCleanupListener to detect it.
      sc.setAttribute(ServletContextScope.class.getName(), appScope);
    }

    beanFactory.registerDependency(HttpSession.class, new SessionObjectSupplier());
    beanFactory.registerDependency(HttpServletRequest.class, new RequestObjectSupplier());
    beanFactory.registerDependency(HttpServletResponse.class, new ResponseObjectSupplier());
    beanFactory.registerDependency(ServletContext.class, sc);
  }

  /**
   * Register web-specific environment beans ("contextParameters", "contextAttributes")
   * with the given BeanFactory, as used by the WebApplicationContext.
   *
   * @param bf the BeanFactory to configure
   * @param sc the ServletContext that we're running within
   */
  public static void registerEnvironmentBeans(ConfigurableBeanFactory bf, @Nullable ServletContext sc) {
    registerEnvironmentBeans(bf, sc, null);
  }

  /**
   * Register web-specific environment beans ("contextParameters", "contextAttributes")
   * with the given BeanFactory, as used by the WebApplicationContext.
   *
   * @param bf the BeanFactory to configure
   * @param servletContext the ServletContext that we're running within
   * @param servletConfig the ServletConfig
   */
  public static void registerEnvironmentBeans(
          ConfigurableBeanFactory bf, @Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {

    if (servletContext != null && !bf.containsBean(WebServletApplicationContext.SERVLET_CONTEXT_BEAN_NAME)) {
      bf.registerSingleton(WebServletApplicationContext.SERVLET_CONTEXT_BEAN_NAME, servletContext);
    }

    if (servletConfig != null && !bf.containsBean(ConfigurableWebServletApplicationContext.SERVLET_CONFIG_BEAN_NAME)) {
      bf.registerSingleton(ConfigurableWebServletApplicationContext.SERVLET_CONFIG_BEAN_NAME, servletConfig);
    }

    if (!bf.containsBean(WebServletApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME)) {
      Map<String, String> parameterMap = new HashMap<>();
      if (servletContext != null) {
        Enumeration<?> paramNameEnum = servletContext.getInitParameterNames();
        while (paramNameEnum.hasMoreElements()) {
          String paramName = (String) paramNameEnum.nextElement();
          parameterMap.put(paramName, servletContext.getInitParameter(paramName));
        }
      }
      if (servletConfig != null) {
        Enumeration<?> paramNameEnum = servletConfig.getInitParameterNames();
        while (paramNameEnum.hasMoreElements()) {
          String paramName = (String) paramNameEnum.nextElement();
          parameterMap.put(paramName, servletConfig.getInitParameter(paramName));
        }
      }
      bf.registerSingleton(WebServletApplicationContext.CONTEXT_PARAMETERS_BEAN_NAME,
              Collections.unmodifiableMap(parameterMap));
    }

    if (!bf.containsBean(WebServletApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME)) {
      Map<String, Object> attributeMap = new HashMap<>();
      if (servletContext != null) {
        Enumeration<?> attrNameEnum = servletContext.getAttributeNames();
        while (attrNameEnum.hasMoreElements()) {
          String attrName = (String) attrNameEnum.nextElement();
          attributeMap.put(attrName, servletContext.getAttribute(attrName));
        }
      }
      bf.registerSingleton(WebServletApplicationContext.CONTEXT_ATTRIBUTES_BEAN_NAME,
              Collections.unmodifiableMap(attributeMap));
    }
  }

  /**
   * Convenient variant of {@link #initServletPropertySources(PropertySources,
   * ServletContext, ServletConfig)} that always provides {@code null} for the
   * {@link ServletConfig} parameter.
   *
   * @see #initServletPropertySources(PropertySources, ServletContext, ServletConfig)
   */
  public static void initServletPropertySources(PropertySources propertySources, ServletContext servletContext) {
    initServletPropertySources(propertySources, servletContext, null);
  }

  /**
   * Replace {@code Servlet}-based {@link StubPropertySource stub property sources} with
   * actual instances populated with the given {@code servletContext} and
   * {@code servletConfig} objects.
   * <p>This method is idempotent with respect to the fact it may be called any number
   * of times but will perform replacement of stub property sources with their
   * corresponding actual property sources once and only once.
   *
   * @param sources the {@link PropertySources} to initialize (must not
   * be {@code null})
   * @param servletContext the current {@link ServletContext} (ignored if {@code null}
   * or if the {@link StandardServletEnvironment#SERVLET_CONTEXT_PROPERTY_SOURCE_NAME
   * servlet context property source} has already been initialized)
   * @param servletConfig the current {@link ServletConfig} (ignored if {@code null}
   * or if the {@link StandardServletEnvironment#SERVLET_CONFIG_PROPERTY_SOURCE_NAME
   * servlet config property source} has already been initialized)
   * @see StubPropertySource
   * @see cn.taketoday.core.env.ConfigurableEnvironment#getPropertySources()
   */
  public static void initServletPropertySources(
          PropertySources sources,
          @Nullable ServletContext servletContext, @Nullable ServletConfig servletConfig) {

    Assert.notNull(sources, "'propertySources' must not be null");
    String name = StandardServletEnvironment.SERVLET_CONTEXT_PROPERTY_SOURCE_NAME;
    if (servletContext != null && sources.get(name) instanceof StubPropertySource) {
      sources.replace(name, new ServletContextPropertySource(name, servletContext));
    }
    name = StandardServletEnvironment.SERVLET_CONFIG_PROPERTY_SOURCE_NAME;
    if (servletConfig != null && sources.get(name) instanceof StubPropertySource) {
      sources.replace(name, new ServletConfigPropertySource(name, servletConfig));
    }
  }

  /**
   * Factory that exposes the current request object on demand.
   */
  @SuppressWarnings("serial")
  private static class RequestObjectSupplier implements Supplier<ServletRequest>, Serializable {

    @Override
    public ServletRequest get() {
      return ServletUtils.getServletRequest(RequestContextHolder.currentContext());
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
  private static class ResponseObjectSupplier implements Supplier<ServletResponse>, Serializable {

    @Override
    public ServletResponse get() {
      return ServletUtils.getServletResponse(RequestContextHolder.currentContext());
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
      return ServletUtils.getHttpSession(RequestContextHolder.currentContext());
    }

    @Override
    public String toString() {
      return "Current HttpSession";
    }

  }
}
