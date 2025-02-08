/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.aop.aspectj.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Test;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.aot.BeanFactoryInitializationAotContribution;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.lang.Nullable;

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