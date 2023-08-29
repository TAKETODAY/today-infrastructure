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

/**
 * Class with nested configuration properties.
 *
 * @author Hrishikesh Joshi
 */
public class ClassWithNestedProperties {

  public static class NestedParentClass {

    private int parentClassProperty = 10;

    public int getParentClassProperty() {
      return this.parentClassProperty;
    }

    public void setParentClassProperty(int parentClassProperty) {
      this.parentClassProperty = parentClassProperty;
    }

  }

  @ConfigurationProperties(prefix = "nestedChildProps")
  public static class NestedChildClass extends NestedParentClass {

    private int childClassProperty = 20;

    public int getChildClassProperty() {
      return this.childClassProperty;
    }

    public void setChildClassProperty(int childClassProperty) {
      this.childClassProperty = childClassProperty;
    }

  }

}
