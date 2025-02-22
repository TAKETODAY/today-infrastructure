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

package infra.annotation.config.ssl;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import infra.core.io.DefaultResourceLoader;
import infra.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;

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
    assertThatIllegalStateException().isThrownBy(() -> property.toWatchPath(new DefaultResourceLoader()))
            .withMessage("Unable to convert value of property 'name' to a path");
  }

  @Test
  void toWatchPathWhenPathReturnsPath() throws URISyntaxException {
    URL resource = getClass().getResource("keystore.jks");
    Path file = Path.of(resource.toURI()).toAbsolutePath();
    BundleContentProperty property = new BundleContentProperty("name", file.toString());
    assertThat(property.toWatchPath(new DefaultResourceLoader())).isEqualTo(file);
  }

  @Test
  void toWatchPathUsesResourceLoader() throws URISyntaxException {
    URL resource = getClass().getResource("keystore.jks");
    Path file = Path.of(resource.toURI()).toAbsolutePath();
    BundleContentProperty property = new BundleContentProperty("name", file.toString());
    ResourceLoader resourceLoader = spy(new DefaultResourceLoader());
    assertThat(property.toWatchPath(resourceLoader)).isEqualTo(file);
    then(resourceLoader).should(atLeastOnce()).getResource(file.toString());
  }

  @Test
  void shouldThrowBundleContentNotWatchableExceptionIfContentIsNotWatchable() {
    BundleContentProperty property = new BundleContentProperty("name", "https://example.com/");
    assertThatExceptionOfType(BundleContentNotWatchableException.class)
            .isThrownBy(() -> property.toWatchPath(new DefaultResourceLoader()))
            .withMessageContaining("Only 'file:' resources are watchable");
  }

}
