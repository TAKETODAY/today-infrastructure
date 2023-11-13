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

package cn.taketoday.framework.context.config;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigTreeConfigDataResource}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ConfigTreeConfigDataResourceTests {

  @Test
  void constructorWhenPathStringIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigTreeConfigDataResource((String) null))
            .withMessage("Path is required");
  }

  @Test
  void constructorWhenPathIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigTreeConfigDataResource((Path) null))
            .withMessage("Path is required");
  }

  @Test
  void equalsWhenPathIsTheSameReturnsTrue() {
    ConfigTreeConfigDataResource location = new ConfigTreeConfigDataResource("/etc/config");
    ConfigTreeConfigDataResource other = new ConfigTreeConfigDataResource("/etc/config");
    assertThat(location).isEqualTo(other);
  }

  @Test
  void equalsWhenPathIsDifferentReturnsFalse() {
    ConfigTreeConfigDataResource location = new ConfigTreeConfigDataResource("/etc/config");
    ConfigTreeConfigDataResource other = new ConfigTreeConfigDataResource("other-location");
    assertThat(location).isNotEqualTo(other);
  }

  @Test
  void toStringReturnsDescriptiveString() {
    ConfigTreeConfigDataResource location = new ConfigTreeConfigDataResource("/etc/config");
    assertThat(location.toString()).isEqualTo("config tree [" + new File("/etc/config").getAbsolutePath() + "]");
  }

}
