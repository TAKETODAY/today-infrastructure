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

import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageConfig;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * A Stack ID.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class StackId {

  private static final String LABEL_NAME = "io.buildpacks.stack.id";

  private final String value;

  StackId(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return this.value.equals(((StackId) obj).value);
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }

  @Override
  public String toString() {
    return this.value;
  }

  /**
   * Factory method to create a {@link StackId} from an {@link Image}.
   *
   * @param image the source image
   * @return the extracted stack ID
   */
  static StackId fromImage(Image image) {
    Assert.notNull(image, "Image must not be null");
    return fromImageConfig(image.getConfig());
  }

  /**
   * Factory method to create a {@link StackId} from an {@link ImageConfig}.
   *
   * @param imageConfig the source image config
   * @return the extracted stack ID
   */
  private static StackId fromImageConfig(ImageConfig imageConfig) {
    String value = imageConfig.getLabels().get(LABEL_NAME);
    Assert.state(StringUtils.hasText(value), () -> "Missing '" + LABEL_NAME + "' stack label");
    return new StackId(value);
  }

  /**
   * Factory method to create a {@link StackId} with a given value.
   *
   * @param value the stack ID value
   * @return a new stack ID instance
   */
  static StackId of(String value) {
    Assert.hasText(value, "Value must not be empty");
    return new StackId(value);
  }

}
