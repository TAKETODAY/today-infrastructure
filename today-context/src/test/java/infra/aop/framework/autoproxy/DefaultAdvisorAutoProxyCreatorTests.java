/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.aop.framework.autoproxy;

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
    public MethodInterceptor getAdvice() {
      return invocation -> "Advised: " + invocation.proceed();
    }
  }

}
