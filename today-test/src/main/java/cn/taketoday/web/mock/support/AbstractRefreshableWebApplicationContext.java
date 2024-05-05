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
import cn.taketoday.context.support.AbstractRefreshableConfigApplicationContext;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.web.mock.ConfigurableWebApplicationContext;
import cn.taketoday.web.mock.ConfigurableWebEnvironment;
import cn.taketoday.web.mock.MockConfigAware;
import cn.taketoday.web.mock.MockContextAware;
import cn.taketoday.web.mock.MockContextAwareProcessor;

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
public abstract class AbstractRefreshableWebApplicationContext
        extends AbstractRefreshableConfigApplicationContext implements ConfigurableWebApplicationContext {

  /** Servlet context that this context runs in. */
  @Nullable
  private MockContext mockContext;

  /** Servlet config that this context runs in, if any. */
  @Nullable
  private MockConfig mockConfig;

  /** Namespace of this context, or {@code null} if root. */
  @Nullable
  private String namespace;

  public AbstractRefreshableWebApplicationContext() {
    setDisplayName("Root WebApplicationContext");
  }

  @Override
  public void setMockContext(@Nullable MockContext mockContext) {
    this.mockContext = mockContext;
  }

  @Override
  @Nullable
  public MockContext getMockContext() {
    return this.mockContext;
  }

  @Override
  public void setMockConfig(@Nullable MockConfig mockConfig) {
    this.mockConfig = mockConfig;
    if (mockConfig != null && this.mockContext == null) {
      setMockContext(mockConfig.getMockContext());
    }
  }

  @Override
  @Nullable
  public MockConfig getMockConfig() {
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

  @Override
  public String[] getConfigLocations() {
    return super.getConfigLocations();
  }

  /**
   * Create and return a new {@link StandardMockEnvironment}. Subclasses may override
   * in order to configure the environment or specialize the environment type returned.
   */
  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardMockEnvironment();
  }

  /**
   * Register request/session scopes, a {@link MockContextAwareProcessor}, etc.
   */
  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(new MockContextAwareProcessor(this.mockContext, this.mockConfig));
    beanFactory.ignoreDependencyInterface(MockContextAware.class);
    beanFactory.ignoreDependencyInterface(MockConfigAware.class);

    WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.mockContext);
    WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.mockContext, this.mockConfig);
  }

  /**
   * This implementation supports file paths beneath the root of the MockContext.
   *
   * @see MockContextResource
   */
  @Override
  protected Resource getResourceByPath(String path) {
    Assert.state(this.mockContext != null, "No MockContext available");
    return new MockContextResource(this.mockContext, path);
  }

  /**
   * This implementation supports pattern matching in unexpanded WARs too.
   *
   * @see MockContextResourcePatternLoader
   */
  @Override
  protected MockContextResourcePatternLoader getPatternResourceLoader() {
    return new MockContextResourcePatternLoader(this);
  }

  /**
   * {@inheritDoc}
   * <p>Replace {@code Servlet}-related property sources.
   */
  @Override
  protected void initPropertySources() {
    ConfigurableEnvironment env = getEnvironment();
    if (env instanceof ConfigurableWebEnvironment) {
      ((ConfigurableWebEnvironment) env).initPropertySources(this.mockContext, this.mockConfig);
    }
  }

}
