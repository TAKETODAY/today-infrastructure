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
 * Exception throw if a {@link ConfigDataLocation} is not supported.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
public class UnsupportedConfigDataLocationException extends ConfigDataException {

  private final ConfigDataLocation location;

  /**
   * Create a new {@link UnsupportedConfigDataLocationException} instance.
   *
   * @param location the unsupported location
   */
  UnsupportedConfigDataLocationException(ConfigDataLocation location) {
    super("Unsupported config data location '" + location + "'", null);
    this.location = location;
  }

  /**
   * Return the unsupported location reference.
   *
   * @return the unsupported location reference
   */
  public ConfigDataLocation getLocation() {
    return this.location;
  }

}
