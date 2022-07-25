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

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.context.ConfigurableWebApplicationContext;
import cn.taketoday.web.context.ConfigurableWebEnvironment;
import cn.taketoday.web.context.ConfigurableWebServletApplicationContext;
import cn.taketoday.web.servlet.ServletContextAware;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * Subclass of {@link GenericApplicationContext}, suitable for web servlet environments.
 *
 * <p>Interprets resource paths as servlet context resources, i.e. as paths beneath
 * the web application root. Absolute paths &mdash; for example, for files outside
 * the web app root &mdash; can be accessed via {@code file:} URLs, as implemented
 * by {@code AbstractApplicationContext}.
 *
 * <p>If you wish to register annotated <em>component classes</em> with a
 * {@code GenericWebApplicationContext}, you can use an
 * {@link AnnotatedBeanDefinitionReader
 * AnnotatedBeanDefinitionReader}, as demonstrated in the following example.
 * Component classes include in particular
 * {@link cn.taketoday.context.annotation.Configuration @Configuration}
 * classes but also plain {@link cn.taketoday.lang.Component @Component}
 * classes as well as JSR-330 compliant classes using {@code jakarta.inject} annotations.
 *
 * <pre class="code">
 * GenericWebApplicationContext context = new GenericWebApplicationContext();
 * AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(context);
 * reader.register(AppConfig.class, UserController.class, UserRepository.class);</pre>
 *
 * <p>If you intend to implement a {@code WebApplicationContext} that reads bean definitions
 * from configuration files, consider deriving from {@link AbstractRefreshableWebApplicationContext},
 * reading the bean definitions in an implementation of the {@code loadBeanDefinitions}
 * method.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 21:03
 */
public class GenericWebServletApplicationContext extends GenericWebApplicationContext
        implements ConfigurableWebServletApplicationContext {

  @Nullable
  private ServletContext servletContext;

  /**
   * Create a new {@code GenericWebApplicationContext}.
   *
   * @see #setServletContext
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericWebServletApplicationContext() {
    super();
  }

  /**
   * Create a new {@code GenericWebApplicationContext} with the given {@link StandardBeanFactory}.
   *
   * @param beanFactory the {@code StandardBeanFactory} instance to use for this context
   * @see #setServletContext
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericWebServletApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  /**
   * Create a new {@code GenericWebApplicationContext} for the given {@link ServletContext}.
   *
   * @param servletContext the {@code ServletContext} to run in
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericWebServletApplicationContext(@Nullable ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  /**
   * Create a new {@code GenericWebApplicationContext} with the given {@link StandardBeanFactory}
   * and {@link ServletContext}.
   *
   * @param beanFactory the {@code StandardBeanFactory} instance to use for this context
   * @param servletContext the {@code ServletContext} to run in
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericWebServletApplicationContext(StandardBeanFactory beanFactory, @Nullable ServletContext servletContext) {
    super(beanFactory);
    this.servletContext = servletContext;
  }

  /**
   * Set the {@link ServletContext} that this {@code WebApplicationContext} runs in.
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
  public String getApplicationName() {
    return servletContext != null ? servletContext.getContextPath() : "";
  }

  @Override
  public String getContextPath() {
    return servletContext != null ? servletContext.getContextPath() : "";
  }

  /**
   * Create and return a new {@link StandardServletEnvironment}.
   */
  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardServletEnvironment();
  }

  /**
   * This implementation supports file paths beneath the root of the {@link ServletContext}.
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
  protected PatternResourceLoader getPatternResourceLoader() {
    return new ServletContextResourcePatternLoader(this);
  }

  /**
   * Register request/session scopes, environment beans, a {@link ServletContextAwareProcessor}, etc.
   */
  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);
    if (this.servletContext != null) {
      beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext));
      beanFactory.ignoreDependencyInterface(ServletContextAware.class);
    }
    WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.servletContext);
  }

  @Override
  protected void invokeBeanFactoryPostProcessors(ConfigurableBeanFactory beanFactory) {
    super.invokeBeanFactoryPostProcessors(beanFactory);
    WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.servletContext);
  }

  /**
   * {@inheritDoc}
   * <p>Replace {@code Servlet}-related property sources.
   */
  @Override
  protected void initPropertySources() {
    ConfigurableEnvironment env = getEnvironment();
    if (env instanceof ConfigurableWebEnvironment) {
      ((ConfigurableWebEnvironment) env).initPropertySources(this.servletContext, null);
    }
  }

  // ---------------------------------------------------------------------
  // Pseudo-implementation of ConfigurableWebServletApplicationContext
  // ---------------------------------------------------------------------

  @Override
  public void setServletConfig(@Nullable ServletConfig servletConfig) {
    // no-op
  }

  @Override
  @Nullable
  public ServletConfig getServletConfig() {
    throw new UnsupportedOperationException(
            "GenericWebApplicationContext does not support getServletConfig()");
  }
}
