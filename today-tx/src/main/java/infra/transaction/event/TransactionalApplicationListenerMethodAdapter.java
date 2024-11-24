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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import infra.context.ApplicationEvent;
import infra.context.event.ApplicationListenerMethodAdapter;
import infra.context.event.EventListener;
import infra.context.event.GenericApplicationListener;
import infra.core.annotation.AnnotatedElementUtils;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionalEventListener
 * @see TransactionalApplicationListener
 * @see TransactionalApplicationListenerAdapter
 * @since 4.0
 */
public class TransactionalApplicationListenerMethodAdapter extends ApplicationListenerMethodAdapter
        implements TransactionalApplicationListener<ApplicationEvent> {

  private static final Logger log = LoggerFactory.getLogger(TransactionalApplicationListenerMethodAdapter.class);

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
    TransactionalEventListener eventAnn =
            AnnotatedElementUtils.findMergedAnnotation(getTargetMethod(), TransactionalEventListener.class);
    if (eventAnn == null) {
      throw new IllegalStateException("No TransactionalEventListener annotation found on method: " + method);
    }
    this.transactionPhase = eventAnn.phase();
  }

  @Override
  public TransactionPhase getTransactionPhase() {
    return this.transactionPhase;
  }

  @Override
  public void addCallback(SynchronizationCallback callback) {
    Assert.notNull(callback, "SynchronizationCallback is required");
    this.callbacks.add(callback);
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    if (TransactionalApplicationListenerSynchronization.register(event, this, this.callbacks)) {
      if (log.isDebugEnabled()) {
        log.debug("Registered transaction synchronization for {}", event);
      }
    }
    else if (defaultExecution) {
      if (getTransactionPhase() == TransactionPhase.AFTER_ROLLBACK && log.isWarnEnabled()) {
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
