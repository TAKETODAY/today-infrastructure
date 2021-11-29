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

package cn.taketoday.aop.support;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author TODAY 2021/3/6 15:55
 * @since 3.0
 */
public final class SuppliedMethodInterceptor implements MethodInterceptor {
  private final String name;
  private final BeanFactory beanFactory;
  private final boolean singleton;

  private MethodInterceptor interceptor;

  public SuppliedMethodInterceptor(BeanFactory beanFactory, String name, boolean singleton) {
    this.name = name;
    this.singleton = singleton;
    this.beanFactory = beanFactory;
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    MethodInterceptor interceptor = getInterceptor();
    Assert.state(interceptor != null, "No MethodInterceptor");
    return interceptor.invoke(invocation);
  }

  @Nullable
  private MethodInterceptor getInterceptor() {
    if (singleton) {
      if (interceptor == null) {
        interceptor = beanFactory.getBean(name, MethodInterceptor.class);
      }
      return interceptor;
    }
    return beanFactory.getBean(name, MethodInterceptor.class);
  }
}
