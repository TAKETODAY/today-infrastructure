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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.RuntimeHints;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;

import static infra.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/11/24 21:02
 */
class AspectJAdvisorBeanRegistrationAotProcessorTests {

  private final GenerationContext generationContext = new TestGenerationContext();

  private final RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();

  @Test
  void shouldProcessAspectJClass() {
    process(AspectJClass.class);
    assertThat(reflection().onType(AspectJClass.class).withMemberCategory(MemberCategory.ACCESS_DECLARED_FIELDS))
            .accepts(this.runtimeHints);
  }

  @Test
  void shouldSkipRegularClass() {
    process(RegularClass.class);
    assertThat(this.runtimeHints.reflection().typeHints()).isEmpty();
  }

  void process(Class<?> beanClass) {
    BeanRegistrationAotContribution contribution = createContribution(beanClass);
    if (contribution != null) {
      contribution.applyTo(this.generationContext, mock());
    }
  }

  @Nullable
  private static BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
    return new AspectJAdvisorBeanRegistrationAotProcessor()
            .processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
  }

  static class AspectJClass {
    private static java.lang.Throwable ajc$initFailureCause;
  }

  static class RegularClass {
    private static java.lang.Throwable initFailureCause;
  }

}