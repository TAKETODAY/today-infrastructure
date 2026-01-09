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
