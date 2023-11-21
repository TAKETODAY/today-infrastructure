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

import cn.taketoday.lang.Assert;

/**
 * Identifying information about the tooling that created a builder.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Creator {

  private final String version;

  Creator(String version) {
    this.version = version;
  }

  /**
   * Return the name of the builder creator.
   *
   * @return the name
   */
  public String getName() {
    return "Infra Application";
  }

  /**
   * Return the version of the builder creator.
   *
   * @return the version
   */
  public String getVersion() {
    return this.version;
  }

  /**
   * Create a new {@code Creator} using the provided version.
   *
   * @param version the creator version
   * @return a new creator instance
   */
  public static Creator withVersion(String version) {
    Assert.notNull(version, "Version is required");
    return new Creator(version);
  }

  @Override
  public String toString() {
    return getName() + " version " + getVersion();
  }

}
