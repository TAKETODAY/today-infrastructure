/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.config;

import cn.taketoday.lang.Nullable;

/**
 * Assist with configuring {@code HandlerRegistry}'s with path matching options.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/19 20:41
 */
public class PathMatchConfigurer {

  @Nullable
  private Boolean trailingSlashMatch;

  @Nullable
  private Boolean caseSensitiveMatch;

  /**
   * Whether to match to URLs irrespective of their case.
   * If enabled a method mapped to "/users" won't match to "/Users/".
   * <p>The default value is {@code false}.
   */
  public PathMatchConfigurer setUseCaseSensitiveMatch(@Nullable Boolean caseSensitiveMatch) {
    this.caseSensitiveMatch = caseSensitiveMatch;
    return this;
  }

  /**
   * Whether to match to URLs irrespective of the presence of a trailing slash.
   * If enabled a method mapped to "/users" also matches to "/users/".
   * <p>The default value is {@code true}.
   */
  public PathMatchConfigurer setUseTrailingSlashMatch(@Nullable Boolean trailingSlashMatch) {
    this.trailingSlashMatch = trailingSlashMatch;
    return this;
  }

  @Nullable
  public Boolean isUseTrailingSlashMatch() {
    return this.trailingSlashMatch;
  }

  @Nullable
  public Boolean isUseCaseSensitiveMatch() {
    return this.caseSensitiveMatch;
  }

}
