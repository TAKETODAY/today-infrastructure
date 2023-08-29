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

package cn.taketoday.context.properties.sample.simple;

import cn.taketoday.context.properties.sample.ConfigurationProperties;
import cn.taketoday.context.properties.sample.DeprecatedConfigurationProperty;

/**
 * Configuration properties with a single deprecated element.
 *
 * @author Phillip Webb
 */
@ConfigurationProperties("singledeprecated")
public class DeprecatedSingleProperty {

  private String newName;

  @Deprecated
  @DeprecatedConfigurationProperty(reason = "renamed", replacement = "singledeprecated.new-name", since = "1.2.3")
  public String getName() {
    return getNewName();
  }

  @Deprecated
  public void setName(String name) {
    setNewName(name);
  }

  public String getNewName() {
    return this.newName;
  }

  public void setNewName(String newName) {
    this.newName = newName;
  }

}
