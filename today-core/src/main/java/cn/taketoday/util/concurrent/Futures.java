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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.function.ThrowingBiFunction;
import cn.taketoday.util.function.ThrowingFunction;

/**
 * Combinator operations on {@linkplain Future futures}.
 * <p>
 * Used for implementing {@link Future#map(ThrowingFunction)}
 * and {@link Future#flatMap(ThrowingFunction)}
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

  public static final FutureContextListener<Future<Object>, CompletableFuture<Object>> completableAdapter = (completed, context) -> {
    Throwable cause = completed.getCause();
    if (cause != null) {
      context.completeExceptionally(cause);
    }
    else {
      context.complete(completed.getNow());
    }
  };

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
  @SuppressWarnings("unchecked")
  public static <V, R> Future<R> map(Future<V> future, ThrowingFunction<V, R> mapper) {
    if (future.isFailed()) {
      // Cast is safe because the result type is not used in failed futures.
      return (Future<R>) future;
    }
    Executor executor = future.executor();
    if (future.isSuccess()) {
      var task = new ListenableFutureTask<>(executor, new CallableMapper<>(future, mapper));
      executor.execute(task);
      return task;
    }
    SettableFuture<R> settable = Future.forSettable(executor);
    future.onCompleted(new Mapper<>(settable, mapper));
    settable.onCompleted(propagateCancel(), future);
    return settable;
  }

  /**
   * Creates a new {@link Future} that will complete with the result of the given
   * {@link Future} flat-mapped through the given mapper function.
   * <p>
   * The "flat" in "flat-map" means the given mapper function produces a result
   * that itself is a future-of-R, yet this method also returns a future-of-R,
   * rather than a future-of-future-of-R. In other words, if the same mapper
   * function was used with the {@link #map(Future, ThrowingFunction)} method, you would
   * get back a {@code Future<Future<R>>}. These nested futures are "flattened"
   * into a {@code Future<R>} by this method.
   * <p>
   * Effectively, this method behaves similar to this serial code, except
   * asynchronously and with proper exception and cancellation handling:
   * <pre>{@code
   * V x = future.sync().getNow();
   * Future<R> y = mapper.apply(x);
   * R result = y.sync().getNow();
   * }</pre>
   * <p>
   * If the given future fails, then the returned future will fail as well, with
   * the same exception. Cancellation of either future will cancel the other. If
   * the mapper function throws, the returned future will fail, but the given
   * future will be unaffected.
   *
   * @param mapper The function that will convert the result of the given future
   * into the result of the returned future.
   * @param <R> The result type of the mapper function, and of the returned future.
   * @return A new future instance that will complete with the mapped result of the given future.
   */
  public static <V, R> SettableFuture<R> flatMap(Future<V> future, ThrowingFunction<V, Future<R>> mapper) {
    SettableFuture<R> settable = Future.forSettable(future.executor());
    future.onCompleted(new FlatMapper<>(settable, mapper));
    if (!future.isSuccess()) {
      // Propagate cancellation if future is either incomplete or failed.
      // Failed means it could be cancelled, so that needs to be propagated.
      settable.onCompleted(propagateCancel(), future);
    }
    return settable;
  }

  /**
   * Handles a failure of this Future by returning another result.
   * <p>
   * Example:
   * <pre>{@code
   * // = "oh!"
   * Future.run(() -> new Error("oh!")).recover(Throwable::getMessage);
   * }</pre>
   *
   * @param recoverFunc A function which takes the exception of a failure and returns a new value.
   * @return A new Future.
   */
  public static <V> Future<V> errorHandling(Future<V> future, Function<Throwable, V> recoverFunc) {
    Executor executor = future.executor();
    Throwable cause = future.getCause();
    if (cause != null) {
      // already failed
      var task = new ListenableFutureTask<>(executor, () -> recoverFunc.apply(cause));
      executor.execute(task);
      return task;
    }

    SettableFuture<V> settable = Future.forSettable(executor);
    future.onCompleted(completed -> {
      if (completed.isSuccess()) {
        settable.setSuccess(completed.getNow());
      }
      else if (completed.isCancelled()) {
        settable.cancel();
      }
      else {
        V result = recoverFunc.apply(completed.getCause());
        settable.setSuccess(result);
      }
    });

    if (!future.isSuccess()) {
      // Propagate cancellation if future is either incomplete or failed.
      // Failed means it could be cancelled, so that needs to be propagated.
      settable.onCompleted(propagateCancel(), future);
    }
    return settable;
  }

  /**
   * Returns this and that Future result combined using a given combinator function.
   * <p>
   * If this Future failed the result contains this failure. Otherwise, the
   * result contains that failure or a combination of both successful Future results.
   *
   * @param that Another Future
   * @param combinator The combinator function
   * @param <U> Result type of {@code that}
   * @param <R> Result type of {@code f}
   * @return A new Future that returns both Future results.
   */
  public static <U, R, V> Future<R> zipWith(Future<V> future, Future<U> that, ThrowingBiFunction<V, U, R> combinator) {
    SettableFuture<R> recipient = Future.forSettable(future.executor());
    future.onCompleted(completed -> {
      Throwable cause = completed.getCause();
      if (cause != null) {
        // failed
        recipient.tryFailure(cause);
      }
      else {
        // succeed
        that.onCompleted(t -> {
          Throwable c = t.getCause();
          if (c != null) {
            recipient.setFailure(c);
          }
          else {
            try {
              V first = completed.getNow();
              U second = t.getNow();
              recipient.trySuccess(combinator.applyWithException(first, second));
            }
            catch (Throwable e) {
              tryFailure(recipient, e, logger);
            }
          }
        });
      }
    });

    if (!future.isSuccess()) {
      // Propagate cancellation if future is either incomplete or failed.
      // Failed means it could be cancelled, so that needs to be propagated.
      recipient.onCompleted(propagateCancel(), future);
    }
    return recipient;
  }

  @SuppressWarnings("unchecked")
  static <V, R> FutureContextListener<SettableFuture<V>, Future<R>> propagateCancel() {
    return (FutureContextListener<SettableFuture<V>, Future<R>>) (FutureContextListener<?, ?>) PROPAGATE_CANCEL;
  }

  @SuppressWarnings("unchecked")
  static <R, V> FutureContextListener<Future<V>, SettableFuture<R>> passThrough() {
    return (FutureContextListener<Future<V>, SettableFuture<R>>) (FutureContextListener<?, ?>) PASS_THROUGH;
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

  private static final class PropagateCancel implements FutureContextListener<Future<Object>, Future<Object>> {

    @Override
    public void operationComplete(Future<Object> completed, Future<Object> context) {
      if (completed.isCancelled()) {
        context.cancel();
      }
    }
  }

  private static final class PassThrough<R> implements FutureContextListener<Future<R>, SettableFuture<R>> {

    @Override
    public void operationComplete(Future<R> completed, SettableFuture<R> recipient) {
      if (completed.isSuccess()) {
        try {
          R result = completed.getNow();
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

    private final ThrowingFunction<T, R> mapper;

    CallableMapper(Future<T> future, ThrowingFunction<T, R> mapper) {
      this.future = future;
      this.mapper = mapper;
    }

    @Override
    public R call() {
      try {
        return mapper.applyWithException(future.getNow());
      }
      catch (Throwable e) {
        throw ExceptionUtils.sneakyThrow(e);
      }
    }
  }

  private static final class Mapper<R, T> implements FutureListener<Future<T>> {

    private final SettableFuture<R> recipient;

    private final ThrowingFunction<T, R> mapper;

    Mapper(SettableFuture<R> recipient, ThrowingFunction<T, R> mapper) {
      this.recipient = recipient;
      this.mapper = mapper;
    }

    @Override
    public void operationComplete(Future<T> completed) {
      if (completed.isSuccess()) {
        try {
          R mapped = mapper.applyWithException(completed.getNow());
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

    private final ThrowingFunction<T, Future<R>> mapper;

    FlatMapper(SettableFuture<R> recipient, ThrowingFunction<T, Future<R>> mapper) {
      this.recipient = recipient;
      this.mapper = mapper;
    }

    @Override
    public void operationComplete(Future<T> completed) {
      if (completed.isSuccess()) {
        try {
          Future<R> mapped = mapper.applyWithException(completed.getNow());
          if (mapped.isSuccess()) {
            recipient.trySuccess(mapped.getNow());
          }
          else if (mapped.isFailed()) {
            propagateUncommonCompletion(mapped, recipient);
          }
          else {
            mapped.onCompleted(passThrough(), recipient);
            recipient.onCompleted(propagateCancel(), mapped);
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
   * @param settable the {@link SettableFuture} which will be notified
   * @param <V> the type of the value.
   */
  static <V> void cascade(final Future<V> future, final SettableFuture<V> settable) {
    Assert.notNull(settable, "SettableFuture is required");

    if (!future.isSuccess()) {
      // Propagate cancellation if future is either incomplete or failed.
      // Failed means it could be cancelled, so that needs to be propagated.
      settable.onCompleted(propagateCancel(), future);
    }
    future.onCompleted(passThrough(), settable);
  }

  /**
   * Try to mark the {@link SettableFuture} as failure and log if {@code logger} is not {@code null} in case this fails.
   */
  static void tryFailure(SettableFuture<?> p, Throwable cause, @Nullable Logger logger) {
    if (!p.tryFailure(cause) && logger != null) {
      Throwable err = p.getCause();
      if (err == null) {
        logger.warn("Failed to mark a SettableFuture as failure because it has succeeded already: {}", p, cause);
      }
      else if (logger.isWarnEnabled()) {
        logger.warn("Failed to mark a SettableFuture as failure because it has failed already: {}, unnotified cause: {}",
                p, ExceptionUtils.stackTraceToString(err), cause);
      }
    }
  }
}
