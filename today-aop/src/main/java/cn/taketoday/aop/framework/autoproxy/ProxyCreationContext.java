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

package cn.taketoday.aop.framework.autoproxy;

import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.lang.Nullable;

/**
 * Holder for the current proxy creation context, as exposed by auto-proxy creators
 * such as {@link AbstractAdvisorAutoProxyCreator}.
 *
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/9 11:44
 */
public class ProxyCreationContext {

  /** ThreadLocal holding the current proxied bean name during Advisor matching. */
  private static final ThreadLocal<String> currentProxiedBeanName =
          new NamedThreadLocal<>("Name of currently proxied bean");

  private ProxyCreationContext() { }

  /**
   * Return the name of the currently proxied bean instance.
   *
   * @return the name of the bean, or {@code null} if none available
   */
  @Nullable
  public static String getCurrentProxiedBeanName() {
    return currentProxiedBeanName.get();
  }

  /**
   * Set the name of the currently proxied bean instance.
   *
   * @param beanName the name of the bean, or {@code null} to reset it
   */
  static void setCurrentProxiedBeanName(@Nullable String beanName) {
    if (beanName != null) {
      currentProxiedBeanName.set(beanName);
    }
    else {
      currentProxiedBeanName.remove();
    }
  }

}
