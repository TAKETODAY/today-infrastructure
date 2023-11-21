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

import cn.taketoday.buildpack.platform.docker.type.VolumeName;
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
    VOLUME("volume"),

    /**
     * A cache stored as a bind mount.
     */
    BIND("bind mount");

    private final String description;

    Format(String description) {
      this.description = description;
    }

    public String getDescription() {
      return this.description;
    }

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
   * Return the details of the cache if it is a bind cache.
   *
   * @return the cache, or {@code null} if it is not a bind cache
   */
  public Bind getBind() {
    return (this.format.equals(Format.BIND)) ? (Bind) this : null;
  }

  /**
   * Create a new {@code Cache} that uses a volume with the provided name.
   *
   * @param name the cache volume name
   * @return a new cache instance
   */
  public static Cache volume(String name) {
    Assert.notNull(name, "Name is required");
    return new Volume(VolumeName.of(name));
  }

  /**
   * Create a new {@code Cache} that uses a volume with the provided name.
   *
   * @param name the cache volume name
   * @return a new cache instance
   */
  public static Cache volume(VolumeName name) {
    Assert.notNull(name, "Name is required");
    return new Volume(name);
  }

  /**
   * Create a new {@code Cache} that uses a bind mount with the provided source.
   *
   * @param source the cache bind mount source
   * @return a new cache instance
   */
  public static Cache bind(String source) {
    Assert.notNull(source, "Source is required");
    return new Bind(source);
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

    private final VolumeName name;

    Volume(VolumeName name) {
      super(Format.VOLUME);
      this.name = name;
    }

    public String getName() {
      return this.name.toString();
    }

    public VolumeName getVolumeName() {
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

    @Override
    public String toString() {
      return this.format.getDescription() + " '" + this.name + "'";
    }

  }

  /**
   * Details of a cache stored in a bind mount.
   */
  public static class Bind extends Cache {

    private final String source;

    Bind(String source) {
      super(Format.BIND);
      this.source = source;
    }

    public String getSource() {
      return this.source;
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
      Bind other = (Bind) obj;
      return Objects.equals(this.source, other.source);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + ObjectUtils.nullSafeHashCode(this.source);
      return result;
    }

    @Override
    public String toString() {
      return this.format.getDescription() + " '" + this.source + "'";
    }

  }

}
