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

package infra.transaction.reactive;

import reactor.core.publisher.Mono;

/**
 * {@link TransactionSynchronization} implementation that manages a
 * resource object bound through {@link TransactionSynchronizationManager}.
 *
 * @param <O> the resource holder type
 * @param <K> the resource key type
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class ReactiveResourceSynchronization<O, K> implements TransactionSynchronization {

  private final O resourceObject;

  private final K resourceKey;

  private final TransactionSynchronizationManager synchronizationManager;

  private volatile boolean holderActive = true;

  /**
   * Create a new ReactiveResourceSynchronization for the given holder.
   *
   * @param resourceObject the resource object to manage
   * @param resourceKey the key to bind the resource object for
   * @param synchronizationManager the synchronization manager bound to the current transaction
   * @see TransactionSynchronizationManager#bindResource
   */
  public ReactiveResourceSynchronization(
          O resourceObject, K resourceKey, TransactionSynchronizationManager synchronizationManager) {

    this.resourceObject = resourceObject;
    this.resourceKey = resourceKey;
    this.synchronizationManager = synchronizationManager;
  }

  @Override
  public Mono<Void> suspend() {
    if (this.holderActive) {
      this.synchronizationManager.unbindResource(this.resourceKey);
    }
    return Mono.empty();
  }

  @Override
  public Mono<Void> resume() {
    if (this.holderActive) {
      this.synchronizationManager.bindResource(this.resourceKey, this.resourceObject);
    }
    return Mono.empty();
  }

  @Override
  public Mono<Void> beforeCommit(boolean readOnly) {
    return Mono.empty();
  }

  @Override
  public Mono<Void> beforeCompletion() {
    if (shouldUnbindAtCompletion()) {
      this.synchronizationManager.unbindResource(this.resourceKey);
      this.holderActive = false;
      if (shouldReleaseBeforeCompletion()) {
        return releaseResource(this.resourceObject, this.resourceKey);
      }
    }
    return Mono.empty();
  }

  @Override
  public Mono<Void> afterCommit() {
    if (!shouldReleaseBeforeCompletion()) {
      return processResourceAfterCommit(this.resourceObject);
    }
    return Mono.empty();
  }

  @Override
  public Mono<Void> afterCompletion(int status) {
    return Mono.defer(() -> {
      Mono<Void> sync = Mono.empty();
      if (shouldUnbindAtCompletion()) {
        boolean releaseNecessary = false;
        if (this.holderActive) {
          // The thread-bound resource holder might not be available anymore,
          // since afterCompletion might get called from a different thread.
          this.holderActive = false;
          this.synchronizationManager.unbindResourceIfPossible(this.resourceKey);
          releaseNecessary = true;
        }
        else {
          releaseNecessary = shouldReleaseAfterCompletion(this.resourceObject);
        }
        if (releaseNecessary) {
          sync = releaseResource(this.resourceObject, this.resourceKey);
        }
      }
      else {
        // Probably a pre-bound resource...
        sync = cleanupResource(this.resourceObject, this.resourceKey, (status == STATUS_COMMITTED));
      }
      return sync;
    });
  }

  /**
   * Return whether this holder should be unbound at completion
   * (or should rather be left bound to the thread after the transaction).
   * <p>The default implementation returns {@code true}.
   */
  protected boolean shouldUnbindAtCompletion() {
    return true;
  }

  /**
   * Return whether this holder's resource should be released before
   * transaction completion ({@code true}) or rather after
   * transaction completion ({@code false}).
   * <p>Note that resources will only be released when they are
   * unbound from the thread ({@link #shouldUnbindAtCompletion()}).
   * <p>The default implementation returns {@code true}.
   *
   * @see #releaseResource
   */
  protected boolean shouldReleaseBeforeCompletion() {
    return true;
  }

  /**
   * Return whether this holder's resource should be released after
   * transaction completion ({@code true}).
   * <p>The default implementation returns {@code !shouldReleaseBeforeCompletion()},
   * releasing after completion if no attempt was made before completion.
   *
   * @see #releaseResource
   */
  protected boolean shouldReleaseAfterCompletion(O resourceHolder) {
    return !shouldReleaseBeforeCompletion();
  }

  /**
   * After-commit callback for the given resource holder.
   * Only called when the resource hasn't been released yet
   * ({@link #shouldReleaseBeforeCompletion()}).
   *
   * @param resourceHolder the resource holder to process
   */
  protected Mono<Void> processResourceAfterCommit(O resourceHolder) {
    return Mono.empty();
  }

  /**
   * Release the given resource (after it has been unbound from the thread).
   *
   * @param resourceHolder the resource holder to process
   * @param resourceKey the key that the resource object was bound for
   */
  protected Mono<Void> releaseResource(O resourceHolder, K resourceKey) {
    return Mono.empty();
  }

  /**
   * Perform a cleanup on the given resource (which is left bound to the thread).
   *
   * @param resourceHolder the resource holder to process
   * @param resourceKey the key that the resource object was bound for
   * @param committed whether the transaction has committed ({@code true})
   * or rolled back ({@code false})
   */
  protected Mono<Void> cleanupResource(O resourceHolder, K resourceKey, boolean committed) {
    return Mono.empty();
  }

}
