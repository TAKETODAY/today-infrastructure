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

/**
 * Parent for {@link HierarchicalProperties}.
 *
 * @author Stephane Nicoll
 */
public abstract class HierarchicalPropertiesParent extends HierarchicalPropertiesGrandparent {

  private String second = "two";

  public String getSecond() {
    return this.second;
  }

  public void setSecond(String second) {
    this.second = second;
  }

  // Overridden properties should belong to this class, not
  // HierarchicalPropertiesGrandparent

  @Override
  public String getFirst() {
    return super.getFirst();
  }

  @Override
  public void setFirst(String first) {
    super.setFirst(first);
  }

}
