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

package cn.taketoday.jmx.access;

import java.io.IOException;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import cn.taketoday.jmx.MBeanServerNotFoundException;
import cn.taketoday.jmx.support.JmxUtils;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Internal helper class for managing a JMX connector.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
class ConnectorDelegate {

  private static final Logger logger = LoggerFactory.getLogger(ConnectorDelegate.class);

  @Nullable
  private JMXConnector connector;

  /**
   * Connects to the remote {@code MBeanServer} using the configured {@code JMXServiceURL}:
   * to the specified JMX service, or to a local MBeanServer if no service URL specified.
   *
   * @param serviceUrl the JMX service URL to connect to (may be {@code null})
   * @param environment the JMX environment for the connector (may be {@code null})
   * @param agentId the local JMX MBeanServer's agent id (may be {@code null})
   */
  public MBeanServerConnection connect(@Nullable JMXServiceURL serviceUrl,
          @Nullable Map<String, ?> environment, @Nullable String agentId) throws MBeanServerNotFoundException {

    if (serviceUrl != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Connecting to remote MBeanServer at URL [{}]", serviceUrl);
      }
      try {
        this.connector = JMXConnectorFactory.connect(serviceUrl, environment);
        return this.connector.getMBeanServerConnection();
      }
      catch (IOException ex) {
        throw new MBeanServerNotFoundException("Could not connect to remote MBeanServer [" + serviceUrl + "]", ex);
      }
    }
    else {
      logger.debug("Attempting to locate local MBeanServer");
      return JmxUtils.locateMBeanServer(agentId);
    }
  }

  /**
   * Closes any {@code JMXConnector} that may be managed by this interceptor.
   */
  public void close() {
    if (this.connector != null) {
      try {
        this.connector.close();
      }
      catch (IOException ex) {
        logger.debug("Could not close JMX connector", ex);
      }
    }
  }

}
