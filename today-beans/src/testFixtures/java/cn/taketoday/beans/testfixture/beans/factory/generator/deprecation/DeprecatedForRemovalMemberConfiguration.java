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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.testfixture.beans.factory.generator.deprecation;

/**
 * A class with deprecated members for removal to test various use cases.
 *
 * @author Stephane Nicoll
 */
public class DeprecatedForRemovalMemberConfiguration {

  @Deprecated(forRemoval = true)
  public String deprecatedString() {
    return "deprecated";
  }

  @SuppressWarnings("removal")
  public String deprecatedParameter(DeprecatedForRemovalBean bean) {
    return bean.toString();
  }

}
