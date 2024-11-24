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

package infra.core.annotation;

import org.junit.jupiter.api.Test;

import infra.core.annotation.PackagesAnnotationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PackagesAnnotationFilter}.
 *
 * @author Phillip Webb
 */
class PackagesAnnotationFilterTests {

  @Test
  void createWhenPackagesIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    new PackagesAnnotationFilter((String[]) null))
            .withMessage("Packages array is required");
  }

  @Test
  void createWhenPackagesContainsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    new PackagesAnnotationFilter((String) null))
            .withMessage("Packages array must not have empty elements");
  }

  @Test
  void createWhenPackagesContainsEmptyTextThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    new PackagesAnnotationFilter(""))
            .withMessage("Packages array must not have empty elements");
  }

  @Test
  void matchesWhenInPackageReturnsTrue() {
    PackagesAnnotationFilter filter = new PackagesAnnotationFilter("com.example");
    assertThat(filter.matches("com.example.Component")).isTrue();
  }

  @Test
  void matchesWhenNotInPackageReturnsFalse() {
    PackagesAnnotationFilter filter = new PackagesAnnotationFilter("com.example");
    assertThat(filter.matches("infra.stereotype.Component")).isFalse();
  }

  @Test
  void matchesWhenInSimilarPackageReturnsFalse() {
    PackagesAnnotationFilter filter = new PackagesAnnotationFilter("com.example");
    assertThat(filter.matches("com.examples.Component")).isFalse();
  }

  @Test
  void equalsAndHashCode() {
    PackagesAnnotationFilter filter1 = new PackagesAnnotationFilter("com.example",
            "infra");
    PackagesAnnotationFilter filter2 = new PackagesAnnotationFilter(
            "infra", "com.example");
    PackagesAnnotationFilter filter3 = new PackagesAnnotationFilter("com.examples");
    assertThat(filter1.hashCode()).isEqualTo(filter2.hashCode());
    assertThat(filter1).isEqualTo(filter1).isEqualTo(filter2).isNotEqualTo(filter3);
  }

}
