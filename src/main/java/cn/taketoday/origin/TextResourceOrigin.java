/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.origin;

import java.io.IOException;
import java.util.Objects;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link Origin} for an item loaded from a text resource. Provides access to the original
 * {@link Resource} that loaded the text and a {@link Location} within it. If the provided
 * resource provides an {@link Origin} (e.g. it is an {@link OriginTrackedResource}), then
 * it will be used as the {@link Origin#getParent() origin parent}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see OriginTrackedResource
 * @since 4.0
 */
public class TextResourceOrigin implements Origin {

  private final Resource resource;

  private final Location location;

  public TextResourceOrigin(Resource resource, Location location) {
    this.resource = resource;
    this.location = location;
  }

  /**
   * Return the resource where the property originated.
   *
   * @return the text resource or {@code null}
   */
  public Resource getResource() {
    return this.resource;
  }

  /**
   * Return the location of the property within the source (if known).
   *
   * @return the location or {@code null}
   */
  public Location getLocation() {
    return this.location;
  }

  @Override
  public Origin getParent() {
    return Origin.from(this.resource);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (obj instanceof TextResourceOrigin other) {
      return Objects.equals(this.resource, other.resource)
              && Objects.equals(this.location, other.location);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + ObjectUtils.nullSafeHashCode(this.resource);
    result = 31 * result + ObjectUtils.nullSafeHashCode(this.location);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(getResourceDescription(this.resource));
    if (this.location != null) {
      result.append(" - ").append(this.location);
    }
    return result.toString();
  }

  private String getResourceDescription(Resource resource) {
    if (resource instanceof OriginTrackedResource) {
      return getResourceDescription(((OriginTrackedResource) resource).getResource());
    }
    if (resource == null) {
      return "unknown resource [?]";
    }
    if (resource instanceof ClassPathResource) {
      return getResourceDescription((ClassPathResource) resource);
    }
    return resource.toString();
  }

  private String getResourceDescription(ClassPathResource resource) {
    try {
      JarUri jarUri = JarUri.from(resource.getURI());
      if (jarUri != null) {
        return jarUri.getDescription(resource.toString());
      }
    }
    catch (IOException ignored) { }
    return resource.toString();
  }

  /**
   * A location (line and column number) within the resource.
   */
  public static final class Location {

    private final int line;

    private final int column;

    /**
     * Create a new {@link Location} instance.
     *
     * @param line the line number (zero indexed)
     * @param column the column number (zero indexed)
     */
    public Location(int line, int column) {
      this.line = line;
      this.column = column;
    }

    /**
     * Return the line of the text resource where the property originated.
     *
     * @return the line number (zero indexed)
     */
    public int getLine() {
      return this.line;
    }

    /**
     * Return the column of the text resource where the property originated.
     *
     * @return the column number (zero indexed)
     */
    public int getColumn() {
      return this.column;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Location other = (Location) obj;
      return this.line == other.line
              && this.column == other.column;
    }

    @Override
    public int hashCode() {
      return (31 * this.line) + this.column;
    }

    @Override
    public String toString() {
      return (this.line + 1) + ":" + (this.column + 1);
    }

  }

}
