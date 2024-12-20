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

package infra.web.mock.support;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.Configuration;
import infra.context.support.GenericApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.io.PatternResourceLoader;
import infra.core.io.Resource;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.mock.api.MockConfig;
import infra.mock.api.MockContext;
import infra.stereotype.Component;
import infra.util.ObjectUtils;
import infra.util.StringUtils;
import infra.web.mock.ConfigurableWebApplicationContext;
import infra.web.mock.ConfigurableWebEnvironment;
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
    if (env instanceof ConfigurableWebEnvironment) {
      ((ConfigurableWebEnvironment) env).initPropertySources(this.mockContext, null);
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
