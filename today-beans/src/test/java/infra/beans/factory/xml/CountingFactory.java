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

import infra.beans.factory.FactoryBean;
import infra.beans.testfixture.beans.TestBean;

/**
 * @author Juergen Hoeller
 */
public class CountingFactory implements FactoryBean<String> {

  private static int factoryBeanInstanceCount = 0;

  /**
   * Clear static state.
   */
  public static void reset() {
    factoryBeanInstanceCount = 0;
  }

  public static int getFactoryBeanInstanceCount() {
    return factoryBeanInstanceCount;
  }

  public CountingFactory() {
    factoryBeanInstanceCount++;
  }

  public void setTestBean(TestBean tb) {
    if (tb.getSpouse() == null) {
      throw new IllegalStateException("TestBean needs to have spouse");
    }
  }

  @Override
  public String getObject() {
    return "myString";
  }

  @Override
  public Class<String> getObjectType() {
    return String.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
