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

package infra.messaging.rsocket.service;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import infra.aop.StandardProxy;
import infra.aop.framework.Advised;
import infra.aot.generate.GenerationContext;
import infra.aot.hint.RuntimeHints;
import infra.aot.test.generate.TestGenerationContext;
import infra.beans.factory.aot.BeanRegistrationAotContribution;
import infra.beans.factory.support.RegisteredBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.DecoratingProxy;
import infra.messaging.handler.annotation.Payload;

import static infra.aot.hint.predicate.RuntimeHintsPredicates.proxies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RSocketExchangeBeanRegistrationAotProcessor}.
 *
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 * @since 5.0
 */
class RSocketExchangeBeanRegistrationAotProcessorTests {

  private final GenerationContext generationContext = new TestGenerationContext();

  private final RuntimeHints runtimeHints = this.generationContext.getRuntimeHints();

  @Test
  void shouldProcessesAnnotatedInterface() {
    process(AnnotatedInterface.class);
    assertThat(proxies().forInterfaces(AnnotatedInterface.class, StandardProxy.class, Advised.class, DecoratingProxy.class))
            .accepts(this.runtimeHints);
  }

  @Test
  void shouldSkipNonAnnotatedInterface() {
    process(NonAnnotatedInterface.class);
    assertThat(this.runtimeHints.proxies().jdkProxyHints()).isEmpty();
  }

  void process(Class<?> beanClass) {
    BeanRegistrationAotContribution contribution = createContribution(beanClass);
    if (contribution != null) {
      contribution.applyTo(this.generationContext, mock());
    }
  }

  private static @Nullable BeanRegistrationAotContribution createContribution(Class<?> beanClass) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition(beanClass.getName(), new RootBeanDefinition(beanClass));
    return new RSocketExchangeBeanRegistrationAotProcessor()
            .processAheadOfTime(RegisteredBean.of(beanFactory, beanClass.getName()));
  }

  interface NonAnnotatedInterface {

    void notExchange();
  }

  interface AnnotatedInterface {

    @RSocketExchange
    void exchange(@Payload String testPayload);
  }

}
