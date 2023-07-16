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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link LifecycleVersion}.
 *
 * @author Phillip Webb
 */
class LifecycleVersionTests {

  @Test
  void parseWhenValueIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> LifecycleVersion.parse(null))
            .withMessage("Value must not be empty");
  }

  @Test
  void parseWhenTooLongThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> LifecycleVersion.parse("v1.2.3.4"))
            .withMessage("Malformed version number '1.2.3.4'");
  }

  @Test
  void parseWhenNonNumericThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> LifecycleVersion.parse("v1.2.3a"))
            .withMessage("Malformed version number '1.2.3a'");
  }

  @Test
  void compareTo() {
    LifecycleVersion v4 = LifecycleVersion.parse("0.0.4");
    assertThat(LifecycleVersion.parse("0.0.3")).isLessThan(v4);
    assertThat(LifecycleVersion.parse("0.0.4")).isEqualByComparingTo(v4);
    assertThat(LifecycleVersion.parse("0.0.5")).isGreaterThan(v4);
  }

  @Test
  void isEqualOrGreaterThan() {
    LifecycleVersion v4 = LifecycleVersion.parse("0.0.4");
    assertThat(LifecycleVersion.parse("0.0.3").isEqualOrGreaterThan(v4)).isFalse();
    assertThat(LifecycleVersion.parse("0.0.4").isEqualOrGreaterThan(v4)).isTrue();
    assertThat(LifecycleVersion.parse("0.0.5").isEqualOrGreaterThan(v4)).isTrue();
  }

  @Test
  void parseReturnsVersion() {
    assertThat(LifecycleVersion.parse("1.2.3")).hasToString("v1.2.3");
    assertThat(LifecycleVersion.parse("1.2")).hasToString("v1.2.0");
    assertThat(LifecycleVersion.parse("1")).hasToString("v1.0.0");
  }

}
