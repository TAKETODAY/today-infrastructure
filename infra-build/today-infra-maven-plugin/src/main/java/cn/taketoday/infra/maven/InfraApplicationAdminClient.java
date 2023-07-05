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

package cn.taketoday.infra.maven;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * A JMX client for the {@code InfraApplicationAdmin} MBean. Permits to obtain
 * information about a given Infra application.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class InfraApplicationAdminClient {

  // Note: see InfraApplicationJmxAutoConfiguration
  static final String DEFAULT_OBJECT_NAME = "cn.taketoday.app:type=Admin,name=InfraApplication";

  private final MBeanServerConnection connection;

  private final ObjectName objectName;

  InfraApplicationAdminClient(MBeanServerConnection connection, String jmxName) {
    this.connection = connection;
    this.objectName = toObjectName(jmxName);
  }

  /**
   * Check if the Infra application managed by this instance is ready. Returns
   * {@code false} if the mbean is not yet deployed so this method should be repeatedly
   * called until a timeout is reached.
   *
   * @return {@code true} if the application is ready to service requests
   * @throws MojoExecutionException if the JMX service could not be contacted
   */
  boolean isReady() throws MojoExecutionException {
    try {
      return (Boolean) this.connection.getAttribute(this.objectName, "Ready");
    }
    catch (InstanceNotFoundException ex) {
      return false; // Instance not available yet
    }
    catch (AttributeNotFoundException ex) {
      throw new IllegalStateException("Unexpected: attribute 'Ready' not available", ex);
    }
    catch (ReflectionException ex) {
      throw new MojoExecutionException("Failed to retrieve Ready attribute", ex.getCause());
    }
    catch (MBeanException | IOException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
  }

  /**
   * Stop the application managed by this instance.
   *
   * @throws MojoExecutionException if the JMX service could not be contacted
   * @throws IOException if an I/O error occurs
   * @throws InstanceNotFoundException if the lifecycle mbean cannot be found
   */
  void stop() throws MojoExecutionException, IOException, InstanceNotFoundException {
    try {
      this.connection.invoke(this.objectName, "shutdown", null, null);
    }
    catch (ReflectionException ex) {
      throw new MojoExecutionException("Shutdown failed", ex.getCause());
    }
    catch (MBeanException ex) {
      throw new MojoExecutionException("Could not invoke shutdown operation", ex);
    }
  }

  private ObjectName toObjectName(String name) {
    try {
      return new ObjectName(name);
    }
    catch (MalformedObjectNameException ex) {
      throw new IllegalArgumentException("Invalid jmx name '" + name + "'");
    }
  }

  /**
   * Create a connector for an {@link javax.management.MBeanServer} exposed on the
   * current machine and the current port. Security should be disabled.
   *
   * @param port the port on which the mbean server is exposed
   * @return a connection
   * @throws IOException if the connection to that server failed
   */
  static JMXConnector connect(int port) throws IOException {
    String url = "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + port + "/jmxrmi";
    JMXServiceURL serviceUrl = new JMXServiceURL(url);
    return JMXConnectorFactory.connect(serviceUrl, null);
  }

}
