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

package cn.taketoday.web.service.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.framework.StandardProxy;
import cn.taketoday.aot.generate.GenerationContext;
import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.test.generate.TestGenerationContext;
import cn.taketoday.beans.factory.aot.BeanRegistrationAotContribution;
import cn.taketoday.beans.factory.support.RegisteredBean;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.core.DecoratingProxy;
import cn.taketoday.lang.Nullable;

import static cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates.proxies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/29 13:08
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