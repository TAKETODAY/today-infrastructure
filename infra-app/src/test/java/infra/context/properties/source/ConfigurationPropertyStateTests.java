/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.properties.source;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigurationPropertyState}.
 *
 * @author Phillip Webb
 */
class ConfigurationPropertyStateTests {

  @Test
  void searchWhenIterableIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> ConfigurationPropertyState.search(null, (e) -> true))
            .withMessageContaining("Source is required");
  }

  @Test
  void searchWhenPredicateIsNullShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ConfigurationPropertyState.search(Collections.emptyList(), null))
            .withMessageContaining("Predicate is required");
  }

  @Test
  void searchWhenContainsItemShouldReturnPresent() {
    List<String> source = Arrays.asList("a", "b", "c");
    ConfigurationPropertyState result = ConfigurationPropertyState.search(source, "b"::equals);
    assertThat(result).isEqualTo(ConfigurationPropertyState.PRESENT);
  }

  @Test
  void searchWhenContainsNoItemShouldReturnAbsent() {
    List<String> source = Arrays.asList("a", "x", "c");
    ConfigurationPropertyState result = ConfigurationPropertyState.search(source, "b"::equals);
    assertThat(result).isEqualTo(ConfigurationPropertyState.ABSENT);
  }

}
