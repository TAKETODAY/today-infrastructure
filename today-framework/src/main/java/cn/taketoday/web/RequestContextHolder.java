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
package cn.taketoday.web;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Holder class to expose the web request in the form of a thread-bound
 * {@link RequestContext} object.
 * <p>
 * user can replace RequestThreadLocal use {@link #replaceContextHolder(RequestThreadLocal)}
 * to hold RequestContext
 * </p>
 *
 * @author TODAY 2019-03-23 10:29
 * @see #replaceContextHolder(RequestThreadLocal)
 * @since 2.3.7
 */
public abstract class RequestContextHolder {
  private static RequestThreadLocal contextHolder = new DefaultRequestThreadLocal();

  public static void remove() {
    contextHolder.remove();
  }

  public static void set(RequestContext requestContext) {
    contextHolder.set(requestContext);
  }

  /**
   * current context
   */
  @Nullable
  public static RequestContext get() {
    return contextHolder.get();
  }

  public static RequestContext getRequired() {
    RequestContext context = contextHolder.get();
    if (context == null) {
      throw new IllegalStateException("No RequestContext set");
    }
    return context;
  }

  /**
   * replace {@link RequestThreadLocal}
   *
   * @param contextHolder new {@link RequestThreadLocal} object
   * @since 3.0
   */
  public static void replaceContextHolder(RequestThreadLocal contextHolder) {
    Assert.notNull(contextHolder, "contextHolder must not be null");
    RequestContextHolder.contextHolder = contextHolder;
  }

  /**
   * @since 3.0
   */
  public static RequestThreadLocal getRequestThreadLocal() {
    return RequestContextHolder.contextHolder;
  }

}
