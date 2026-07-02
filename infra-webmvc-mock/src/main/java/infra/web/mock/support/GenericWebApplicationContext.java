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

import java.io.Serializable;
import java.util.function.Supplier;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.Configuration;
import infra.context.support.GenericApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.session.Session;
import infra.stereotype.Component;
import infra.web.RequestContextHolder;
import infra.web.RequestContextUtils;
import infra.web.context.StandardWebEnvironment;
import infra.web.mock.ConfigurableWebApplicationContext;
import infra.web.mock.MockContextAware;
import infra.web.mock.MockContextAwareProcessor;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.mock.MockUtils;
import infra.web.mock.api.MockContext;

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
   * Create and return a new {@link StandardWebEnvironment}.
   */
  @Override
  protected ConfigurableEnvironment createEnvironment() {
    return new StandardWebEnvironment();
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);
    if (this.mockContext != null) {
      beanFactory.addBeanPostProcessor(new MockContextAwareProcessor(this.mockContext));
      beanFactory.ignoreDependencyInterface(MockContextAware.class);
    }

    registerWebApplicationScopes(beanFactory, this.mockContext);
  }

  /**
   * Register web-specific scopes ("request", "session")
   * with the given BeanFactory, as used by the WebServletApplicationContext.
   *
   * @param beanFactory the BeanFactory to configure
   * @param sc the MockContext that we're running within
   */
  static void registerWebApplicationScopes(
          ConfigurableBeanFactory beanFactory, @Nullable MockContext sc) {
    RequestContextUtils.registerScopes(beanFactory);

    if (sc != null) {
      beanFactory.registerResolvableDependency(MockContext.class, sc);
    }

    beanFactory.registerResolvableDependency(Session.class, new SessionObjectSupplier());
    beanFactory.registerResolvableDependency(MockRequest.class, new RequestObjectSupplier());
    beanFactory.registerResolvableDependency(MockResponse.class, new ResponseObjectSupplier());
  }

  /**
   * Factory that exposes the current request object on demand.
   */
  @SuppressWarnings("serial")
  private static class RequestObjectSupplier implements Supplier<MockRequest>, Serializable {

    @Override
    public MockRequest get() {
      return MockUtils.getMockRequest(RequestContextHolder.current());
    }

    @Override
    public String toString() {
      return "Current MockRequest";
    }

  }

  /**
   * Factory that exposes the current response object on demand.
   */
  @SuppressWarnings("serial")
  private static class ResponseObjectSupplier implements Supplier<MockResponse>, Serializable {

    @Override
    public MockResponse get() {
      return MockUtils.getMockResponse(RequestContextHolder.current());
    }

    @Override
    public String toString() {
      return "Current HttpMockResponse";
    }
  }

  /**
   * Factory that exposes the current session object on demand.
   */
  @SuppressWarnings("serial")
  private static class SessionObjectSupplier implements Supplier<Session>, Serializable {

    @Override
    public Session get() {
      return RequestContextHolder.required().getSession();
    }

    @Override
    public String toString() {
      return "Current HttpSession";
    }

  }

}
