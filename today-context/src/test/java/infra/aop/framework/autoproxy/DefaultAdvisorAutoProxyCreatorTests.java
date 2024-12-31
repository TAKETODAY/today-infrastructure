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

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.aop.Pointcut;
import infra.aop.support.AbstractPointcutAdvisor;
import infra.aop.support.RootClassFilter;
import infra.aop.support.StaticMethodMatcherPointcut;
import infra.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DefaultAdvisorAutoProxyCreator}.
 *
 * @author Sam Brannen
 */
class DefaultAdvisorAutoProxyCreatorTests {

  /**
   * @see StaticMethodMatcherPointcut#matches(Method, Class)
   */
  @Test
  void staticMethodMatcherPointcutMatchesMethodIsNotInvokedAgainForActualMethodInvocation() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
            DemoBean.class, DemoPointcutAdvisor.class, DefaultAdvisorAutoProxyCreator.class);
    DemoPointcutAdvisor demoPointcutAdvisor = context.getBean(DemoPointcutAdvisor.class);
    DemoBean demoBean = context.getBean(DemoBean.class);

    assertThat(demoPointcutAdvisor.matchesInvocationCount).as("matches() invocations before").isEqualTo(2);
    // Invoke multiple times to ensure additional invocations don't affect the outcome.
    assertThat(demoBean.sayHello()).isEqualTo("Advised: Hello!");
    assertThat(demoBean.sayHello()).isEqualTo("Advised: Hello!");
    assertThat(demoBean.sayHello()).isEqualTo("Advised: Hello!");
    assertThat(demoPointcutAdvisor.matchesInvocationCount).as("matches() invocations after").isEqualTo(2);

    context.close();
  }

  static class DemoBean {

    public String sayHello() {
      return "Hello!";
    }
  }

  @SuppressWarnings("serial")
  static class DemoPointcutAdvisor extends AbstractPointcutAdvisor {

    int matchesInvocationCount = 0;

    @Override
    public Pointcut getPointcut() {
      StaticMethodMatcherPointcut pointcut = new StaticMethodMatcherPointcut() {

        @Override
        public boolean matches(Method method, Class<?> targetClass) {
          if (method.getName().equals("sayHello")) {
            matchesInvocationCount++;
            return true;
          }
          return false;
        }
      };
      pointcut.setClassFilter(new RootClassFilter(DemoBean.class));
      return pointcut;
    }

    @Override
    public Advice getAdvice() {
      return (MethodInterceptor) invocation -> "Advised: " + invocation.proceed();
    }
  }

}
