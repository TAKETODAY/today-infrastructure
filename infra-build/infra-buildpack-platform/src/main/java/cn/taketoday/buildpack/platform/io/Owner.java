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
 * A user and group ID that can be used to indicate file ownership.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public interface Owner {

  /**
   * Owner for root ownership.
   */
  Owner ROOT = Owner.of(0, 0);

  /**
   * Return the user identifier (UID) of the owner.
   *
   * @return the user identifier
   */
  long getUid();

  /**
   * Return the group identifier (GID) of the owner.
   *
   * @return the group identifier
   */
  long getGid();

  /**
   * Factory method to create a new {@link Owner} with specified user/group identifier.
   *
   * @param uid the user identifier
   * @param gid the group identifier
   * @return a new {@link Owner} instance
   */
  static Owner of(long uid, long gid) {
    return new DefaultOwner(uid, gid);
  }

}
