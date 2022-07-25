/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.event;

import java.lang.reflect.Method;

import cn.taketoday.context.ApplicationListener;
import cn.taketoday.core.Ordered;

/**
 * Default {@link EventListenerFactory} implementation that supports the
 * regular {@link EventListener} annotation.
 *
 * <p>Used as "catch-all" implementation by default.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ApplicationListenerMethodAdapter
 * @since 4.0 2021/12/8 21:50
 */
public class DefaultEventListenerFactory implements EventListenerFactory, Ordered {

  private int order = LOWEST_PRECEDENCE;

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public boolean supportsMethod(Method method) {
    return true;
  }

  @Override
  public ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method) {
    return new ApplicationListenerMethodAdapter(beanName, type, method);
  }

}
