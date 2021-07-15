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
package cn.taketoday.context.factory.cycle;

import org.aopalliance.intercept.Joinpoint;
import org.junit.Test;

import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import cn.taketoday.aop.EnableAspectAutoProxy;
import cn.taketoday.aop.Logger;
import cn.taketoday.aop.support.annotation.Around;
import cn.taketoday.aop.support.annotation.Aspect;
import cn.taketoday.aop.support.annotation.JoinPoint;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.Order;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.loader.CandidateComponentScanner;

import static org.junit.Assert.assertEquals;

/**
 * @author TODAY <br>
 * 2019-12-12 09:50
 */
public class CycleDependencyTest {

  @Test
  public void testCycleDependency() {

    CandidateComponentScanner.getSharedInstance().clear();

    try (ApplicationContext applicationContext = new StandardApplicationContext()) {
      applicationContext.load("cn.taketoday.context.factory.cycle");
      assertEquals(5, applicationContext.getBeanDefinitionCount());

      final BeanA beanA = applicationContext.getBean(BeanA.class);
      final BeanB beanB = applicationContext.getBean(BeanB.class);
      applicationContext.getBean(BeanC.class);

      assertEquals(beanA, beanB.beanA);
      assertEquals(beanB, beanA.beanB);
      assertEquals(beanB, beanB.beanB);

      final ConstructorCycleDependency1 one = applicationContext.getBean(ConstructorCycleDependency1.class);
      final ConstructorCycleDependency2 two = applicationContext.getBean(ConstructorCycleDependency2.class);

      assertEquals(two, one.two);
      assertEquals(one, two.one.get());
    }
  }

  @Singleton
//  @Prototype
  public static class BeanA {
    @Autowired
    BeanB beanB;
  }

  @Singleton
//  @Prototype
  public static class BeanB {

    @Autowired
    BeanA beanA;
    @Autowired
    BeanB beanB;
  }

  @Singleton
//  @Prototype
  public static class BeanC {

    int order;
    BeanA beanA;
    BeanB beanB;

    @Order(3)
    @PostConstruct
    public void init(BeanA beanA, BeanB beanB) {
      this.beanA = beanA;
      this.beanB = beanB;
      order = 2;
    }

    @Order(2)
    @PostConstruct
    public void init2(BeanA beanA) {
      assertEquals(this.beanA, beanA);
      assertEquals(order, 2);
      order = 3;
    }

    @Order(1)
    @PostConstruct
    public void init3(BeanC beanC) {
      assertEquals(this, beanC);
      assertEquals(order, 3);
    }
  }

  @Singleton
  public static class ConstructorCycleDependency1 {
    ConstructorCycleDependency2 two;

    ConstructorCycleDependency1(ConstructorCycleDependency2 two) {
      this.two = two;
    }
  }

  @Singleton
  public static class ConstructorCycleDependency2 {
    Supplier<ConstructorCycleDependency1> one;

    ConstructorCycleDependency2(Supplier<ConstructorCycleDependency1> one) {
      this.one = one;
    }
  }

  // proxy

  @Test
  public void testProxyCycleDependency() {
    CandidateComponentScanner.getSharedInstance().clear();

    try (ApplicationContext applicationContext = new StandardApplicationContext()) {

      applicationContext.importBeans(LoggingAspect.class);
      applicationContext.load("cn.taketoday.context.factory.cycle");

      final LoggingBeanA beanA = applicationContext.getBean(LoggingBeanA.class);
      final LoggingBeanB beanB = applicationContext.getBean(LoggingBeanB.class);
      applicationContext.getBean(LoggingBeanC.class);

      System.out.println(beanA.getClass());
      System.out.println(beanB.getClass());

      beanA.doSomething();
      beanB.doSomething();

    }
  }

  @Singleton
//  @Prototype

  @Lazy
  public static class LoggingBeanA {
    @Autowired
    LoggingBeanB beanB;
    @Logger
    void doSomething() {

    }
  }

  @Singleton
//  @Prototype
  @Lazy
  public static class LoggingBeanB {

    @Autowired
    LoggingBeanA beanA;
    @Autowired
    LoggingBeanB beanB;
    @Logger
    void doSomething() {

    }
  }

  @Singleton
//  @Prototype
  public static class LoggingBeanC {

    int order;
    LoggingBeanA beanA;
    LoggingBeanB beanB;

    @Order(3)
    @PostConstruct
    public void init(LoggingBeanA beanA, LoggingBeanB beanB) {
      this.beanA = beanA;
      this.beanB = beanB;
      order = 2;
    }

    @Order(2)
    @PostConstruct
    @Logger
    public void init2(LoggingBeanA beanA) {
      assertEquals(this.beanA, beanA);
      assertEquals(order, 2);
      order = 3;
    }

    @Order(1)
    @PostConstruct
    public void init3(LoggingBeanC beanC) {
      assertEquals(this, beanC);
      assertEquals(order, 3);
    }
  }

  @Aspect
  @EnableAspectAutoProxy
  static class LoggingAspect {

    @Around(Logger.class)
    Object around(@JoinPoint Joinpoint joinPoint) throws Throwable {
      System.out.println("before: " + joinPoint);
      return joinPoint.proceed();
    }

  }

}
