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

package infra.test.web.mock.setup;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

import infra.beans.factory.support.StaticListableBeanFactory;
import infra.context.ApplicationContext;
import infra.core.io.PatternResourceLoader;
import infra.mock.api.MockContext;
import infra.util.ObjectUtils;
import infra.web.mock.WebApplicationContext;
import infra.web.mock.support.GenericWebApplicationContext;
import infra.web.mock.support.MockContextResourcePatternLoader;

/**
 * A stub WebApplicationContext that accepts registrations of object instances.
 *
 * <p>As registered object instances are instantiated and initialized externally,
 * there is no wiring, bean initialization, lifecycle events, as well as no
 * pre-processing and post-processing hooks typically associated with beans
 * managed by an {@link ApplicationContext}. Just a simple lookup into a
 * {@link StaticListableBeanFactory}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
class StubWebApplicationContext extends GenericWebApplicationContext implements WebApplicationContext {

  private final PatternResourceLoader resourcePatternResolver;

  public StubWebApplicationContext(MockContext mockContext) {
    setMockContext(mockContext);
    this.resourcePatternResolver = new MockContextResourcePatternLoader(mockContext);
  }

  @Override
  protected PatternResourceLoader getPatternResourceLoader() {
    return resourcePatternResolver;
  }

  public void addBean(String name, Object bean) {
    this.beanFactory.registerSingleton(name, bean);
  }

  public void addBeans(@Nullable List<?> beans) {
    if (beans != null) {
      for (Object bean : beans) {
        String name = bean.getClass().getName() + "#" + ObjectUtils.getIdentityHexString(bean);
        this.beanFactory.registerSingleton(name, bean);
      }
    }
  }

  public void addBeans(Object... beans) {
    addBeans(Arrays.asList(beans));
  }

}
