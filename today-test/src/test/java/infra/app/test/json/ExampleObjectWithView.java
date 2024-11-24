/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.app.test.json;

import com.fasterxml.jackson.annotation.JsonView;

import infra.util.ObjectUtils;

/**
 * Example object used for serialization/deserialization with view.
 *
 * @author Madhura Bhave
 */
public class ExampleObjectWithView {

  @JsonView(TestView.class)
  private String name;

  private int age;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getAge() {
    return this.age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || obj.getClass() != getClass()) {
      return false;
    }
    ExampleObjectWithView other = (ExampleObjectWithView) obj;
    return ObjectUtils.nullSafeEquals(this.name, other.name) && ObjectUtils.nullSafeEquals(this.age, other.age);
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return this.name + " " + this.age;
  }

  static class TestView {

  }

}
