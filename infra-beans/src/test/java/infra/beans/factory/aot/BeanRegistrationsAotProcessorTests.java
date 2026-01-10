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

package infra.beans.factory.aot;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.testfixture.beans.AnnotatedBean;
import infra.beans.testfixture.beans.TestBean;

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
