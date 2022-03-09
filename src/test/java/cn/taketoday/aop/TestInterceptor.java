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
package cn.taketoday.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import cn.taketoday.aop.support.annotation.Advice;
import cn.taketoday.aop.support.annotation.After;
import cn.taketoday.aop.support.annotation.Aspect;
import cn.taketoday.aop.support.annotation.Returning;
import cn.taketoday.core.annotation.Order;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Today <br>
 *
 *         2018-12-25 21:41
 */
@Slf4j
@Aspect
@Order(10)
@Advice(Logger.class)
//@Advice(pointcut = "test.demo.service.impl.*", method = "login")
public class TestInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(MethodInvocation invocation) throws Throwable {

    log.debug("MethodInterceptor before");

    Object proceed = invocation.proceed();

    log.debug("MethodInterceptor after");

    return proceed;
  }

  @After(Logger.class)
  public Object after(@Returning Object value) {

    log.debug("value: {}", value);

    return value;
  }

}
