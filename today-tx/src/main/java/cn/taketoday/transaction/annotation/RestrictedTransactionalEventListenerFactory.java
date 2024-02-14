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

package cn.taketoday.transaction.annotation;

import java.lang.reflect.Method;

import cn.taketoday.context.ApplicationListener;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.transaction.event.TransactionalEventListenerFactory;

/**
 * Extension of {@link TransactionalEventListenerFactory},
 * Extension of {@link TransactionalEventListenerFactory},
 * detecting invalid transaction configuration for transactional event listeners:
 * {@link Transactional} only supported with {@link Propagation#REQUIRES_NEW}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see cn.taketoday.transaction.event.TransactionalEventListener
 * @see Transactional
 * @since 4.0
 */
public class RestrictedTransactionalEventListenerFactory extends TransactionalEventListenerFactory {

  @Override
  public ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method) {
    Transactional txAnn = AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
    if (txAnn != null) {
      Propagation propagation = txAnn.propagation();
      if (propagation != Propagation.REQUIRES_NEW && propagation != Propagation.NOT_SUPPORTED) {
        throw new IllegalStateException("@TransactionalEventListener method must not be annotated with " +
                "@Transactional unless when declared as REQUIRES_NEW or NOT_SUPPORTED: " + method);
      }
    }
    return super.createApplicationListener(beanName, type, method);
  }

}
