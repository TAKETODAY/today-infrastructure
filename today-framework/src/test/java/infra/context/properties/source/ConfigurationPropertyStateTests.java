/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.context.properties.source;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.context.properties.source.ConfigurationPropertyState;

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