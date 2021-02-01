/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.aop.proxy;

import cn.taketoday.aop.support.AopUtils;

/**
 * Base class for proxy factories.
 * Provides convenient access to a configurable AopProxyFactory.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/2/1 22:55
 * @see #createAopProxy()
 * @since 3.0
 */
public class ProxyCreatorSupport extends AdvisedSupport {

  /** Set to true when the first AOP proxy has been created. */
  private boolean active = false;

  /**
   * Subclasses should call this to get a new AOP proxy. They should <b>not</b>
   * create an AOP proxy with {@code this} as an argument.
   */
  protected final synchronized AopProxy createAopProxy() {
    if (!this.active) {
      activate();
    }
    return AopUtils.createAopProxy(this);
  }

  /**
   * Activate this proxy configuration.
   */
  private void activate() {
    this.active = true;
  }

  /**
   * Subclasses can call this to check whether any AOP proxies have been created yet.
   */
  protected final synchronized boolean isActive() {
    return this.active;
  }

}
