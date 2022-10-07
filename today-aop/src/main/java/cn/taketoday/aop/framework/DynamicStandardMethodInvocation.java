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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.lang.Experimental;

/**
 * @author TODAY 2021/3/8 13:03
 * @since 3.0
 */
@Experimental
public final class DynamicStandardMethodInvocation
        extends StandardMethodInvocation implements MethodInvocation {

  private final int adviceLength;
  private final MethodInterceptor[] advices;

  public DynamicStandardMethodInvocation(
          Object proxy, Object bean, TargetInvocation target, Object[] arguments, MethodInterceptor[] advices) {
    super(proxy, bean, target, arguments);
    this.advices = advices;
    this.adviceLength = advices.length;
  }

  @Override
  protected boolean hasInterceptor() {
    return currentAdviceIndex < adviceLength;
  }

  @Override
  protected Object executeInterceptor() throws Throwable {
    return advices[currentAdviceIndex++].invoke(this);
  }

}
