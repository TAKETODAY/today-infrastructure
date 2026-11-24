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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class MBeanProxyFailureTests {

  private final ObjectName name = ObjectName.getInstance("infra:type=Test");

  MBeanProxyFailureTests() throws Exception { }

  @Test
  void requiresAProxyOrManagementInterface() {
    MBeanProxyFactoryBean factory = new MBeanProxyFactoryBean();
    factory.setConnectOnStartup(false);
    assertThatIllegalArgumentException().isThrownBy(factory::afterPropertiesSet).withMessageContaining("proxyInterface");
  }

  @Test
  void preservesDeclaredExceptionsAndWrapsUndeclaredJmxFailures() throws Exception {
    MBeanServerConnection server = mock(MBeanServerConnection.class);
    CheckedOperations checked = proxy(server, CheckedOperations.class);
    InstanceNotFoundException notFound = new InstanceNotFoundException();
    ReflectionException reflection = new ReflectionException(new NoSuchMethodException());
    IOException io = new IOException("offline");
    given(server.invoke(eq(name), eq("read"), any(), any())).willThrow(notFound, reflection, io);

    assertThatThrownBy(checked::read).isSameAs(notFound);
    assertThatThrownBy(checked::read).isSameAs(reflection);
    assertThatThrownBy(checked::read).isSameAs(io);

    Operations unchecked = proxy(server, Operations.class);
    willThrow(reflection, io).given(server).invoke(eq(name), eq("read"), any(), any());
    assertThatExceptionOfType(InvocationFailureException.class).isThrownBy(unchecked::read)
            .withMessage("JMX access failed").withCause(reflection);
    assertThatExceptionOfType(MBeanConnectFailureException.class).isThrownBy(unchecked::read).withCause(io);
  }

  @ParameterizedTest
  @MethodSource("wrappedOperationsFailures")
  void unwrapsRuntimeOperationsException(RuntimeException wrapped, Throwable expected) throws Exception {
    MBeanServerConnection server = mock(MBeanServerConnection.class);
    given(server.invoke(eq(name), eq("read"), any(), any())).willThrow(new RuntimeOperationsException(wrapped));

    assertThatThrownBy(proxy(server, Operations.class)::read).isSameAs(expected);
  }

  static Stream<Arguments> wrappedOperationsFailures() {
    var failure = new IllegalStateException("failed");
    var error = new AssertionError("failed");
    return Stream.of(arguments(new RuntimeMBeanException(failure), failure),
            arguments(new RuntimeErrorException(error), error), arguments(failure, failure));
  }

  @Test
  void reconnectsAndRetriesAnInvocationAfterAnIoFailure() throws Exception {
    JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://localhost");
    Map<String, ?> environment = Map.of("mode", "test");
    MBeanProxyFactoryBean factory = new MBeanProxyFactoryBean();
    factory.setServiceUrl(url.toString());
    factory.setEnvironment(environment);
    assertThat(factory.getEnvironment()).isEqualTo(environment);
    factory.setAgentId("unused-when-url-is-specified");
    factory.setObjectName(name);
    factory.setProxyInterface(Operations.class);
    factory.setRefreshOnConnectFailure(true);
    JMXConnector original = mock(JMXConnector.class);
    JMXConnector replacement = mock(JMXConnector.class);
    MBeanServerConnection first = mock(MBeanServerConnection.class);
    MBeanServerConnection second = mock(MBeanServerConnection.class);
    given(original.getMBeanServerConnection()).willReturn(first);
    given(replacement.getMBeanServerConnection()).willReturn(second);
    given(first.invoke(eq(name), eq("read"), any(), any())).willThrow(new IOException("disconnected"));
    given(second.invoke(eq(name), eq("read"), any(), any())).willReturn("reconnected");

    try (var connectors = mockStatic(JMXConnectorFactory.class)) {
      connectors.when(() -> JMXConnectorFactory.connect(url, environment)).thenReturn(original, replacement);
      factory.afterPropertiesSet();
      try {
        assertThat(((Operations) factory.getObject()).read()).isEqualTo("reconnected");
      }
      finally {
        factory.destroy();
      }
    }
    verify(replacement).close();
  }

  private <T> T proxy(MBeanServerConnection server, Class<T> managementInterface) throws Exception {
    MBeanProxyFactoryBean factory = new MBeanProxyFactoryBean();
    factory.setServer(server);
    factory.setObjectName(name);
    factory.setManagementInterface(managementInterface);
    factory.setBeanClassLoader(getClass().getClassLoader());
    factory.afterPropertiesSet();
    assertThat(factory.getObjectType()).isEqualTo(managementInterface);
    assertThat(factory.isSingleton()).isTrue();
    return managementInterface.cast(factory.getObject());
  }

  public interface Operations {
    String read();
  }

  public interface CheckedOperations {
    String read() throws InstanceNotFoundException, ReflectionException, IOException;
  }
}
