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
