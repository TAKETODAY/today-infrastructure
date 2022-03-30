/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.builder;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ApplicationEvent;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.core.Ordered;

/**
 * {@link ApplicationContextInitializer} for setting the parent context. Also publishes
 * {@link ParentContextAvailableEvent} when the context is refreshed to signal to other
 * listeners that the context is available and has a parent.
 *
 * @author Dave Syer
 * @since 4.0
 */
public class ParentContextApplicationContextInitializer
        implements ApplicationContextInitializer, Ordered {

  private int order = Ordered.HIGHEST_PRECEDENCE;

  private final ApplicationContext parent;

  public ParentContextApplicationContextInitializer(ApplicationContext parent) {
    this.parent = parent;
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
    if (applicationContext != this.parent) {
      applicationContext.setParent(this.parent);
      applicationContext.addApplicationListener(EventPublisher.INSTANCE);
    }
  }

  private static class EventPublisher implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    private static final EventPublisher INSTANCE = new EventPublisher();

    @Override
    public int getOrder() {
      return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
      ApplicationContext context = event.getApplicationContext();
      if (context instanceof ConfigurableApplicationContext && context == event.getSource()) {
        context.publishEvent(new ParentContextAvailableEvent((ConfigurableApplicationContext) context));
      }
    }

  }

  /**
   * {@link ApplicationEvent} fired when a parent context is available.
   */
  @SuppressWarnings("serial")
  public static class ParentContextAvailableEvent extends ApplicationEvent {

    public ParentContextAvailableEvent(ConfigurableApplicationContext applicationContext) {
      super(applicationContext);
    }

    public ConfigurableApplicationContext getApplicationContext() {
      return (ConfigurableApplicationContext) getSource();
    }

  }

}
