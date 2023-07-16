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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ImageArchiveManifest}.
 *
 * @author Scott Frederick
 * @author Andy Wilkinson
 */
class ImageArchiveManifestTests extends AbstractJsonTests {

  @Test
  void getLayersReturnsLayers() throws Exception {
    ImageArchiveManifest manifest = getManifest();
    List<String> expectedLayers = new ArrayList<>();
    for (int blankLayersCount = 0; blankLayersCount < 46; blankLayersCount++) {
      expectedLayers.add("blank_" + blankLayersCount);
    }
    expectedLayers.add("bb09e17fd1bd2ee47155f1349645fcd9fff31e1247c7ed99cad469f1c16a4216.tar");
    assertThat(manifest.getEntries()).hasSize(1);
    assertThat(manifest.getEntries().get(0).getLayers()).hasSize(47);
    assertThat(manifest.getEntries().get(0).getLayers()).isEqualTo(expectedLayers);
  }

  @Test
  void getLayersWithNoLayersReturnsEmptyList() throws Exception {
    String content = "[{\"Layers\": []}]";
    ImageArchiveManifest manifest = new ImageArchiveManifest(getObjectMapper().readTree(content));
    assertThat(manifest.getEntries()).hasSize(1);
    assertThat(manifest.getEntries().get(0).getLayers()).hasSize(0);
  }

  @Test
  void getLayersWithEmptyManifestReturnsEmptyList() throws Exception {
    String content = "[]";
    ImageArchiveManifest manifest = new ImageArchiveManifest(getObjectMapper().readTree(content));
    assertThat(manifest.getEntries()).isEmpty();
  }

  private ImageArchiveManifest getManifest() throws IOException {
    return new ImageArchiveManifest(getObjectMapper().readTree(getContent("image-archive-manifest.json")));
  }

}
