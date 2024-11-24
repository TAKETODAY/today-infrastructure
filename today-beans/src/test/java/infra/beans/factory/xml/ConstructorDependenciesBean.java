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

package infra.beans.factory.xml;

import java.beans.ConstructorProperties;
import java.io.Serializable;

import infra.beans.testfixture.beans.IndexedTestBean;
import infra.beans.testfixture.beans.TestBean;

/**
 * Simple bean used to check constructor dependency checking.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/6/20 12:57
 */
@SuppressWarnings("serial")
public class ConstructorDependenciesBean implements Serializable {

  private int age;

  private String name;

  private TestBean spouse1;

  private TestBean spouse2;

  private IndexedTestBean other;

  public ConstructorDependenciesBean(int age) {
    this.age = age;
  }

  public ConstructorDependenciesBean(String name) {
    this.name = name;
  }

  public ConstructorDependenciesBean(TestBean spouse1) {
    this.spouse1 = spouse1;
  }

  public ConstructorDependenciesBean(TestBean spouse1, TestBean spouse2) {
    this.spouse1 = spouse1;
    this.spouse2 = spouse2;
  }

  @ConstructorProperties({ "spouse", "otherSpouse", "myAge" })
  public ConstructorDependenciesBean(TestBean spouse1, TestBean spouse2, int age) {
    this.spouse1 = spouse1;
    this.spouse2 = spouse2;
    this.age = age;
  }

  public ConstructorDependenciesBean(TestBean spouse1, TestBean spouse2, IndexedTestBean other) {
    this.spouse1 = spouse1;
    this.spouse2 = spouse2;
    this.other = other;
  }

  public int getAge() {
    return age;
  }

  public String getName() {
    return name;
  }

  public TestBean getSpouse1() {
    return spouse1;
  }

  public TestBean getSpouse2() {
    return spouse2;
  }

  public IndexedTestBean getOther() {
    return other;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public void setName(String name) {
    this.name = name;
  }

}
