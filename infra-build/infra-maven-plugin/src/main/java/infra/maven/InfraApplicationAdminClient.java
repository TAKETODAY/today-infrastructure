/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.maven;

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
  static final String DEFAULT_OBJECT_NAME = "infra.app:type=Admin,name=InfraApplication";

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
