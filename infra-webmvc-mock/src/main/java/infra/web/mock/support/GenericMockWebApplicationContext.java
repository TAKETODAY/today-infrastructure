/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.mock.support;

import java.io.Serializable;
import java.util.function.Supplier;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotatedBeanDefinitionReader;
import infra.context.annotation.Configuration;
import infra.context.support.GenericApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.session.Session;
import infra.stereotype.Component;
import infra.web.RequestContextHolder;
import infra.web.RequestContextUtils;
import infra.web.context.StandardWebEnvironment;
import infra.web.mock.MockRequest;
import infra.web.mock.MockResponse;
import infra.web.mock.MockUtils;

/**
 * Subclass of {@link GenericApplicationContext}, suitable for web mock environments.
 *
 * <p>Interprets resource paths as mock context resources, i.e. as paths beneath
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
public class GenericMockWebApplicationContext extends GenericApplicationContext
        implements ConfigurableApplicationContext {

  /**
   * Create a new {@code GenericWebApplicationContext}.
   *
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericMockWebApplicationContext() {
    super();
  }

  /**
   * Create a new {@code GenericWebApplicationContext} with the given {@link StandardBeanFactory}.
   *
   * @param beanFactory the {@code StandardBeanFactory} instance to use for this context
   * @see #registerBeanDefinition
   * @see #refresh
   */
  public GenericMockWebApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
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

    registerWebApplicationScopes(beanFactory);
  }

  /**
   * Register web-specific scopes ("request", "session")
   * with the given BeanFactory, as used by the WebServletApplicationContext.
   *
   * @param beanFactory the BeanFactory to configure
   */
  static void registerWebApplicationScopes(ConfigurableBeanFactory beanFactory) {
    RequestContextUtils.registerScopes(beanFactory);

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
