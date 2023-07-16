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

import java.util.Objects;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;

/**
 * Details of a cache for use by the CNB builder.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Cache {

  /**
   * The format of the cache.
   */
  public enum Format {

    /**
     * A cache stored as a volume in the Docker daemon.
     */
    VOLUME;

  }

  protected final Format format;

  Cache(Format format) {
    this.format = format;
  }

  /**
   * Return the details of the cache if it is a volume cache.
   *
   * @return the cache, or {@code null} if it is not a volume cache
   */
  public Volume getVolume() {
    return (this.format.equals(Format.VOLUME)) ? (Volume) this : null;
  }

  /**
   * Create a new {@code Cache} that uses a volume with the provided name.
   *
   * @param name the cache volume name
   * @return a new cache instance
   */
  public static Cache volume(String name) {
    Assert.notNull(name, "Name must not be null");
    return new Volume(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Cache other = (Cache) obj;
    return Objects.equals(this.format, other.format);
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(this.format);
  }

  /**
   * Details of a cache stored in a Docker volume.
   */
  public static class Volume extends Cache {

    private final String name;

    Volume(String name) {
      super(Format.VOLUME);
      this.name = name;
    }

    public String getName() {
      return this.name;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      if (!super.equals(obj)) {
        return false;
      }
      Volume other = (Volume) obj;
      return Objects.equals(this.name, other.name);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + ObjectUtils.nullSafeHashCode(this.name);
      return result;
    }

  }

}
