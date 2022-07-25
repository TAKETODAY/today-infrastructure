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

package cn.taketoday.tests.sample.objects;

public class TestObject implements ITestObject, ITestInterface, Comparable<Object> {

  private String name;

  private int age;

  private TestObject spouse;

  public TestObject() {
  }

  public TestObject(String name, int age) {
    this.name = name;
    this.age = age;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public int getAge() {
    return this.age;
  }

  @Override
  public void setAge(int age) {
    this.age = age;
  }

  @Override
  public TestObject getSpouse() {
    return this.spouse;
  }

  @Override
  public void setSpouse(TestObject spouse) {
    this.spouse = spouse;
  }

  @Override
  public void absquatulate() {
  }

  @Override
  public int compareTo(Object o) {
    if (this.name != null && o instanceof TestObject) {
      return this.name.compareTo(((TestObject) o).getName());
    }
    else {
      return 1;
    }
  }
}
