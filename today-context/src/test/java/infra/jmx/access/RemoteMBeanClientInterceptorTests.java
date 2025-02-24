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

package infra.jmx.access;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;

import java.net.BindException;
import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import infra.core.testfixture.net.TestSocketUtils;

/**
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 */
class RemoteMBeanClientInterceptorTests extends MBeanClientInterceptorTests {

  private final int servicePort = TestSocketUtils.findAvailableTcpPort();

  private final String serviceUrl = "service:jmx:jmxmp://localhost:" + servicePort;

  private JMXConnectorServer connectorServer;

  private JMXConnector connector;

  @Override
  public void onSetUp() throws Exception {
    super.onSetUp();
    this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(getServiceUrl(), null, getServer());
    try {
      this.connectorServer.start();
    }
    catch (BindException ex) {
      runTests = false;
      Assumptions.abort("Skipping remote JMX tests because binding to local port [" +
              this.servicePort + "] failed: " + ex.getMessage());
    }
  }

  private JMXServiceURL getServiceUrl() throws MalformedURLException {
    return new JMXServiceURL(this.serviceUrl);
  }

  @Override
  protected MBeanServerConnection getServerConnection() throws Exception {
    this.connector = JMXConnectorFactory.connect(getServiceUrl());
    return this.connector.getMBeanServerConnection();
  }

  @AfterEach
  @Override
  public void tearDown() throws Exception {
    if (this.connector != null) {
      this.connector.close();
    }
    if (this.connectorServer != null) {
      this.connectorServer.stop();
    }
    if (runTests) {
      super.tearDown();
    }
  }

}
