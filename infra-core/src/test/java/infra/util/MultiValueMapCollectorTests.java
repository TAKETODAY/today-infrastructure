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

package infra.util;

import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/12/14 20:41
 */
class MultiValueMapCollectorTests {

  @Test
  void ofFactoryMethod() {
    Function<Integer, String> keyFunction = i -> (i % 2 == 0 ? "even" : "odd");
    Function<Integer, Integer> valueFunction = i -> -i;

    var collector = MultiValueMapCollector.of(keyFunction, valueFunction);
    var multiValueMap = Stream.of(1, 2, 3, 4, 5).collect(collector);

    assertThat(multiValueMap).containsOnlyKeys("even", "odd");
    assertThat(multiValueMap.get("odd")).containsOnly(-1, -3, -5);
    assertThat(multiValueMap.get("even")).containsOnly(-2, -4);
  }

  @Test
  void indexingByFactoryMethod() {
    var collector = MultiValueMapCollector.indexingBy(String::length);
    var multiValueMap = Stream.of("abc", "ABC", "123", "1234", "cat", "abcdef", "ABCDEF").collect(collector);

    assertThat(multiValueMap).containsOnlyKeys(3, 4, 6);
    assertThat(multiValueMap.get(3)).containsOnly("abc", "ABC", "123", "cat");
    assertThat(multiValueMap.get(4)).containsOnly("1234");
    assertThat(multiValueMap.get(6)).containsOnly("abcdef", "ABCDEF");
  }

}