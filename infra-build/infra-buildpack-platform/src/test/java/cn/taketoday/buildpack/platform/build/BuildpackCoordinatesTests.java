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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import cn.taketoday.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link BuildpackCoordinates}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class BuildpackCoordinatesTests extends AbstractJsonTests {

  private final Path archive = Paths.get("/buildpack/path");

  @Test
  void fromToml() throws IOException {
    BuildpackCoordinates coordinates = BuildpackCoordinates
            .fromToml(createTomlStream("example/buildpack1", "0.0.1", true, false), this.archive);
    assertThat(coordinates.getId()).isEqualTo("example/buildpack1");
    assertThat(coordinates.getVersion()).isEqualTo("0.0.1");
  }

  @Test
  void fromTomlWhenMissingDescriptorThrowsException() {
    ByteArrayInputStream coordinates = new ByteArrayInputStream("".getBytes());
    assertThatIllegalArgumentException().isThrownBy(() -> BuildpackCoordinates.fromToml(coordinates, this.archive))
            .withMessageContaining("Buildpack descriptor 'buildpack.toml' is required")
            .withMessageContaining(this.archive.toString());
  }

  @Test
  void fromTomlWhenMissingIDThrowsException() throws IOException {
    try (InputStream coordinates = createTomlStream(null, null, true, false)) {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> BuildpackCoordinates.fromToml(coordinates, this.archive))
              .withMessageContaining("Buildpack descriptor must contain ID")
              .withMessageContaining(this.archive.toString());
    }
  }

  @Test
  void fromTomlWhenMissingVersionThrowsException() throws IOException {
    try (InputStream coordinates = createTomlStream("example/buildpack1", null, true, false)) {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> BuildpackCoordinates.fromToml(coordinates, this.archive))
              .withMessageContaining("Buildpack descriptor must contain version")
              .withMessageContaining(this.archive.toString());
    }
  }

  @Test
  void fromTomlWhenMissingStacksAndOrderThrowsException() throws IOException {
    try (InputStream coordinates = createTomlStream("example/buildpack1", "0.0.1", false, false)) {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> BuildpackCoordinates.fromToml(coordinates, this.archive))
              .withMessageContaining("Buildpack descriptor must contain either 'stacks' or 'order'")
              .withMessageContaining(this.archive.toString());
    }
  }

  @Test
  void fromTomlWhenContainsBothStacksAndOrderThrowsException() throws IOException {
    try (InputStream coordinates = createTomlStream("example/buildpack1", "0.0.1", true, true)) {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> BuildpackCoordinates.fromToml(coordinates, this.archive))
              .withMessageContaining("Buildpack descriptor must not contain both 'stacks' and 'order'")
              .withMessageContaining(this.archive.toString());
    }
  }

  @Test
  void fromBuildpackMetadataWhenMetadataIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> BuildpackCoordinates.fromBuildpackMetadata(null))
            .withMessage("BuildpackMetadata must not be null");
  }

  @Test
  void fromBuildpackMetadataReturnsCoordinates() throws Exception {
    BuildpackMetadata metadata = BuildpackMetadata.fromJson(getContentAsString("buildpack-metadata.json"));
    BuildpackCoordinates coordinates = BuildpackCoordinates.fromBuildpackMetadata(metadata);
    assertThat(coordinates.getId()).isEqualTo("example/hello-universe");
    assertThat(coordinates.getVersion()).isEqualTo("0.0.1");
  }

  @Test
  void ofWhenIdIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> BuildpackCoordinates.of(null, null))
            .withMessage("ID must not be empty");
  }

  @Test
  void ofReturnsCoordinates() {
    BuildpackCoordinates coordinates = BuildpackCoordinates.of("id", "1");
    assertThat(coordinates).hasToString("id@1");
  }

  @Test
  void getIdReturnsId() {
    BuildpackCoordinates coordinates = BuildpackCoordinates.of("id", "1");
    assertThat(coordinates.getId()).isEqualTo("id");
  }

  @Test
  void getVersionReturnsVersion() {
    BuildpackCoordinates coordinates = BuildpackCoordinates.of("id", "1");
    assertThat(coordinates.getVersion()).isEqualTo("1");
  }

  @Test
  void getVersionWhenVersionIsNullReturnsNull() {
    BuildpackCoordinates coordinates = BuildpackCoordinates.of("id", null);
    assertThat(coordinates.getVersion()).isNull();
  }

  @Test
  void toStringReturnsNiceString() {
    BuildpackCoordinates coordinates = BuildpackCoordinates.of("id", "1");
    assertThat(coordinates).hasToString("id@1");
  }

  @Test
  void equalsAndHashCode() {
    BuildpackCoordinates c1a = BuildpackCoordinates.of("id", "1");
    BuildpackCoordinates c1b = BuildpackCoordinates.of("id", "1");
    BuildpackCoordinates c2 = BuildpackCoordinates.of("id", "2");
    assertThat(c1a).isEqualTo(c1a).isEqualTo(c1b).isNotEqualTo(c2);
    assertThat(c1a).hasSameHashCodeAs(c1b);
  }

  private InputStream createTomlStream(String id, String version, boolean includeStacks, boolean includeOrder) {
    StringBuilder builder = new StringBuilder();
    builder.append("[buildpack]\n");
    if (id != null) {
      builder.append("id = \"").append(id).append("\"\n");
    }
    if (version != null) {
      builder.append("version = \"").append(version).append("\"\n");
    }
    builder.append("name = \"Example buildpack\"\n");
    builder.append("homepage = \"https://github.com/example/example-buildpack\"\n");
    if (includeStacks) {
      builder.append("[[stacks]]\n");
      builder.append("id = \"io.buildpacks.stacks.bionic\"\n");
    }
    if (includeOrder) {
      builder.append("[[order]]\n");
      builder.append("group = [ { id = \"example/buildpack2\", version=\"0.0.2\" } ]\n");
    }
    return new ByteArrayInputStream(builder.toString().getBytes(StandardCharsets.UTF_8));
  }

}
