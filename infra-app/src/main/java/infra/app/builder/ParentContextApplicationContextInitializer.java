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

package infra.app.builder;

import java.io.Serial;

import infra.context.ApplicationContext;
import infra.context.ApplicationContextInitializer;
import infra.context.ApplicationEvent;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.context.event.ContextRefreshedEvent;
import infra.core.Ordered;
import infra.core.OrderedSupport;

/**
 * {@link ApplicationContextInitializer} for setting the parent context. Also publishes
 * {@link ParentContextAvailableEvent} when the context is refreshed to signal to other
 * listeners that the context is available and has a parent.
 *
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ParentContextApplicationContextInitializer extends OrderedSupport implements ApplicationContextInitializer, Ordered {

  private final ApplicationContext parent;

  public ParentContextApplicationContextInitializer(ApplicationContext parent) {
    super(Ordered.HIGHEST_PRECEDENCE);
    this.parent = parent;
  }

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    if (applicationContext != parent) {
      applicationContext.setParent(parent);
      applicationContext.addApplicationListener(new EventPublisher());
    }
  }

  private static final class EventPublisher implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
      if (event.getApplicationContext()
              instanceof ConfigurableApplicationContext context && context == event.getSource()) {
        context.publishEvent(new ParentContextAvailableEvent(context));
      }
    }

  }

  /**
   * {@link ApplicationEvent} fired when a parent context is available.
   */
  public static class ParentContextAvailableEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    public ParentContextAvailableEvent(ConfigurableApplicationContext applicationContext) {
      super(applicationContext);
    }

    public ConfigurableApplicationContext getApplicationContext() {
      return (ConfigurableApplicationContext) getSource();
    }

  }

}
