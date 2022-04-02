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

package cn.taketoday.test.web;

import cn.taketoday.util.ObjectUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Person {

  @NotNull
  private String name;

  private double someDouble;

  private boolean someBoolean;

  public Person() {
  }

  public Person(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Person setName(String name) {
    this.name = name;
    return this;
  }

  public double getSomeDouble() {
    return someDouble;
  }

  public Person setSomeDouble(double someDouble) {
    this.someDouble = someDouble;
    return this;
  }

  public boolean isSomeBoolean() {
    return someBoolean;
  }

  public Person setSomeBoolean(boolean someBoolean) {
    this.someBoolean = someBoolean;
    return this;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Person otherPerson)) {
      return false;
    }
    return (ObjectUtils.nullSafeEquals(this.name, otherPerson.name) &&
            ObjectUtils.nullSafeEquals(this.someDouble, otherPerson.someDouble) &&
            ObjectUtils.nullSafeEquals(this.someBoolean, otherPerson.someBoolean));
  }

  @Override
  public int hashCode() {
    return Person.class.hashCode();
  }

  @Override
  public String toString() {
    return "Person [name=" + this.name + ", someDouble=" + this.someDouble
            + ", someBoolean=" + this.someBoolean + "]";
  }

}
