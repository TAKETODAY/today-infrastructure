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
 * Tests for {@link BuildpackMetadata}.
 *
 * @author Scott Frederick
 */
class BuildpackMetadataTests extends AbstractJsonTests {

  @Test
  void fromImageLoadsMetadata() throws IOException {
    Image image = Image.of(getContent("buildpack-image.json"));
    BuildpackMetadata metadata = BuildpackMetadata.fromImage(image);
    assertThat(metadata.getId()).isEqualTo("example/hello-universe");
    assertThat(metadata.getVersion()).isEqualTo("0.0.1");
  }

  @Test
  void fromImageWhenImageIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> BuildpackMetadata.fromImage(null))
            .withMessage("Image is required");
  }

  @Test
  void fromImageWhenImageConfigIsNullThrowsException() {
    Image image = mock(Image.class);
    assertThatIllegalArgumentException().isThrownBy(() -> BuildpackMetadata.fromImage(image))
            .withMessage("ImageConfig is required");
  }

  @Test
  void fromImageConfigWhenLabelIsMissingThrowsException() {
    Image image = mock(Image.class);
    ImageConfig imageConfig = mock(ImageConfig.class);
    given(image.getConfig()).willReturn(imageConfig);
    given(imageConfig.getLabels()).willReturn(Collections.singletonMap("alpha", "a"));
    assertThatIllegalArgumentException().isThrownBy(() -> BuildpackMetadata.fromImage(image))
            .withMessage("No 'io.buildpacks.buildpackage.metadata' label found in image config labels 'alpha'");
  }

  @Test
  void fromJsonLoadsMetadata() throws IOException {
    BuildpackMetadata metadata = BuildpackMetadata.fromJson(getContentAsString("buildpack-metadata.json"));
    assertThat(metadata.getId()).isEqualTo("example/hello-universe");
    assertThat(metadata.getVersion()).isEqualTo("0.0.1");
    assertThat(metadata.getHomepage()).isEqualTo("https://github.com/example/tree/main/buildpacks/hello-universe");
  }

}
