/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.aop;

import org.aopalliance.intercept.Joinpoint;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aop.annotation.AfterReturning;
import cn.taketoday.aop.annotation.AfterThrowing;
import cn.taketoday.aop.annotation.Around;
import cn.taketoday.aop.annotation.Aspect;
import cn.taketoday.aop.annotation.Attribute;
import cn.taketoday.aop.annotation.Before;
import cn.taketoday.aop.annotation.JoinPoint;
import cn.taketoday.aop.annotation.Throwing;
import cn.taketoday.aop.proxy.DefaultAutoProxyCreator;
import cn.taketoday.aop.support.AnnotationMatchingPointcut;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.annotation.AspectAutoProxyCreator;
import cn.taketoday.aop.target.PrototypeTargetSource;
import cn.taketoday.aop.target.TargetSourceCreator;
import cn.taketoday.context.AttributeAccessor;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.StandardBeanFactory;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author TODAY <br>
 * 2018-08-10 21:29
 */
@Slf4j
public class AopTest {

  @Import({ //
          DefaultUserService.class, //
          LogAspect.class, //
          MemUserDao.class, //
          TestInterceptor.class, //
          TimeAspect.class//
  })
  static class AopConfig {

  }

  @Test
  public void testAop() throws Throwable {

    try (StandardApplicationContext context = new StandardApplicationContext()) {
//      DebuggingClassWriter.setDebugLocation("D:\\dev\\temp\\debug");

      final StandardBeanFactory beanFactory = context.getBeanFactory();

      beanFactory.importBeans(AopConfig.class);
      final AspectAutoProxyCreator proxyCreator = new AspectAutoProxyCreator();
      proxyCreator.setBeanFactory(beanFactory);
      proxyCreator.setProxyTargetClass(true);
      context.addBeanPostProcessor(proxyCreator);

      UserService userService = context.getBean(UserService.class);

      final Class<? extends UserService> userServiceClass = userService.getClass();
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

  static class Bean {

    static void testStatic() {
      System.out.println("testStatic");
    }

    void test() {
      System.out.println("test");
    }

    void test1() {
      System.out.println("test1");
    }

    int testReturn() {
      return 100;
    }

    void none() {
      System.out.println("none aop");
    }

  }

  //  @Test
//  public void testStandardProxyCreator() throws Throwable {
////    DebuggingClassWriter.setDebugLocation("D:\\dev\\temp\\debug");
//
//    try (StandardApplicationContext context = new StandardApplicationContext("", "cn.taketoday.aop.proxy")) {
//
//      final OldStandardProxyGenerator proxyGenerator = new OldStandardProxyGenerator(context);
//      final Bean target = new Bean();
//      proxyGenerator.setTarget(target);
//      proxyGenerator.setTargetClass(Bean.class);
//
//      final OldTargetSource targetSource = new OldTargetSource(target, Bean.class);
//      proxyGenerator.setTargetSource(targetSource);
//
//      final Map<Method, List<MethodInterceptor>> mapping = new LinkedHashMap<>();
//      final List<MethodInterceptor> advices = new ArrayList<>();
//
//      advices.add(invocation -> {
//        System.out.println("=========before========");
//        final Object proceed = invocation.proceed();
//        System.out.println("=========after========");
//        return proceed;
//      });
//
//      mapping.put(Bean.class.getDeclaredMethod("test"), advices);
//      mapping.put(Bean.class.getDeclaredMethod("test1"), advices);
//      mapping.put(Bean.class.getDeclaredMethod("testReturn"), advices);
//      targetSource.setAspectMappings(mapping);
//
//      final Bean created = (Bean) proxyGenerator.create();
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
      final long start = (long) accessor.getAttribute("Time");
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
  public void testAttributeAccessor() throws Throwable {

    try (StandardApplicationContext context = new StandardApplicationContext()) {

      final StandardBeanFactory beanFactory = context.getBeanFactory();
      final AspectAutoProxyCreator proxyCreator = new AspectAutoProxyCreator();
      proxyCreator.setBeanFactory(beanFactory);

      context.addBeanPostProcessor(proxyCreator);

      beanFactory.importBeans(TimerAspect.class, PrinterBean.class);

      final PrinterBean bean = context.getBean(PrinterBean.class);
      bean.print();
    }
  }

  //

  static class LoggingInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      log.debug("LoggingInterceptor @Around Before method");
      final Object proceed = invocation.proceed();
      log.debug("LoggingInterceptor @Around After method");
      return proceed;
    }

  }

  static class MyInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      final Object proceed = invocation.proceed();
      final Object aThis = invocation.getThis();
      System.out.println(aThis);
      return proceed;
    }

  }

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

    @Singleton
    public DefaultPointcutAdvisor advisor(MyInterceptor interceptor) {
      AnnotationMatchingPointcut pointcut = new AnnotationMatchingPointcut(null, Aware.class);

      DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
      advisor.setPointcut(pointcut);
      advisor.setAdvice(interceptor);

      return advisor;
    }

  }

  @Test
  public void testNewVersionAop() throws Throwable {

    try (StandardApplicationContext context = new StandardApplicationContext()) {

      final TargetSourceCreator targetSourceCreator = new TargetSourceCreator() {

        @Override
        public TargetSource getTargetSource(BeanDefinition def) {

          return new PrototypeTargetSource() {

            @Override
            public Class<?> getTargetClass() {
              return PrinterBean.class;
            }

            @Override
            protected Object newPrototypeInstance() {
              return new PrinterBean();
            }
          };
        }
      };

      final StandardBeanFactory beanFactory = context.getBeanFactory();
      final DefaultAutoProxyCreator autoProxyCreator = new DefaultAutoProxyCreator();
      context.addBeanPostProcessor(autoProxyCreator);
      autoProxyCreator.setBeanFactory(beanFactory);
      autoProxyCreator.setFrozen(true);
      autoProxyCreator.setExposeProxy(true);
//      autoProxyCreator.setUsingCglib(true);

//      autoProxyCreator.setTargetSourceCreators(targetSourceCreator);

      beanFactory.importBeans(LoggingConfig.class, PrinterBean.class);
//      DebuggingClassWriter.setDebugLocation("D:\\dev\\temp\\debug");

      final PrinterBean bean = beanFactory.getBean(PrinterBean.class);
      final DefaultPointcutAdvisor pointcutAdvisor = beanFactory.getBean(DefaultPointcutAdvisor.class);
      System.out.println(pointcutAdvisor);

      bean.print();

      bean.none();
      bean.none("TODAY");
      final int none = bean.none(1);
      assertThat(none).isEqualTo(1);

      System.out.println(none);

//      Advised advised = (Advised) bean;
//      System.out.println(Arrays.toString(advised.getAdvisors()));

    }
  }

}
