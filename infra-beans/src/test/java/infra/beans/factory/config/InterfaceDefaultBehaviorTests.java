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

package infra.beans.factory.config;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import infra.beans.factory.support.GenericBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterfaceDefaultBehaviorTests {

  @Test
  void beanDefinitionDefaultsReflectWhetherArgumentsAndPropertiesArePresent() {
    var definition = new GenericBeanDefinition();

    assertThat(definition.hasConstructorArgumentValues()).isFalse();
    assertThat(definition.hasPropertyValues()).isFalse();

    definition.getConstructorArgumentValues().addGenericArgumentValue("argument");
    definition.getPropertyValues().add("property", "value");

    assertThat(definition.hasConstructorArgumentValues()).isTrue();
    assertThat(definition.hasPropertyValues()).isTrue();
  }

  @Test
  void beanDefinitionInterfaceDefaultsAreEvaluatedDirectly() {
    var definition = mock(BeanDefinition.class, CALLS_REAL_METHODS);
    var constructorArguments = new ConstructorArgumentValues();
    var properties = new infra.beans.PropertyValues();
    when(definition.getConstructorArgumentValues()).thenReturn(constructorArguments);
    when(definition.getPropertyValues()).thenReturn(properties);

    assertThat(definition.hasConstructorArgumentValues()).isFalse();
    assertThat(definition.hasPropertyValues()).isFalse();

    constructorArguments.addGenericArgumentValue("argument");
    properties.add("property", "value");

    assertThat(definition.hasConstructorArgumentValues()).isTrue();
    assertThat(definition.hasPropertyValues()).isTrue();
  }

  @Test
  void instantiationAwareDefaultsAllowInstantiationAndContinuePopulation() {
    var processor = mock(InstantiationAwareBeanPostProcessor.class, CALLS_REAL_METHODS);

    assertThat(processor.postProcessBeforeInstantiation(String.class, "bean")).isNull();
    assertThat(processor.postProcessAfterInstantiation(new Object(), "bean")).isTrue();
  }

  @Test
  void smartInstantiationDefaultsDoNotPredictOrSelectAndPreserveTheBean() {
    var processor = mock(SmartInstantiationAwareBeanPostProcessor.class, CALLS_REAL_METHODS);

    assertThat(processor.predictBeanType(String.class, "bean")).isNull();
    assertThat(processor.determineBeanType(String.class, "bean")).isSameAs(String.class);
    assertThat(processor.determineCandidateConstructors(String.class, "bean")).isNull();
    var bean = new Object();
    assertThat(processor.getEarlyBeanReference(bean, "bean")).isSameAs(bean);
  }

  @Test
  void destructionAwareDefaultRequiresDestruction() {
    var processor = mock(DestructionAwareBeanPostProcessor.class, CALLS_REAL_METHODS);

    assertThat(processor.requiresDestruction(new Object())).isTrue();
  }

  @Test
  void scopeDefaultHasNoConversationIdentifier() {
    Scope scope = new Scope() {
      @Override
      public Object get(String beanName, Supplier<?> objectFactory) {
        return objectFactory.get();
      }

      @Override
      public Object remove(String name) {
        return null;
      }

      @Override
      public void registerDestructionCallback(String name, Runnable callback) { }

      @Override
      public Object resolveContextualObject(String key) {
        return null;
      }
    };

    assertThat(scope.getConversationId()).isNull();
  }

  @Test
  void autowireDefaultsDelegateWithTheirDocumentedDefaultArguments() {
    var factory = mock(AutowireCapableBeanFactory.class, CALLS_REAL_METHODS);

    factory.createBean(String.class);
    verify(factory).createBean(String.class, AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR, false);

    factory.createBean(Integer.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);
    verify(factory).createBean(Integer.class, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

    var bean = new Object();
    factory.autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO);
    verify(factory).autowireBeanProperties(bean, AutowireCapableBeanFactory.AUTOWIRE_NO, false);
  }

  @Test
  void configurableBeanFactoryBootstrapDefaultIsANameableNoOp() {
    var factory = mock(ConfigurableBeanFactory.class, CALLS_REAL_METHODS);

    factory.prepareSingletonBootstrap();
  }

}
