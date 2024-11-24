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

import infra.app.context.config.ConfigDataLocation;
import infra.app.context.config.ConfigDataLocationNotFoundException;
import infra.origin.Origin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLocationNotFoundException}.
 *
 * @author Phillip Webb
 */
class ConfigDataLocationNotFoundExceptionTests {

  private Origin origin = mock(Origin.class);

  private final ConfigDataLocation location = ConfigDataLocation.valueOf("optional:test").withOrigin(this.origin);

  private final ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(
          this.location);

  @Test
  void createWhenLocationIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new ConfigDataLocationNotFoundException(null))
            .withMessage("Location is required");
  }

  @Test
  void getLocationReturnsLocation() {
    assertThat(this.exception.getLocation()).isSameAs(this.location);
  }

  @Test
  void getOriginReturnsLocationOrigin() {
    assertThat(this.exception.getOrigin()).isSameAs(this.origin);
  }

  @Test
  void getReferenceDescriptionReturnsLocationString() {
    assertThat(this.exception.getReferenceDescription()).isEqualTo("location 'optional:test'");
  }

  @Test
  void getMessageReturnsMessage() {
    assertThat(this.exception).hasMessage("Config data location 'optional:test' cannot be found");
  }

}
