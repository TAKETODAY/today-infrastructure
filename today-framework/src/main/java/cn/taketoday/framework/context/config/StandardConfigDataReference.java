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

import cn.taketoday.framework.env.PropertySourceLoader;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * A reference expanded from the original {@link ConfigDataLocation} that can ultimately
 * be resolved to one or more {@link StandardConfigDataResource resources}.
 *
 * @author Phillip Webb
 * @since 4.0
 */
class StandardConfigDataReference {

  private final ConfigDataLocation configDataLocation;

  private final String resourceLocation;

  @Nullable
  private final String directory;

  @Nullable
  private final String profile;

  private final PropertySourceLoader propertySourceLoader;

  /**
   * Create a new {@link StandardConfigDataReference} instance.
   *
   * @param configDataLocation the original location passed to the resolver
   * @param directory the directory of the resource or {@code null} if the reference is
   * to a file
   * @param root the root of the resource location
   * @param profile the profile being loaded
   * @param extension the file extension for the resource
   * @param propertySourceLoader the property source loader that should be used for this
   * reference
   */
  StandardConfigDataReference(ConfigDataLocation configDataLocation, @Nullable String directory, String root, @Nullable String profile,
          @Nullable String extension, PropertySourceLoader propertySourceLoader) {
    this.configDataLocation = configDataLocation;
    String profileSuffix = (StringUtils.hasText(profile)) ? "-" + profile : "";
    this.resourceLocation = root + profileSuffix + ((extension != null) ? "." + extension : "");
    this.directory = directory;
    this.profile = profile;
    this.propertySourceLoader = propertySourceLoader;
  }

  ConfigDataLocation getConfigDataLocation() {
    return this.configDataLocation;
  }

  String getResourceLocation() {
    return this.resourceLocation;
  }

  boolean isMandatoryDirectory() {
    return !this.configDataLocation.isOptional() && this.directory != null;
  }

  @Nullable
  String getDirectory() {
    return this.directory;
  }

  @Nullable
  String getProfile() {
    return this.profile;
  }

  boolean isSkippable() {
    return this.configDataLocation.isOptional() || this.directory != null || this.profile != null;
  }

  PropertySourceLoader getPropertySourceLoader() {
    return this.propertySourceLoader;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (getClass() != obj.getClass())) {
      return false;
    }
    StandardConfigDataReference other = (StandardConfigDataReference) obj;
    return this.resourceLocation.equals(other.resourceLocation);
  }

  @Override
  public int hashCode() {
    return this.resourceLocation.hashCode();
  }

  @Override
  public String toString() {
    return this.resourceLocation;
  }

}
