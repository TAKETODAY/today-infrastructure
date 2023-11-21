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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.origin;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;

/**
 * {@link Origin} for an item loaded from the system environment. Provides access to the
 * original property name.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Madhura Bhave
 * @since 4.0
 */
public class SystemEnvironmentOrigin implements Origin {

  private final String property;

  public SystemEnvironmentOrigin(String property) {
    Assert.notNull(property, "Property name is required");
    Assert.hasText(property, "Property name must not be empty");
    this.property = property;
  }

  public String getProperty() {
    return this.property;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SystemEnvironmentOrigin other = (SystemEnvironmentOrigin) obj;
    return ObjectUtils.nullSafeEquals(this.property, other.property);
  }

  @Override
  public int hashCode() {
    return ObjectUtils.nullSafeHashCode(this.property);
  }

  @Override
  public String toString() {
    return "System Environment Property \"" + this.property + "\"";
  }

}
