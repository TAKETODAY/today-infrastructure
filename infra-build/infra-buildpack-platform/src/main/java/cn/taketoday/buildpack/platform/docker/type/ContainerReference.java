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

import cn.taketoday.lang.Assert;

/**
 * A reference to a Docker container.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public final class ContainerReference {

  private final String value;

  private ContainerReference(String value) {
    Assert.hasText(value, "Value must not be empty");
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
    ContainerReference other = (ContainerReference) obj;
    return this.value.equals(other.value);
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
   * Factory method to create a {@link ContainerReference} with a specific value.
   *
   * @param value the container reference value
   * @return a new container reference instance
   */
  public static ContainerReference of(String value) {
    return new ContainerReference(value);
  }

}
