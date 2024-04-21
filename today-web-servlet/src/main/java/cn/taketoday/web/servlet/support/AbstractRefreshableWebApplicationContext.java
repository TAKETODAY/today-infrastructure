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

package cn.taketoday.web.servlet.support;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.support.AbstractRefreshableConfigApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.servlet.ConfigurableWebApplicationContext;
import cn.taketoday.web.servlet.ConfigurableWebEnvironment;
import cn.taketoday.web.servlet.ServletConfigAware;
import cn.taketoday.web.servlet.ServletContextAware;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * {@link cn.taketoday.context.support.AbstractRefreshableApplicationContext}
 * subclass which implements the
 * {@link ConfigurableWebApplicationContext}
 * interface for web environments. Provides a "configLocations" property,
 * to be populated through the ConfigurableWebApplicationContext interface
 * on web application startup.
 *
 * <p>This class is as easy to subclass as AbstractRefreshableApplicationContext:
 * All you need to implements is the {@link #loadBeanDefinitions} method;
 * see the superclass javadoc for details. Note that implementations are supposed
 * to load bean definitions from the files specified by the locations returned
 * by the {@link #getConfigLocations} method.
 *
 * <p>Interprets resource paths as servlet context resources, i.e. as paths beneath
 * the web application root. Absolute paths, e.g. for files outside the web app root,
 * can be accessed via "file:" URLs, as implemented by
 * {@link cn.taketoday.core.io.DefaultResourceLoader}.
 *
 * <p><b>This is the web context to be subclassed for a different bean definition format.</b>
 *
 * <p>Note that WebApplicationContext implementations are generally supposed
 * to configure themselves based on the configuration received through the
 * {@link ConfigurableWebApplicationContext} interface. In contrast, a standalone
 * application context might allow for configuration in custom startup code
 * (for example, {@link cn.taketoday.context.support.GenericApplicationContext}).
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #loadBeanDefinitions
 * @see ConfigurableWebApplicationContext#setConfigLocations
 * @since 4.0 2022/2/20 17:36
 */
@Deprecated
public abstract class AbstractRefreshableWebApplicationContext
        extends AbstractRefreshableConfigApplicationContext implements ConfigurableWebApplicationContext {

  /** Servlet context that this context runs in. */
  @Nullable
  private ServletContext servletContext;

  /** Servlet config that this context runs in, if any. */
  @Nullable
  private ServletConfig servletConfig;

  /** Namespace of this context, or {@code null} if root. */
  @Nullable
  private String namespace;

  public AbstractRefreshableWebApplicationContext() {
    setDisplayName("Root WebApplicationContext");
  }

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
      setServletContext(servletConfig.getServletContext());
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

  public String getContextPath() {
    Assert.state(servletContext != null, "No servletContext.");
    return servletContext.getContextPath();
  }

  @Override
  public String[] getConfigLocations() {
    return super.getConfigLocations();
  }

  @Override
  public String getApplicationName() {
    return (this.servletContext != null ? this.servletContext.getContextPath() : "");
  }

  /**
   * Create and return a new {@link StandardServletEnvironment}. Subclasses may override
   * in order to configure the environment or specialize the environment type returned.
   */
  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardServletEnvironment();
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
   * {@inheritDoc}
   * <p>Replace {@code Servlet}-related property sources.
   */
  @Override
  protected void initPropertySources() {
    ConfigurableEnvironment env = getEnvironment();
    if (env instanceof ConfigurableWebEnvironment) {
      ((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, this.servletConfig);
    }
  }

}
