/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.core;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Supplier;

import infra.lang.Assert;
import infra.util.concurrent.Future;
import infra.util.concurrent.FutureListener;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;

/**
 * Emits the value or error produced by the wrapped Future.
 * <p>
 * Note that if Subscribers cancel their subscriptions, the Future
 * is not cancelled.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 1.0 2025/8/3 11:34
 */
public abstract class FutureMono<T> extends Mono<T> {

  /**
   * Convert a {@link Future} into {@link Mono}. {@link Mono#subscribe(Subscriber)}
   * will bridge to {@link Future#onCompleted(FutureListener)}.
   *
   * @param future the future to convert from
   * @param <F> the future type
   * @return A {@link Mono} forwarding {@link Future} success or failure
   */
  public static <T, F extends Future<T>> Mono<T> of(F future) {
    Assert.notNull(future, "future is required");
    return new ImmediateFutureMono<>(future);
  }

  /**
   * Convert a supplied {@link Future} for each subscriber into {@link Mono}.
   * {@link Mono#subscribe(Subscriber)}
   * will bridge to {@link Future#onCompleted(FutureListener)}.
   *
   * @param deferredFuture the future to evaluate and convert from
   * @param <F> the future type
   * @return A {@link Mono} forwarding {@link Future} success or failure
   */
  public static <T, F extends Future<T>> Mono<T> deferFuture(Supplier<F> deferredFuture) {
    Assert.notNull(deferredFuture, "deferredFuture is required");
    return new DeferredFutureMono<>(deferredFuture);
  }

  @SuppressWarnings({ "FutureReturnValueIgnored", "NullAway" })
  private static <T, F extends Future<T>> void doSubscribe(CoreSubscriber<? super T> s, F future) {
    if (future.isDone()) {
      if (future.isSuccess()) {
        T value = future.getNow();
        if (value != null) {
          s.onSubscribe(Operators.scalarSubscription(s, value));
        }
        else {
          Operators.complete(s);
        }
      }
      else {
        Operators.error(s, future.getCause());
      }
      return;
    }

    FutureSubscription<T, F> fs = new FutureSubscription<>(future, s);
    // propagate subscription before adding listener to avoid any race between finishing future and onSubscribe
    // is called
    s.onSubscribe(fs);
  }

  private static final class ImmediateFutureMono<T, F extends Future<T>> extends FutureMono<T> {

    final F future;

    ImmediateFutureMono(F future) {
      this.future = future;
    }

    @Override
    public void subscribe(final CoreSubscriber<? super T> s) {
      doSubscribe(s, future);
    }
  }

  private static final class DeferredFutureMono<T, F extends Future<T>> extends FutureMono<T> {

    final Supplier<F> deferredFuture;

    DeferredFutureMono(Supplier<F> deferredFuture) {
      this.deferredFuture = deferredFuture;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> s) {
      F f;
      try {
        f = deferredFuture.get();
      }
      catch (Throwable t) {
        Operators.error(s, t);
        return;
      }

      if (f == null) {
        Operators.error(s, Operators.onOperatorError(new NullPointerException("Deferred supplied null"), s.currentContext()));
        return;
      }

      doSubscribe(s, f);
    }
  }

  private static final class FutureSubscription<T, F extends Future<T>>
          implements FutureListener<F>, Subscription {

    final CoreSubscriber<? super T> s;

    final F future;

    volatile boolean cancelled;

    volatile int requestedOnce;

    @SuppressWarnings("rawtypes")
    static final AtomicIntegerFieldUpdater<FutureSubscription> REQUESTED_ONCE =
            AtomicIntegerFieldUpdater.newUpdater(FutureSubscription.class, "requestedOnce");

    FutureSubscription(F future, CoreSubscriber<? super T> s) {
      this.s = s;
      this.future = future;
    }

    @Override
    public void request(long n) {
      if (this.cancelled) {
        return;
      }

      if (this.requestedOnce == 1 || !REQUESTED_ONCE.compareAndSet(this, 0, 1)) {
        return;
      }

      future.onCompleted(this);
    }

    @Override
    public void cancel() {
      this.cancelled = true;
      try {
        future.cancel();
      }
      catch (Throwable t) {
        Operators.onErrorDropped(t, s.currentContext());
      }
    }

    @Override
    public void operationComplete(F future) {
      if (future.isSuccess()) {
        T now = future.getNow();
        if (now != null) {
          s.onNext(now);
        }
        s.onComplete();
      }
      else if (future.isFailure()) {
        s.onError(future.getCause());
      }
    }
  }

}
