/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.framework.autoproxy;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.EnableAspectJAutoProxy;
import infra.test.annotation.DirtiesContext;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for advice invocation order for advice configured via
 * AspectJ auto-proxy support.
 *
 * @author Sam Brannen
 */
class AspectJAutoProxyAdviceOrderIntegrationTests {

  /**
   * {@link After @After} advice declared as first <em>after</em> method in source code.
   */
  @Nested
  @JUnitConfig(AfterAdviceFirstConfig.class)
  @DirtiesContext
  class AfterAdviceFirstTests {

    @Test
    void afterAdviceIsInvokedLast(@Autowired Echo echo, @Autowired AfterAdviceFirstAspect aspect) throws Exception {
      assertThat(aspect.invocations).isEmpty();
      assertThat(echo.echo(42)).isEqualTo(42);
      assertThat(aspect.invocations).containsExactly("around - start", "before", "after returning", "after", "around - end");

      aspect.invocations.clear();
      assertThatExceptionOfType(Exception.class).isThrownBy(
              () -> echo.echo(new Exception()));
      assertThat(aspect.invocations).containsExactly("around - start", "before", "after throwing", "after", "around - end");
    }
  }

  /**
   * This test class uses {@link AfterAdviceLastAspect} which declares its
   * {@link After @After} advice as the last <em>after advice type</em> method
   * in its source code.
   *
   * <p>On Java versions prior to JDK 7, we would have expected the {@code @After}
   * advice method to be invoked before {@code @AfterThrowing} and
   * {@code @AfterReturning} advice methods due to the AspectJ precedence
   * rules implemented in
   * {@link infra.aop.aspectj.autoproxy.AspectJPrecedenceComparator}.
   */
  @Nested
  @JUnitConfig(AfterAdviceLastConfig.class)
  @DirtiesContext
  class AfterAdviceLastTests {

    @Test
    void afterAdviceIsInvokedLast(@Autowired Echo echo, @Autowired AfterAdviceLastAspect aspect) throws Exception {
      assertThat(aspect.invocations).isEmpty();
      assertThat(echo.echo(42)).isEqualTo(42);
      assertThat(aspect.invocations).containsExactly("around - start", "before", "after returning", "after", "around - end");

      aspect.invocations.clear();
      assertThatExceptionOfType(Exception.class).isThrownBy(
              () -> echo.echo(new Exception()));
      assertThat(aspect.invocations).containsExactly("around - start", "before", "after throwing", "after", "around - end");
    }
  }

  @Configuration
  @EnableAspectJAutoProxy(proxyTargetClass = true)
  static class AfterAdviceFirstConfig {

    @Bean
    AfterAdviceFirstAspect echoAspect() {
      return new AfterAdviceFirstAspect();
    }

    @Bean
    Echo echo() {
      return new Echo();
    }
  }

  @Configuration
  @EnableAspectJAutoProxy(proxyTargetClass = true)
  static class AfterAdviceLastConfig {

    @Bean
    AfterAdviceLastAspect echoAspect() {
      return new AfterAdviceLastAspect();
    }

    @Bean
    Echo echo() {
      return new Echo();
    }
  }

  static class Echo {

    Object echo(Object obj) throws Exception {
      if (obj instanceof Exception) {
        throw (Exception) obj;
      }
      return obj;
    }
  }

  /**
   * {@link After @After} advice declared as first <em>after</em> method in source code.
   */
  @Aspect
  static class AfterAdviceFirstAspect {

    List<String> invocations = new ArrayList<>();

    @Pointcut("execution(* echo(*))")
    void echo() {
    }

    @After("echo()")
    void after() {
      invocations.add("after");
    }

    @AfterReturning("echo()")
    void afterReturning() {
      invocations.add("after returning");
    }

    @AfterThrowing("echo()")
    void afterThrowing() {
      invocations.add("after throwing");
    }

    @Before("echo()")
    void before() {
      invocations.add("before");
    }

    @Around("echo()")
    Object around(ProceedingJoinPoint joinPoint) throws Throwable {
      invocations.add("around - start");
      try {
        return joinPoint.proceed();
      }
      finally {
        invocations.add("around - end");
      }
    }
  }

  /**
   * {@link After @After} advice declared as last <em>after</em> method in source code.
   */
  @Aspect
  static class AfterAdviceLastAspect {

    List<String> invocations = new ArrayList<>();

    @Pointcut("execution(* echo(*))")
    void echo() {
    }

    @Around("echo()")
    Object around(ProceedingJoinPoint joinPoint) throws Throwable {
      invocations.add("around - start");
      try {
        return joinPoint.proceed();
      }
      finally {
        invocations.add("around - end");
      }
    }

    @Before("echo()")
    void before() {
      invocations.add("before");
    }

    @AfterReturning("echo()")
    void afterReturning() {
      invocations.add("after returning");
    }

    @AfterThrowing("echo()")
    void afterThrowing() {
      invocations.add("after throwing");
    }

    @After("echo()")
    void after() {
      invocations.add("after");
    }
  }

}
