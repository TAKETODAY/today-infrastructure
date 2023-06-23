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

package cn.taketoday.jmx.access;

import org.junit.jupiter.api.AfterEach;

import java.net.BindException;
import java.net.MalformedURLException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import cn.taketoday.core.testfixture.net.TestSocketUtils;

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
      System.out.println("Skipping remote JMX tests because binding to local port ["
              + this.servicePort + "] failed: " + ex.getMessage());
      runTests = false;
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
