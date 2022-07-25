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

import cn.taketoday.lang.Assert;

/**
 * Extension of {@link CompletableFuture} which allows for cancelling
 * a delegate along with the {@link CompletableFuture} itself.
 *
 * @param <T> the result type returned by this Future's {@code get} method
 * @author Juergen Hoeller
 * @since 4.0
 */
class DelegatingCompletableFuture<T> extends CompletableFuture<T> {

  private final Future<T> delegate;

  public DelegatingCompletableFuture(Future<T> delegate) {
    Assert.notNull(delegate, "Delegate must not be null");
    this.delegate = delegate;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    boolean result = this.delegate.cancel(mayInterruptIfRunning);
    super.cancel(mayInterruptIfRunning);
    return result;
  }

}
