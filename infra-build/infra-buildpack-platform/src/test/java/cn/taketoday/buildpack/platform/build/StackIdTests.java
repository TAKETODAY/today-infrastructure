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

import java.util.Collections;

import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link StackId}.
 *
 * @author Phillip Webb
 */
class StackIdTests {

  @Test
  void fromImageWhenImageIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> StackId.fromImage(null))
            .withMessage("Image must not be null");
  }

  @Test
  void fromImageWhenLabelIsMissingThrowsException() {
    Image image = mock(Image.class);
    ImageConfig imageConfig = mock(ImageConfig.class);
    given(image.getConfig()).willReturn(imageConfig);
    assertThatIllegalStateException().isThrownBy(() -> StackId.fromImage(image))
            .withMessage("Missing 'io.buildpacks.stack.id' stack label");
  }

  @Test
  void fromImageCreatesStackId() {
    Image image = mock(Image.class);
    ImageConfig imageConfig = mock(ImageConfig.class);
    given(image.getConfig()).willReturn(imageConfig);
    given(imageConfig.getLabels()).willReturn(Collections.singletonMap("io.buildpacks.stack.id", "test"));
    StackId stackId = StackId.fromImage(image);
    assertThat(stackId).hasToString("test");
  }

  @Test
  void ofCreatesStackId() {
    StackId stackId = StackId.of("test");
    assertThat(stackId).hasToString("test");
  }

  @Test
  void equalsAndHashCode() {
    StackId s1 = StackId.of("a");
    StackId s2 = StackId.of("a");
    StackId s3 = StackId.of("b");
    assertThat(s1).hasSameHashCodeAs(s2);
    assertThat(s1).isEqualTo(s1).isEqualTo(s2).isNotEqualTo(s3);
  }

  @Test
  void toStringReturnsValue() {
    StackId stackId = StackId.of("test");
    assertThat(stackId).hasToString("test");
  }

}
