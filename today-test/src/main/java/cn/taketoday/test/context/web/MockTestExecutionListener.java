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

package cn.taketoday.test.context.web;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.Conventions;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.mock.api.MockContext;
import cn.taketoday.mock.web.HttpMockRequestImpl;
import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.mock.web.MockHttpResponseImpl;
import cn.taketoday.test.context.TestContext;
import cn.taketoday.test.context.TestExecutionListener;
import cn.taketoday.test.context.support.AbstractTestExecutionListener;
import cn.taketoday.test.context.support.DependencyInjectionTestExecutionListener;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.mock.MockRequestContext;
import cn.taketoday.web.mock.WebApplicationContext;

/**
 * {@code TestExecutionListener} which provides mock Servlet API support to
 * {@link WebApplicationContext WebServletApplicationContext} loaded by the <em>Infra
 * TestContext Framework</em>.
 *
 * <p>Specifically, {@code ServletTestExecutionListener} sets up thread-local
 * state via Infra Web's {@link RequestContextHolder} during {@linkplain
 * #prepareTestInstance(TestContext) test instance preparation} and {@linkplain
 * #beforeTestMethod(TestContext) before each test method} and creates a {@link
 * HttpMockRequestImpl}, {@link MockHttpResponseImpl}, and
 * {@link cn.taketoday.web.RequestContext} based on the {@link MockContextImpl} present in
 * the {@code WebApplicationContext}. This listener also ensures that the
 * {@code MockHttpServletResponse} and {@code ServletWebRequest} can be injected
 * into the test instance, and once the test is complete this listener {@linkplain
 * #afterTestMethod(TestContext) cleans up} thread-local state.
 *
 * <p>Note that {@code ServletTestExecutionListener} is enabled by default but
 * generally takes no action if the {@linkplain TestContext#getTestClass() test
 * class} is not annotated with {@link WebAppConfiguration @WebAppConfiguration}.
 * See the javadocs for individual methods in this class for details.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MockTestExecutionListener extends AbstractTestExecutionListener {

  /**
   * Attribute name for a {@link TestContext} attribute which indicates
   * whether or not the {@code ServletTestExecutionListener} should {@linkplain
   * RequestContextHolder#cleanup() reset} Infra Web's
   * {@code RequestContextHolder} in {@link #afterTestMethod(TestContext)}.
   * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
   */
  public static final String RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          MockTestExecutionListener.class, "resetRequestContextHolder");

  /**
   * Attribute name for a {@link TestContext} attribute which indicates that
   * {@code ServletTestExecutionListener} has already populated Infra Web's
   * {@code RequestContextHolder}.
   * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
   */
  public static final String POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          MockTestExecutionListener.class, "populatedRequestContextHolder");

  /**
   * Attribute name for a request attribute which indicates that the
   * {@link HttpMockRequestImpl} stored in the {@link cn.taketoday.web.RequestContext}
   * in Infra Web's {@link RequestContextHolder} was created by the TestContext
   * framework.
   * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
   */
  public static final String CREATED_BY_THE_TESTCONTEXT_FRAMEWORK = Conventions.getQualifiedAttributeName(
          MockTestExecutionListener.class, "createdByTheTestContextFramework");

  /**
   * Attribute name for a {@link TestContext} attribute which indicates that the
   * {@code ServletTestExecutionListener} should be activated. When not set to
   * {@code true}, activation occurs when the {@linkplain TestContext#getTestClass()
   * test class} is annotated with {@link WebAppConfiguration @WebAppConfiguration}.
   * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
   */
  public static final String ACTIVATE_LISTENER = Conventions.getQualifiedAttributeName(
          MockTestExecutionListener.class, "activateListener");

  private static final Logger logger = LoggerFactory.getLogger(MockTestExecutionListener.class);

  /**
   * Returns {@code 1000}.
   */
  @Override
  public final int getOrder() {
    return 1000;
  }

  /**
   * Sets up thread-local state during the <em>test instance preparation</em>
   * callback phase via Infra Web's {@link RequestContextHolder}, but only if
   * the {@linkplain TestContext#getTestClass() test class} is annotated with
   * {@link WebAppConfiguration @WebAppConfiguration}.
   *
   * @see TestExecutionListener#prepareTestInstance(TestContext)
   * @see #setUpRequestContextIfNecessary(TestContext)
   */
  @Override
  public void prepareTestInstance(TestContext testContext) throws Exception {
    setUpRequestContextIfNecessary(testContext);
  }

  /**
   * Sets up thread-local state before each test method via Infra Web's
   * {@link RequestContextHolder}, but only if the
   * {@linkplain TestContext#getTestClass() test class} is annotated with
   * {@link WebAppConfiguration @WebAppConfiguration}.
   *
   * @see TestExecutionListener#beforeTestMethod(TestContext)
   * @see #setUpRequestContextIfNecessary(TestContext)
   */
  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    setUpRequestContextIfNecessary(testContext);
  }

  /**
   * If the {@link #RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} in the supplied
   * {@code TestContext} has a value of {@link Boolean#TRUE}, this method will
   * (1) clean up thread-local state after each test method by {@linkplain
   * RequestContextHolder#cleanup() resetting} Infra Web's
   * {@code RequestContextHolder} and (2) ensure that new mocks are injected
   * into the test instance for subsequent tests by setting the
   * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE}
   * in the test context to {@code true}.
   * <p>The {@link #RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} and
   * {@link #POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE} will be subsequently
   * removed from the test context, regardless of their values.
   *
   * @see TestExecutionListener#afterTestMethod(TestContext)
   */
  @Override
  public void afterTestMethod(TestContext testContext) throws Exception {
    if (Boolean.TRUE.equals(testContext.getAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE))) {
      logger.debug("Resetting RequestContextHolder for test context {}.", testContext);
      RequestContextHolder.cleanup();
      testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE,
              Boolean.TRUE);
    }
    testContext.removeAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
    testContext.removeAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE);
  }

  private boolean isActivated(TestContext testContext) {
    return Boolean.TRUE.equals(testContext.getAttribute(ACTIVATE_LISTENER))
            || AnnotatedElementUtils.hasAnnotation(testContext.getTestClass(), WebAppConfiguration.class);
  }

  private boolean alreadyPopulatedRequestContextHolder(TestContext testContext) {
    return Boolean.TRUE.equals(testContext.getAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE));
  }

  private void setUpRequestContextIfNecessary(TestContext testContext) {
    if (!isActivated(testContext) || alreadyPopulatedRequestContextHolder(testContext)) {
      return;
    }

    ApplicationContext context = testContext.getApplicationContext();

    if (context instanceof WebApplicationContext wac) {
      MockContext mockContext = wac.getMockContext();
      Assert.state(mockContext instanceof MockContextImpl,
              () -> "The WebApplicationContext for test context %s must be configured with a MockContext.".formatted(testContext));

      logger.debug("Setting up MockHttpRequest, MockHttpResponse, WebRequest, and RequestContextHolder for test context .",
              testContext);

      HttpMockRequestImpl request = new HttpMockRequestImpl(mockContext);
      request.setAttribute(CREATED_BY_THE_TESTCONTEXT_FRAMEWORK, Boolean.TRUE);
      MockHttpResponseImpl response = new MockHttpResponseImpl();

      RequestContextHolder.set(new MockRequestContext(wac, request, response));
//      RequestContextHolder.setRequestAttributes(servletWebRequest);
      testContext.setAttribute(POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);
      testContext.setAttribute(RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE, Boolean.TRUE);

      if (wac instanceof ConfigurableApplicationContext configurableApplicationContext) {
        ConfigurableBeanFactory bf = configurableApplicationContext.getBeanFactory();
        bf.registerResolvableDependency(MockHttpResponseImpl.class, response);
      }
    }
  }

}
