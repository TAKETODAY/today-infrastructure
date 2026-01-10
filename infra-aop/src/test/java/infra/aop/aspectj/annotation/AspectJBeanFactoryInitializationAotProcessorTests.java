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

package infra.aop.aspectj.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/23 11:27
 */
class AspectJBeanFactoryInitializationAotProcessorTests {

  private final GenerationContext generationContext = new TestGenerationContext();

  @Test
  void shouldSkipEmptyClass() {
    assertThat(createContribution(EmptyClass.class)).isNull();
  }

  @Test
  void shouldProcessAspect() {
    process(TestAspect.class);
    assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(TestAspect.class, "alterReturnValue"))
            .accepts(this.generationContext.getRuntimeHints());
  }

  private void process(Class<?> beanClass) {
    BeanFactoryInitializationAotContribution contribution = createContribution(beanClass);
    if (contribution != null) {
      contribution.applyTo(this.generationContext, mock());
    }
  }

  @Nullable
  private static BeanFactoryInitializationAotContribution createContribution(Class<?> beanClass) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
    return new AspectJBeanFactoryInitializationAotProcessor().processAheadOfTime(beanFactory);
  }

  static class EmptyClass { }

  @Aspect
  static class TestAspect {

    @Around("pointcut()")
    public Object alterReturnValue(ProceedingJoinPoint joinPoint) throws Throwable {
      joinPoint.proceed();
      return "A-from-aspect";
    }

    @Pointcut("execution(* com.example.aspect.Test*.methodA(..))")
    private void pointcut() {
    }

  }

}