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

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.jmx.AbstractMBeanServerTests;
import cn.taketoday.jmx.JmxTestBean;
import cn.taketoday.jmx.access.NotificationListenerRegistrar;
import cn.taketoday.jmx.export.naming.SelfNaming;
import cn.taketoday.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rob Harrop
 * @author Mark Fisher
 * @author Sam Brannen
 */
public class NotificationListenerTests extends AbstractMBeanServerTests {

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testRegisterNotificationListenerForMBean() throws Exception {
    ObjectName objectName = ObjectName.getInstance("spring:name=Test");
    JmxTestBean bean = new JmxTestBean();

    Map<String, Object> beans = new HashMap<>();
    beans.put(objectName.getCanonicalName(), bean);

    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

    Map notificationListeners = new HashMap();
    notificationListeners.put(objectName, listener);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    exporter.setNotificationListenerMappings(notificationListeners);
    start(exporter);

    // update the attribute
    String attributeName = "Name";
    server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
    assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testRegisterNotificationListenerWithWildcard() throws Exception {
    ObjectName objectName = ObjectName.getInstance("spring:name=Test");
    JmxTestBean bean = new JmxTestBean();

    Map<String, Object> beans = new HashMap<>();
    beans.put(objectName.getCanonicalName(), bean);

    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

    Map notificationListeners = new HashMap();
    notificationListeners.put("*", listener);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    exporter.setNotificationListenerMappings(notificationListeners);
    start(exporter);

    // update the attribute
    String attributeName = "Name";
    server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
    assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);
  }

  @Test
  public void testRegisterNotificationListenerWithHandback() throws Exception {
    String objectName = "spring:name=Test";
    JmxTestBean bean = new JmxTestBean();

    Map<String, Object> beans = new HashMap<>();
    beans.put(objectName, bean);

    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
    Object handback = new Object();

    NotificationListenerBean listenerBean = new NotificationListenerBean();
    listenerBean.setNotificationListener(listener);
    listenerBean.setMappedObjectName("spring:name=Test");
    listenerBean.setHandback(handback);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    exporter.setNotificationListeners(listenerBean);
    start(exporter);

    // update the attribute
    String attributeName = "Name";
    server.setAttribute(ObjectNameManager.getInstance("spring:name=Test"), new Attribute(attributeName,
            "Rob Harrop"));

    assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);
    assertThat(listener.getLastHandback(attributeName)).as("Handback object not transmitted correctly").isEqualTo(handback);
  }

  @Test
  public void testRegisterNotificationListenerForAllMBeans() throws Exception {
    ObjectName objectName = ObjectName.getInstance("spring:name=Test");
    JmxTestBean bean = new JmxTestBean();

    Map<String, Object> beans = new HashMap<>();
    beans.put(objectName.getCanonicalName(), bean);

    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

    NotificationListenerBean listenerBean = new NotificationListenerBean();
    listenerBean.setNotificationListener(listener);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    exporter.setNotificationListeners(listenerBean);
    start(exporter);

    // update the attribute
    String attributeName = "Name";
    server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));

    assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);
  }

  @SuppressWarnings("serial")
  @Test
  public void testRegisterNotificationListenerWithFilter() throws Exception {
    ObjectName objectName = ObjectName.getInstance("spring:name=Test");
    JmxTestBean bean = new JmxTestBean();

    Map<String, Object> beans = new HashMap<>();
    beans.put(objectName.getCanonicalName(), bean);

    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

    NotificationListenerBean listenerBean = new NotificationListenerBean();
    listenerBean.setNotificationListener(listener);
    listenerBean.setNotificationFilter(notification -> {
      if (notification instanceof AttributeChangeNotification changeNotification) {
        return "Name".equals(changeNotification.getAttributeName());
      }
      else {
        return false;
      }
    });

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    exporter.setNotificationListeners(listenerBean);
    start(exporter);

    // update the attributes
    String nameAttribute = "Name";
    String ageAttribute = "Age";

    server.setAttribute(objectName, new Attribute(nameAttribute, "Rob Harrop"));
    server.setAttribute(objectName, new Attribute(ageAttribute, 90));

    assertThat(listener.getCount(nameAttribute)).as("Listener not notified for Name").isEqualTo(1);
    assertThat(listener.getCount(ageAttribute)).as("Listener incorrectly notified for Age").isEqualTo(0);
  }

  @Test
  public void testCreationWithNoNotificationListenerSet() {
    assertThatIllegalArgumentException().as("no NotificationListener supplied").isThrownBy(
            new NotificationListenerBean()::afterPropertiesSet);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testRegisterNotificationListenerWithBeanNameAndBeanNameInBeansMap() throws Exception {
    String beanName = "testBean";
    ObjectName objectName = ObjectName.getInstance("spring:name=Test");

    SelfNamingTestBean testBean = new SelfNamingTestBean();
    testBean.setObjectName(objectName);

    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerSingleton(beanName, testBean);

    Map<String, Object> beans = new HashMap<>();
    beans.put(beanName, beanName);

    Map listenerMappings = new HashMap();
    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
    listenerMappings.put(beanName, listener);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    exporter.setNotificationListenerMappings(listenerMappings);
    exporter.setBeanFactory(factory);
    start(exporter);
    assertIsRegistered("Should have registered MBean", objectName);

    server.setAttribute(objectName, new Attribute("Age", 77));
    assertThat(listener.getCount("Age")).as("Listener not notified").isEqualTo(1);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testRegisterNotificationListenerWithBeanNameAndBeanInstanceInBeansMap() throws Exception {
    String beanName = "testBean";
    ObjectName objectName = ObjectName.getInstance("spring:name=Test");

    SelfNamingTestBean testBean = new SelfNamingTestBean();
    testBean.setObjectName(objectName);

    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerSingleton(beanName, testBean);

    Map<String, Object> beans = new HashMap<>();
    beans.put(beanName, testBean);

    Map listenerMappings = new HashMap();
    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
    listenerMappings.put(beanName, listener);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    exporter.setNotificationListenerMappings(listenerMappings);
    exporter.setBeanFactory(factory);
    start(exporter);
    assertIsRegistered("Should have registered MBean", objectName);

    server.setAttribute(objectName, new Attribute("Age", 77));
    assertThat(listener.getCount("Age")).as("Listener not notified").isEqualTo(1);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testRegisterNotificationListenerWithBeanNameBeforeObjectNameMappedToSameBeanInstance() throws Exception {
    String beanName = "testBean";
    ObjectName objectName = ObjectName.getInstance("spring:name=Test");

    SelfNamingTestBean testBean = new SelfNamingTestBean();
    testBean.setObjectName(objectName);

    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerSingleton(beanName, testBean);

    Map<String, Object> beans = new HashMap<>();
    beans.put(beanName, testBean);

    Map listenerMappings = new HashMap();
    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
    listenerMappings.put(beanName, listener);
    listenerMappings.put(objectName, listener);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    exporter.setNotificationListenerMappings(listenerMappings);
    exporter.setBeanFactory(factory);
    start(exporter);
    assertIsRegistered("Should have registered MBean", objectName);

    server.setAttribute(objectName, new Attribute("Age", 77));
    assertThat(listener.getCount("Age")).as("Listener should have been notified exactly once").isEqualTo(1);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testRegisterNotificationListenerWithObjectNameBeforeBeanNameMappedToSameBeanInstance() throws Exception {
    String beanName = "testBean";
    ObjectName objectName = ObjectName.getInstance("spring:name=Test");

    SelfNamingTestBean testBean = new SelfNamingTestBean();
    testBean.setObjectName(objectName);

    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerSingleton(beanName, testBean);

    Map<String, Object> beans = new HashMap<>();
    beans.put(beanName, testBean);

    Map listenerMappings = new HashMap();
    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
    listenerMappings.put(objectName, listener);
    listenerMappings.put(beanName, listener);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    exporter.setNotificationListenerMappings(listenerMappings);
    exporter.setBeanFactory(factory);
    start(exporter);
    assertIsRegistered("Should have registered MBean", objectName);

    server.setAttribute(objectName, new Attribute("Age", 77));
    assertThat(listener.getCount("Age")).as("Listener should have been notified exactly once").isEqualTo(1);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Test
  public void testRegisterNotificationListenerWithTwoBeanNamesMappedToDifferentBeanInstances() throws Exception {
    String beanName1 = "testBean1";
    String beanName2 = "testBean2";

    ObjectName objectName1 = ObjectName.getInstance("spring:name=Test1");
    ObjectName objectName2 = ObjectName.getInstance("spring:name=Test2");

    SelfNamingTestBean testBean1 = new SelfNamingTestBean();
    testBean1.setObjectName(objectName1);

    SelfNamingTestBean testBean2 = new SelfNamingTestBean();
    testBean2.setObjectName(objectName2);

    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerSingleton(beanName1, testBean1);
    factory.registerSingleton(beanName2, testBean2);

    Map<String, Object> beans = new HashMap<>();
    beans.put(beanName1, testBean1);
    beans.put(beanName2, testBean2);

    Map listenerMappings = new HashMap();
    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();
    listenerMappings.put(beanName1, listener);
    listenerMappings.put(beanName2, listener);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    exporter.setNotificationListenerMappings(listenerMappings);
    exporter.setBeanFactory(factory);
    start(exporter);
    assertIsRegistered("Should have registered MBean", objectName1);
    assertIsRegistered("Should have registered MBean", objectName2);

    server.setAttribute(ObjectNameManager.getInstance(objectName1), new Attribute("Age", 77));
    assertThat(listener.getCount("Age")).as("Listener not notified for testBean1").isEqualTo(1);

    server.setAttribute(ObjectNameManager.getInstance(objectName2), new Attribute("Age", 33));
    assertThat(listener.getCount("Age")).as("Listener not notified for testBean2").isEqualTo(2);
  }

  @Test
  public void testNotificationListenerRegistrar() throws Exception {
    ObjectName objectName = ObjectName.getInstance("spring:name=Test");
    JmxTestBean bean = new JmxTestBean();

    Map<String, Object> beans = new HashMap<>();
    beans.put(objectName.getCanonicalName(), bean);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    start(exporter);

    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

    NotificationListenerRegistrar registrar = new NotificationListenerRegistrar();
    registrar.setServer(server);
    registrar.setNotificationListener(listener);
    registrar.setMappedObjectName(objectName);
    registrar.afterPropertiesSet();

    // update the attribute
    String attributeName = "Name";
    server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
    assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);

    registrar.destroy();

    // try to update the attribute again
    server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
    assertThat(listener.getCount(attributeName)).as("Listener notified after destruction").isEqualTo(1);
  }

  @Test
  public void testNotificationListenerRegistrarWithMultipleNames() throws Exception {
    ObjectName objectName = ObjectName.getInstance("spring:name=Test");
    ObjectName objectName2 = ObjectName.getInstance("spring:name=Test2");
    JmxTestBean bean = new JmxTestBean();
    JmxTestBean bean2 = new JmxTestBean();

    Map<String, Object> beans = new HashMap<>();
    beans.put(objectName.getCanonicalName(), bean);
    beans.put(objectName2.getCanonicalName(), bean2);

    MBeanExporter exporter = new MBeanExporter();
    exporter.setServer(server);
    exporter.setBeans(beans);
    start(exporter);

    CountingAttributeChangeNotificationListener listener = new CountingAttributeChangeNotificationListener();

    NotificationListenerRegistrar registrar = new NotificationListenerRegistrar();
    registrar.setServer(server);
    registrar.setNotificationListener(listener);
    //registrar.setMappedObjectNames(new Object[] {objectName, objectName2});
    registrar.setMappedObjectNames("spring:name=Test", "spring:name=Test2");
    registrar.afterPropertiesSet();

    // update the attribute
    String attributeName = "Name";
    server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
    assertThat(listener.getCount(attributeName)).as("Listener not notified").isEqualTo(1);

    registrar.destroy();

    // try to update the attribute again
    server.setAttribute(objectName, new Attribute(attributeName, "Rob Harrop"));
    assertThat(listener.getCount(attributeName)).as("Listener notified after destruction").isEqualTo(1);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static class CountingAttributeChangeNotificationListener implements NotificationListener {

    private Map attributeCounts = new HashMap();

    private Map attributeHandbacks = new HashMap();

    @Override
    public void handleNotification(Notification notification, Object handback) {
      if (notification instanceof AttributeChangeNotification attNotification) {
        String attributeName = attNotification.getAttributeName();

        Integer currentCount = (Integer) this.attributeCounts.get(attributeName);

        if (currentCount != null) {
          int count = currentCount.intValue() + 1;
          this.attributeCounts.put(attributeName, count);
        }
        else {
          this.attributeCounts.put(attributeName, 1);
        }

        this.attributeHandbacks.put(attributeName, handback);
      }
    }

    public int getCount(String attribute) {
      Integer count = (Integer) this.attributeCounts.get(attribute);
      return (count == null) ? 0 : count.intValue();
    }

    public Object getLastHandback(String attributeName) {
      return this.attributeHandbacks.get(attributeName);
    }
  }

  public static class SelfNamingTestBean implements SelfNaming {

    private ObjectName objectName;

    private int age;

    public void setObjectName(ObjectName objectName) {
      this.objectName = objectName;
    }

    @Override
    public ObjectName getObjectName() throws MalformedObjectNameException {
      return this.objectName;
    }

    public void setAge(int age) {
      this.age = age;
    }

    public int getAge() {
      return this.age;
    }
  }

}
