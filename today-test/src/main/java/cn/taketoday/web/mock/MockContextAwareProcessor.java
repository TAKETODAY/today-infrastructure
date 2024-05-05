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

package cn.taketoday.web.mock;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.api.MockConfig;
import cn.taketoday.mock.api.MockContext;

/**
 * {@link BeanPostProcessor} implementation
 * that passes the MockContext to beans that implement the
 * {@link MockContextAware} interface.
 *
 * <p>Web application contexts will automatically register this with their
 * underlying bean factory. Applications do not use this directly.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MockContextAware
 * @since 4.0 2022/2/20 20:57
 */
public class MockContextAwareProcessor implements InitializationBeanPostProcessor {

  @Nullable
  private final MockContext mockContext;

  @Nullable
  private final MockConfig mockConfig;

  /**
   * Create a new MockContextAwareProcessor for the given context.
   */
  public MockContextAwareProcessor(MockContext mockContext) {
    this(mockContext, null);
  }

  /**
   * Create a new MockContextAwareProcessor for the given context and config.
   */
  public MockContextAwareProcessor(@Nullable MockContext mockContext, @Nullable MockConfig mockConfig) {
    this.mockContext = mockContext;
    this.mockConfig = mockConfig;
  }

  /**
   * Returns the {@link MockContext} to be injected or {@code null}. This method
   * can be overridden by subclasses when a context is obtained after the post-processor
   * has been registered.
   */
  @Nullable
  protected MockContext getMockContext() {
    if (this.mockContext == null && getMockConfig() != null) {
      return getMockConfig().getMockContext();
    }
    return this.mockContext;
  }

  /**
   * Returns the {@link MockConfig} to be injected or {@code null}. This method
   * can be overridden by subclasses when a context is obtained after the post-processor
   * has been registered.
   */
  @Nullable
  protected MockConfig getMockConfig() {
    return this.mockConfig;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (getMockContext() != null && bean instanceof MockContextAware) {
      ((MockContextAware) bean).setMockContext(getMockContext());
    }
    if (getMockConfig() != null && bean instanceof MockConfigAware) {
      ((MockConfigAware) bean).setMockConfig(getMockConfig());
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    return bean;
  }

}

