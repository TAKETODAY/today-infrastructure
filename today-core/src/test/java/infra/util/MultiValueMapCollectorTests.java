/*
 * Copyright 2017 - 2025 the original author or authors.
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