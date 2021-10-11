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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.aop.support.annotation;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

import cn.taketoday.aop.AfterAdvice;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanFactory;

/**
 * @author TODAY 2018-10-13 11:03
 * @see After
 */
class AfterMethodInterceptor
        extends AbstractAnnotationMethodInterceptor implements AfterAdvice {

  public AfterMethodInterceptor(Method method, BeanFactory beanFactory, BeanDefinition aspectDef) {
    super(method, beanFactory, aspectDef);
  }

  @Override
  public Object invoke(MethodInvocation inv) throws Throwable {
    return invokeAdviceMethod(inv, inv.proceed(), null);
  }

  @Override
  public int getOrder() {
    return After.DEFAULT_ORDER;
  }

}
