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

package cn.taketoday.web.servlet.support;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.servlet.ConfigurableWebApplicationContext;
import cn.taketoday.web.servlet.ServletConfigAware;
import cn.taketoday.web.servlet.ServletContextAware;
import cn.taketoday.web.servlet.WebApplicationContext;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * Static {@link WebApplicationContext}
 * implementation for testing. Not intended for use in production applications.
 *
 * <p>Implements the {@link ConfigurableWebApplicationContext}
 * interface to allow for direct replacement of an {@link XmlWebApplicationContext},
 * despite not actually supporting external configuration files.
 *
 * <p>Interprets resource paths as servlet context resources, i.e. as paths beneath
 * the web application root. Absolute paths, e.g. for files outside the web app root,
 * can be accessed via "file:" URLs, as implemented by
 * {@link cn.taketoday.core.io.DefaultResourceLoader}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/7 13:52
 */
public class StaticWebApplicationContext
        extends StaticApplicationContext implements ConfigurableWebApplicationContext {

  @Nullable
  private ServletContext servletContext;

  @Nullable
  private ServletConfig servletConfig;

  @Nullable
  private String namespace;

  public StaticWebApplicationContext() {
    setDisplayName("Root WebApplicationContext");
  }

  public StaticWebApplicationContext(@Nullable ServletContext context) {
    this();
    setServletContext(context);
  }

  /**
   * Set the ServletContext that this WebApplicationContext runs in.
   */
  @Override
  public void setServletContext(@Nullable ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  @Override
  @Nullable
  public ServletContext getServletContext() {
    return this.servletContext;
  }

  @Override
  public void setServletConfig(@Nullable ServletConfig servletConfig) {
    this.servletConfig = servletConfig;
    if (servletConfig != null && this.servletContext == null) {
      this.servletContext = servletConfig.getServletContext();
    }
  }

  @Override
  @Nullable
  public ServletConfig getServletConfig() {
    return this.servletConfig;
  }

  @Override
  public void setNamespace(@Nullable String namespace) {
    this.namespace = namespace;
    if (namespace != null) {
      setDisplayName("WebApplicationContext for namespace '" + namespace + "'");
    }
  }

  @Override
  @Nullable
  public String getNamespace() {
    return this.namespace;
  }

  /**
   * The {@link StaticWebApplicationContext} class does not support this method.
   *
   * @throws UnsupportedOperationException <b>always</b>
   */
  @Override
  public void setConfigLocation(String configLocation) {
    throw new UnsupportedOperationException("StaticWebApplicationContext does not support config locations");
  }

  /**
   * The {@link StaticWebApplicationContext} class does not support this method.
   *
   * @throws UnsupportedOperationException <b>always</b>
   */
  @Override
  public void setConfigLocations(String... configLocations) {
    throw new UnsupportedOperationException("StaticWebApplicationContext does not support config locations");
  }

  @Override
  public String[] getConfigLocations() {
    return null;
  }

  /**
   * Register request/session scopes, a {@link ServletContextAwareProcessor}, etc.
   */
  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
    beanFactory.ignoreDependencyInterface(ServletContextAware.class);
    beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

    WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
    WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext, this.servletConfig);
  }

  /**
   * This implementation supports file paths beneath the root of the ServletContext.
   *
   * @see ServletContextResource
   */
  @Override
  protected Resource getResourceByPath(String path) {
    Assert.state(this.servletContext != null, "No ServletContext available");
    return new ServletContextResource(this.servletContext, path);
  }

  /**
   * This implementation supports pattern matching in unexpanded WARs too.
   *
   * @see ServletContextResourcePatternLoader
   */
  @Override
  protected ServletContextResourcePatternLoader getPatternResourceLoader() {
    return new ServletContextResourcePatternLoader(this);
  }

  /**
   * Create and return a new {@link StandardServletEnvironment}.
   */
  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardServletEnvironment();
  }

  @Override
  protected void initPropertySources() {
    WebApplicationContextUtils.initServletPropertySources(getEnvironment().getPropertySources(),
            this.servletContext, this.servletConfig);
  }

}
