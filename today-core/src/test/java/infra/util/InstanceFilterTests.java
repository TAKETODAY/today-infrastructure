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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 19:45
 */
class InstanceFilterTests {

  @Test
  void emptyFilterApplyMatchIfEmpty() {
    InstanceFilter<String> filter = new InstanceFilter<>(null, null);
    match(filter, "foo");
    match(filter, "bar");
  }

  @Test
  void includesFilter() {
    InstanceFilter<String> filter = new InstanceFilter<>(List.of("First", "Second"), null);
    match(filter, "Second");
    doNotMatch(filter, "foo");
  }

  @Test
  void excludesFilter() {
    InstanceFilter<String> filter = new InstanceFilter<>(null, List.of("First", "Second"));
    doNotMatch(filter, "Second");
    match(filter, "foo");
  }

  @Test
  void includesAndExcludesFilters() {
    InstanceFilter<String> filter = new InstanceFilter<>(List.of("foo", "Bar"), List.of("First", "Second"));
    doNotMatch(filter, "Second");
    match(filter, "foo");
  }

  @Test
  void includesAndExcludesFiltersConflict() {
    InstanceFilter<String> filter = new InstanceFilter<>(List.of("First"), List.of("First"));
    doNotMatch(filter, "First");
  }

  private static <T> void match(InstanceFilter<T> filter, T candidate) {
    assertThat(filter.match(candidate)).as("filter '" + filter + "' should match " + candidate).isTrue();
  }

  private static <T> void doNotMatch(InstanceFilter<T> filter, T candidate) {
    assertThat(filter.match(candidate)).as("filter '" + filter + "' should not match " + candidate).isFalse();
  }

}