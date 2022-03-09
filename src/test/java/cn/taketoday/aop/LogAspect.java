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
package cn.taketoday.aop;

import org.aopalliance.intercept.Joinpoint;

import cn.taketoday.aop.aspectj.annotation.Annotated;
import cn.taketoday.aop.aspectj.annotation.Arguments;
import cn.taketoday.aop.aspectj.annotation.JoinPoint;
import cn.taketoday.aop.aspectj.annotation.Returning;
import cn.taketoday.aop.aspectj.annotation.Throwing;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.Order;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 * 2018-11-06 17:52
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogAspect {

  @Autowired
  public LogAspect(UserService service) {
    log.debug("{}", service);
  }

  //  public void afterReturning(@Returning Object returnValue) {
  public void afterReturning() {
//        log.debug("LogAspect @AfterReturning returnValue: [{}]", returnValue);
    log.debug("LogAspect @AfterReturning");
  }

  public void afterThrowing(@Throwing Throwable throwable) {
    log.error("LogAspect @AfterThrowing With Msg: [{}]", throwable.getMessage(), throwable);
  }

  public void before(@Annotated Logger logger) {
//	public void before(@Annotated Logger logger, @Argument User user) {
//        log.debug("LogAspect @Before method in class with logger: [{}] , Argument:[{}]", logger.value(), user);
    log.debug("LogAspect @Before method in class with logger: [{}]", logger.value());
  }

  public Object after(@Returning User returnValue, @Arguments Object[] arguments) {
    log.debug("LogAspect @After method in class");
    return returnValue.setIntroduce("女");
  }

  public Object around(@JoinPoint Joinpoint joinpoint) throws Throwable {
    log.debug("LogAspect @Around Before method");
//        int i = 1 / 0;
    Object proceed = joinpoint.proceed();
    log.debug("LogAspect @Around After method");
    return proceed;
  }

}
