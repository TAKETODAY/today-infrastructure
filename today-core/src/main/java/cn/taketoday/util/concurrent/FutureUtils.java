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
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import cn.taketoday.lang.Assert;

/**
 * Convenience utilities for working with {@link java.util.concurrent.Future}
 * and implementations.
 *
 * @author Arjen Poutsma
 * @since 4.0
 */
public abstract class FutureUtils {

  /**
   * Return a new {@code CompletableFuture} that is asynchronously completed
   * by a task running in the {@link ForkJoinPool#commonPool()} with
   * the value obtained by calling the given {@code Callable}.
   *
   * @param callable a function that returns the value to be used, or throws
   * an exception
   * @return the new CompletableFuture
   * @see CompletableFuture#supplyAsync(Supplier)
   */
  public static <T> CompletableFuture<T> callAsync(Callable<T> callable) {
    Assert.notNull(callable, "Callable is required");

    CompletableFuture<T> result = new CompletableFuture<>();
    return result.completeAsync(toSupplier(callable, result));
  }

  /**
   * Return a new {@code CompletableFuture} that is asynchronously completed
   * by a task running in the given executor with the value obtained
   * by calling the given {@code Callable}.
   *
   * @param callable a function that returns the value to be used, or throws
   * an exception
   * @param executor the executor to use for asynchronous execution
   * @return the new CompletableFuture
   * @see CompletableFuture#supplyAsync(Supplier, Executor)
   */
  public static <T> CompletableFuture<T> callAsync(Callable<T> callable, Executor executor) {
    Assert.notNull(callable, "Callable is required");
    Assert.notNull(executor, "Executor is required");

    CompletableFuture<T> result = new CompletableFuture<>();
    return result.completeAsync(toSupplier(callable, result), executor);
  }

  private static <T> Supplier<T> toSupplier(Callable<T> callable, CompletableFuture<T> result) {
    return () -> {
      try {
        return callable.call();
      }
      catch (Exception ex) {
        // wrap the exception just like CompletableFuture::supplyAsync does
        result.completeExceptionally((ex instanceof CompletionException) ? ex : new CompletionException(ex));
        return null;
      }
    };
  }

}
