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

package infra.test.context.web;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.core.Conventions;
import infra.core.annotation.AnnotatedElementUtils;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.mock.api.MockContext;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockContextImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.test.context.TestContext;
import infra.test.context.TestExecutionListener;
import infra.test.context.support.AbstractTestExecutionListener;
import infra.test.context.support.DependencyInjectionTestExecutionListener;
import infra.web.RequestContextHolder;
import infra.web.mock.MockRequestContext;
import infra.web.mock.WebApplicationContext;

/**
 * {@code TestExecutionListener} which provides mock Web API support to
 * {@link WebApplicationContext WebMockApplicationContext} loaded by the <em>Infra
 * TestContext Framework</em>.
 *
 * <p>Specifically, {@code MockTestExecutionListener} sets up thread-local
 * state via Infra Web's {@link RequestContextHolder} during {@linkplain
 * #prepareTestInstance(TestContext) test instance preparation} and {@linkplain
 * #beforeTestMethod(TestContext) before each test method} and creates a {@link
 * HttpMockRequestImpl}, {@link MockHttpResponseImpl}, and
 * {@link infra.web.RequestContext} based on the {@link MockContextImpl} present in
 * the {@code WebApplicationContext}. This listener also ensures that the
 * {@code MockHttpMockResponse} and {@code MockWebRequest} can be injected
 * into the test instance, and once the test is complete this listener {@linkplain
 * #afterTestMethod(TestContext) cleans up} thread-local state.
 *
 * <p>Note that {@code MockTestExecutionListener} is enabled by default but
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
   * whether or not the {@code MockTestExecutionListener} should {@linkplain
   * RequestContextHolder#cleanup() reset} Infra Web's
   * {@code RequestContextHolder} in {@link #afterTestMethod(TestContext)}.
   * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
   */
  public static final String RESET_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          MockTestExecutionListener.class, "resetRequestContextHolder");

  /**
   * Attribute name for a {@link TestContext} attribute which indicates that
   * {@code MockTestExecutionListener} has already populated Infra Web's
   * {@code RequestContextHolder}.
   * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
   */
  public static final String POPULATED_REQUEST_CONTEXT_HOLDER_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          MockTestExecutionListener.class, "populatedRequestContextHolder");

  /**
   * Attribute name for a request attribute which indicates that the
   * {@link HttpMockRequestImpl} stored in the {@link infra.web.RequestContext}
   * in Infra Web's {@link RequestContextHolder} was created by the TestContext
   * framework.
   * <p>Permissible values include {@link Boolean#TRUE} and {@link Boolean#FALSE}.
   */
  public static final String CREATED_BY_THE_TESTCONTEXT_FRAMEWORK = Conventions.getQualifiedAttributeName(
          MockTestExecutionListener.class, "createdByTheTestContextFramework");

  /**
   * Attribute name for a {@link TestContext} attribute which indicates that the
   * {@code MockTestExecutionListener} should be activated. When not set to
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
