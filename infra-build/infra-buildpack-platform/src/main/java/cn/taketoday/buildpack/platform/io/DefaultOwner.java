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

package cn.taketoday.buildpack.platform.io;

/**
 * Default {@link Owner} implementation.
 *
 * @author Phillip Webb
 * @see Owner#of(long, long)
 */
class DefaultOwner implements Owner {

  private final long uid;

  private final long gid;

  DefaultOwner(long uid, long gid) {
    this.uid = uid;
    this.gid = gid;
  }

  @Override
  public long getUid() {
    return this.uid;
  }

  @Override
  public long getGid() {
    return this.gid;
  }

  @Override
  public String toString() {
    return this.uid + "/" + this.gid;
  }

}
