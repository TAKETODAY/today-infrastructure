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

package cn.taketoday.aop.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;

import cn.taketoday.aop.AfterReturningAdvice;
import cn.taketoday.aop.MethodBeforeAdvice;
import cn.taketoday.aop.ThrowsAdvice;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.logger.Logger;
import cn.taketoday.logger.LoggerFactory;
import lombok.ToString;

/**
 * @author TODAY 2021/2/20 21:26
 */
public class ProxyFactoryBeanTests {
  static final Logger log = LoggerFactory.getLogger(ProxyFactoryBeanTests.class);

  //需要实现接口，确定哪个通知，及告诉Aop应该执行哪个方法
  @Singleton
  static class MyAspect implements MethodInterceptor {
    public Object invoke(MethodInvocation mi) throws Throwable {
      log.debug("方法执行之前");
      // 0
      Object obj = mi.proceed();

      // test == 0

      final TargetBean obj1 = (TargetBean) mi.getThis();
      obj1.test = 10;
      obj = mi.proceed(); // toString

      log.debug("方法执行之后");
      return obj;
    }
  }

  @Singleton
  static class MyAfterReturning implements AfterReturningAdvice {

    @Override
    public void afterReturning(Object returnValue, MethodInvocation invocation) throws Throwable {
      // test == 0
      log.debug("方法执行之后 返回值： " + returnValue);
    }
  }

  @Singleton
  static class MyBefore implements MethodBeforeAdvice {

    @Override
    public void before(MethodInvocation invocation) throws Throwable {
      log.info("之前");
    }
  }

  @Singleton
  static class MyThrows implements ThrowsAdvice {

    @Override
    public Object afterThrowing(Throwable ex, MethodInvocation invocation) {
      log.info(ex.toString());

      return "异常数据";
    }
  }

  @ToString
  @Singleton
  static class TargetBean {

    int test;

    String throwsTest() {
      int i = 1 / 0;
      return "ok";
    }

  }

  @Test
  public void test() {
//    DebuggingClassWriter.setDebugLocation("D:\\dev\\temp\\debug");

    try (StandardApplicationContext context = new StandardApplicationContext("", "cn.taketoday.aop.support")) {
      final ProxyFactoryBean proxyFactoryBean = new ProxyFactoryBean();
      proxyFactoryBean.setProxyTargetClass(true);
      proxyFactoryBean.setBeanFactory(context);
      proxyFactoryBean.setExposeProxy(true);

      proxyFactoryBean.setInterceptorNames("myAspect", "myAfterReturning", "myBefore", "myThrows");
      proxyFactoryBean.setTargetName("targetBean");

      final Object bean = proxyFactoryBean.getBean();
      log.debug(bean.toString());

      final String ret = ((TargetBean) bean).throwsTest();

      assert ret.equals("异常数据");
    }
  }
}
