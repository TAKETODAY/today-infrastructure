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

package cn.taketoday.jmx.export.notification;

import org.junit.jupiter.api.Test;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

import cn.taketoday.jmx.export.SpringModelMBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Rick Evans
 * @author Chris Beams
 */
public class ModelMBeanNotificationPublisherTests {

	@Test
	public void testCtorWithNullMBean() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ModelMBeanNotificationPublisher(null, createObjectName(), this));
	}

	@Test
	public void testCtorWithNullObjectName() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ModelMBeanNotificationPublisher(new SpringModelMBean(), null, this));
	}

	@Test
	public void testCtorWithNullManagedResource() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ModelMBeanNotificationPublisher(new SpringModelMBean(), createObjectName(), null));
	}

	@Test
	public void testSendNullNotification() throws Exception {
		NotificationPublisher publisher
				= new ModelMBeanNotificationPublisher(new SpringModelMBean(), createObjectName(), this);
		assertThatIllegalArgumentException().isThrownBy(() ->
				publisher.sendNotification(null));
	}

	public void testSendVanillaNotification() throws Exception {
		StubSpringModelMBean mbean = new StubSpringModelMBean();
		Notification notification = new Notification("network.alarm.router", mbean, 1872);
		ObjectName objectName = createObjectName();

		NotificationPublisher publisher = new ModelMBeanNotificationPublisher(mbean, objectName, mbean);
		publisher.sendNotification(notification);

		assertThat(mbean.getActualNotification()).isNotNull();
		assertThat(mbean.getActualNotification()).as("The exact same Notification is not being passed through from the publisher to the mbean.").isSameAs(notification);
		assertThat(mbean.getActualNotification().getSource()).as("The 'source' property of the Notification is not being set to the ObjectName of the associated MBean.").isSameAs(objectName);
	}

	public void testSendAttributeChangeNotification() throws Exception {
		StubSpringModelMBean mbean = new StubSpringModelMBean();
		Notification notification = new AttributeChangeNotification(mbean, 1872, System.currentTimeMillis(), "Shall we break for some tea?", "agree", "java.lang.Boolean", Boolean.FALSE, Boolean.TRUE);
		ObjectName objectName = createObjectName();

		NotificationPublisher publisher = new ModelMBeanNotificationPublisher(mbean, objectName, mbean);
		publisher.sendNotification(notification);

		assertThat(mbean.getActualNotification()).isNotNull();
		boolean condition = mbean.getActualNotification() instanceof AttributeChangeNotification;
		assertThat(condition).isTrue();
		assertThat(mbean.getActualNotification()).as("The exact same Notification is not being passed through from the publisher to the mbean.").isSameAs(notification);
		assertThat(mbean.getActualNotification().getSource()).as("The 'source' property of the Notification is not being set to the ObjectName of the associated MBean.").isSameAs(objectName);
	}

	public void testSendAttributeChangeNotificationWhereSourceIsNotTheManagedResource() throws Exception {
		StubSpringModelMBean mbean = new StubSpringModelMBean();
		Notification notification = new AttributeChangeNotification(this, 1872, System.currentTimeMillis(), "Shall we break for some tea?", "agree", "java.lang.Boolean", Boolean.FALSE, Boolean.TRUE);
		ObjectName objectName = createObjectName();

		NotificationPublisher publisher = new ModelMBeanNotificationPublisher(mbean, objectName, mbean);
		publisher.sendNotification(notification);

		assertThat(mbean.getActualNotification()).isNotNull();
		boolean condition = mbean.getActualNotification() instanceof AttributeChangeNotification;
		assertThat(condition).isTrue();
		assertThat(mbean.getActualNotification()).as("The exact same Notification is not being passed through from the publisher to the mbean.").isSameAs(notification);
		assertThat(mbean.getActualNotification().getSource()).as("The 'source' property of the Notification is *wrongly* being set to the ObjectName of the associated MBean.").isSameAs(this);
	}

	private static ObjectName createObjectName() throws MalformedObjectNameException {
		return ObjectName.getInstance("foo:type=bar");
	}


	private static class StubSpringModelMBean extends SpringModelMBean {

		private Notification actualNotification;

		public StubSpringModelMBean() throws MBeanException, RuntimeOperationsException {
		}

		public Notification getActualNotification() {
			return this.actualNotification;
		}

		@Override
		public void sendNotification(Notification notification) throws RuntimeOperationsException {
			this.actualNotification = notification;
		}

		@Override
		public void sendAttributeChangeNotification(AttributeChangeNotification notification) throws RuntimeOperationsException {
			this.actualNotification = notification;
		}
	}

}
