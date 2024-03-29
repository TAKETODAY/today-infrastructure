/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.netty;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestThreadLocal;
import io.netty.util.concurrent.FastThreadLocal;

/**
 * Netty fast ThreadLocal
 *
 * @author TODAY 2021/4/2 17:17
 * @see FastThreadLocal
 */
final class FastRequestThreadLocal extends RequestThreadLocal {
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
