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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 19:45
 */
class InstanceFilterTests {

  @Test
  void matchWithEmptyIncludesAndExcludesReturnsMatchIfEmpty() {
    InstanceFilter<String> filter = new InstanceFilter<>(null, null, true);
    assertThat(filter.match("test")).isTrue();

    filter = new InstanceFilter<>(null, null, false);
    assertThat(filter.match("test")).isFalse();
  }

  @Test
  void matchWithNullInstanceThrowsException() {
    InstanceFilter<String> filter = new InstanceFilter<>(null, null, true);
    assertThatThrownBy(() -> filter.match(null))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void matchWithOnlyIncludesMatchesIncludedElement() {
    InstanceFilter<String> filter = new InstanceFilter<>(List.of("foo", "bar"), null, false);
    assertThat(filter.match("foo")).isTrue();
    assertThat(filter.match("bar")).isTrue();
    assertThat(filter.match("baz")).isFalse();
  }

  @Test
  void matchWithOnlyExcludesMatchesNonExcludedElement() {
    InstanceFilter<String> filter = new InstanceFilter<>(null, List.of("foo", "bar"), true);
    assertThat(filter.match("foo")).isFalse();
    assertThat(filter.match("bar")).isFalse();
    assertThat(filter.match("baz")).isTrue();
  }

  @Test
  void matchWithBothIncludesAndExcludesMatchesIncludedNonExcludedElement() {
    InstanceFilter<String> filter = new InstanceFilter<>(
            List.of("foo", "bar", "baz"),
            List.of("bar", "qux"),
            false);

    assertThat(filter.match("foo")).isTrue();
    assertThat(filter.match("bar")).isFalse();
    assertThat(filter.match("baz")).isTrue();
    assertThat(filter.match("qux")).isFalse();
    assertThat(filter.match("other")).isFalse();
  }

  @Test
  void matchWithCustomMatchingLogic() {
    InstanceFilter<String> filter = new InstanceFilter<>(List.of("foo"), null, false) {
      @Override
      protected boolean match(String instance, String candidate) {
        return instance.startsWith(candidate);
      }
    };

    assertThat(filter.match("foobar")).isTrue();
    assertThat(filter.match("barfoo")).isFalse();
  }

  @Test
  void toStringContainsFilterDetails() {
    InstanceFilter<String> filter = new InstanceFilter<>(
            List.of("foo"), List.of("bar"), true);

    assertThat(filter.toString())
            .contains("includes=[foo]")
            .contains("excludes=[bar]")
            .contains("matchIfEmpty=true");
  }

  @Test
  void matchWithEmptyCollectionsAndNullCollectionsAreTreatedTheSame() {
    InstanceFilter<String> filter1 = new InstanceFilter<>(Collections.emptyList(), Collections.emptyList(), true);
    InstanceFilter<String> filter2 = new InstanceFilter<>(null, null, true);

    assertThat(filter1.match("test")).isEqualTo(filter2.match("test"));
  }

  @Test
  void matchWithDuplicateIncludesAndExcludes() {
    InstanceFilter<String> filter = new InstanceFilter<>(
            List.of("foo", "foo"),
            List.of("bar", "bar"),
            false);

    assertThat(filter.match("foo")).isTrue();
    assertThat(filter.match("bar")).isFalse();
  }

}