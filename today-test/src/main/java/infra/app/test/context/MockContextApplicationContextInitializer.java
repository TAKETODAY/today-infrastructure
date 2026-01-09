/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.test.context;

import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.core.Ordered;
import infra.mock.api.MockContext;
import infra.web.mock.ConfigurableWebApplicationContext;
import infra.web.mock.WebApplicationContext;

/**
 * {@link ApplicationContextInitializer} for setting the mock context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MockContextApplicationContextInitializer implements ApplicationContextInitializer, Ordered {

  private int order = Ordered.HIGHEST_PRECEDENCE;

  private final MockContext mockContext;

  private final boolean addApplicationContextAttribute;

  /**
   * Create a new {@link MockContextApplicationContextInitializer} instance.
   *
   * @param mockContext the mock that should be ultimately set.
   */
  public MockContextApplicationContextInitializer(MockContext mockContext) {
    this(mockContext, false);
  }

  /**
   * Create a new {@link MockContextApplicationContextInitializer} instance.
   *
   * @param mockContext the mock that should be ultimately set.
   * @param addApplicationContextAttribute if the {@link ApplicationContext} should be
   * stored as an attribute in the {@link MockContext}
   */
  public MockContextApplicationContextInitializer(MockContext mockContext,
          boolean addApplicationContextAttribute) {
    this.mockContext = mockContext;
    this.addApplicationContextAttribute = addApplicationContextAttribute;
  }

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    if (applicationContext instanceof ConfigurableWebApplicationContext cwa) {
      cwa.setMockContext(this.mockContext);

      if (this.addApplicationContextAttribute) {
        this.mockContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
      }
    }
  }

}
