/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.aop.framework;

import cn.taketoday.core.NamedThreadLocal;

/**
 * Class containing static methods used to obtain information about the current AOP invocation.
 *
 * <p>The {@code currentProxy()} method is usable if the AOP framework is configured to
 * expose the current proxy (not the default). It returns the AOP proxy in use. Target objects
 * or advice can use this to make advised calls, in the same way as {@code getEJBObject()}
 * can be used in EJBs. They can also use it to find advice configuration.
 *
 * <p>AOP framework does not expose proxies by default, as there is a performance cost
 * in doing so.
 *
 * <p>The functionality in this class might be used by a target object that needed access
 * to resources on the invocation. However, this approach should not be used when there is
 * a reasonable alternative, as it makes application code dependent on usage under AOP and
 * the  AOP framework in particular.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 19:39
 * @since 3.0
 */
public final class AopContext {

  /**
   * ThreadLocal holder for AOP proxy associated with this thread.
   * Will contain {@code null} unless the "exposeProxy" property on
   * the controlling proxy configuration has been set to "true".
   *
   * @see ProxyConfig#setExposeProxy
   */
  private static final ThreadLocal<Object> currentProxy = new NamedThreadLocal<>("Current AOP proxy");

  /**
   * Try to return the current AOP proxy. This method is usable only if the
   * calling method has been invoked via AOP, and the AOP framework has been set
   * to expose proxies. Otherwise, this method will throw an IllegalStateException.
   *
   * @return the current AOP proxy (never returns {@code null})
   * @throws IllegalStateException if the proxy cannot be found, because the
   * method was invoked outside an AOP invocation context, or because the
   * AOP framework has not been configured to expose the proxy
   */
  public static Object currentProxy() {
    Object proxy = currentProxy.get();
    if (proxy == null) {
      throw new IllegalStateException(
              "Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it available, and " +
                      "ensure that AopContext.currentProxy() is invoked in the same thread as the AOP invocation context.");
    }
    return proxy;
  }

  /**
   * Make the given proxy available via the {@code currentProxy()} method.
   * <p>Note that the caller should be careful to keep the old value as appropriate.
   *
   * @param proxy the proxy to expose (or {@code null} to reset it)
   * @return the old proxy, which may be {@code null} if none was bound
   * @see #currentProxy()
   */
  static Object setCurrentProxy(Object proxy) {
    Object old = currentProxy.get();
    if (proxy != null) {
      currentProxy.set(proxy);
    }
    else {
      currentProxy.remove();
    }
    return old;
  }

}
