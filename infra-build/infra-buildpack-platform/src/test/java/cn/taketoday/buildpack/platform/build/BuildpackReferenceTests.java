/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BuildpackReference}.
 *
 * @author Phillip Webb
 */
class BuildpackReferenceTests {

  @Test
  void ofWhenValueIsEmptyThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> BuildpackReference.of(""))
            .withMessage("Value must not be empty");
  }

  @Test
  void ofCreatesInstance() {
    BuildpackReference reference = BuildpackReference.of("test");
    assertThat(reference).isNotNull();
  }

  @Test
  void toStringReturnsValue() {
    BuildpackReference reference = BuildpackReference.of("test");
    assertThat(reference).hasToString("test");
  }

  @Test
  void equalsAndHashCode() {
    BuildpackReference a = BuildpackReference.of("test1");
    BuildpackReference b = BuildpackReference.of("test1");
    BuildpackReference c = BuildpackReference.of("test2");
    assertThat(a).isEqualTo(a).isEqualTo(b).isNotEqualTo(c);
    assertThat(a).hasSameHashCodeAs(b);
  }

  @Test
  void hasPrefixWhenPrefixMatchReturnsTrue() {
    BuildpackReference reference = BuildpackReference.of("test");
    assertThat(reference.hasPrefix("te")).isTrue();
  }

  @Test
  void hasPrefixWhenPrefixMismatchReturnsFalse() {
    BuildpackReference reference = BuildpackReference.of("test");
    assertThat(reference.hasPrefix("st")).isFalse();
  }

  @Test
  void getSubReferenceWhenPrefixMatchReturnsSubReference() {
    BuildpackReference reference = BuildpackReference.of("test");
    assertThat(reference.getSubReference("te")).isEqualTo("st");
  }

  @Test
  void getSubReferenceWhenPrefixMismatchReturnsNull() {
    BuildpackReference reference = BuildpackReference.of("test");
    assertThat(reference.getSubReference("st")).isNull();
  }

  @Test
  void asPathWhenFileUrlReturnsPath() {
    BuildpackReference reference = BuildpackReference.of("file:///test.dat");
    assertThat(reference.asPath()).isEqualTo(Paths.get("/test.dat"));
  }

  @Test
  void asPathWhenPathReturnsPath() {
    BuildpackReference reference = BuildpackReference.of("/test.dat");
    assertThat(reference.asPath()).isEqualTo(Paths.get("/test.dat"));
  }

}
