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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanSupplier;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.lang.Assert;

/**
 * @author TODAY 2021/3/6 15:55
 * @since 3.0
 */
public final class SuppliedMethodInterceptor implements MethodInterceptor {
  private final BeanSupplier<MethodInterceptor> beanSupplier;

  public SuppliedMethodInterceptor(BeanFactory beanFactory, BeanDefinition interceptorDef) {
    this.beanSupplier = BeanSupplier.from(
            beanFactory, MethodInterceptor.class, interceptorDef.getBeanName(), interceptorDef.isSingleton());
  }

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {
    MethodInterceptor interceptor = beanSupplier.get();
    Assert.state(interceptor != null, "No MethodInterceptor");
    return interceptor.invoke(invocation);
  }

}
