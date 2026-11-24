/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jmx.access;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import infra.jmx.JmxException;
import infra.jmx.MBeanServerNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NotificationListenerRegistrarTests {

  private final NotificationListener listener = (notification, handback) -> { };

  @Test
  void requiresListenerAndObjectNames() {
    NotificationListenerRegistrar registrar = new NotificationListenerRegistrar();
    assertThatIllegalArgumentException().isThrownBy(registrar::afterPropertiesSet).withMessageContaining("notificationListener");
    registrar.setNotificationListener(listener);
    registrar.setMappedObjectNames();
    assertThatIllegalArgumentException().isThrownBy(registrar::afterPropertiesSet).withMessageContaining("mappedObjectName");
    registrar.destroy();
  }

  @Test
  void noRegistrationWhenMappingIsClearedBeforePrepare() throws Exception {
    NotificationListenerRegistrar registrar = new NotificationListenerRegistrar();
    MBeanServerConnection server = mock(MBeanServerConnection.class);
    registrar.setServer(server);
    registrar.setMappedObjectName("infra:type=Previous");
    registrar.setMappedObjectName(null);
    registrar.prepare();
    registrar.destroy();

    verifyNoInteractions(server);
  }

  @Test
  void locatesPlatformServerWhenAgentIdIsEmpty() throws Exception {
    NotificationListenerRegistrar registrar = new NotificationListenerRegistrar();
    registrar.setAgentId("");
    registrar.setNotificationListener(listener);
    registrar.setMappedObjectName(ObjectName.getInstance("JMImplementation:type=MBeanServerDelegate"));
    registrar.afterPropertiesSet();
    try {
      assertThat(new ConnectorDelegate().connect(null, null, "")).isSameAs(ManagementFactory.getPlatformMBeanServer());
    }
    finally {
      registrar.destroy();
    }
  }

  @Test
  void continuesUnregisteringAndClosesConnectionWhenListenerIsAlreadyGone() throws Exception {
    JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost");
    Map<String, ?> environment = Map.of("mode", "test");
    NotificationListenerRegistrar registrar = new NotificationListenerRegistrar();
    registrar.setServiceUrl(url.toString());
    registrar.setEnvironment(environment);
    assertThat(registrar.getEnvironment()).isEqualTo(environment);
    registrar.setNotificationListener(listener);
    ObjectName first = ObjectName.getInstance("infra:type=First");
    ObjectName second = ObjectName.getInstance("infra:type=Second");
    registrar.setMappedObjectNames(first, second);
    JMXConnector connector = mock(JMXConnector.class);
    MBeanServerConnection server = mock(MBeanServerConnection.class);
    given(connector.getMBeanServerConnection()).willReturn(server);
    willThrow(new ListenerNotFoundException()).given(server).removeNotificationListener(first, listener, null, null);
    willThrow(new IOException("close failed")).given(connector).close();

    try (var factory = mockStatic(JMXConnectorFactory.class)) {
      factory.when(() -> JMXConnectorFactory.connect(url, environment)).thenReturn(connector);
      registrar.afterPropertiesSet();
      registrar.destroy();
    }

    verify(server).addNotificationListener(first, listener, null, null);
    verify(server).addNotificationListener(second, listener, null, null);
    verify(server).removeNotificationListener(second, listener, null, null);
    verify(connector).close();
  }

  @Test
  void distinguishesRemoteConnectionAndRegistrationFailures() throws Exception {
    JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost");
    IOException failure = new IOException("unreachable");
    NotificationListenerRegistrar registrar = new NotificationListenerRegistrar();
    registrar.setServiceUrl(url.toString());
    registrar.setNotificationListener(listener);
    ObjectName name = ObjectName.getInstance("infra:type=Missing");
    registrar.setMappedObjectName(name);
    try (var factory = mockStatic(JMXConnectorFactory.class)) {
      factory.when(() -> JMXConnectorFactory.connect(url, null)).thenThrow(failure);
      assertThatExceptionOfType(MBeanServerNotFoundException.class).isThrownBy(registrar::afterPropertiesSet)
              .withMessageContaining(url.toString()).withCause(failure);
    }

    MBeanServerConnection server = mock(MBeanServerConnection.class);
    registrar.setServer(server);
    InstanceNotFoundException missing = new InstanceNotFoundException(name.toString());
    willThrow(failure, missing).given(server).addNotificationListener(name, listener, null, null);
    assertThatExceptionOfType(MBeanServerNotFoundException.class).isThrownBy(registrar::afterPropertiesSet).withCause(failure);
    assertThatExceptionOfType(JmxException.class).isThrownBy(registrar::afterPropertiesSet)
            .withMessage("Unable to register NotificationListener").withCause(missing);
  }
}
