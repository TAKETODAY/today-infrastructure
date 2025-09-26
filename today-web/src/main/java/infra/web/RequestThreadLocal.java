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

package infra.web;

import org.jspecify.annotations.Nullable;

import infra.core.NamedThreadLocal;
import infra.lang.TodayStrategies;
import infra.lang.VisibleForTesting;
import infra.util.ClassUtils;
import io.netty.util.concurrent.FastThreadLocal;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 3.0 2021/4/1 19:28
 */
public abstract class RequestThreadLocal {

  public abstract void remove();

  @Nullable
  public abstract RequestContext get();

  public abstract void set(@Nullable RequestContext context);

  /**
   * Static factory method
   */
  public static RequestThreadLocal lookup() {
    RequestThreadLocal ret = TodayStrategies.findFirst(RequestThreadLocal.class, null);
    if (ret == null) {
      if (ClassUtils.isPresent("io.netty.util.concurrent.FastThreadLocal")) {
        return new Netty();
      }
      return new Default();
    }
    return ret;
  }

  @VisibleForTesting
  static final class Default extends RequestThreadLocal {
    private final NamedThreadLocal<RequestContext> threadLocal = new NamedThreadLocal<>("Current Request Context");

    @Override
    public void remove() {
      threadLocal.remove();
    }

    @Nullable
    @Override
    public RequestContext get() {
      return threadLocal.get();
    }

    @Override
    public void set(@Nullable RequestContext context) {
      threadLocal.set(context);
    }
  }

  @VisibleForTesting
  static final class Netty extends RequestThreadLocal {
    private final FastThreadLocal<RequestContext> threadLocal = new FastThreadLocal<>();

    @Override
    public void remove() {
      threadLocal.remove();
    }

    @Override
    public RequestContext get() {
      return threadLocal.get();
    }

    @Override
    public void set(@Nullable RequestContext context) {
      threadLocal.set(context);
    }
  }

}
