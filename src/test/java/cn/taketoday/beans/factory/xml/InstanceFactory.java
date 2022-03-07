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

package cn.taketoday.beans.factory.xml;

import cn.taketoday.beans.testfixture.beans.FactoryMethods;
import cn.taketoday.beans.testfixture.beans.TestBean;

/**
 * Test class for Spring's ability to create objects using
 * static factory methods, rather than constructors.
 *
 * @author Rod Johnson
 */
public class InstanceFactory {

  protected static int count = 0;

  private String factoryBeanProperty;

  public InstanceFactory() {
    count++;
  }

  public void setFactoryBeanProperty(String s) {
    this.factoryBeanProperty = s;
  }

  public String getFactoryBeanProperty() {
    return this.factoryBeanProperty;
  }

  public FactoryMethods defaultInstance() {
    TestBean tb = new TestBean();
    tb.setName(this.factoryBeanProperty);
    return FactoryMethods.newInstance(tb);
  }

  /**
   * Note that overloaded methods are supported.
   */
  public FactoryMethods newInstance(TestBean tb) {
    return FactoryMethods.newInstance(tb);
  }

  public FactoryMethods newInstance(TestBean tb, int num, String name) {
    return FactoryMethods.newInstance(tb, num, name);
  }

}
