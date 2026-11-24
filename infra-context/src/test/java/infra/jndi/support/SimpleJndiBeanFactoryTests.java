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

package infra.jndi.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.naming.Context;
import javax.naming.NamingException;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanNotOfRequiredTypeException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.core.ParameterizedTypeReference;
import infra.core.ResolvableType;
import infra.jndi.JndiTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

class SimpleJndiBeanFactoryTests {

  private final Context namingContext = mock(Context.class);

  private final SimpleJndiBeanFactory factory = new SimpleJndiBeanFactory();

  @BeforeEach
  void configureNamingContext() {
    factory.setJndiTemplate(new JndiTemplate() {
      @Override
      protected Context createInitialContext() {
        return namingContext;
      }
    });
  }

  @Test
  void cachesShareableObjectsEvenWhenFirstAccessedByType() throws NamingException {
    factory.setShareableResources("service");
    var service = new Object();
    given(namingContext.lookup("java:comp/env/service")).willReturn(service);

    assertThat(factory.isSingleton("service")).isTrue();
    assertThat(factory.isPrototype("service")).isFalse();
    assertThat(factory.getType("service")).isEqualTo(Object.class);
    assertThat(factory.getBean("service")).isSameAs(service);
    assertThat(factory.containsBean("service")).isTrue();
    assertThatThrownBy(() -> factory.getBean("service", String.class))
            .isInstanceOf(BeanNotOfRequiredTypeException.class).hasMessageContaining("service");
    then(namingContext).should().lookup("java:comp/env/service");
  }

  @Test
  void cachesOnlyTheTypeOfNonShareableObjects() throws NamingException {
    var first = new ArrayList<String>();
    var second = new ArrayList<String>();
    given(namingContext.lookup("java:comp/env/items")).willReturn(new ArrayList<>(), first, second);

    assertThat(factory.isSingleton("items")).isFalse();
    assertThat(factory.isPrototype("items")).isTrue();
    assertThat(factory.containsBean("items")).isTrue();
    assertThat(factory.getType("items")).isEqualTo(ArrayList.class);
    assertThat(factory.isTypeMatch("items", List.class)).isTrue();
    assertThat(factory.isTypeMatch("items", ResolvableType.forClass(String.class))).isFalse();
    assertThat(factory.containsBean("items")).isTrue();
    assertThat(factory.getBean("items")).isSameAs(first);
    assertThat(factory.getBean("items")).isSameAs(second);
    then(namingContext).should(times(3)).lookup("java:comp/env/items");
  }

  @Test
  void fallsBackToOriginalNameAndSupportsLookupByType() throws NamingException {
    factory.addShareableResource("String");
    given(namingContext.lookup("String")).willReturn("shared");

    var provider = factory.getBeanProvider(String.class);
    assertThat(provider.get()).isEqualTo("shared");
    assertThat(provider.getIfAvailable()).isEqualTo("shared");
    assertThat(provider.getIfUnique()).isEqualTo("shared");
    assertThat(provider.get((Object[]) null)).isEqualTo("shared");
    assertThat(factory.getBean("String", (Object[]) null)).isEqualTo("shared");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> provider.get("arg"))
            .withMessageContaining("explicit bean creation arguments");
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> factory.getBean("String", "arg"))
            .withMessageContaining("explicit bean creation arguments");
    then(namingContext).should().lookup("java:comp/env/String");
    then(namingContext).should().lookup("String");
  }

  @Test
  void checksRequestedTypeReferences() throws NamingException {
    var items = new StringList();
    given(namingContext.lookup("java:comp/env/items")).willReturn(items);

    assertThat(factory.getBean("items", new ParameterizedTypeReference<List<String>>() { })).isSameAs(items);
    assertThatThrownBy(() -> factory.getBean("items", new ParameterizedTypeReference<String>() { }))
            .isInstanceOf(BeanNotOfRequiredTypeException.class).hasMessageContaining("items");
  }

  @Test
  void distinguishesMissingNamesFromNamingServiceFailures() throws NamingException {
    var failure = new NamingException("service offline");
    given(namingContext.lookup("java:global/offline")).willThrow(failure);

    assertThat(factory.containsBean("missing")).isFalse();
    assertThatThrownBy(() -> factory.getBean("missing")).isInstanceOf(NoSuchBeanDefinitionException.class);
    assertThatThrownBy(() -> factory.getType("missing")).isInstanceOf(NoSuchBeanDefinitionException.class);
    assertThat(factory.getType("java:global/offline")).isNull();
    assertThat(factory.isTypeMatch("java:global/offline", String.class)).isFalse();
    assertThat(factory.isTypeMatch("java:global/offline", ResolvableType.forClass(String.class))).isFalse();
    assertThat(factory.isTypeMatch("java:global/offline", (Class<?>) null)).isTrue();
    assertThatThrownBy(() -> factory.getBean("java:global/offline"))
            .isInstanceOf(BeanDefinitionStoreException.class).hasCause(failure);
  }

  @Test
  void exposesNoAliasesAndReusesItsInjector() {
    assertThat(factory.getAliases("service")).isEmpty();
    assertThat(factory.getInjector()).isNotNull().isSameAs(factory.getInjector());
  }

  @ParameterizedTest
  @MethodSource("unsupportedDiscoveryOperations")
  void doesNotSupportDiscoveringBeans(Consumer<SimpleJndiBeanFactory> operation) {
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> operation.accept(factory))
            .withMessageContaining("SimpleJndiBeanFactory does not support");
  }

  private static Stream<Consumer<SimpleJndiBeanFactory>> unsupportedDiscoveryOperations() {
    var type = ResolvableType.forClass(String.class);
    return Stream.of(
            factory -> factory.getBeanProvider(type),
            factory -> factory.getBeanProvider(type, false),
            factory -> factory.getBeanProvider(String.class, false),
            factory -> factory.findSynthesizedAnnotation("service", Deprecated.class),
            factory -> factory.findAnnotationOnBean("service", Deprecated.class),
            factory -> factory.findAnnotationOnBean("service", Deprecated.class, false),
            factory -> factory.findAllAnnotationsOnBean("service", Deprecated.class, false),
            factory -> factory.getBeansWithAnnotation(Deprecated.class, false),
            factory -> factory.getBeanNamesForAnnotation(Deprecated.class),
            factory -> factory.getBeansOfType(String.class, true, false),
            factory -> factory.getBeansOfType(type, true, false),
            factory -> factory.getBeanNamesForType(String.class),
            factory -> factory.getBeanNamesForType(String.class, true, false),
            factory -> factory.getBeanNamesForType(type),
            factory -> factory.getBeanNamesForType(type, true, false),
            factory -> factory.containsBeanDefinition("service"),
            factory -> factory.getBeanDefinition("service"),
            SimpleJndiBeanFactory::getBeanDefinitionCount,
            SimpleJndiBeanFactory::getBeanDefinitionNames);
  }

  @SuppressWarnings("serial")
  private static class StringList extends ArrayList<String> {
  }

}
