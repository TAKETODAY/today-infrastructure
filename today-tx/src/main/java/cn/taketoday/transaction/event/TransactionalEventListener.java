/*
 * Copyright 2017 - 2023 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.event.EventListener;
import cn.taketoday.core.annotation.AliasFor;

/**
 * An {@link EventListener} that is invoked according to a {@link TransactionPhase}.
 * This is an annotation-based equivalent of {@link TransactionalApplicationListener}.
 *
 * <p>If the event is not published within an active transaction, the event is discarded
 * unless the {@link #fallbackExecution} flag is explicitly set. If a transaction is
 * running, the event is handled according to its {@code TransactionPhase}.
 *
 * <p>Adding {@link cn.taketoday.core.annotation.Order @Order} to your annotated
 * method allows you to prioritize that listener amongst other listeners running before
 * or after transaction completion.
 *
 * <p>Transactional event listeners can work with thread-bound transactions managed
 * by a {@link cn.taketoday.transaction.PlatformTransactionManager} as well as reactive
 * transactions managed by a {@link cn.taketoday.transaction.ReactiveTransactionManager}.
 * For the former, listeners are guaranteed to see the current thread-bound transaction.
 * Since the latter uses the Reactor context instead of thread-local variables, the transaction
 * context needs to be included in the published event instance as the event source:
 * see {@link cn.taketoday.transaction.reactive.TransactionalEventPublisher}.
 *
 * <p><strong>WARNING:</strong> if the {@code TransactionPhase} is set to
 * {@link TransactionPhase#AFTER_COMMIT AFTER_COMMIT} (the default),
 * {@link TransactionPhase#AFTER_ROLLBACK AFTER_ROLLBACK}, or
 * {@link TransactionPhase#AFTER_COMPLETION AFTER_COMPLETION}, the transaction will
 * have been committed or rolled back already, but the transactional resources might
 * still be active and accessible. As a consequence, any data access code triggered
 * at this point will still "participate" in the original transaction, but changes
 * will not be committed to the transactional resource. See
 * {@link cn.taketoday.transaction.support.TransactionSynchronization#afterCompletion(int)
 * TransactionSynchronization.afterCompletion(int)} for details.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see TransactionalApplicationListener
 * @see TransactionalApplicationListenerMethodAdapter
 * @since 4.0
 */
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EventListener
public @interface TransactionalEventListener {

  /**
   * Phase to bind the handling of an event to.
   * <p>The default phase is {@link TransactionPhase#AFTER_COMMIT}.
   * <p>If no transaction is in progress, the event is not processed at
   * all unless {@link #fallbackExecution} has been enabled explicitly.
   */
  TransactionPhase phase() default TransactionPhase.AFTER_COMMIT;

  /**
   * Whether the event should be handled if no transaction is running.
   */
  boolean fallbackExecution() default false;

  /**
   * Alias for {@link #event}.
   */
  @AliasFor(annotation = EventListener.class, attribute = "event")
  Class<?>[] value() default {};

  /**
   * The event classes that this listener handles.
   * <p>If this attribute is specified with a single value, the annotated
   * method may optionally accept a single parameter. However, if this
   * attribute is specified with multiple values, the annotated method
   * must <em>not</em> declare any parameters.
   */
  @AliasFor(annotation = EventListener.class, attribute = "event")
  Class<?>[] event() default {};

  /**
   * Spring Expression Language (SpEL) attribute used for making the event
   * handling conditional.
   * <p>The default is {@code ""}, meaning the event is always handled.
   *
   * @see EventListener#condition
   */
  @AliasFor(annotation = EventListener.class, attribute = "condition")
  String condition() default "";

  /**
   * An optional identifier for the listener, defaulting to the fully-qualified
   * signature of the declaring method (e.g. "mypackage.MyClass.myMethod()").
   *
   * @see EventListener#id
   * @see TransactionalApplicationListener#getListenerId()
   */
  @AliasFor(annotation = EventListener.class, attribute = "id")
  String id() default "";

}
