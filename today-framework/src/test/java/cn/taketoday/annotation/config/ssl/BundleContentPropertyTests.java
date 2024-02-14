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

package cn.taketoday.annotation.config.ssl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link BundleContentProperty}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class BundleContentPropertyTests {

  private static final String PEM_TEXT = """
          -----BEGIN CERTIFICATE-----
          -----END CERTIFICATE-----
          """;

  @TempDir
  Path temp;

  @Test
  void isPemContentWhenValueIsPemTextReturnsTrue() {
    BundleContentProperty property = new BundleContentProperty("name", PEM_TEXT);
    assertThat(property.isPemContent()).isTrue();
  }

  @Test
  void isPemContentWhenValueIsNotPemTextReturnsFalse() {
    BundleContentProperty property = new BundleContentProperty("name", "file.pem");
    assertThat(property.isPemContent()).isFalse();
  }

  @Test
  void hasValueWhenHasValueReturnsTrue() {
    BundleContentProperty property = new BundleContentProperty("name", "file.pem");
    assertThat(property.hasValue()).isTrue();
  }

  @Test
  void hasValueWhenHasNullValueReturnsFalse() {
    BundleContentProperty property = new BundleContentProperty("name", null);
    assertThat(property.hasValue()).isFalse();
  }

  @Test
  void hasValueWhenHasEmptyValueReturnsFalse() {
    BundleContentProperty property = new BundleContentProperty("name", "");
    assertThat(property.hasValue()).isFalse();
  }

  @Test
  void toWatchPathWhenNotPathThrowsException() {
    BundleContentProperty property = new BundleContentProperty("name", PEM_TEXT);
    assertThatIllegalStateException().isThrownBy(property::toWatchPath)
            .withMessage("Unable to convert value of property 'name' to a path");
  }

  @Test
  void toWatchPathWhenPathReturnsPath() {
    Path file = this.temp.toAbsolutePath().resolve("file.txt");
    BundleContentProperty property = new BundleContentProperty("name", file.toString());
    assertThat(property.toWatchPath()).isEqualTo(file);
  }

}
