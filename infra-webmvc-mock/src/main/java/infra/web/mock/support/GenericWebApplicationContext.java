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
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.Configuration;
import infra.context.support.GenericApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;
import infra.lang.Assert;
import infra.mock.api.MockConfig;
import infra.mock.api.MockContext;
import infra.stereotype.Component;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.mock.ConfigurableWebApplicationContext;
import infra.web.mock.ConfigurableMockEnvironment;
import infra.web.mock.MockContextAware;
import infra.web.mock.MockContextAwareProcessor;

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
 * {@link Configuration @Configuration}
 * classes but also plain {@link Component @Component}
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
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 21:03
 */
public class GenericWebApplicationContext extends GenericApplicationContext
        implements ConfigurableWebApplicationContext {

  @Nullable
  private MockContext mockContext;

  /**
   * Create a new {@code GenericWebApplicationContext}.
   *
   * @see #setMockContext
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericWebApplicationContext() {
    super();
  }

  /**
   * Create a new {@code GenericWebApplicationContext} with the given {@link StandardBeanFactory}.
   *
   * @param beanFactory the {@code StandardBeanFactory} instance to use for this context
   * @see #setMockContext
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericWebApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
  }

  /**
   * Create a new {@code GenericWebApplicationContext} for the given {@link MockContext}.
   *
   * @param mockContext the {@code MockContext} to run in
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericWebApplicationContext(@Nullable MockContext mockContext) {
    this.mockContext = mockContext;
  }

  /**
   * Create a new {@code GenericWebApplicationContext} with the given {@link StandardBeanFactory}
   * and {@link MockContext}.
   *
   * @param beanFactory the {@code StandardBeanFactory} instance to use for this context
   * @param mockContext the {@code MockContext} to run in
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericWebApplicationContext(StandardBeanFactory beanFactory, @Nullable MockContext mockContext) {
    super(beanFactory);
    this.mockContext = mockContext;
  }

  /**
   * Set the {@link MockContext} that this {@code WebApplicationContext} runs in.
   */
  @Override
  public void setMockContext(@Nullable MockContext mockContext) {
    this.mockContext = mockContext;
  }

  @Override
  @Nullable
  public MockContext getMockContext() {
    return this.mockContext;
  }

  /**
   * Create and return a new {@link StandardMockEnvironment}.
   */
  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardMockEnvironment();
  }

  /**
   * This implementation supports file paths beneath the root of the {@link MockContext}.
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
  protected PatternResourceLoader getPatternResourceLoader() {
    return new MockContextResourcePatternLoader(this);
  }

  /**
   * Register request/session scopes, environment beans, a {@link MockContextAwareProcessor}, etc.
   */
  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);
    if (this.mockContext != null) {
      beanFactory.addBeanPostProcessor(new MockContextAwareProcessor(this.mockContext));
      beanFactory.ignoreDependencyInterface(MockContextAware.class);
    }
    WebApplicationContextUtils.registerWebApplicationScopes(beanFactory, this.mockContext);
    WebApplicationContextUtils.registerEnvironmentBeans(beanFactory, this.mockContext);
  }

  /**
   * {@inheritDoc}
   * <p>Replace {@code Servlet}-related property sources.
   */
  @Override
  protected void initPropertySources() {
    ConfigurableEnvironment env = getEnvironment();
    if (env instanceof ConfigurableMockEnvironment) {
      ((ConfigurableMockEnvironment) env).initPropertySources(this.mockContext, null);
    }
  }

  // ---------------------------------------------------------------------
  // Pseudo-implementation of ConfigurableWebApplicationContext
  // ---------------------------------------------------------------------

  @Override
  public void setMockConfig(@Nullable MockConfig mockConfig) {
    // no-op
  }

  @Override
  @Nullable
  public MockConfig getMockConfig() {
    throw new UnsupportedOperationException(
            "GenericWebApplicationContext does not support getServletConfig()");
  }

  // ---------------------------------------------------------------------
  // Pseudo-implementation of ConfigurableWebApplicationContext
  // ---------------------------------------------------------------------

  @Override
  public void setNamespace(@Nullable String namespace) {
    // no-op
  }

  @Override
  @Nullable
  public String getNamespace() {
    throw new UnsupportedOperationException(
            "GenericWebApplicationContext does not support getNamespace()");
  }

  @Override
  public void setConfigLocation(String configLocation) {
    if (StringUtils.hasText(configLocation)) {
      throw new UnsupportedOperationException(
              "GenericWebApplicationContext does not support setConfigLocation(). " +
                      "Do you still have a 'contextConfigLocation' init-param set?");
    }
  }

  @Override
  public void setConfigLocations(String... configLocations) {
    if (ObjectUtils.isNotEmpty(configLocations)) {
      throw new UnsupportedOperationException(
              "GenericWebApplicationContext does not support setConfigLocations(). " +
                      "Do you still have a 'contextConfigLocations' init-param set?");
    }
  }

  @Override
  public String[] getConfigLocations() {
    throw new UnsupportedOperationException(
            "GenericWebApplicationContext does not support getConfigLocations()");
  }

}
