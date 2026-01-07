/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.test.web;

import org.jspecify.annotations.Nullable;

import infra.util.ObjectUtils;
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
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof Person that &&
            ObjectUtils.nullSafeEquals(this.name, that.name) &&
            ObjectUtils.nullSafeEquals(this.someDouble, that.someDouble) &&
            ObjectUtils.nullSafeEquals(this.someBoolean, that.someBoolean)));
  }

  @Override
  public int hashCode() {
    return Person.class.hashCode();
  }

  @Override
  public String toString() {
    return "Person [name=" + this.name + ", someDouble=" + this.someDouble +
            ", someBoolean=" + this.someBoolean + "]";
  }

}
