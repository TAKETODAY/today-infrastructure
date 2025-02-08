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
