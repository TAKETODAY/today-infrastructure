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

package cn.taketoday.framework.context;

import java.util.concurrent.atomic.AtomicLong;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextInitializer;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.util.StringUtils;

/**
 * {@link ApplicationContextInitializer} that sets the Spring
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
