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

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.buildpack.platform.docker.type.Layer;
import cn.taketoday.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Buildpacks}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
class BuildpacksTests {

  @Test
  void ofWhenBuildpacksIsNullReturnsEmpty() {
    Buildpacks buildpacks = Buildpacks.of(null);
    assertThat(buildpacks).isSameAs(Buildpacks.EMPTY);
    assertThat(buildpacks.getBuildpacks()).isEmpty();
  }

  @Test
  void ofReturnsBuildpacks() {
    List<Buildpack> buildpackList = new ArrayList<>();
    buildpackList.add(new TestBuildpack("example/buildpack1", "0.0.1"));
    buildpackList.add(new TestBuildpack("example/buildpack2", "0.0.2"));
    Buildpacks buildpacks = Buildpacks.of(buildpackList);
    assertThat(buildpacks.getBuildpacks()).isEqualTo(buildpackList);
  }

  @Test
  void applyWritesLayersAndOrderLayer() throws Exception {
    List<Buildpack> buildpackList = new ArrayList<>();
    buildpackList.add(new TestBuildpack("example/buildpack1", "0.0.1"));
    buildpackList.add(new TestBuildpack("example/buildpack2", "0.0.2"));
    buildpackList.add(new TestBuildpack("example/buildpack3", null));
    Buildpacks buildpacks = Buildpacks.of(buildpackList);
    List<Layer> layers = new ArrayList<>();
    buildpacks.apply(layers::add);
    assertThat(layers).hasSize(4);
    assertThatLayerContentIsCorrect(layers.get(0), "example_buildpack1/0.0.1");
    assertThatLayerContentIsCorrect(layers.get(1), "example_buildpack2/0.0.2");
    assertThatLayerContentIsCorrect(layers.get(2), "example_buildpack3/null");
    assertThatOrderLayerContentIsCorrect(layers.get(3));
  }

  private void assertThatLayerContentIsCorrect(Layer layer, String path) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    layer.writeTo(out);
    try (TarArchiveInputStream tar = new TarArchiveInputStream(new ByteArrayInputStream(out.toByteArray()))) {
      assertThat(tar.getNextEntry().getName()).isEqualTo("/cnb/buildpacks/" + path + "/buildpack.toml");
      assertThat(tar.getNextEntry()).isNull();
    }
  }

  private void assertThatOrderLayerContentIsCorrect(Layer layer) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    layer.writeTo(out);
    try (TarArchiveInputStream tar = new TarArchiveInputStream(new ByteArrayInputStream(out.toByteArray()))) {
      assertThat(tar.getNextEntry().getName()).isEqualTo("/cnb/order.toml");
      byte[] content = StreamUtils.copyToByteArray(tar);
      String toml = new String(content, StandardCharsets.UTF_8);
      assertThat(toml).isEqualTo(getExpectedToml());
    }
  }

  private String getExpectedToml() {
    StringBuilder toml = new StringBuilder();
    toml.append("[[order]]\n");
    toml.append("\n");
    toml.append("  [[order.group]]\n");
    toml.append("    id = \"example/buildpack1\"\n");
    toml.append("    version = \"0.0.1\"\n");
    toml.append("\n");
    toml.append("  [[order.group]]\n");
    toml.append("    id = \"example/buildpack2\"\n");
    toml.append("    version = \"0.0.2\"\n");
    toml.append("\n");
    toml.append("  [[order.group]]\n");
    toml.append("    id = \"example/buildpack3\"\n");
    toml.append("\n");
    return toml.toString();
  }

}
