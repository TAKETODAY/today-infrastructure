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

package cn.taketoday.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Extend {@link Future} with the capability to accept completion callbacks.
 * If the future has completed when the callback is added, the callback is
 * triggered immediately.
 *
 * <p>Inspired by {@code com.google.common.util.concurrent.ListenableFuture}.
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 4.0
 */
public interface ListenableFuture<T> extends Future<T> {

  /**
   * Register the given {@code ListenableFutureCallback}.
   *
   * @param callback the callback to register
   */
  void addCallback(ListenableFutureCallback<? super T> callback);

  /**
   * Java 8 lambda-friendly alternative with success and failure callbacks.
   *
   * @param successCallback the success callback
   * @param failureCallback the failure callback
   */
  void addCallback(SuccessCallback<? super T> successCallback, FailureCallback failureCallback);

  /**
   * Expose this {@link ListenableFuture} as a JDK {@link CompletableFuture}.
   */
  default CompletableFuture<T> completable() {
    CompletableFuture<T> completable = new DelegatingCompletableFuture<>(this);
    addCallback(completable::complete, completable::completeExceptionally);
    return completable;
  }

}
