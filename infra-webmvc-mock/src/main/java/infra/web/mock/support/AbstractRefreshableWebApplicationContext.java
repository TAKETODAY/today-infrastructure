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

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.support.AbstractRefreshableApplicationContext;
import infra.context.support.AbstractRefreshableConfigApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.DefaultResourceLoader;
import infra.core.io.Resource;
import infra.lang.Assert;
import infra.mock.api.MockConfig;
import infra.mock.api.MockContext;
import infra.web.mock.ConfigurableWebApplicationContext;
import infra.web.mock.ConfigurableMockEnvironment;
import infra.web.mock.MockConfigAware;
import infra.web.mock.MockContextAware;
import infra.web.mock.MockContextAwareProcessor;

/**
 * {@link AbstractRefreshableApplicationContext}
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
 * {@link DefaultResourceLoader}.
 *
 * <p><b>This is the web context to be subclassed for a different bean definition format.</b>
 *
 * <p>Note that WebApplicationContext implementations are generally supposed
 * to configure themselves based on the configuration received through the
 * {@link ConfigurableWebApplicationContext} interface. In contrast, a standalone
 * application context might allow for configuration in custom startup code
 * (for example, {@link GenericApplicationContext}).
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
  public String @Nullable [] getConfigLocations() {
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
    if (env instanceof ConfigurableMockEnvironment) {
      ((ConfigurableMockEnvironment) env).initPropertySources(this.mockContext, this.mockConfig);
    }
  }

}
