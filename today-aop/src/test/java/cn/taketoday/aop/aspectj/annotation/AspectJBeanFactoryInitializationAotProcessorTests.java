/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.aop.aspectj.annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.jupiter.api.Test;

import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.aot.BeanFactoryInitializationAotContribution;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.lang.Nullable;

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
    assertThat(RuntimeHintsPredicates.reflection().onMethod(TestAspect.class, "alterReturnValue").invoke())
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