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
import java.util.List;
import java.util.Map;

import cn.taketoday.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link Image}.
 *
 * @author Phillip Webb
 */
class ImageTests extends AbstractJsonTests {

  @Test
  void getConfigEnvContainsParsedValues() throws Exception {
    Image image = getImage();
    Map<String, String> env = image.getConfig().getEnv();
    assertThat(env).contains(entry("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"),
            entry("CNB_USER_ID", "2000"), entry("CNB_GROUP_ID", "2000"),
            entry("CNB_STACK_ID", "org.cloudfoundry.stacks.cflinuxfs3"));
  }

  @Test
  void getConfigLabelsReturnsLabels() throws Exception {
    Image image = getImage();
    Map<String, String> labels = image.getConfig().getLabels();
    assertThat(labels).contains(entry("io.buildpacks.stack.id", "org.cloudfoundry.stacks.cflinuxfs3"));
  }

  @Test
  void getLayersReturnsImageLayers() throws Exception {
    Image image = getImage();
    List<LayerId> layers = image.getLayers();
    assertThat(layers).hasSize(46);
    assertThat(layers.get(0))
            .hasToString("sha256:733a8e5ce32984099ef675fce04730f6e2a6dcfdf5bd292fea01a8f936265342");
    assertThat(layers.get(45))
            .hasToString("sha256:5f70bf18a086007016e948b04aed3b82103a36bea41755b6cddfaf10ace3c6ef");
  }

  @Test
  void getOsReturnsOs() throws Exception {
    Image image = getImage();
    assertThat(image.getOs()).isEqualTo("linux");
  }

  @Test
  void getCreatedReturnsDate() throws Exception {
    Image image = getImage();
    assertThat(image.getCreated()).isEqualTo("2019-10-30T19:34:56.296666503Z");
  }

  private Image getImage() throws IOException {
    return Image.of(getContent("image.json"));
  }

}
