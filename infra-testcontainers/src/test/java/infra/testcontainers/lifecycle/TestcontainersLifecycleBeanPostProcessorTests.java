/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.testcontainers.lifecycle;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.TestcontainersConfiguration;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TestcontainersLifecycleBeanPostProcessorTests {

  private final ConfigurableBeanFactory beanFactory = mock(ConfigurableBeanFactory.class);

  private final TestcontainersLifecycleBeanPostProcessor processor =
          new TestcontainersLifecycleBeanPostProcessor(this.beanFactory, TestcontainersStartup.SEQUENTIAL);

  @Test
  void containerInitializationRetriesWhenAContainerIsCurrentlyInCreation() {
    given(this.beanFactory.isConfigurationFrozen()).willReturn(true);
    given(this.beanFactory.getBeanNamesForType(ContainerState.class, true, false))
            .willReturn(new String[] { "container" });
    given(this.beanFactory.getBean("container"))
            .willThrow(new BeanCurrentlyInCreationException("container")).willReturn(mock(ContainerState.class));

    this.processor.postProcessAfterInitialization(new Object(), "first");
    this.processor.postProcessAfterInitialization(new Object(), "second");
    this.processor.postProcessAfterInitialization(new Object(), "third");

    verify(this.beanFactory, times(2)).getBean("container");
  }

  @Test
  void startableInitializationRetriesAndLaterStartsBeansIndividually() {
    Startable first = mock(Startable.class);
    Startable second = mock(Startable.class);
    Startable late = mock(Startable.class);
    given(this.beanFactory.getBeanNamesForType(Startable.class, true, false))
            .willReturn(new String[] { "first", "second" });
    given(this.beanFactory.getBean("second")).willThrow(new BeanCurrentlyInCreationException("second"))
            .willReturn(second);

    assertThat(this.processor.postProcessAfterInitialization(first, "first")).isSameAs(first);
    verify(first, never()).start();
    this.processor.postProcessAfterInitialization(first, "first");
    this.processor.postProcessAfterInitialization(late, "late");

    verify(first).start();
    verify(second).start();
    verify(late).start();
    verify(this.beanFactory, times(2)).getBeanNamesForType(Startable.class, true, false);
  }

  @Test
  void reportsCreationFailuresUnrelatedToCurrentlyCreatingBeans() {
    given(this.beanFactory.getBeanNamesForType(Startable.class, true, false)).willReturn(new String[] { "broken" });
    var failure = new BeanCreationException("broken", "cannot create container");
    given(this.beanFactory.getBean("broken")).willThrow(failure);

    assertThatThrownBy(() -> this.processor.postProcessAfterInitialization(mock(Startable.class), "first"))
            .isSameAs(failure);
  }

  @Test
  void closesManuallyRegisteredStartableWithoutABeanDefinition() {
    Startable singleton = mock(Startable.class);
    given(this.beanFactory.getBeanNamesForType(Startable.class, false, false))
            .willReturn(new String[] { "singleton", "managed" });
    given(this.beanFactory.getBeanDefinition("singleton")).willThrow(new NoSuchBeanDefinitionException("singleton"));
    RootBeanDefinition managed = new RootBeanDefinition();
    managed.setDestroyMethodName("stop");
    given(this.beanFactory.getBeanDefinition("managed")).willReturn(managed);

    new TestcontainersLifecycleBeanFactoryPostProcessor().postProcessBeanFactory(this.beanFactory);
    this.processor.postProcessBeforeDestruction(singleton, "singleton");

    verify(singleton).close();
    assertThat(managed.getDestroyMethodName()).isEqualTo("stop");
  }

  @Test
  void reusableContainerIsKeptOnlyIfTheEnvironmentSupportsReuse() {
    GenericContainer<?> reusable = mock(GenericContainer.class);
    given(reusable.isShouldBeReused()).willReturn(true);
    RootBeanDefinition definition = new RootBeanDefinition();
    definition.setDestroyMethodName("");
    given(this.beanFactory.getBeanDefinition("container")).willReturn(definition);
    TestcontainersConfiguration configuration = mock(TestcontainersConfiguration.class);

    try (var configurationAccess = mockStatic(TestcontainersConfiguration.class)) {
      configurationAccess.when(TestcontainersConfiguration::getInstance).thenReturn(configuration);
      given(configuration.environmentSupportsReuse()).willReturn(true, false);
      this.processor.postProcessBeforeDestruction(reusable, "container");
      verify(reusable, never()).close();
      this.processor.postProcessBeforeDestruction(reusable, "container");
      verify(reusable).close();
    }
  }

}
