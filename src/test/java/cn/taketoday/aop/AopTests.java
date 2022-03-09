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
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.proxy.DefaultAdvisorAutoProxyCreator;
import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.annotation.AfterReturning;
import cn.taketoday.aop.support.annotation.AfterThrowing;
import cn.taketoday.aop.support.annotation.Around;
import cn.taketoday.aop.support.annotation.Aspect;
import cn.taketoday.aop.support.annotation.AspectAutoProxyCreator;
import cn.taketoday.aop.support.annotation.Attribute;
import cn.taketoday.aop.support.annotation.Before;
import cn.taketoday.aop.support.annotation.JoinPoint;
import cn.taketoday.aop.support.annotation.Throwing;
import cn.taketoday.aop.target.PrototypeTargetSource;
import cn.taketoday.aop.target.TargetSourceCreator;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.EnableAspectAutoProxy;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.lang.Singleton;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author TODAY <br>
 * 2018-08-10 21:29
 */
@Slf4j
@Disabled
class AopTests {

//  static {
//    DebuggingClassWriter.setDebugLocation("~/temp/debug");
//  }

  @Import({ //
          DefaultUserService.class, //
          LogAspect.class, //
          MemUserDao.class, //
          TestInterceptor.class, //
          TimeAspect.class//
  })
  @EnableAspectAutoProxy
  static class AopConfig {

  }

  @Test
  void testAop() throws Throwable {

    try (StandardApplicationContext context = new StandardApplicationContext()) {
//      DebuggingClassWriter.setDebugLocation("D:\\dev\\temp\\debug");

      StandardBeanFactory beanFactory = context.getBeanFactory();

      context.register(AopConfig.class);

      AspectAutoProxyCreator proxyCreator = new AspectAutoProxyCreator();
      proxyCreator.setBeanFactory(beanFactory);
      proxyCreator.setProxyTargetClass(true);
      beanFactory.addBeanPostProcessor(proxyCreator);
//      DebuggingClassWriter.setDebugLocation("/Users/today/temp/debug");
      context.refresh();

      UserService userService = context.getBean(UserService.class);

      Class<? extends UserService> userServiceClass = userService.getClass();
      assertNotEquals(DefaultUserService.class, userServiceClass);
      assertEquals(DefaultUserService.class, userServiceClass.getSuperclass());

      User user = new User();
      user.setPassword("666");
      user.setEmail("666");
      long start = System.currentTimeMillis();
      User login = userService.login(user);

//    for (int i = 0; i < 1000; i++) {
//      login = bean.login(user);
//    }

      log.debug("{}ms", System.currentTimeMillis() - start);
      log.debug("Result:[{}]", login);
      log.debug("{}ms", System.currentTimeMillis() - start);

      TestInterceptor bean2 = context.getBean(TestInterceptor.class);

      System.err.println(bean2);
    }
  }

  //  @Test
//  public void testStandardProxyCreator() throws Throwable {
////    DebuggingClassWriter.setDebugLocation("D:\\dev\\temp\\debug");
//
//    try (StandardApplicationContext context = new StandardApplicationContext("", "cn.taketoday.aop.proxy")) {
//
//      OldStandardProxyGenerator proxyGenerator = new OldStandardProxyGenerator(context);
//      Bean target = new Bean();
//      proxyGenerator.setTarget(target);
//      proxyGenerator.setTargetClass(Bean.class);
//
//      OldTargetSource targetSource = new OldTargetSource(target, Bean.class);
//      proxyGenerator.setTargetSource(targetSource);
//
//      Map<Method, List<MethodInterceptor>> mapping = new LinkedHashMap<>();
//      List<MethodInterceptor> advices = new ArrayList<>();
//
//      advices.add(invocation -> {
//        System.out.println("=========before========");
//        Object proceed = invocation.proceed();
//        System.out.println("=========after========");
//        return proceed;
//      });
//
//      mapping.put(Bean.class.getDeclaredMethod("test"), advices);
//      mapping.put(Bean.class.getDeclaredMethod("test1"), advices);
//      mapping.put(Bean.class.getDeclaredMethod("testReturn"), advices);
//      targetSource.setAspectMappings(mapping);
//
//      Bean created = (Bean) proxyGenerator.create();
//
//      System.out.println(created);
//
//      Bean.testStatic();
//      created.test();
//      created.test1();
//      System.out.println(created.testReturn());
//    }
//  }

  @Aspect
  static class TimerAspect {

    @AfterReturning(TimeAware.class)
    public void afterReturning(@JoinPoint Joinpoint joinpoint, @Attribute AttributeAccessor accessor) {
      long start = (long) accessor.getAttribute("Time");
      log.debug("TimeAspect @AfterReturning Use [{}] ms", System.currentTimeMillis() - start);
    }

    @AfterThrowing(TimeAware.class)
    public void afterThrowing(@Throwing Throwable throwable) {
      log.error("TimeAspect @AfterThrowing With Msg: [{}]", throwable.getMessage(), throwable);
    }

    @Before(TimeAware.class)
    public void before(AttributeAccessor accessor) {
      accessor.setAttribute("Time", System.currentTimeMillis());
      log.debug("TimeAspect @Before method");
    }

    @Around(TimeAware.class)
    public Object around(@JoinPoint Joinpoint joinpoint) throws Throwable {
      log.debug("TimeAspect @Around Before method");
      Object proceed = joinpoint.proceed();
      log.debug("TimeAspect @Around After method");
      return proceed;
    }

  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.METHOD, ElementType.TYPE })
  public @interface Aware { }

  static class PrinterBean {
    ObjectSupplier<PrinterBean> selfSupplier;

    PrinterBean(ObjectSupplier<PrinterBean> selfSupplier) {
      this.selfSupplier = selfSupplier;
    }

    @Aware
    void print() {
      System.out.println("print");
    }

    void none() {
      System.out.println("none");
    }

    void none(String arg) {
      System.out.println("none" + arg);
    }

    @Aware
    int none(Integer input) {
      System.out.println("none" + input);
      return input;
    }

  }

  @Test
  void testAttributeAccessor() throws Throwable {

    try (StandardApplicationContext context = new StandardApplicationContext()) {

      StandardBeanFactory beanFactory = context.getBeanFactory();
      AspectAutoProxyCreator proxyCreator = new AspectAutoProxyCreator();
      proxyCreator.setBeanFactory(beanFactory);

      beanFactory.addBeanPostProcessor(proxyCreator);

      context.register(TimerAspect.class, PrinterBean.class);
      context.refresh();

      PrinterBean bean = context.getBean(PrinterBean.class);
      bean.print();
    }
  }

  //

  static class LoggingInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      System.out.println("LoggingInterceptor @Around Before method");
      Object proceed = invocation.proceed();
      System.out.println("LoggingInterceptor @Around After method");
      return proceed;
    }

  }

  static class MyInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      Object proceed = invocation.proceed();
      Object aThis = invocation.getThis();
      System.out.println(aThis);
      return proceed;
    }

  }

  @Configuration
  @EnableAspectAutoProxy
  @Import({ LoggingInterceptor.class, MyInterceptor.class })
  static class LoggingConfig {

    @Singleton
    public DefaultPointcutAdvisor loggingAdvisor(LoggingInterceptor loggingAspect) {
      AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, Aware.class);

      DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
      advisor.setPointcut(pointcut);
      advisor.setAdvice(loggingAspect);

      return advisor;
    }

  }

  @Test
  void testNewVersionAop() throws Throwable {

    try (StandardApplicationContext context = new StandardApplicationContext()) {

      TargetSourceCreator targetSourceCreator = (beanClass, beanName) -> {
        if (beanClass != PrinterBean.class) {
          return null;
        }
        return new PrototypeTargetSource() {

          @Override
          public Class<?> getTargetClass() {
            return PrinterBean.class;
          }

          @Override
          protected Object newPrototypeInstance() {
            return new PrinterBean(null);
          }
        };
      };

      StandardBeanFactory beanFactory = context.getBeanFactory();
      DefaultAdvisorAutoProxyCreator autoProxyCreator = new DefaultAdvisorAutoProxyCreator();
      beanFactory.addBeanPostProcessor(autoProxyCreator);
      autoProxyCreator.setBeanFactory(beanFactory);
      autoProxyCreator.setFrozen(true);
      autoProxyCreator.setOpaque(false);
      autoProxyCreator.setExposeProxy(true);
      autoProxyCreator.setTargetSourceCreators(targetSourceCreator);

      context.register(LoggingConfig.class, PrinterBean.class);
      context.refresh();
      // TODO 调试 构造器问题
//      DebuggingClassWriter.setDebugLocation("/Users/today/temp/debug");

      PrinterBean bean = beanFactory.getBean(PrinterBean.class);

      bean.print();

      bean.none();
      bean.none("TODAY");
      int none = bean.none(1);
      assertThat(none).isEqualTo(1);

      assertThat(bean).isInstanceOf(Advised.class);
      Advised advised = (Advised) bean;

      assertThat(advised.getAdvisors()).hasSize(1);

      Class<?> targetClass = advised.getTargetClass();
      assertThat(targetClass).isEqualTo(PrinterBean.class);

      assertThat(advised.isFrozen())
              .isEqualTo(advised.isExposeProxy())
              .isEqualTo(advised.isPreFiltered()).isTrue();

    }
  }

}
