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

import org.aopalliance.intercept.Joinpoint;

import cn.taketoday.aop.support.annotation.After;
import cn.taketoday.aop.support.annotation.AfterReturning;
import cn.taketoday.aop.support.annotation.AfterThrowing;
import cn.taketoday.aop.support.annotation.Annotated;
import cn.taketoday.aop.support.annotation.Arguments;
import cn.taketoday.aop.support.annotation.Around;
import cn.taketoday.aop.support.annotation.Aspect;
import cn.taketoday.aop.support.annotation.Before;
import cn.taketoday.aop.support.annotation.JoinPoint;
import cn.taketoday.aop.support.annotation.Returning;
import cn.taketoday.aop.support.annotation.Throwing;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2018-11-06 17:52
 */
@Slf4j
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogAspect {

  @Autowired
  public LogAspect(UserService service) {
    log.debug("{}", service);
  }

  @AfterReturning(Logger.class)
//  public void afterReturning(@Returning Object returnValue) {
  public void afterReturning() {
//        log.debug("LogAspect @AfterReturning returnValue: [{}]", returnValue);
    log.debug("LogAspect @AfterReturning");
  }

  @AfterThrowing(Logger.class)
  public void afterThrowing(@Throwing Throwable throwable) {
    log.error("LogAspect @AfterThrowing With Msg: [{}]", throwable.getMessage(), throwable);
  }

  @Before(Logger.class)
  public void before(@Annotated Logger logger) {
//	public void before(@Annotated Logger logger, @Argument User user) {
//        log.debug("LogAspect @Before method in class with logger: [{}] , Argument:[{}]", logger.value(), user);
    log.debug("LogAspect @Before method in class with logger: [{}]", logger.value());
  }

  @After(Logger.class)
  public Object after(@Returning User returnValue, @Arguments Object[] arguments) {
    log.debug("LogAspect @After method in class");
    return returnValue.setIntroduce("女");
  }

  @Around(Logger.class)
  public Object around(@JoinPoint Joinpoint joinpoint) throws Throwable {
    log.debug("LogAspect @Around Before method");
//        int i = 1 / 0;
    Object proceed = joinpoint.proceed();
    log.debug("LogAspect @Around After method");
    return proceed;
  }

}
