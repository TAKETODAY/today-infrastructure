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

package test.mixin;

import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.aop.support.DelegatingIntroductionInterceptor;
import test.aop.Lockable;

/**
 * Mixin to provide stateful locking functionality.
 * Test/demonstration of AOP mixin support rather than a
 * useful interceptor in its own right.
 *
 * @author Rod Johnson
 * @since 10.07.2003
 */
@SuppressWarnings("serial")
public class LockMixin extends DelegatingIntroductionInterceptor implements Lockable {

  /** This field demonstrates additional state in the mixin */
  private boolean locked;

  @Override
  public void lock() {
    this.locked = true;
  }

  @Override
  public void unlock() {
    this.locked = false;
  }

  /**
   * @see Lockable#locked()
   */
  @Override
  public boolean locked() {
    return this.locked;
  }

  /**
   * Note that we need to override around advice.
   * If the method is a setter and we're locked, prevent execution.
   * Otherwise let super.invoke() handle it, and do normal
   * Lockable(this) then target behaviour.
   *
   * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
   */
  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    if (locked() && invocation.getMethod().getName().indexOf("set") == 0) {
      throw new LockedException();
    }
    return super.invoke(invocation);
  }

}
