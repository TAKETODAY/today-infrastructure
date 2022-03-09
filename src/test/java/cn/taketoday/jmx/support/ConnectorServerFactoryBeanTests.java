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

package cn.taketoday.jmx.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import cn.taketoday.core.testfixture.net.TestSocketUtils;
import cn.taketoday.jmx.AbstractMBeanServerTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link ConnectorServerFactoryBean}.
 *
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 */
class ConnectorServerFactoryBeanTests extends AbstractMBeanServerTests {

	private static final String OBJECT_NAME = "spring:type=connector,name=test";

	@SuppressWarnings("deprecation")
	private final String serviceUrl = "service:jmx:jmxmp://localhost:" + TestSocketUtils.findAvailableTcpPort();


	@Test
	void startupWithLocatedServer() throws Exception {
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.setServiceUrl(this.serviceUrl);
		bean.afterPropertiesSet();

		try {
			checkServerConnection(getServer());
		}
		finally {
			bean.destroy();
		}
	}

	@Test
	void startupWithSuppliedServer() throws Exception {
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.setServiceUrl(this.serviceUrl);
		bean.setServer(getServer());
		bean.afterPropertiesSet();

		try {
			checkServerConnection(getServer());
		}
		finally {
			bean.destroy();
		}
	}

	@Test
	void registerWithMBeanServer() throws Exception {
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.setServiceUrl(this.serviceUrl);
		bean.setObjectName(OBJECT_NAME);
		bean.afterPropertiesSet();

		try {
			// Try to get the connector bean.
			ObjectInstance instance = getServer().getObjectInstance(ObjectName.getInstance(OBJECT_NAME));
			assertThat(instance).as("ObjectInstance should not be null").isNotNull();
		}
		finally {
			bean.destroy();
		}
	}

	@Test
	void noRegisterWithMBeanServer() throws Exception {
		ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
		bean.setServiceUrl(this.serviceUrl);
		bean.afterPropertiesSet();
		try {
			// Try to get the connector bean.
			assertThatExceptionOfType(InstanceNotFoundException.class).isThrownBy(() ->
				getServer().getObjectInstance(ObjectName.getInstance(OBJECT_NAME)));
		}
		finally {
			bean.destroy();
		}
	}

	private void checkServerConnection(MBeanServer hostedServer) throws IOException, MalformedURLException {
		// Try to connect using client.
		JMXServiceURL serviceURL = new JMXServiceURL(this.serviceUrl);
		JMXConnector connector = JMXConnectorFactory.connect(serviceURL);

		assertThat(connector).as("Client Connector should not be null").isNotNull();

		// Get the MBean server connection.
		MBeanServerConnection connection = connector.getMBeanServerConnection();
		assertThat(connection).as("MBeanServerConnection should not be null").isNotNull();

		// Test for MBean server equality.
		assertThat(connection.getMBeanCount()).as("Registered MBean count should be the same").isEqualTo(hostedServer.getMBeanCount());
	}

}
