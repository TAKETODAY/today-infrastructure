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

package infra.web.bind.resolver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/13 21:56
 */
class ParameterResolvingStrategiesTests {
  ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();

  @Test
  void indexOf() {
    assertThat(strategies.indexOf(MapMethodProcessor.class)).isEqualTo(-1);
    strategies.add(new MapMethodProcessor());

    assertThat(strategies.indexOf(CookieParameterResolver.class)).isEqualTo(-1);
    strategies.add(new CookieParameterResolver());

    assertThat(strategies.indexOf(ErrorsMethodArgumentResolver.class)).isEqualTo(-1);
    strategies.add(new ErrorsMethodArgumentResolver());

    assertThat(strategies.indexOf(MapMethodProcessor.class)).isEqualTo(0);
    assertThat(strategies.indexOf(CookieParameterResolver.class)).isEqualTo(1);
    assertThat(strategies.indexOf(ErrorsMethodArgumentResolver.class)).isEqualTo(2);

    assertThat(strategies.get(CookieParameterResolver.class)).isNotNull();
    assertThat(strategies.get(ErrorsMethodArgumentResolver.class)).isNotNull();
    assertThat(strategies.get(MapMethodProcessor.class)).isInstanceOf(MapMethodProcessor.class);
    assertThat(strategies.get(AbstractNamedValueResolvingStrategy.class)).isNull();

  }

  @Test
  void lastIndexOf() {
    assertThat(strategies.lastIndexOf(MapMethodProcessor.class)).isEqualTo(-1);
    strategies.add(new MapMethodProcessor());

    assertThat(strategies.lastIndexOf(CookieParameterResolver.class)).isEqualTo(-1);
    strategies.add(new CookieParameterResolver());

    assertThat(strategies.lastIndexOf(ErrorsMethodArgumentResolver.class)).isEqualTo(-1);
    strategies.add(new ErrorsMethodArgumentResolver());

    assertThat(strategies.lastIndexOf(MapMethodProcessor.class)).isEqualTo(2);
    assertThat(strategies.lastIndexOf(MapMethodProcessor.class)).isEqualTo(2);
    assertThat(strategies.lastIndexOf(CookieParameterResolver.class)).isEqualTo(1);
    assertThat(strategies.lastIndexOf(ErrorsMethodArgumentResolver.class)).isEqualTo(0);
    assertThat(strategies.lastIndexOf(ParameterResolvingStrategy.class)).isEqualTo(-1);

  }

  @Test
  void replace() {
    strategies.add(new MapMethodProcessor());
    strategies.add(new CookieParameterResolver());
    strategies.add(new ErrorsMethodArgumentResolver());
    assertThat(strategies.indexOf(MapMethodProcessor.class)).isEqualTo(0);
    assertThat(strategies.indexOf(CookieParameterResolver.class)).isEqualTo(1);

    // replace true
    assertThat(strategies.replace(MapMethodProcessor.class, new CookieParameterResolver())).isTrue();
    assertThat(strategies.indexOf(MapMethodProcessor.class)).isEqualTo(-1);
    assertThat(strategies.indexOf(CookieParameterResolver.class)).isEqualTo(0);

    // replace false
    assertThat(strategies.replace(ParameterResolvingStrategy.class, new CookieParameterResolver())).isFalse();
    assertThat(strategies.indexOf(MapMethodProcessor.class)).isEqualTo(-1);
    assertThat(strategies.indexOf(CookieParameterResolver.class)).isEqualTo(0);

  }

  @Test
  void contains() {
    strategies.add(new MapMethodProcessor());
    strategies.add(new CookieParameterResolver());

    assertThat(strategies.contains(MapMethodProcessor.class)).isTrue();
    assertThat(strategies.contains(CookieParameterResolver.class)).isTrue();
    assertThat(strategies.contains(ParameterResolvingStrategy.class)).isFalse();

    assertThat(strategies.removeIf(strategy -> strategy instanceof CookieParameterResolver)).isTrue();
    assertThat(strategies.contains(CookieParameterResolver.class)).isFalse();
    assertThat(strategies.removeIf(strategy -> strategy instanceof CookieParameterResolver)).isFalse();

    assertThat(strategies.size()).isEqualTo(1);
    assertThat(strategies.set(0, new CookieParameterResolver())).isInstanceOf(MapMethodProcessor.class);

    assertThatThrownBy(() -> strategies.set(1, new CookieParameterResolver()))
            .isInstanceOf(IndexOutOfBoundsException.class);
  }

  @Test
  void constructorWithInitialCapacity() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies(5);

    assertThat(strategies).isNotNull();
    assertThat(strategies.isEmpty()).isTrue();
    assertThat(strategies.size()).isEqualTo(0);
  }

  @Test
  void addSingleStrategy() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    MapMethodProcessor processor = new MapMethodProcessor();

    strategies.add(processor);

    assertThat(strategies.size()).isEqualTo(1);
    assertThat(strategies.contains(MapMethodProcessor.class)).isTrue();
  }

  @Test
  void addMultipleStrategiesAsArray() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    MapMethodProcessor processor1 = new MapMethodProcessor();
    CookieParameterResolver resolver = new CookieParameterResolver();

    strategies.add(processor1, resolver);

    assertThat(strategies.size()).isEqualTo(2);
    assertThat(strategies.contains(MapMethodProcessor.class)).isTrue();
    assertThat(strategies.contains(CookieParameterResolver.class)).isTrue();
  }

  @Test
  void addMultipleStrategiesAsList() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    MapMethodProcessor processor1 = new MapMethodProcessor();
    CookieParameterResolver resolver = new CookieParameterResolver();
    java.util.List<ParameterResolvingStrategy> list = java.util.Arrays.asList(processor1, resolver);

    strategies.add(list);

    assertThat(strategies.size()).isEqualTo(2);
    assertThat(strategies.contains(MapMethodProcessor.class)).isTrue();
    assertThat(strategies.contains(CookieParameterResolver.class)).isTrue();
  }

  @Test
  void addNullStrategy() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();

    strategies.add((ParameterResolvingStrategy) null);

    assertThat(strategies.size()).isEqualTo(0);
  }

  @Test
  void addNullStrategyArray() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();

    strategies.add((ParameterResolvingStrategy[]) null);

    assertThat(strategies.size()).isEqualTo(0);
  }

  @Test
  void setStrategies() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    strategies.add(new MapMethodProcessor());

    java.util.List<ParameterResolvingStrategy> newList = java.util.Arrays.asList(new CookieParameterResolver(), new ErrorsMethodArgumentResolver());
    strategies.set(newList);

    assertThat(strategies.size()).isEqualTo(2);
    assertThat(strategies.contains(MapMethodProcessor.class)).isFalse();
    assertThat(strategies.contains(CookieParameterResolver.class)).isTrue();
    assertThat(strategies.contains(ErrorsMethodArgumentResolver.class)).isTrue();
  }

  @Test
  void setNullStrategies() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    strategies.add(new MapMethodProcessor());

    strategies.set(null);

    assertThat(strategies.size()).isEqualTo(0);
    assertThat(strategies.isEmpty()).isTrue();
  }

  @Test
  void iterator() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    MapMethodProcessor processor = new MapMethodProcessor();
    CookieParameterResolver resolver = new CookieParameterResolver();
    strategies.add(processor);
    strategies.add(resolver);

    java.util.List<ParameterResolvingStrategy> collected = new java.util.ArrayList<>();
    for (ParameterResolvingStrategy strategy : strategies) {
      collected.add(strategy);
    }

    assertThat(collected).containsExactly(processor, resolver);
  }

  @Test
  void forEach() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    MapMethodProcessor processor = new MapMethodProcessor();
    CookieParameterResolver resolver = new CookieParameterResolver();
    strategies.add(processor);
    strategies.add(resolver);

    java.util.List<ParameterResolvingStrategy> collected = new java.util.ArrayList<>();
    strategies.forEach(collected::add);

    assertThat(collected).containsExactly(processor, resolver);
  }

  @Test
  void spliterator() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    MapMethodProcessor processor = new MapMethodProcessor();
    CookieParameterResolver resolver = new CookieParameterResolver();
    strategies.add(processor);
    strategies.add(resolver);

    java.util.Spliterator<ParameterResolvingStrategy> spliterator = strategies.spliterator();
    java.util.List<ParameterResolvingStrategy> collected = new java.util.ArrayList<>();
    spliterator.forEachRemaining(collected::add);

    assertThat(collected).containsExactly(processor, resolver);
  }

  @Test
  void toStringMethod() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    strategies.add(new MapMethodProcessor());
    strategies.add(new CookieParameterResolver());

    String toString = strategies.toString();

    assertThat(toString).contains("strategies = 2");
  }

  @Test
  void trimToSize() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    strategies.add(new MapMethodProcessor());

    // This test mainly ensures the method can be called without exception
    strategies.trimToSize();

    assertThat(strategies.size()).isEqualTo(1);
  }

  @Test
  void getStrategiesReturnsInternalList() {
    ParameterResolvingStrategies strategies = new ParameterResolvingStrategies();
    MapMethodProcessor processor = new MapMethodProcessor();
    strategies.add(processor);

    java.util.ArrayList<ParameterResolvingStrategy> internalList = strategies.getStrategies();

    assertThat(internalList).hasSize(1);
    assertThat(internalList.get(0)).isSameAs(processor);
  }

}