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

package cn.taketoday.beans.testfixture.beans;

/**
 * @author Costin Leau
 */
public class DummyBean {

  private Object value;
  private String name;
  private int age;
  private TestBean spouse;

  public DummyBean(Object value) {
    this.value = value;
  }

  public DummyBean(String name, int age) {
    this.name = name;
    this.age = age;
  }

  public DummyBean(int ageRef, String nameRef) {
    this.name = nameRef;
    this.age = ageRef;
  }

  public DummyBean(String name, TestBean spouse) {
    this.name = name;
    this.spouse = spouse;
  }

  public DummyBean(String name, Object value, int age) {
    this.name = name;
    this.value = value;
    this.age = age;
  }

  public Object getValue() {
    return value;
  }

  public String getName() {
    return name;
  }

  public int getAge() {
    return age;
  }

  public TestBean getSpouse() {
    return spouse;
  }

}
