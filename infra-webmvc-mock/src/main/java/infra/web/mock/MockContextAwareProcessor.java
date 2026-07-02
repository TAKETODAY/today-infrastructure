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

package infra.web.mock;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.web.mock.api.MockContext;

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
@Deprecated
public class MockContextAwareProcessor implements InitializationBeanPostProcessor {

  private final @Nullable MockContext mockContext;

  /**
   * Create a new MockContextAwareProcessor for the given context.
   */
  public MockContextAwareProcessor(@Nullable MockContext mockContext) {
    this.mockContext = mockContext;
  }

  /**
   * Returns the {@link MockContext} to be injected or {@code null}. This method
   * can be overridden by subclasses when a context is obtained after the post-processor
   * has been registered.
   */
  @Nullable
  protected MockContext getMockContext() {
    return this.mockContext;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    if (getMockContext() != null && bean instanceof MockContextAware) {
      ((MockContextAware) bean).setMockContext(getMockContext());
    }
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) {
    return bean;
  }

}

