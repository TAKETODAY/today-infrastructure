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

import java.util.Objects;

import cn.taketoday.lang.Assert;

/**
 * Volume bindings to apply when creating a container.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class Binding {

  private final String value;

  private Binding(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Binding binding)) {
      return false;
    }
    return Objects.equals(this.value, binding.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.value);
  }

  @Override
  public String toString() {
    return this.value;
  }

  /**
   * Create a {@link Binding} with the specified value containing a host source,
   * container destination, and options.
   *
   * @param value the volume binding value
   * @return a new {@link Binding} instance
   */
  public static Binding of(String value) {
    Assert.notNull(value, "Value is required");
    return new Binding(value);
  }

  /**
   * Create a {@link Binding} from the specified source and destination.
   *
   * @param sourceVolume the volume binding host source
   * @param destination the volume binding container destination
   * @return a new {@link Binding} instance
   */
  public static Binding from(VolumeName sourceVolume, String destination) {
    Assert.notNull(sourceVolume, "SourceVolume is required");
    return from(sourceVolume.toString(), destination);
  }

  /**
   * Create a {@link Binding} from the specified source and destination.
   *
   * @param source the volume binding host source
   * @param destination the volume binding container destination
   * @return a new {@link Binding} instance
   */
  public static Binding from(String source, String destination) {
    Assert.notNull(source, "Source is required");
    Assert.notNull(destination, "Destination is required");
    return new Binding(source + ":" + destination);
  }

}
