/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.core.io;

import cn.taketoday.lang.Assert;

/**
 * Region of a {@link Resource} implementation, materialized by a {@code position}
 * within the {@link Resource} and a byte {@code count} for the length of that region.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/6 12:28
 */
public class ResourceRegion {

  private final long count;

  private final long position;

  private final Resource resource;

  /**
   * Create a new {@code ResourceRegion} from a given {@link Resource}.
   * This region of a resource is represented by a start {@code position}
   * and a byte {@code count} within the given {@code Resource}.
   *
   * @param resource a Resource
   * @param position the start position of the region in that resource
   * @param count the byte count of the region in that resource
   */
  public ResourceRegion(Resource resource, long position, long count) {
    Assert.notNull(resource, "Resource is required");
    Assert.isTrue(position >= 0, "'position' must be greater than or equal to 0");
    Assert.isTrue(count >= 0, "'count' must be greater than or equal to 0");
    this.resource = resource;
    this.position = position;
    this.count = count;
  }

  /**
   * Return the underlying {@link Resource} for this {@code ResourceRegion}.
   */
  public Resource getResource() {
    return this.resource;
  }

  /**
   * Return the start position of this region in the underlying {@link Resource}.
   */
  public long getPosition() {
    return this.position;
  }

  /**
   * Return the byte count of this region in the underlying {@link Resource}.
   */
  public long getCount() {
    return this.count;
  }

}
