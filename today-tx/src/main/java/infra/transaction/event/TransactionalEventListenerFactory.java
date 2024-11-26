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

package infra.transaction.event;

import java.lang.reflect.Method;

import infra.context.ApplicationListener;
import infra.context.event.EventListenerFactory;
import infra.core.Ordered;
import infra.core.annotation.AnnotatedElementUtils;

/**
 * {@link EventListenerFactory} implementation that handles {@link TransactionalEventListener}
 * annotated methods.
 *
 * @author Stephane Nicoll
 * @see TransactionalApplicationListenerMethodAdapter
 * @since 4.0
 */
public class TransactionalEventListenerFactory implements EventListenerFactory, Ordered {

  private int order = 50;

  public void setOrder(int order) {
    this.order = order;
  }

  @Override
  public int getOrder() {
    return this.order;
  }

  @Override
  public boolean supportsMethod(Method method) {
    return AnnotatedElementUtils.hasAnnotation(method, TransactionalEventListener.class);
  }

  @Override
  public ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method) {
    return new TransactionalApplicationListenerMethodAdapter(beanName, type, method);
  }

}
