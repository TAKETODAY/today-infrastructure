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

package infra.transaction.reactive;

import java.util.Collection;

import infra.aop.scope.ScopedObject;
import infra.core.InfraProxy;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Utility methods for triggering specific {@link TransactionSynchronization}
 * callback methods on all currently registered synchronizations.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @see TransactionSynchronization
 * @see TransactionSynchronizationManager#getSynchronizations()
 * @since 4.0
 */
abstract class TransactionSynchronizationUtils {

  private static final Logger logger = LoggerFactory.getLogger(TransactionSynchronizationUtils.class);

  private static final boolean aopAvailable = ClassUtils.isPresent(
          "infra.aop.scope.ScopedObject", TransactionSynchronizationUtils.class.getClassLoader());

  /**
   * Unwrap the given resource handle if necessary; otherwise return
   * the given handle as-is.
   *
   * @see InfraProxy#getWrappedObject()
   */
  static Object unwrapResourceIfNecessary(Object resource) {
    Assert.notNull(resource, "Resource is required");
    Object resourceRef = resource;
    // unwrap infrastructure proxy
    if (resourceRef instanceof InfraProxy) {
      resourceRef = ((InfraProxy) resourceRef).getWrappedObject();
    }
    if (aopAvailable) {
      // now unwrap scoped proxy
      resourceRef = ScopedProxyUnwrapper.unwrapIfNecessary(resourceRef);
    }
    return resourceRef;
  }

  /**
   * Actually invoke the {@code triggerBeforeCommit} methods of the
   * given Framework TransactionSynchronization objects.
   *
   * @param synchronizations a List of TransactionSynchronization objects
   * @see TransactionSynchronization#beforeCommit(boolean)
   */
  public static Mono<Void> triggerBeforeCommit(Collection<TransactionSynchronization> synchronizations, boolean readOnly) {
    return Flux.fromIterable(synchronizations).concatMap(it -> it.beforeCommit(readOnly)).then();
  }

  /**
   * Actually invoke the {@code beforeCompletion} methods of the
   * given Framework TransactionSynchronization objects.
   *
   * @param synchronizations a List of TransactionSynchronization objects
   * @see TransactionSynchronization#beforeCompletion()
   */
  public static Mono<Void> triggerBeforeCompletion(Collection<TransactionSynchronization> synchronizations) {
    return Flux.fromIterable(synchronizations)
            .concatMap(TransactionSynchronization::beforeCompletion).onErrorContinue((t, o) ->
                    logger.error("TransactionSynchronization.beforeCompletion threw exception", t)).then();
  }

  /**
   * Actually invoke the {@code afterCommit} methods of the
   * given Framework TransactionSynchronization objects.
   *
   * @param synchronizations a List of TransactionSynchronization objects
   * @see TransactionSynchronization#afterCommit()
   */
  public static Mono<Void> invokeAfterCommit(Collection<TransactionSynchronization> synchronizations) {
    return Flux.fromIterable(synchronizations)
            .concatMap(TransactionSynchronization::afterCommit)
            .then();
  }

  /**
   * Actually invoke the {@code afterCompletion} methods of the
   * given Framework TransactionSynchronization objects.
   *
   * @param synchronizations a List of TransactionSynchronization objects
   * @param completionStatus the completion status according to the
   * constants in the TransactionSynchronization interface
   * @see TransactionSynchronization#afterCompletion(int)
   * @see TransactionSynchronization#STATUS_COMMITTED
   * @see TransactionSynchronization#STATUS_ROLLED_BACK
   * @see TransactionSynchronization#STATUS_UNKNOWN
   */
  public static Mono<Void> invokeAfterCompletion(
          Collection<TransactionSynchronization> synchronizations, int completionStatus) {

    return Flux.fromIterable(synchronizations).concatMap(it -> it.afterCompletion(completionStatus))
            .onErrorContinue((t, o) -> logger.error("TransactionSynchronization.afterCompletion threw exception", t)).then();
  }

  /**
   * Inner class to avoid hard-coded dependency on AOP module.
   */
  private static class ScopedProxyUnwrapper {

    public static Object unwrapIfNecessary(Object resource) {
      if (resource instanceof ScopedObject) {
        return ((ScopedObject) resource).getTargetObject();
      }
      else {
        return resource;
      }
    }
  }

}
