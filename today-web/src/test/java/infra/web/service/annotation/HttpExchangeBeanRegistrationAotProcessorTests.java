/*
 * Copyright 2017 - 2023 the original author or authors.
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

package infra.web.service.annotation;

import org.junit.jupiter.api.Test;

import infra.aop.framework.Advised;
import infra.aop.framework.StandardProxy;
import infra.aot.generate.GenerationContext;
import infra.aot.hint.RuntimeHints;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.DecoratingProxy;
import infra.lang.Nullable;

import static infra.aot.hint.predicate.RuntimeHintsPredicates.proxies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpExchangeBeanRegistrationAotProcessor}.
 *
 * @author Sebastien Deleuze
 */
class HttpExchangeBeanRegistrationAotProcessorTests {

  private final GenerationContext generationContext = new TestGenerationContext();

  private final RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();

  @Test
  void shouldSkipNonAnnotatedInterface() {
    process(NonAnnotatedInterface.class);
    assertThat(this.runtimeHints.proxies().jdkProxyHints()).isEmpty();
  }

  @Test
  void shouldProcessAnnotatedInterface() {
    process(AnnotatedInterface.class);
    assertThat(proxies().forInterfaces(AnnotatedInterface.class,
            StandardProxy.class, Advised.class, DecoratingProxy.class))
            .accepts(this.runtimeHints);
  }

  private void process(Class<?> beanClass) {
    BeanRegistrationAotContribution contribution = createContribution(beanClass);
    if (contribution != null) {
      contribution.applyTo(this.generationContext, mock());
    }
  }

  @Nullable
  private static BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
    return new HttpExchangeBeanRegistrationAotProcessor()
            .processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
  }

  interface NonAnnotatedInterface {

    void notExchange();
  }

  interface AnnotatedInterface {

    @GetExchange
    void get();
  }

}
