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

package cn.taketoday.web;

import cn.taketoday.core.NamedThreadLocal;

/**
 * @author TODAY 2021/4/2 16:53
 * @since 3.0
 */
final class DefaultRequestThreadLocal extends RequestThreadLocal {
  private final NamedThreadLocal<RequestContext> threadLocal = new NamedThreadLocal<>("Current Request Context");

  @Override
  public void remove() {
    threadLocal.remove();
  }

  @Override
  public RequestContext get() {
    return threadLocal.get();
  }

  @Override
  public void set(final RequestContext context) {
    threadLocal.set(context);
  }
}
