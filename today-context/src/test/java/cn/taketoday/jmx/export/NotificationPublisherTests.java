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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ReflectionException;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.jmx.AbstractMBeanServerTests;
import cn.taketoday.jmx.export.notification.NotificationPublisher;
import cn.taketoday.jmx.export.notification.NotificationPublisherAware;
import cn.taketoday.jmx.support.ObjectNameManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Infra JMX {@link NotificationPublisher} functionality.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public class NotificationPublisherTests extends AbstractMBeanServerTests {

  private CountingNotificationListener listener = new CountingNotificationListener();

  @Test
  public void testSimpleBean() throws Exception {
    // start the MBeanExporter
    ConfigurableApplicationContext ctx = loadContext("cn/taketoday/jmx/export/notificationPublisherTests.xml");
    this.server.addNotificationListener(ObjectNameManager.getInstance("spring:type=Publisher"), listener, null,
            null);

    MyNotificationPublisher publisher = (MyNotificationPublisher) ctx.getBean("publisher");
    assertThat(publisher.getNotificationPublisher()).as("NotificationPublisher should not be null").isNotNull();
    publisher.sendNotification();
    assertThat(listener.count).as("Notification not sent").isEqualTo(1);
  }

  @Test
  public void testSimpleBeanRegisteredManually() throws Exception {
    // start the MBeanExporter
    ConfigurableApplicationContext ctx = loadContext("cn/taketoday/jmx/export/notificationPublisherTests.xml");
    MBeanExporter exporter = (MBeanExporter) ctx.getBean("exporter");
    MyNotificationPublisher publisher = new MyNotificationPublisher();
    exporter.registerManagedResource(publisher, ObjectNameManager.getInstance("spring:type=Publisher2"));
    this.server.addNotificationListener(ObjectNameManager.getInstance("spring:type=Publisher2"), listener, null,
            null);

    assertThat(publisher.getNotificationPublisher()).as("NotificationPublisher should not be null").isNotNull();
    publisher.sendNotification();
    assertThat(listener.count).as("Notification not sent").isEqualTo(1);
  }

  @Test
  public void testMBean() throws Exception {
    // start the MBeanExporter
    ConfigurableApplicationContext ctx = loadContext("cn/taketoday/jmx/export/notificationPublisherTests.xml");
    this.server.addNotificationListener(ObjectNameManager.getInstance("spring:type=PublisherMBean"), listener,
            null, null);

    MyNotificationPublisherMBean publisher = (MyNotificationPublisherMBean) ctx.getBean("publisherMBean");
    publisher.sendNotification();
    assertThat(listener.count).as("Notification not sent").isEqualTo(1);
  }

	/*
	@Test
	public void testStandardMBean() throws Exception {
		// start the MBeanExporter
		ApplicationContext ctx = new ClassPathXmlApplicationContext("cn/taketoday/jmx/export/notificationPublisherTests.xml");
		this.server.addNotificationListener(ObjectNameManager.getInstance("spring:type=PublisherStandardMBean"), listener, null, null);

		MyNotificationPublisherStandardMBean publisher = (MyNotificationPublisherStandardMBean) ctx.getBean("publisherStandardMBean");
		publisher.sendNotification();
		assertEquals("Notification not sent", 1, listener.count);
	}
	*/

  @Test
  public void testLazyInit() throws Exception {
    // start the MBeanExporter
    ConfigurableApplicationContext ctx = loadContext("cn/taketoday/jmx/export/notificationPublisherLazyTests.xml");
    assertThat(ctx.getBeanFactory().containsSingleton("publisher")).as("Should not have instantiated the bean yet").isFalse();

    // need to touch the MBean proxy
    server.getAttribute(ObjectNameManager.getInstance("spring:type=Publisher"), "Name");
    this.server.addNotificationListener(ObjectNameManager.getInstance("spring:type=Publisher"), listener, null,
            null);

    MyNotificationPublisher publisher = (MyNotificationPublisher) ctx.getBean("publisher");
    assertThat(publisher.getNotificationPublisher()).as("NotificationPublisher should not be null").isNotNull();
    publisher.sendNotification();
    assertThat(listener.count).as("Notification not sent").isEqualTo(1);
  }

  private static class CountingNotificationListener implements NotificationListener {

    private int count;

    private Notification lastNotification;

    @Override
    public void handleNotification(Notification notification, Object handback) {
      this.lastNotification = notification;
      this.count++;
    }

    @SuppressWarnings("unused")
    public int getCount() {
      return count;
    }

    @SuppressWarnings("unused")
    public Notification getLastNotification() {
      return lastNotification;
    }
  }

  public static class MyNotificationPublisher implements NotificationPublisherAware {

    private NotificationPublisher notificationPublisher;

    @Override
    public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
      this.notificationPublisher = notificationPublisher;
    }

    public NotificationPublisher getNotificationPublisher() {
      return notificationPublisher;
    }

    public void sendNotification() {
      this.notificationPublisher.sendNotification(new Notification("test", this, 1));
    }

    public String getName() {
      return "Rob Harrop";
    }
  }

  public static class MyNotificationPublisherMBean extends NotificationBroadcasterSupport implements DynamicMBean {

    @Override
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException,
            ReflectionException {
      return null;
    }

    @Override
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
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
    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException,
            ReflectionException {
      return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
      return new MBeanInfo(MyNotificationPublisherMBean.class.getName(), "", new MBeanAttributeInfo[0],
              new MBeanConstructorInfo[0], new MBeanOperationInfo[0], new MBeanNotificationInfo[0]);
    }

    public void sendNotification() {
      sendNotification(new Notification("test", this, 1));
    }
  }

  public static class MyNotificationPublisherStandardMBean extends NotificationBroadcasterSupport implements MyMBean {

    @Override
    public void sendNotification() {
      sendNotification(new Notification("test", this, 1));
    }
  }

  public interface MyMBean {

    void sendNotification();
  }

}
