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

package cn.taketoday.buildpack.platform.docker.type;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Binding}.
 *
 * @author Scott Frederick
 */
class BindingTests {

  @Test
  void ofReturnsValue() {
    Binding binding = Binding.of("host-src:container-dest:ro");
    assertThat(binding).hasToString("host-src:container-dest:ro");
  }

  @Test
  void ofWithNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Binding.of(null))
            .withMessageContaining("Value is required");
  }

  @Test
  void fromReturnsValue() {
    Binding binding = Binding.from("host-src", "container-dest");
    assertThat(binding).hasToString("host-src:container-dest");
  }

  @Test
  void fromWithNullSourceThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Binding.from((String) null, "container-dest"))
            .withMessageContaining("Source is required");
  }

  @Test
  void fromWithNullDestinationThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Binding.from("host-src", null))
            .withMessageContaining("Destination is required");
  }

  @Test
  void fromVolumeNameSourceReturnsValue() {
    Binding binding = Binding.from(VolumeName.of("host-src"), "container-dest");
    assertThat(binding).hasToString("host-src:container-dest");
  }

  @Test
  void fromVolumeNameSourceWithNullSourceThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Binding.from((VolumeName) null, "container-dest"))
            .withMessageContaining("SourceVolume is required");
  }

}
