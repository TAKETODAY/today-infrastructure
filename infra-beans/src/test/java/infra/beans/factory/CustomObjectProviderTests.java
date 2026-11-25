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

package infra.beans.factory;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import infra.beans.BeansException;
import infra.beans.testfixture.beans.TestBean;
import infra.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Juergen Hoeller
 * @since 5.0
 */
public class CustomObjectProviderTests {

  @Test
  void unsupportedDefaultRetrieval() {
    ObjectProvider<String> provider = new ObjectProvider<>() { };
    assertThatThrownBy(() -> provider.get("argument")).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(provider::stream).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void callbacksAndFallbacksRespectAvailabilityAndUniqueness() {
    TestBean bean = new TestBean("available");
    TestBean fallback = new TestBean("fallback");
    var consumed = new ArrayList<TestBean>();
    ObjectProvider<TestBean> available = providerOf(bean);
    ObjectProvider<TestBean> empty = providerOf();
    ObjectProvider<TestBean> ambiguous = providerOf(bean, fallback);

    assertThat(available.ifAvailable(consumed::add)).isTrue();
    assertThat(available.ifUnique(consumed::add)).isTrue();
    assertThat(empty.ifAvailable(consumed::add)).isFalse();
    assertThat(empty.ifUnique(consumed::add)).isFalse();
    assertThat(ambiguous.ifUnique(consumed::add)).isFalse();
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
            .isThrownBy(() -> ambiguous.ifAvailable(consumed::add));
    assertThat(consumed).containsExactly(bean, bean);

    assertThat(available.getIfAvailable(() -> fallback)).isSameAs(bean);
    assertThat(available.getIfUnique(() -> fallback)).isSameAs(bean);
    assertThat(empty.getIfAvailable(() -> fallback)).isSameAs(fallback);
    assertThat(empty.getIfUnique(() -> fallback)).isSameAs(fallback);
    assertThat(ambiguous.getIfUnique(() -> fallback)).isSameAs(fallback);
  }

  @Test
  void streamFilteringAndCollectionMethodsPreserveTheirOrderingContracts() {
    Ordered first = () -> 1;
    Ordered last = () -> 10;
    ObjectProvider<Object> provider = providerOf(last, "text", first);
    assertThat(provider.stream(Ordered.class::isAssignableFrom)).containsExactly(last, first);
    assertThat(provider.orderedStream(Ordered.class::isAssignableFrom)).containsExactly(first, last);
    assertThatThrownBy(() -> provider.stream(ObjectProvider.UNFILTERED, false))
            .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> provider.orderedStream(ObjectProvider.UNFILTERED, false))
            .isInstanceOf(UnsupportedOperationException.class);
    assertThat(provider.orderedList()).containsExactly(first, last, "text");
    assertThat(provider.toList()).containsExactly(last, "text", first);
    var ordered = new ArrayList<Object>(List.of("existing"));
    var unordered = new ArrayList<Object>(List.of("existing"));
    provider.addOrderedTo(ordered);
    provider.addTo(unordered);
    assertThat(ordered).containsExactly("existing", first, last, "text");
    assertThat(unordered).containsExactly("existing", last, "text", first);
    assertThat(providerOf().orderedList()).isEmpty();
    assertThat(providerOf().toList()).isEmpty();
    providerOf().addTo(unordered);
    providerOf().addOrderedTo(ordered);
    assertThat(ordered).hasSize(4);
    assertThat(unordered).hasSize(4);
  }

  @SafeVarargs
  private static <T> ObjectProvider<T> providerOf(T... values) {
    return new ObjectProvider<>() {
      @Override
      public Stream<T> stream() {
        return Stream.of(values);
      }
    };
  }

  @Test
  void getObject() {
    TestBean tb1 = new TestBean("tb1");

    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public TestBean get() throws BeansException {
        return tb1;
      }
    };

    assertThat(provider.get()).isSameAs(tb1);
    assertThat(provider.getIfAvailable()).isSameAs(tb1);
    assertThat(provider.getIfUnique()).isSameAs(tb1);
  }

  @Test
  void noObject() {
    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public TestBean get() throws BeansException {
        throw new NoSuchBeanDefinitionException(Object.class);
      }
    };

    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(provider::get);
    assertThat(provider.getIfAvailable()).isNull();
    assertThat(provider.getIfUnique()).isNull();
  }

  @Test
  void noUniqueObject() {
    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public TestBean get() throws BeansException {
        throw new NoUniqueBeanDefinitionException(Object.class);
      }
    };

    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::get);
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::getIfAvailable);
    assertThat(provider.getIfUnique()).isNull();
  }

  @Test
  void emptyStream() {
    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public Stream<TestBean> stream() {
        return Stream.empty();
      }
    };

    assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(provider::get);
    assertThat(provider.getIfAvailable()).isNull();
    assertThat(provider.getIfUnique()).isNull();
  }

  @Test
  void streamWithOneObject() {
    TestBean tb1 = new TestBean("tb1");

    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public Stream<TestBean> stream() {
        return Stream.of(tb1);
      }
    };

    assertThat(provider.get()).isSameAs(tb1);
    assertThat(provider.getIfAvailable()).isSameAs(tb1);
    assertThat(provider.getIfUnique()).isSameAs(tb1);
  }

  @Test
  void streamWithTwoObjects() {
    TestBean tb1 = new TestBean("tb1");
    TestBean tb2 = new TestBean("tb2");

    ObjectProvider<TestBean> provider = new ObjectProvider<>() {
      @Override
      public Stream<TestBean> stream() {
        return Stream.of(tb1, tb2);
      }
    };

    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::get);
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class).isThrownBy(provider::getIfAvailable);
    assertThat(provider.getIfUnique()).isNull();
  }

}
