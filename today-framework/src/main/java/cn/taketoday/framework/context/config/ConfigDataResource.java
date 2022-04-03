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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.context.config;

/**
 * A single resource from which {@link ConfigData} can be loaded. Implementations must
 * implement a valid {@link #equals(Object) equals}, {@link #hashCode() hashCode} and
 * {@link #toString() toString} methods.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public abstract class ConfigDataResource {

  private final boolean optional;

  /**
   * Create a new non-optional {@link ConfigDataResource} instance.
   */
  public ConfigDataResource() {
    this(false);
  }

  /**
   * Create a new {@link ConfigDataResource} instance.
   *
   * @param optional if the resource is optional
   */
  protected ConfigDataResource(boolean optional) {
    this.optional = optional;
  }

  boolean isOptional() {
    return this.optional;
  }

}
