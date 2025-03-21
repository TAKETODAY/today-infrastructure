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

package infra.origin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SystemEnvironmentOrigin}.
 *
 * @author Madhura Bhave
 */
class SystemEnvironmentOriginTests {

  @Test
  void createWhenPropertyIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new SystemEnvironmentOrigin(null));
  }

  @Test
  void createWhenPropertyNameIsEmptyShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new SystemEnvironmentOrigin(""));
  }

  @Test
  void getPropertyShouldReturnProperty() {
    SystemEnvironmentOrigin origin = new SystemEnvironmentOrigin("FOO_BAR");
    assertThat(origin.getProperty()).isEqualTo("FOO_BAR");
  }

  @Test
  void toStringShouldReturnStringWithDetails() {
    SystemEnvironmentOrigin origin = new SystemEnvironmentOrigin("FOO_BAR");
    assertThat(origin.toString()).isEqualTo("System Environment Property \"FOO_BAR\"");
  }

}
