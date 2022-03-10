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

package cn.taketoday.jmx.export;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class TestDynamicMBean implements DynamicMBean {

  public void setFailOnInit(boolean failOnInit) {
    if (failOnInit) {
      throw new IllegalArgumentException("Failing on initialization");
    }
  }

  @Override
  public Object getAttribute(String attribute) {
    if ("Name".equals(attribute)) {
      return "Rob Harrop";
    }
    return null;
  }

  @Override
  public void setAttribute(Attribute attribute) {
  }

  @Override
  public AttributeList getAttributes(String[] attributes) {
    return null;
  }

  @Override
  public AttributeList setAttributes(AttributeList attributes) {
    return null;
  }

  @Override
  public Object invoke(String actionName, Object[] params, String[] signature) {
    return null;
  }

  @Override
  public MBeanInfo getMBeanInfo() {
    MBeanAttributeInfo attr = new MBeanAttributeInfo("name", "java.lang.String", "", true, false, false);
    return new MBeanInfo(
            TestDynamicMBean.class.getName(), "",
            new MBeanAttributeInfo[] { attr },
            new MBeanConstructorInfo[0],
            new MBeanOperationInfo[0],
            new MBeanNotificationInfo[0]);
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof TestDynamicMBean);
  }

  @Override
  public int hashCode() {
    return TestDynamicMBean.class.hashCode();
  }

}
