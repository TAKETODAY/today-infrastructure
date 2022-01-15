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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.servlet;

import java.io.Serializable;
import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.StandardEnvironment;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.ServletContextAware;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * @author TODAY <br>
 * 2018-07-10 1:16:17
 */
public class StandardWebServletApplicationContext
        extends StandardApplicationContext implements WebServletApplicationContext {

  /** Servlet context */
  private ServletContext servletContext;

  /**
   * Default Constructor
   */
  public StandardWebServletApplicationContext() {
    this(new StandardEnvironment());
  }

  /**
   * Construct with given {@link ConfigurableEnvironment}
   *
   * @param env {@link ConfigurableEnvironment} instance
   */
  public StandardWebServletApplicationContext(ConfigurableEnvironment env) {
    setEnvironment(env);
  }

  public StandardWebServletApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  public StandardWebServletApplicationContext(ServletContext servletContext) {
    this();
    this.servletContext = servletContext;
  }

  /**
   * @param classes class set
   * @param servletContext {@link ServletContext}
   * @since 2.3.3
   */
  public StandardWebServletApplicationContext(Set<Class<?>> classes, ServletContext servletContext) {
    this(servletContext);
    registerBean(classes);
    refresh();
  }

  /**
   * @param servletContext {@link ServletContext}
   * @param propertiesLocation properties location
   * @param locations package locations
   * @since 2.3.3
   */
  public StandardWebServletApplicationContext(ServletContext servletContext, String propertiesLocation, String... locations) {
    this(servletContext);
    setPropertiesLocation(propertiesLocation);
    scan(locations);
  }

  @Override
  protected void registerFrameworkComponents(ConfigurableBeanFactory beanFactory) {
    super.registerFrameworkComponents(beanFactory);

    beanFactory.registerDependency(HttpSession.class, new SessionObjectSupplier());
    beanFactory.registerDependency(HttpServletRequest.class, new RequestObjectSupplier());
    beanFactory.registerDependency(HttpServletResponse.class, new ResponseObjectSupplier());
    beanFactory.registerDependency(ServletContext.class, (Supplier<?>) this::getServletContext);
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);
    beanFactory.ignoreDependencyInterface(ServletContextAware.class);

  }

  @Override
  public String getContextPath() {
    return servletContext.getContextPath();
  }

  @Override
  public ServletContext getServletContext() {
    return servletContext;
  }

  @Override
  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
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
