/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jmx.support;

import org.junit.jupiter.api.Test;

import javax.management.DynamicMBean;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.beans.BeanWrapperImpl;
import cn.taketoday.jmx.IJmxTestBean;
import cn.taketoday.jmx.JmxTestBean;
import cn.taketoday.jmx.MBeanTestUtils;
import cn.taketoday.jmx.export.TestDynamicMBean;
import cn.taketoday.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JmxUtils}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class JmxUtilsTests {

  @Test
  void isMBean() {
    // Correctly returns true for a class
    assertThat(JmxUtils.isMBean(JmxClass.class)).isTrue();

    // Correctly returns false since JmxUtils won't navigate to the extended interface
    assertThat(JmxUtils.isMBean(SpecializedJmxInterface.class)).isFalse();

    // Incorrectly returns true since it doesn't detect that this is an interface
    assertThat(JmxUtils.isMBean(JmxInterface.class)).isFalse();
  }

  @Test
  void isMBeanWithDynamicMBean() {
    DynamicMBean mbean = new TestDynamicMBean();
    assertThat(JmxUtils.isMBean(mbean.getClass())).as("Dynamic MBean not detected correctly").isTrue();
  }

  @Test
  void isMBeanWithStandardMBeanWrapper() throws NotCompliantMBeanException {
    StandardMBean mbean = new StandardMBean(new JmxTestBean(), IJmxTestBean.class);
    assertThat(JmxUtils.isMBean(mbean.getClass())).as("Standard MBean not detected correctly").isTrue();
  }

  @Test
  void isMBeanWithStandardMBeanInherited() throws NotCompliantMBeanException {
    StandardMBean mbean = new StandardMBeanImpl();
    assertThat(JmxUtils.isMBean(mbean.getClass())).as("Standard MBean not detected correctly").isTrue();
  }

  @Test
  void notAnMBean() {
    assertThat(JmxUtils.isMBean(Object.class)).as("Object incorrectly identified as an MBean").isFalse();
  }

  @Test
  void simpleMBean() {
    Foo foo = new Foo();
    assertThat(JmxUtils.isMBean(foo.getClass())).as("Simple MBean not detected correctly").isTrue();
  }

  @Test
  void simpleMXBean() {
    FooX foo = new FooX();
    assertThat(JmxUtils.isMBean(foo.getClass())).as("Simple MXBean not detected correctly").isTrue();
  }

  @Test
  void simpleMBeanThroughInheritance() {
    Bar bar = new Bar();
    Abc abc = new Abc();
    assertThat(JmxUtils.isMBean(bar.getClass())).as("Simple MBean (through inheritance) not detected correctly").isTrue();
    assertThat(JmxUtils.isMBean(abc.getClass())).as("Simple MBean (through 2 levels of inheritance) not detected correctly").isTrue();
  }

  @Test
  void getAttributeNameWithStrictCasing() {
    BeanProperty pd = new BeanWrapperImpl(AttributeTestBean.class).getBeanProperty("name");
    String attributeName = JmxUtils.getAttributeName(pd, true);
    assertThat(attributeName).as("Incorrect casing on attribute name").isEqualTo("Name");
  }

  @Test
  void getAttributeNameWithoutStrictCasing() {
    BeanProperty pd = new BeanWrapperImpl(AttributeTestBean.class).getBeanProperty("name");
    String attributeName = JmxUtils.getAttributeName(pd, false);
    assertThat(attributeName).as("Incorrect casing on attribute name").isEqualTo("name");
  }

  @Test
  void appendIdentityToObjectName() throws MalformedObjectNameException {
    ObjectName objectName = ObjectNameManager.getInstance("spring:type=Test");
    Object managedResource = new Object();
    ObjectName uniqueName = JmxUtils.appendIdentityToObjectName(objectName, managedResource);

    String typeProperty = "type";

    assertThat(uniqueName.getDomain()).as("Domain of transformed name is incorrect").isEqualTo(objectName.getDomain());
    assertThat(uniqueName.getKeyProperty("type")).as("Type key is incorrect").isEqualTo(objectName.getKeyProperty(typeProperty));
    assertThat(uniqueName.getKeyProperty(JmxUtils.IDENTITY_OBJECT_NAME_KEY)).as("Identity key is incorrect").isEqualTo(ObjectUtils.getIdentityHexString(managedResource));
  }

  @Test
  void locatePlatformMBeanServer() {
    MBeanServer server = null;
    try {
      server = JmxUtils.locateMBeanServer();
      assertThat(server).isNotNull();
    }
    finally {
      if (server != null) {
        MBeanTestUtils.releaseMBeanServer(server);
      }
    }
  }

  public static class AttributeTestBean {

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  public static class StandardMBeanImpl extends StandardMBean implements IJmxTestBean {

    public StandardMBeanImpl() throws NotCompliantMBeanException {
      super(IJmxTestBean.class);
    }

    @Override
    public int add(int x, int y) {
      return 0;
    }

    @Override
    public long myOperation() {
      return 0;
    }

    @Override
    public int getAge() {
      return 0;
    }

    @Override
    public void setAge(int age) {
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public void dontExposeMe() {
    }
  }

  public interface FooMBean {

    String getName();
  }

  public static class Foo implements FooMBean {

    @Override
    public String getName() {
      return "Rob Harrop";
    }
  }

  public interface FooMXBean {

    String getName();
  }

  public static class FooX implements FooMXBean {

    @Override
    public String getName() {
      return "Rob Harrop";
    }
  }

  public static class Bar extends Foo {
  }

  public static class Abc extends Bar {
  }

  private interface JmxInterfaceMBean {
  }

  private interface JmxInterface extends JmxInterfaceMBean {
  }

  private interface SpecializedJmxInterface extends JmxInterface {
  }

  private interface JmxClassMBean {
  }

  private static class JmxClass implements JmxClassMBean {
  }

}
