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

package cn.taketoday.util.concurrent;

import java.util.concurrent.Callable;
import java.util.function.Function;

import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

import static cn.taketoday.util.concurrent.SettableFutureNotifier.tryFailure;

/**
 * Combinator operations on {@linkplain Future futures}.
 * <p>
 * Used for implementing {@link Future#map(Function)}
 * and {@link Future#flatMap(Function)}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @implNote The operations themselves are implemented
 * as static inner classes instead of lambdas to aid debugging.
 * @since 4.0
 */
final class Futures {

  private static final Logger logger = LoggerFactory.getLogger(Futures.class);

  private static final PassThrough<?> PASS_THROUGH = new PassThrough<>();

  private static final PropagateCancel PROPAGATE_CANCEL = new PropagateCancel();

  /**
   * Creates a new {@link Future} that will complete with the result of the given
   * {@link Future} mapped through the given mapper function.
   * <p>
   * If the given future fails, then the returned future will fail as well, with
   * the same exception. Cancellation of either future will cancel the other. If
   * the mapper function throws, the returned future will fail, but the given
   * future will be unaffected.
   *
   * @param future The future whose result will flow to the returned future, through
   * the mapping function.
   * @param mapper The function that will convert the result of the given future
   * into the result of the returned
   * future.
   * @param <R> The result type of the mapper function, and of the returned future.
   * @return A new future instance that will complete with the mapped result of the given future.
   */
  public static <V, R> Future<R> map(Future<V> future, Function<V, R> mapper) {
    Assert.notNull(future, "future");
    Assert.notNull(mapper, "mapper");
    if (future.isFailed()) {
      @SuppressWarnings("unchecked") // Cast is safe because the result type is not used in failed futures.
      Future<R> failed = (Future<R>) future;
      return failed;
    }
    if (future.isSuccess()) {
      var futureTask = new ListenableFutureTask<>(new CallableMapper<>(future, mapper));
      future.executor().execute(futureTask);
      return futureTask;
    }
    SettableFuture<R> promise = Future.forSettable(future.executor());
    future.addListener(new Mapper<>(promise, mapper));
    promise.addListener(propagateCancel(), future);
    return promise;
  }

  /**
   * Creates a new {@link Future} that will complete with the result of the given {@link Future} flat-mapped through
   * the given mapper function.
   * <p>
   * The "flat" in "flat-map" means the given mapper function produces a result that itself is a future-of-R, yet this
   * method also returns a future-of-R, rather than a future-of-future-of-R. In other words, if the same mapper
   * function was used with the {@link #map(Future, Function)} method, you would get back a {@code Future<Future<R>>}.
   * These nested futures are "flattened" into a {@code Future<R>} by this method.
   * <p>
   * Effectively, this method behaves similar to this serial code, except asynchronously and with proper exception and
   * cancellation handling:
   * <pre>{@code
   * V x = future.sync().getNow();
   * Future<R> y = mapper.apply(x);
   * R result = y.sync().getNow();
   * }</pre>
   * <p>
   * If the given future fails, then the returned future will fail as well, with the same exception. Cancellation of
   * either future will cancel the other. If the mapper function throws, the returned future will fail, but the given
   * future will be unaffected.
   *
   * @param mapper The function that will convert the result of the given future into the result of the returned
   * future.
   * @param <R> The result type of the mapper function, and of the returned future.
   * @return A new future instance that will complete with the mapped result of the given future.
   */
  public static <V, R> Future<R> flatMap(Future<V> future, Function<V, Future<R>> mapper) {
    Assert.notNull(future, "future");
    Assert.notNull(mapper, "mapper");

    SettableFuture<R> promise = Future.forSettable(future.executor());
    future.addListener(new FlatMapper<>(promise, mapper));
    if (!future.isSuccess()) {
      // Propagate cancellation if future is either incomplete or failed.
      // Failed means it could be cancelled, so that needs to be propagated.
      promise.addListener(propagateCancel(), future);
    }
    return promise;
  }

  @SuppressWarnings("unchecked")
  static <V, R> FutureContextListener<Future<R>, SettableFuture<V>> propagateCancel() {
    return (FutureContextListener<Future<R>, SettableFuture<V>>) (FutureContextListener<?, ?>) PROPAGATE_CANCEL;
  }

  @SuppressWarnings("unchecked")
  static <R, V> FutureContextListener<SettableFuture<R>, Future<V>> passThrough() {
    return (FutureContextListener<SettableFuture<R>, Future<V>>) (FutureContextListener<?, ?>) PASS_THROUGH;
  }

  static <A, B> void propagateUncommonCompletion(Future<? extends A> completed, SettableFuture<B> recipient) {
    if (completed.isCancelled()) {
      // Don't check or log if cancellation propagation fails.
      // Propagation goes both ways, which means at least one future will already be cancelled here.
      recipient.cancel();
    }
    else {
      Throwable cause = completed.getCause();
      recipient.tryFailure(cause);
    }
  }

  private Futures() {
  }

  private static final class PropagateCancel implements FutureContextListener<Future<Object>, Future<Object>> {

    @Override
    public void operationComplete(Future<Object> future, Future<Object> context) throws Exception {
      if (future.isCancelled()) {
        context.cancel();
      }
    }
  }

  private static final class PassThrough<R> implements FutureContextListener<SettableFuture<R>, Future<Object>> {

    @Override
    public void operationComplete(Future<Object> completed, SettableFuture<R> recipient) throws Exception {
      if (completed.isSuccess()) {
        try {
          @SuppressWarnings("unchecked")
          R result = (R) completed.getNow();
          recipient.trySuccess(result);
        }
        catch (Throwable e) {
          tryFailure(recipient, e, logger);
        }
      }
      else {
        propagateUncommonCompletion(completed, recipient);
      }
    }
  }

  private static final class CallableMapper<R, T> implements Callable<R> {
    private final Future<T> future;
    private final Function<T, R> mapper;

    CallableMapper(Future<T> future, Function<T, R> mapper) {
      this.future = future;
      this.mapper = mapper;
    }

    @Override
    public R call() throws Exception {
      return mapper.apply(future.getNow());
    }
  }

  private static final class Mapper<R, T> implements FutureListener<Future<T>> {
    private final SettableFuture<R> recipient;
    private final Function<T, R> mapper;

    Mapper(SettableFuture<R> recipient, Function<T, R> mapper) {
      this.recipient = recipient;
      this.mapper = mapper;
    }

    @Override
    public void operationComplete(Future<T> completed) throws Exception {
      if (completed.isSuccess()) {
        try {
          T result = completed.getNow();
          R mapped = mapper.apply(result);
          recipient.trySuccess(mapped);
        }
        catch (Throwable e) {
          tryFailure(recipient, e, logger);
        }
      }
      else {
        propagateUncommonCompletion(completed, recipient);
      }
    }
  }

  private static final class FlatMapper<R, T> implements FutureListener<Future<T>> {

    private final SettableFuture<R> recipient;

    private final Function<T, Future<R>> mapper;

    FlatMapper(SettableFuture<R> recipient, Function<T, Future<R>> mapper) {
      this.recipient = recipient;
      this.mapper = mapper;
    }

    @Override
    public void operationComplete(Future<T> completed) throws Exception {
      if (completed.isSuccess()) {
        try {
          T result = completed.getNow();
          Future<R> future = mapper.apply(result);
          if (future.isSuccess()) {
            recipient.trySuccess(future.getNow());
          }
          else if (future.isFailed()) {
            propagateUncommonCompletion(future, recipient);
          }
          else {
            future.addListener(passThrough(), recipient);
            recipient.addListener(propagateCancel(), future);
          }
        }
        catch (Throwable e) {
          tryFailure(recipient, e, logger);
        }
      }
      else {
        propagateUncommonCompletion(completed, recipient);
      }
    }
  }

  /**
   * Link the {@link Future} and {@link SettableFuture} such that if the {@link Future} completes the {@link SettableFuture}
   * will be notified. Cancellation is propagated both ways such that if the {@link Future} is cancelled
   * the {@link SettableFuture} is cancelled and vice-versa.
   *
   * @param future the {@link Future} which will be used to listen to for notifying the {@link SettableFuture}.
   * @param promise the {@link SettableFuture} which will be notified
   * @param <V> the type of the value.
   */
  static <V> void cascade(final Future<V> future, final SettableFuture<? super V> promise) {
    Assert.notNull(future, "future");
    Assert.notNull(promise, "promise");

    if (!future.isSuccess()) {
      // Propagate cancellation if future is either incomplete or failed.
      // Failed means it could be cancelled, so that needs to be propagated.
      promise.addListener(propagateCancel(), future);
    }
    future.addListener(passThrough(), promise);
  }
}
