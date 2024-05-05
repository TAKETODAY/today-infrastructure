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

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.web.mock.ConfigurableWebApplicationContext;
import cn.taketoday.web.mock.ServletConfigAware;
import cn.taketoday.web.mock.MockContextAware;
import cn.taketoday.web.mock.ServletContextAwareProcessor;
import cn.taketoday.web.mock.WebApplicationContext;

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
public class StaticWebApplicationContext extends StaticApplicationContext implements ConfigurableWebApplicationContext {

  @Nullable
  private MockContext mockContext;

  @Nullable
  private MockConfig mockConfig;

  @Nullable
  private String namespace;

  public StaticWebApplicationContext() {
    setDisplayName("Root WebApplicationContext");
  }

  public StaticWebApplicationContext(@Nullable MockContext context) {
    this();
    setServletContext(context);
  }

  /**
   * Set the ServletContext that this WebApplicationContext runs in.
   */
  @Override
  public void setServletContext(@Nullable MockContext mockContext) {
    this.mockContext = mockContext;
  }

  @Override
  @Nullable
  public MockContext getServletContext() {
    return this.mockContext;
  }

  @Override
  public void setServletConfig(@Nullable MockConfig mockConfig) {
    this.mockConfig = mockConfig;
    if (mockConfig != null && this.mockContext == null) {
      this.mockContext = mockConfig.getMockContext();
    }
  }

  @Override
  @Nullable
  public MockConfig getServletConfig() {
    return this.mockConfig;
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
    beanFactory.addBeanPostProcessor(new ServletContextAwareProcessor(this.mockContext, this.mockConfig));
    beanFactory.ignoreDependencyInterface(MockContextAware.class);
    beanFactory.ignoreDependencyInterface(ServletConfigAware.class);

    WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.mockContext);
    WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.mockContext, this.mockConfig);
  }

  /**
   * This implementation supports file paths beneath the root of the ServletContext.
   *
   * @see ServletContextResource
   */
  @Override
  protected Resource getResourceByPath(String path) {
    Assert.state(this.mockContext != null, "No ServletContext available");
    return new ServletContextResource(this.mockContext, path);
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
            this.mockContext, this.mockConfig);
  }

}
