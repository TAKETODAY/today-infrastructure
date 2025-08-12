/*
 * Copyright 2017 - 2025 the original author or authors.
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

  static final class ImmediateFutureMono<T, F extends Future<T>> extends FutureMono<T> {

    final F future;

    ImmediateFutureMono(F future) {
      this.future = future;
    }

    @Override
    public void subscribe(final CoreSubscriber<? super T> s) {
      doSubscribe(s, future);
    }
  }

  static final class DeferredFutureMono<T, F extends Future<T>> extends FutureMono<T> {

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

  @SuppressWarnings("FutureReturnValueIgnored")
  static <T, F extends Future<T>> void doSubscribe(CoreSubscriber<? super T> s, F future) {
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

  static final class FutureSubscription<T, F extends Future<T>>
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
