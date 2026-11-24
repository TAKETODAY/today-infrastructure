/*
 * Copyright 2002-present the original author or authors.
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

package infra.jmx.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.MBeanServerForwarder;

import infra.core.testfixture.net.TestSocketUtils;
import infra.jmx.AbstractMBeanServerTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for {@link ConnectorServerFactoryBean}.
 *
 * @author Rob Harrop
 * @author Chris Beams
 * @author Sam Brannen
 */
@Execution(ExecutionMode.SAME_THREAD)
class ConnectorServerFactoryBeanTests extends AbstractMBeanServerTests {

  private static final String OBJECT_NAME = "spring:type=connector,name=test";

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

  @Test
  void forwardsEnvironmentAndStartsConnectorInDaemonThread() throws Exception {
    MBeanServerForwarder forwarder = mock(MBeanServerForwarder.class);
    JMXConnectorServer connector = mock(JMXConnectorServer.class);
    CountDownLatch started = new CountDownLatch(1);
    AtomicReference<Thread> startingThread = new AtomicReference<>();
    willAnswer(invocation -> {
      startingThread.set(Thread.currentThread());
      started.countDown();
      return null;
    }).given(connector).start();
    ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
    bean.setServer(getServer());
    bean.setServiceUrl(this.serviceUrl);
    bean.setForwarder(forwarder);
    bean.setThreaded(true);
    bean.setDaemon(true);
    Properties properties = new Properties();
    properties.setProperty("mode", "original");
    properties.setProperty("source", "properties");
    bean.setEnvironment(properties);
    bean.setEnvironmentMap(null);
    bean.setEnvironmentMap(Map.of("mode", "overridden"));
    assertThat(bean.getObject()).isNull();
    assertThat(bean.getObjectType()).isEqualTo(JMXConnectorServer.class);
    assertThat(bean.isSingleton()).isTrue();
    bean.destroy();

    JMXServiceURL url = new JMXServiceURL(this.serviceUrl);
    MBeanServer server = getServer();
    try (var factory = mockStatic(JMXConnectorServerFactory.class)) {
      factory.when(() -> JMXConnectorServerFactory.newJMXConnectorServer(
              url, Map.of("mode", "overridden", "source", "properties"), server)).thenReturn(connector);
      try {
        bean.afterPropertiesSet();
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(startingThread.get().isDaemon()).isTrue();
        assertThat(startingThread.get().getName()).contains(this.serviceUrl);
        assertThat(bean.getObject()).isSameAs(connector);
        assertThat(bean.getObjectType()).isEqualTo(connector.getClass());
        verify(connector).setMBeanServerForwarder(forwarder);
      }
      finally {
        bean.destroy();
      }
      verify(connector).stop();
    }
  }

  @Test
  void unregistersConnectorWhenStartupFails() throws Exception {
    MBeanServer server = mock(MBeanServer.class);
    JMXConnectorServer connector = mock(JMXConnectorServer.class);
    JMXServiceURL url = new JMXServiceURL(this.serviceUrl);
    ObjectName name = ObjectName.getInstance(OBJECT_NAME);
    given(server.registerMBean(connector, name)).willReturn(new ObjectInstance(name, JMXConnectorServer.class.getName()));
    given(server.isRegistered(name)).willReturn(true);
    IOException failure = new IOException("start failed");
    willThrow(failure).given(connector).start();
    ConnectorServerFactoryBean bean = new ConnectorServerFactoryBean();
    bean.setServer(server);
    bean.setServiceUrl(this.serviceUrl);
    bean.setObjectName(name);

    try (var factory = mockStatic(JMXConnectorServerFactory.class)) {
      factory.when(() -> JMXConnectorServerFactory.newJMXConnectorServer(url, Map.of(), server)).thenReturn(connector);
      assertThatIOException().isThrownBy(bean::afterPropertiesSet).isSameAs(failure);
    }

    verify(server).unregisterMBean(name);
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
