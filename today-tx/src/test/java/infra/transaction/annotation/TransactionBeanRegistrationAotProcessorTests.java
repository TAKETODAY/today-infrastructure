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

package infra.transaction.annotation;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.MemberCategory;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/29 11:29
 */
class TransactionBeanRegistrationAotProcessorTests {

  private final TransactionBeanRegistrationAotProcessor processor = new TransactionBeanRegistrationAotProcessor();

  private final GenerationContext generationContext = new TestGenerationContext();

  @Test
  void shouldSkipNonAnnotatedType() {
    process(NonAnnotatedBean.class);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
  }

  @Test
  void shouldSkipAnnotatedTypeWithNoInterface() {
    process(NoInterfaceBean.class);
    assertThat(this.generationContext.getRuntimeHints().reflection().typeHints()).isEmpty();
  }

  @Test
  void shouldProcessTransactionalOnClass() {
    process(TransactionalOnTypeBean.class);
    assertThat(RuntimeHintsPredicates.reflection().onType(NonAnnotatedTransactionalInterface.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessJakartaTransactionalOnClass() {
    process(JakartaTransactionalOnTypeBean.class);
    assertThat(RuntimeHintsPredicates.reflection().onType(NonAnnotatedTransactionalInterface.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessTransactionalOnInterface() {
    process(TransactionalOnTypeInterface.class);
    assertThat(RuntimeHintsPredicates.reflection().onType(TransactionalOnTypeInterface.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessTransactionalOnClassMethod() {
    process(TransactionalOnClassMethodBean.class);
    assertThat(RuntimeHintsPredicates.reflection().onType(NonAnnotatedTransactionalInterface.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.generationContext.getRuntimeHints());
  }

  @Test
  void shouldProcessTransactionalOnInterfaceMethod() {
    process(TransactionalOnInterfaceMethodBean.class);
    assertThat(RuntimeHintsPredicates.reflection().onType(TransactionalOnMethodInterface.class)
            .withMemberCategory(MemberCategory.INVOKE_DECLARED_METHODS)).accepts(this.generationContext.getRuntimeHints());
  }

  private void process(Class<?> beanClass) {
    BeanRegistrationAotContribution contribution = createContribution(beanClass);
    if (contribution != null) {
      contribution.applyTo(this.generationContext, mock());
    }
  }

  @Nullable
  private BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
    return this.processor.processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
  }


  @SuppressWarnings("unused")
  static class NonAnnotatedBean {

    public void notTransactional() {
    }
  }

  @SuppressWarnings("unused")
  @Transactional
  static class NoInterfaceBean {

    public void notTransactional() {
    }
  }

  @Transactional
  static class TransactionalOnTypeBean implements NonAnnotatedTransactionalInterface {

    @Override
    public void transactional() {
    }
  }

  @jakarta.transaction.Transactional
  static class JakartaTransactionalOnTypeBean implements NonAnnotatedTransactionalInterface {

    @Override
    public void transactional() {
    }
  }

  interface NonAnnotatedTransactionalInterface {

    void transactional();
  }

  @Transactional
  interface TransactionalOnTypeInterface {

    void transactional();
  }

  static class TransactionalOnClassMethodBean implements NonAnnotatedTransactionalInterface {

    @Override
    @Transactional
    public void transactional() {
    }
  }

  interface TransactionalOnMethodInterface {

    @Transactional
    void transactional();
  }

  static class TransactionalOnInterfaceMethodBean implements TransactionalOnMethodInterface {

    @Override
    public void transactional() {
    }
  }
}