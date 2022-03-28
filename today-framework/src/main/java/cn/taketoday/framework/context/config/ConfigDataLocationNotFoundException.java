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

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.origin.Origin;

/**
 * {@link ConfigDataNotFoundException} thrown when a {@link ConfigDataLocation} cannot be
 * found.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public class ConfigDataLocationNotFoundException extends ConfigDataNotFoundException {

  private final ConfigDataLocation location;

  /**
   * Create a new {@link ConfigDataLocationNotFoundException} instance.
   *
   * @param location the location that could not be found
   */
  public ConfigDataLocationNotFoundException(ConfigDataLocation location) {
    this(location, null);
  }

  /**
   * Create a new {@link ConfigDataLocationNotFoundException} instance.
   *
   * @param location the location that could not be found
   * @param cause the exception cause
   */
  public ConfigDataLocationNotFoundException(ConfigDataLocation location, @Nullable Throwable cause) {
    this(location, getMessage(location), cause);
  }

  /**
   * Create a new {@link ConfigDataLocationNotFoundException} instance.
   *
   * @param location the location that could not be found
   * @param message the exception message
   * @param cause the exception cause
   */
  public ConfigDataLocationNotFoundException(ConfigDataLocation location, @Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
    Assert.notNull(location, "Location must not be null");
    this.location = location;
  }

  /**
   * Return the location that could not be found.
   *
   * @return the location
   */
  public ConfigDataLocation getLocation() {
    return this.location;
  }

  @Override
  public Origin getOrigin() {
    return Origin.from(this.location);
  }

  @Override
  public String getReferenceDescription() {
    return getReferenceDescription(this.location);
  }

  private static String getMessage(ConfigDataLocation location) {
    return String.format("Config data %s cannot be found", getReferenceDescription(location));
  }

  private static String getReferenceDescription(ConfigDataLocation location) {
    return String.format("location '%s'", location);
  }

}
