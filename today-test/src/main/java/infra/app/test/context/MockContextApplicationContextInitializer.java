/*
 * Copyright 2017 - 2025 the original author or authors.
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
