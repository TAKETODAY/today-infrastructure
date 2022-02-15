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

package cn.taketoday.web.resource;

import cn.taketoday.core.io.Resource;

/**
 * A {@code VersionStrategy} that relies on a fixed version applied as a request
 * path prefix, e.g. reduced SHA, version name, release date, etc.
 *
 * <p>This is useful for example when {@link ContentVersionStrategy} cannot be
 * used such as when using JavaScript module loaders which are in charge of
 * loading the JavaScript resources and need to know their relative paths.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see VersionResourceResolver
 * @since 4.0
 */
public class FixedVersionStrategy extends AbstractVersionStrategy {

  private final String version;

  /**
   * Create a new FixedVersionStrategy with the given version string.
   *
   * @param version the fixed version string to use
   */
  public FixedVersionStrategy(String version) {
    super(new PrefixVersionPathStrategy(version));
    this.version = version;
  }

  @Override
  public String getResourceVersion(Resource resource) {
    return this.version;
  }

}
