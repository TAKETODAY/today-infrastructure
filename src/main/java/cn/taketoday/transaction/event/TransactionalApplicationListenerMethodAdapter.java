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

package cn.taketoday.transaction.event;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.taketoday.context.event.EventListener;
import cn.taketoday.context.event.GenericApplicationListener;
import cn.taketoday.context.event.MethodApplicationListener;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

/**
 * {@link GenericApplicationListener} adapter that delegates the processing of
 * an event to a {@link TransactionalEventListener} annotated method. Supports
 * the exact same features as any regular {@link EventListener} annotated method
 * but is aware of the transactional context of the event publisher.
 *
 * <p>Processing of {@link TransactionalEventListener} is enabled automatically
 * when Framework's transaction management is enabled. For other cases, registering
 * a bean of type {@link TransactionalEventListenerFactory} is required.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @see TransactionalEventListener
 * @see TransactionalApplicationListener
 * @see TransactionalApplicationListenerAdapter
 * @since 4.0
 */
public class TransactionalApplicationListenerMethodAdapter
        extends MethodApplicationListener
        implements TransactionalApplicationListener<Object> {
  private static final Logger log = LoggerFactory.getLogger(TransactionalApplicationListenerMethodAdapter.class);

  private final TransactionalEventListener annotation;

  private final TransactionPhase transactionPhase;

  private final List<SynchronizationCallback> callbacks = new CopyOnWriteArrayList<>();

  /**
   * Construct a new TransactionalApplicationListenerMethodAdapter.
   *
   * @param beanName the name of the bean to invoke the listener method on
   * @param targetClass the target class that the method is declared on
   * @param method the listener method to invoke
   */
  public TransactionalApplicationListenerMethodAdapter(String beanName, Class<?> targetClass, Method method) {
    super(beanName, targetClass, method);
    TransactionalEventListener ann =
            AnnotatedElementUtils.findMergedAnnotation(method, TransactionalEventListener.class);
    if (ann == null) {
      throw new IllegalStateException("No TransactionalEventListener annotation found on method: " + method);
    }
    this.annotation = ann;
    this.transactionPhase = ann.phase();
  }

  @Override
  public TransactionPhase getTransactionPhase() {
    return this.transactionPhase;
  }

  @Override
  public void addCallback(SynchronizationCallback callback) {
    Assert.notNull(callback, "SynchronizationCallback must not be null");
    this.callbacks.add(callback);
  }

  @Override
  public void processEvent(Object event) {
    onApplicationEvent(event);
  }

  @Override
  public void onApplicationEvent(Object event) {
    if (TransactionSynchronizationManager.isSynchronizationActive()
            && TransactionSynchronizationManager.isActualTransactionActive()) {
      TransactionSynchronizationManager.registerSynchronization(
              new TransactionalApplicationListenerSynchronization<>(event, this, this.callbacks));
    }
    else if (this.annotation.fallbackExecution()) {
      if (this.annotation.phase() == TransactionPhase.AFTER_ROLLBACK && log.isWarnEnabled()) {
        log.warn("Processing {} as a fallback execution on AFTER_ROLLBACK phase", event);
      }
      processEvent(event);
    }
    else {
      // No transactional event execution at all
      if (log.isDebugEnabled()) {
        log.debug("No transaction is active - skipping {}", event);
      }
    }
  }

}
