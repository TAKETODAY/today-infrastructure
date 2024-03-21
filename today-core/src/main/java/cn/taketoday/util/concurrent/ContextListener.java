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

import java.util.Objects;

import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/21 21:27
 */
public final class ContextListener<F extends Future<?>, C> implements FutureListener<F> {

  private final FutureContextListener<C, F> listener;

  @Nullable
  private final C context;

  public ContextListener(FutureContextListener<C, F> listener, @Nullable C context) {
    this.listener = listener;
    this.context = context;
  }

  @Override
  public void operationComplete(F future) throws Throwable {
    listener.operationComplete(future, context);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ContextListener<?, ?> that))
      return false;
    return Objects.equals(listener, that.listener)
            && Objects.equals(context, that.context);
  }

  @Override
  public int hashCode() {
    return Objects.hash(listener, context);
  }

}
