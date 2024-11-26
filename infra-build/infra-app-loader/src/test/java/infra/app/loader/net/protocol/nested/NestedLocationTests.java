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

package infra.app.loader.net.protocol.nested;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import infra.app.loader.net.protocol.Handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link NestedLocation}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class NestedLocationTests {

  @TempDir
  File temp;

  @BeforeAll
  static void registerHandlers() {
    Handlers.register();
  }

  @Test
  void createWhenPathIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new NestedLocation(null, "nested.jar"))
            .withMessageContaining("'path' is required");
  }

  @Test
  void createWhenNestedEntryNameIsNull() {
    NestedLocation location = new NestedLocation(Path.of("test.jar"), null);
    assertThat(location.path().toString()).contains("test.jar");
    assertThat(location.nestedEntryName()).isNull();
  }

  @Test
  void createWhenNestedEntryNameIsEmpty() {
    NestedLocation location = new NestedLocation(Path.of("test.jar"), "");
    assertThat(location.path().toString()).contains("test.jar");
    assertThat(location.nestedEntryName()).isNull();
  }

  @Test
  void fromUrlWhenUrlIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> NestedLocation.fromUrl(null))
            .withMessageContaining("'url' is required");
  }

  @Test
  void fromUrlWhenNotNestedProtocolThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> NestedLocation.fromUrl(new URL("file://test.jar")))
            .withMessageContaining("must use 'nested' protocol");
  }

  @Test
  void fromUrlWhenNoPathThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> NestedLocation.fromUrl(new URL("nested:")))
            .withMessageContaining("'location' must not be empty");
  }

  @Test
  void fromUrlWhenNoSeparator() throws Exception {
    File file = new File(this.temp, "test.jar");
    NestedLocation location = NestedLocation.fromUrl(new URL("nested:" + file.getAbsolutePath() + "/"));
    assertThat(location.path()).isEqualTo(file.toPath());
    assertThat(location.nestedEntryName()).isNull();
  }

  @Test
  void fromUrlReturnsNestedLocation() throws Exception {
    File file = new File(this.temp, "test.jar");
    NestedLocation location = NestedLocation
            .fromUrl(new URL("nested:" + file.getAbsolutePath() + "/!lib/nested.jar"));
    assertThat(location.path()).isEqualTo(file.toPath());
    assertThat(location.nestedEntryName()).isEqualTo("lib/nested.jar");
  }

  @Test
  void fromUriWhenUrlIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> NestedLocation.fromUri(null))
            .withMessageContaining("'uri' is required");
  }

  @Test
  void fromUriWhenNotNestedProtocolThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> NestedLocation.fromUri(new URI("file://test.jar")))
            .withMessageContaining("must use 'nested' scheme");
  }

  @Test
  @Disabled
  void fromUriWhenNoSeparator() throws Exception {
    NestedLocation location = NestedLocation.fromUri(new URI("nested:test.jar!nested.jar"));
    assertThat(location.path().toString()).contains("test.jar!nested.jar");
    assertThat(location.nestedEntryName()).isNull();
  }

  @Test
  void fromUriReturnsNestedLocation() throws Exception {
    File file = new File(this.temp, "test.jar");
    NestedLocation location = NestedLocation
            .fromUri(new URI("nested:" + file.getAbsoluteFile().toURI().getPath() + "/!lib/nested.jar"));
    assertThat(location.path()).isEqualTo(file.toPath());
    assertThat(location.nestedEntryName()).isEqualTo("lib/nested.jar");
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void windowsUncPathIsHandledCorrectly() throws MalformedURLException {
    NestedLocation location = NestedLocation.fromUrl(
            new URL("nested://localhost/c$/dev/temp/demo/build/libs/demo-0.0.1-SNAPSHOT.jar/!APP-INF/classes/"));
    assertThat(location.path()).asString()
            .isEqualTo("\\\\localhost\\c$\\dev\\temp\\demo\\build\\libs\\demo-0.0.1-SNAPSHOT.jar");
  }

}
