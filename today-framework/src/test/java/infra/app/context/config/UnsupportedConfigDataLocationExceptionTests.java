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

package infra.app.context.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UnsupportedConfigDataLocationException}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class UnsupportedConfigDataLocationExceptionTests {

  @Test
  void createSetsMessage() {
    UnsupportedConfigDataLocationException exception = new UnsupportedConfigDataLocationException(
            ConfigDataLocation.valueOf("test"));
    assertThat(exception).hasMessage("Unsupported config data location 'test'");
  }

  @Test
  void getLocationReturnsLocation() {
    ConfigDataLocation location = ConfigDataLocation.valueOf("test");
    UnsupportedConfigDataLocationException exception = new UnsupportedConfigDataLocationException(location);
    assertThat(exception.getLocation()).isEqualTo(location);
  }

}
