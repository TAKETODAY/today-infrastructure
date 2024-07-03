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

package cn.taketoday.beans.factory.aot;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.AnnotatedBean;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeanRegistrationsAotProcessor}.
 *
 * @author Phillip Webb
 * @author Sebastien Deleuze
 */
class BeanRegistrationsAotProcessorTests {

  @Test
  void beanRegistrationsAotProcessorIsRegistered() {
    assertThat(AotServices.factoriesAndBeans(new StandardBeanFactory())
            .load(BeanFactoryInitializationAotProcessor.class))
            .anyMatch(BeanRegistrationsAotProcessor.class::isInstance);
  }

  @Test
  void processAheadOfTimeReturnsBeanRegistrationsAotContributionWithRegistrations() {
    BeanRegistrationsAotProcessor processor = new BeanRegistrationsAotProcessor();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("b1", new RootBeanDefinition(TestBean.class));
    beanFactory.registerBeanDefinition("b2",
            new RootBeanDefinition(AnnotatedBean.class));
    BeanRegistrationsAotContribution contribution = processor
            .processAheadOfTime(beanFactory);
    assertThat(contribution).extracting("registrations")
            .asInstanceOf(InstanceOfAssertFactories.LIST).hasSize(2);
  }

  @Test
  void processAheadOfTimeReturnsBeanRegistrationsAotContributionWithAliases() {
    BeanRegistrationsAotProcessor processor = new BeanRegistrationsAotProcessor();
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("test", new RootBeanDefinition(TestBean.class));
    beanFactory.registerAlias("test", "testAlias");
    BeanRegistrationsAotContribution contribution = processor
            .processAheadOfTime(beanFactory);
    assertThat(contribution).extracting("registrations").asInstanceOf(InstanceOfAssertFactories.LIST)
            .singleElement().satisfies(registration ->
                    assertThat(registration).extracting("aliases").asInstanceOf(InstanceOfAssertFactories.ARRAY)
                            .singleElement().isEqualTo("testAlias"));
  }

}
