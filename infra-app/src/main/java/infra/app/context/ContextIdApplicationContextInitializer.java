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

package infra.app.context;

import java.util.concurrent.atomic.AtomicLong;

import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.core.Ordered;
import infra.core.env.ConfigurableEnvironment;
import infra.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that sets the Infra
 * {@link ApplicationContext#getId() ApplicationContext ID}. The
 * {@code app.name} property is used to create the ID. If the property is
 * not set {@code application} is used.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/6 21:56
 */
public class ContextIdApplicationContextInitializer implements ApplicationContextInitializer, Ordered {

  private int order = Ordered.LOWEST_PRECEDENCE - 10;

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    ContextId contextId = getContextId(applicationContext);
    applicationContext.setId(contextId.getId());
    applicationContext.getBeanFactory().registerSingleton(ContextId.class.getName(), contextId);
  }

  private ContextId getContextId(ConfigurableApplicationContext applicationContext) {
    ApplicationContext parent = applicationContext.getParent();
    if (parent != null && parent.containsBean(ContextId.class.getName())) {
      return parent.getBean(ContextId.class).createChildId();
    }
    return new ContextId(getApplicationId(applicationContext.getEnvironment()));
  }

  private String getApplicationId(ConfigurableEnvironment environment) {
    String name = environment.getProperty(ApplicationContext.APPLICATION_NAME);
    return StringUtils.hasText(name) ? name : "application";
  }

  /**
   * The ID of a context.
   */
  static class ContextId {

    private final AtomicLong children = new AtomicLong();

    private final String id;

    ContextId(String id) {
      this.id = id;
    }

    ContextId createChildId() {
      return new ContextId(this.id + "-" + this.children.incrementAndGet());
    }

    String getId() {
      return this.id;
    }

  }

}
