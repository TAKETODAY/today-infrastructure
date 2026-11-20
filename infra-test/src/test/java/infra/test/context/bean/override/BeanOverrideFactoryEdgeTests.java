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

package infra.test.context.bean.override;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.StandardBeanFactory;
import infra.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class BeanOverrideFactoryEdgeTests {

  @Test
  void handlersAndCustomizersRejectUnrelatedValuesAndDifferentStrategies() {
    var handler = handler("value", BeanOverrideStrategy.REPLACE);
    assertThat(handler).isNotEqualTo(null).isNotEqualTo(new Object())
            .isNotEqualTo(handler("value", BeanOverrideStrategy.WRAP));
    var customizer = new BeanOverrideContextCustomizer(Set.of(handler));
    assertThat(customizer).isEqualTo(customizer).isNotEqualTo(null).isNotEqualTo(new Object());
    assertThat(customizer.getBeanOverrideHandlers()).containsExactly(handler);
  }

  @Test
  void typeLevelErrorsDoNotRequireFieldDescriptions() {
    StandardBeanFactory factory = new StandardBeanFactory();
    assertThatIllegalStateException().isThrownBy(() -> process(factory, handler("&value", BeanOverrideStrategy.REPLACE)))
            .withMessageContaining("Unable to override bean '&value':");
    assertThatIllegalStateException().isThrownBy(() -> process(factory, handler(null, BeanOverrideStrategy.REPLACE)))
            .withMessageContaining("there are no beans of type java.lang.String.");
    factory.registerSingleton("one", "one");
    factory.registerSingleton("two", "two");
    assertThatIllegalStateException().isThrownBy(() -> process(factory, handler(null, BeanOverrideStrategy.REPLACE)))
            .withMessageContaining("found 2 beans");
  }

  @Test
  void unsupportedFactoriesFailBeforeRegisteringAnOverride() {
    ConfigurableBeanFactory factory = mock(ConfigurableBeanFactory.class);
    given(factory.getBeanNamesForType(any(ResolvableType.class), anyBoolean(), anyBoolean())).willReturn(new String[0]);
    given(factory.getBeanNamesForType(any(Class.class), anyBoolean(), anyBoolean())).willReturn(new String[0]);
    assertThatIllegalStateException().isThrownBy(() -> process(factory, handler("value", BeanOverrideStrategy.REPLACE_OR_CREATE)))
            .withMessageContaining("does not implement BeanDefinitionRegistry");
    ConfigurableBeanFactory registry = mock(ConfigurableBeanFactory.class,
            withSettings().extraInterfaces(infra.beans.factory.support.BeanDefinitionRegistry.class));
    given(registry.getBeanNamesForType(any(ResolvableType.class), anyBoolean(), anyBoolean())).willReturn(new String[0]);
    given(registry.getBeanNamesForType(any(Class.class), anyBoolean(), anyBoolean())).willReturn(new String[0]);
    given(registry.containsSingleton("value")).willReturn(true);
    assertThatIllegalStateException().isThrownBy(() -> process(registry, handler("value", BeanOverrideStrategy.REPLACE_OR_CREATE)))
            .withMessageContaining("does not implement StandardBeanFactory");
  }

  @Test
  void registryRejectsDuplicateHandlersAndUnregisteredLookupReturnsNull() {
    StandardBeanFactory factory = new StandardBeanFactory();
    BeanOverrideRegistry registry = new BeanOverrideRegistry(factory);
    BeanOverrideHandler handler = handler("value", BeanOverrideStrategy.REPLACE);
    assertThat(registry.getBeanForHandler(handler, String.class)).isNull();
    registry.registerBeanOverrideHandler(handler, "value");
    assertThatIllegalStateException().isThrownBy(() -> registry.registerBeanOverrideHandler(handler, "value"))
            .withMessageContaining("multiple registrations");
    Object value = new Object();
    assertThat(registry.wrapBeanIfNecessary(value, "unregistered")).isSameAs(value);
  }

  private void process(ConfigurableBeanFactory factory, BeanOverrideHandler handler) {
    new BeanOverrideBeanFactoryPostProcessor(Set.of(handler), new BeanOverrideRegistry(factory)).postProcessBeanFactory(factory);
  }

  private BeanOverrideHandler handler(String name, BeanOverrideStrategy strategy) {
    return new BeanOverrideHandler((Field) null, ResolvableType.forClass(String.class), name, "", strategy) {
      @Override
      protected Object createOverrideInstance(String beanName, BeanDefinition definition, Object instance) {
        return "replacement";
      }
    };
  }

}
