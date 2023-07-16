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

import java.io.IOException;
import java.util.Collections;

import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageConfig;
import cn.taketoday.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BuildpackLayersMetadata}.
 *
 * @author Scott Frederick
 */
class BuildpackLayersMetadataTests extends AbstractJsonTests {

  @Test
  void fromImageLoadsMetadata() throws IOException {
    Image image = Image.of(getContent("buildpack-image.json"));
    BuildpackLayersMetadata metadata = BuildpackLayersMetadata.fromImage(image);
    assertThat(metadata.getBuildpack("example/hello-moon", "0.0.3")).extracting("homepage", "layerDiffId")
            .containsExactly("https://github.com/example/tree/main/buildpacks/hello-moon",
                    "sha256:4bfdc8714aee68da6662c43bc28d3b41202c88e915641c356523dabe729814c2");
    assertThat(metadata.getBuildpack("example/hello-world", "0.0.2")).extracting("homepage", "layerDiffId")
            .containsExactly("https://github.com/example/tree/main/buildpacks/hello-world",
                    "sha256:f752fe099c846e501bdc991d1a22f98c055ddc62f01cfc0495fff2c69f8eb940");
    assertThat(metadata.getBuildpack("example/hello-world", "version-does-not-exist")).isNull();
    assertThat(metadata.getBuildpack("id-does-not-exist", "9.9.9")).isNull();
  }

  @Test
  void fromImageWhenImageIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> BuildpackLayersMetadata.fromImage(null))
            .withMessage("Image must not be null");
  }

  @Test
  void fromImageWhenImageConfigIsNullThrowsException() {
    Image image = mock(Image.class);
    assertThatIllegalArgumentException().isThrownBy(() -> BuildpackLayersMetadata.fromImage(image))
            .withMessage("ImageConfig must not be null");
  }

  @Test
  void fromImageConfigWhenLabelIsMissingThrowsException() {
    Image image = mock(Image.class);
    ImageConfig imageConfig = mock(ImageConfig.class);
    given(image.getConfig()).willReturn(imageConfig);
    given(imageConfig.getLabels()).willReturn(Collections.singletonMap("alpha", "a"));
    assertThatIllegalArgumentException().isThrownBy(() -> BuildpackLayersMetadata.fromImage(image))
            .withMessage("No 'io.buildpacks.buildpack.layers' label found in image config labels 'alpha'");
  }

  @Test
  void fromJsonLoadsMetadata() throws IOException {
    BuildpackLayersMetadata metadata = BuildpackLayersMetadata
            .fromJson(getContentAsString("buildpack-layers-metadata.json"));
    assertThat(metadata.getBuildpack("example/hello-moon", "0.0.3")).extracting("name", "homepage", "layerDiffId")
            .containsExactly("Example hello-moon buildpack",
                    "https://github.com/example/tree/main/buildpacks/hello-moon",
                    "sha256:4bfdc8714aee68da6662c43bc28d3b41202c88e915641c356523dabe729814c2");
    assertThat(metadata.getBuildpack("example/hello-world", "0.0.1")).extracting("name", "homepage", "layerDiffId")
            .containsExactly("Example hello-world buildpack",
                    "https://github.com/example/tree/main/buildpacks/hello-world",
                    "sha256:1c90e0b80d92555a0523c9ee6500845328fc39ba9dca9d30a877ff759ffbff28");
    assertThat(metadata.getBuildpack("example/hello-world", "0.0.2")).extracting("name", "homepage", "layerDiffId")
            .containsExactly("Example hello-world buildpack",
                    "https://github.com/example/tree/main/buildpacks/hello-world",
                    "sha256:f752fe099c846e501bdc991d1a22f98c055ddc62f01cfc0495fff2c69f8eb940");
    assertThat(metadata.getBuildpack("example/hello-world", "version-does-not-exist")).isNull();
    assertThat(metadata.getBuildpack("id-does-not-exist", "9.9.9")).isNull();
  }

}
