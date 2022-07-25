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

package cn.taketoday.aop.aspectj.autoproxy.benchmark;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.aop.Advisor;
import cn.taketoday.aop.AfterReturningAdvice;
import cn.taketoday.aop.MethodBeforeAdvice;
import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.aop.support.StaticMethodMatcherPointcut;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AspectJ auto proxying. Includes mixing with Framework AOP
 * Advisors to demonstrate that existing autoproxying contract is honoured.
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
public class BenchmarkTests {

  private static final Class<?> CLASS = BenchmarkTests.class;

  private static final String ASPECTJ_CONTEXT = CLASS.getSimpleName() + "-aspectj.xml";

  private static final String SPRING_AOP_CONTEXT = CLASS.getSimpleName() + "-springAop.xml";

  @Test
  public void testRepeatedAroundAdviceInvocationsWithAspectJ() {
    testRepeatedAroundAdviceInvocations(ASPECTJ_CONTEXT, getCount(), "AspectJ");
  }

  @Test
  public void testRepeatedAroundAdviceInvocationsWithSpringAop() {
    testRepeatedAroundAdviceInvocations(SPRING_AOP_CONTEXT, getCount(), "Spring AOP");
  }

  @Test
  public void testRepeatedBeforeAdviceInvocationsWithAspectJ() {
    testBeforeAdviceWithoutJoinPoint(ASPECTJ_CONTEXT, getCount(), "AspectJ");
  }

  @Test
  public void testRepeatedBeforeAdviceInvocationsWithSpringAop() {
    testBeforeAdviceWithoutJoinPoint(SPRING_AOP_CONTEXT, getCount(), "Spring AOP");
  }

  @Test
  public void testRepeatedAfterReturningAdviceInvocationsWithAspectJ() {
    testAfterReturningAdviceWithoutJoinPoint(ASPECTJ_CONTEXT, getCount(), "AspectJ");
  }

  @Test
  public void testRepeatedAfterReturningAdviceInvocationsWithSpringAop() {
    testAfterReturningAdviceWithoutJoinPoint(SPRING_AOP_CONTEXT, getCount(), "Spring AOP");
  }

  @Test
  public void testRepeatedMixWithAspectJ() {
    testMix(ASPECTJ_CONTEXT, getCount(), "AspectJ");
  }

  @Test
  public void testRepeatedMixWithSpringAop() {
    testMix(SPRING_AOP_CONTEXT, getCount(), "Spring AOP");
  }

  /**
   * Change the return number to a higher number to make this test useful.
   */
  protected int getCount() {
    return 10;
  }

  private long testRepeatedAroundAdviceInvocations(String file, int howmany, String technology) {
    ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(file, CLASS);

    StopWatch sw = new StopWatch();
    sw.start(howmany + " repeated around advice invocations with " + technology);
    ITestBean adrian = (ITestBean) bf.getBean("adrian");

    assertThat(AopUtils.isAopProxy(adrian)).isTrue();
    assertThat(adrian.getAge()).isEqualTo(68);

    for (int i = 0; i < howmany; i++) {
      adrian.getAge();
    }

    sw.stop();
    System.out.println(sw.prettyPrint());
    return sw.getLastTaskTimeMillis();
  }

  private long testBeforeAdviceWithoutJoinPoint(String file, int howmany, String technology) {
    ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(file, CLASS);

    StopWatch sw = new StopWatch();
    sw.start(howmany + " repeated before advice invocations with " + technology);
    ITestBean adrian = (ITestBean) bf.getBean("adrian");

    assertThat(AopUtils.isAopProxy(adrian)).isTrue();
    Advised a = (Advised) adrian;
    assertThat(a.getAdvisors().length >= 3).isTrue();
    assertThat(adrian.getName()).isEqualTo("adrian");

    for (int i = 0; i < howmany; i++) {
      adrian.getName();
    }

    sw.stop();
    System.out.println(sw.prettyPrint());
    return sw.getLastTaskTimeMillis();
  }

  private long testAfterReturningAdviceWithoutJoinPoint(String file, int howmany, String technology) {
    ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(file, CLASS);

    StopWatch sw = new StopWatch();
    sw.start(howmany + " repeated after returning advice invocations with " + technology);
    ITestBean adrian = (ITestBean) bf.getBean("adrian");

    assertThat(AopUtils.isAopProxy(adrian)).isTrue();
    Advised a = (Advised) adrian;
    assertThat(a.getAdvisors().length >= 3).isTrue();
    // Hits joinpoint
    adrian.setAge(25);

    for (int i = 0; i < howmany; i++) {
      adrian.setAge(i);
    }

    sw.stop();
    System.out.println(sw.prettyPrint());
    return sw.getLastTaskTimeMillis();
  }

  private long testMix(String file, int howmany, String technology) {
    ClassPathXmlApplicationContext bf = new ClassPathXmlApplicationContext(file, CLASS);

    StopWatch sw = new StopWatch();
    sw.start(howmany + " repeated mixed invocations with " + technology);
    ITestBean adrian = (ITestBean) bf.getBean("adrian");

    assertThat(AopUtils.isAopProxy(adrian)).isTrue();
    Advised a = (Advised) adrian;
    assertThat(a.getAdvisors().length >= 3).isTrue();

    for (int i = 0; i < howmany; i++) {
      // Hit all 3 joinpoints
      adrian.getAge();
      adrian.getName();
      adrian.setAge(i);

      // Invoke three non-advised methods
      adrian.getDoctor();
      adrian.getLawyer();
      adrian.getSpouse();
    }

    sw.stop();
    System.out.println(sw.prettyPrint());
    return sw.getLastTaskTimeMillis();
  }

}

class MultiplyReturnValueInterceptor implements MethodInterceptor {

  private int multiple = 2;

  public int invocations;

  public void setMultiple(int multiple) {
    this.multiple = multiple;
  }

  public int getMultiple() {
    return this.multiple;
  }

  @Override
  public Object invoke(MethodInvocation mi) throws Throwable {
    ++invocations;
    int result = (Integer) mi.proceed();
    return result * this.multiple;
  }

}

class TraceAfterReturningAdvice implements AfterReturningAdvice {

  public int afterTakesInt;

  @Override
  public void afterReturning(Object returnValue, MethodInvocation invocation) throws Throwable {
    ++afterTakesInt;
  }

  public static Advisor advisor() {
    return new DefaultPointcutAdvisor(
            new StaticMethodMatcherPointcut() {
              @Override
              public boolean matches(Method method, Class<?> targetClass) {
                return method.getParameterCount() == 1 &&
                        method.getParameterTypes()[0].equals(Integer.class);
              }
            },
            new TraceAfterReturningAdvice());
  }

}

@Aspect
class TraceAspect {

  public int beforeStringReturn;

  public int afterTakesInt;

  @Before("execution(String *.*(..))")
  public void traceWithoutJoinPoint() {
    ++beforeStringReturn;
  }

  @AfterReturning("execution(void *.*(int))")
  public void traceWithoutJoinPoint2() {
    ++afterTakesInt;
  }

}

class TraceBeforeAdvice implements MethodBeforeAdvice {

  public int beforeStringReturn;

  public static Advisor advisor() {
    return new DefaultPointcutAdvisor(
            new StaticMethodMatcherPointcut() {
              @Override
              public boolean matches(Method method, Class<?> targetClass) {
                return method.getReturnType().equals(String.class);
              }
            },
            new TraceBeforeAdvice());
  }

  @Override
  public void before(MethodInvocation invocation) throws Throwable {
    ++beforeStringReturn;

  }
}
