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

import java.util.Map;

import cn.taketoday.buildpack.platform.io.Owner;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * The {@link Owner} that should perform the build.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BuildOwner implements Owner {

  private static final String USER_PROPERTY_NAME = "CNB_USER_ID";

  private static final String GROUP_PROPERTY_NAME = "CNB_GROUP_ID";

  private final long uid;

  private final long gid;

  BuildOwner(Map<String, String> env) {
    this.uid = getValue(env, USER_PROPERTY_NAME);
    this.gid = getValue(env, GROUP_PROPERTY_NAME);
  }

  BuildOwner(long uid, long gid) {
    this.uid = uid;
    this.gid = gid;
  }

  private long getValue(Map<String, String> env, String name) {
    String value = env.get(name);
    Assert.state(StringUtils.hasText(value),
            () -> "Missing '" + name + "' value from the builder environment '" + env + "'");
    try {
      return Long.parseLong(value);
    }
    catch (NumberFormatException ex) {
      throw new IllegalStateException(
              "Malformed '" + name + "' value '" + value + "' in the builder environment '" + env + "'", ex);
    }
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

  /**
   * Factory method to create the {@link BuildOwner} by inspecting the image env for
   * {@code CNB_USER_ID}/{@code CNB_GROUP_ID} variables.
   *
   * @param env the env to parse
   * @return a {@link BuildOwner} instance extracted from the env
   * @throws IllegalStateException if the env does not contain the correct CNB variables
   */
  static BuildOwner fromEnv(Map<String, String> env) {
    Assert.notNull(env, "Env must not be null");
    return new BuildOwner(env);
  }

  /**
   * Factory method to create a new {@link BuildOwner} with specified user/group
   * identifier.
   *
   * @param uid the user identifier
   * @param gid the group identifier
   * @return a new {@link BuildOwner} instance
   */
  static BuildOwner of(long uid, long gid) {
    return new BuildOwner(uid, gid);
  }

}
