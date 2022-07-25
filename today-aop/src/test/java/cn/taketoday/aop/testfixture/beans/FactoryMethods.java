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

package cn.taketoday.aop.testfixture.beans;

import java.util.Collections;
import java.util.List;

/**
 * Test class for Framework's ability to create objects using static
 * factory methods, rather than constructors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class FactoryMethods {

  public static FactoryMethods nullInstance() {
    return null;
  }

  public static FactoryMethods defaultInstance() {
    TestBean tb = new TestBean();
    tb.setName("defaultInstance");
    return new FactoryMethods(tb, "default", 0);
  }

  /**
   * Note that overloaded methods are supported.
   */
  public static FactoryMethods newInstance(TestBean tb) {
    return new FactoryMethods(tb, "default", 0);
  }

  public static FactoryMethods newInstance(TestBean tb, int num, String name) {
    if (name == null) {
      throw new IllegalStateException("Should never be called with null value");
    }
    return new FactoryMethods(tb, name, num);
  }

  static ExtendedFactoryMethods newInstance(TestBean tb, int num, Integer something) {
    if (something != null) {
      throw new IllegalStateException("Should never be called with non-null value");
    }
    return new ExtendedFactoryMethods(tb, null, num);
  }

  @SuppressWarnings("unused")
  private static List<?> listInstance() {
    return Collections.EMPTY_LIST;
  }

  private int num = 0;
  private String name = "default";
  private final TestBean tb;
  private String stringValue;

  /**
   * Constructor is private: not for use outside this class,
   * even by IoC container.
   */
  private FactoryMethods(TestBean tb, String name, int num) {
    this.tb = tb;
    this.name = name;
    this.num = num;
  }

  public void setStringValue(String stringValue) {
    this.stringValue = stringValue;
  }

  public String getStringValue() {
    return this.stringValue;
  }

  public TestBean getTestBean() {
    return this.tb;
  }

  protected TestBean protectedGetTestBean() {
    return this.tb;
  }

  @SuppressWarnings("unused")
  private TestBean privateGetTestBean() {
    return this.tb;
  }

  public int getNum() {
    return num;
  }

  public String getName() {
    return name;
  }

  /**
   * Set via Setter Injection once instance is created.
   */
  public void setName(String name) {
    this.name = name;
  }

  public static class ExtendedFactoryMethods extends FactoryMethods {

    ExtendedFactoryMethods(TestBean tb, String name, int num) {
      super(tb, name, num);
    }
  }

}
