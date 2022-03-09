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

package cn.taketoday.aop.aspectj;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.MethodBeforeAdvice;
import cn.taketoday.aop.ThrowsAdvice;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.core.OverridingClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 */
public class TrickyAspectJPointcutExpressionTests {

  @Test
  public void testManualProxyJavaWithUnconditionalPointcut() throws Exception {
    TestService target = new TestServiceImpl();
    LogUserAdvice logAdvice = new LogUserAdvice();
    testAdvice(new DefaultPointcutAdvisor(logAdvice), logAdvice, target, "TestServiceImpl");
  }

  @Test
  public void testManualProxyJavaWithStaticPointcut() throws Exception {
    TestService target = new TestServiceImpl();
    LogUserAdvice logAdvice = new LogUserAdvice();
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression(String.format("execution(* %s.TestService.*(..))", getClass().getName()));
    testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "TestServiceImpl");
  }

  @Test
  public void testManualProxyJavaWithDynamicPointcut() throws Exception {
    TestService target = new TestServiceImpl();
    LogUserAdvice logAdvice = new LogUserAdvice();
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression(String.format("@within(%s.Log)", getClass().getName()));
    testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "TestServiceImpl");
  }

  @Test
  public void testManualProxyJavaWithDynamicPointcutAndProxyTargetClass() throws Exception {
    TestService target = new TestServiceImpl();
    LogUserAdvice logAdvice = new LogUserAdvice();
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression(String.format("@within(%s.Log)", getClass().getName()));
    testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, target, "TestServiceImpl", true);
  }

  @Test
  public void testManualProxyJavaWithStaticPointcutAndTwoClassLoaders() throws Exception {

    LogUserAdvice logAdvice = new LogUserAdvice();
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression(String.format("execution(* %s.TestService.*(..))", getClass().getName()));

    // Test with default class loader first...
    testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, new TestServiceImpl(), "TestServiceImpl");

    // Then try again with a different class loader on the target...
    SimpleThrowawayClassLoader loader = new SimpleThrowawayClassLoader(TestServiceImpl.class.getClassLoader());
    // Make sure the interface is loaded from the  parent class loader
    loader.excludeClass(TestService.class.getName());
    loader.excludeClass(TestException.class.getName());
    TestService other = (TestService) loader.loadClass(TestServiceImpl.class.getName()).getDeclaredConstructor().newInstance();
    testAdvice(new DefaultPointcutAdvisor(pointcut, logAdvice), logAdvice, other, "TestServiceImpl");
  }

  private void testAdvice(Advisor advisor, LogUserAdvice logAdvice, TestService target, String message)
          throws Exception {
    testAdvice(advisor, logAdvice, target, message, false);
  }

  private void testAdvice(Advisor advisor, LogUserAdvice logAdvice, TestService target, String message,
          boolean proxyTargetClass) throws Exception {

    logAdvice.reset();

    ProxyFactory factory = new ProxyFactory(target);
    factory.setProxyTargetClass(proxyTargetClass);
    factory.addAdvisor(advisor);
    TestService bean = (TestService) factory.getProxy();

    assertThat(logAdvice.getCountThrows()).isEqualTo(0);
    assertThatExceptionOfType(TestException.class).isThrownBy(
            bean::sayHello).withMessageContaining(message);
    assertThat(logAdvice.getCountThrows()).isEqualTo(1);
  }

  public static class SimpleThrowawayClassLoader extends OverridingClassLoader {

    /**
     * Create a new SimpleThrowawayClassLoader for the given class loader.
     *
     * @param parent the ClassLoader to build a throwaway ClassLoader for
     */
    public SimpleThrowawayClassLoader(ClassLoader parent) {
      super(parent);
    }
  }

  @SuppressWarnings("serial")
  public static class TestException extends RuntimeException {

    public TestException(String string) {
      super(string);
    }
  }

  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Inherited
  public static @interface Log {
  }

  public static interface TestService {

    public String sayHello();
  }

  @Log
  public static class TestServiceImpl implements TestService {

    @Override
    public String sayHello() {
      throw new TestException("TestServiceImpl");
    }
  }

  public class LogUserAdvice implements MethodBeforeAdvice, ThrowsAdvice {

    private int countBefore = 0;

    private int countThrows = 0;

    @Override
    public void before(MethodInvocation invocation) throws Throwable {
      countBefore++;
    }

    public void afterThrowing(Exception ex) throws Throwable {
      countThrows++;
      throw ex;
    }

    public int getCountBefore() {
      return countBefore;
    }

    public int getCountThrows() {
      return countThrows;
    }

    public void reset() {
      countThrows = 0;
      countBefore = 0;
    }
  }

}
