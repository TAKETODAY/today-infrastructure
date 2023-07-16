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
import java.util.Map;

import cn.taketoday.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ImageConfig}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ImageConfigTests extends AbstractJsonTests {

  @Test
  void getEnvContainsParsedValues() throws Exception {
    ImageConfig imageConfig = getImageConfig();
    Map<String, String> env = imageConfig.getEnv();
    assertThat(env).contains(entry("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"),
            entry("CNB_USER_ID", "2000"), entry("CNB_GROUP_ID", "2000"),
            entry("CNB_STACK_ID", "org.cloudfoundry.stacks.cflinuxfs3"));
  }

  @Test
  void whenConfigHasNoEnvThenImageConfigEnvIsEmpty() throws Exception {
    ImageConfig imageConfig = getMinimalImageConfig();
    Map<String, String> env = imageConfig.getEnv();
    assertThat(env).isEmpty();
  }

  @Test
  void whenConfigHasNoLabelsThenImageConfigLabelsIsEmpty() throws Exception {
    ImageConfig imageConfig = getMinimalImageConfig();
    Map<String, String> env = imageConfig.getLabels();
    assertThat(env).isEmpty();
  }

  @Test
  void getLabelsReturnsLabels() throws Exception {
    ImageConfig imageConfig = getImageConfig();
    Map<String, String> labels = imageConfig.getLabels();
    assertThat(labels).hasSize(4).contains(entry("io.buildpacks.stack.id", "org.cloudfoundry.stacks.cflinuxfs3"));
  }

  @Test
  void updateWithLabelUpdatesLabels() throws Exception {
    ImageConfig imageConfig = getImageConfig();
    ImageConfig updatedImageConfig = imageConfig
            .copy((update) -> update.withLabel("io.buildpacks.stack.id", "test"));
    assertThat(imageConfig.getLabels()).hasSize(4)
            .contains(entry("io.buildpacks.stack.id", "org.cloudfoundry.stacks.cflinuxfs3"));
    assertThat(updatedImageConfig.getLabels()).hasSize(4).contains(entry("io.buildpacks.stack.id", "test"));
  }

  private ImageConfig getImageConfig() throws IOException {
    return new ImageConfig(getObjectMapper().readTree(getContent("image-config.json")));
  }

  private ImageConfig getMinimalImageConfig() throws IOException {
    return new ImageConfig(getObjectMapper().readTree(getContent("minimal-image-config.json")));
  }

}
