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

package infra.testcontainers.service.connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.service.connection.ConnectionDetails;
import infra.context.service.connection.ConnectionDetailsFactories;
import infra.context.service.connection.ConnectionDetailsFactoryNotFoundException;
import infra.context.service.connection.ConnectionDetailsNotFoundException;
import infra.core.annotation.MergedAnnotation;
import infra.origin.Origin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConnectionDetailsRegistrar}.
 *
 * @author Phillip Webb
 */
class ConnectionDetailsRegistrarTests {

  private Origin origin;

  private PostgreSQLContainer container;

  private MergedAnnotation<ServiceConnection> annotation;

  private ContainerConnectionSource<?> source;

  private ConnectionDetailsFactories factories;

  @BeforeEach
  void setup() {
    this.origin = mock(Origin.class);
    this.container = mock(PostgreSQLContainer.class);
    this.annotation = MergedAnnotation.valueOf(ServiceConnection.class, Map.of("name", "", "type", new Class<?>[0]));
    this.source = new ContainerConnectionSource<>("test", this.origin, PostgreSQLContainer.class, null,
            this.annotation, () -> this.container, null, null);
    this.factories = mock(ConnectionDetailsFactories.class);
  }

  @Test
  void registerBeanDefinitionsWhenConnectionDetailsFactoryNotFoundAndNoConnectionNameThrowsExceptionWithBetterMessage() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory, this.factories);
    given(this.factories.getConnectionDetails(this.source, true))
            .willThrow(new ConnectionDetailsFactoryNotFoundException("fail", null));
    assertThatExceptionOfType(ConnectionDetailsFactoryNotFoundException.class)
            .isThrownBy(() -> registrar.registerBeanDefinitions(beanFactory, this.source))
            .withMessage("fail. You may need to add a 'name' to your @ServiceConnection annotation");
  }

  @Test
  void registerBeanDefinitionsWhenConnectionDetailsNotFoundExceptionAndNoConnectionNameThrowsExceptionWithBetterMessage() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory, this.factories);
    given(this.factories.getConnectionDetails(this.source, true))
            .willThrow(new ConnectionDetailsNotFoundException("fail", null));
    assertThatExceptionOfType(ConnectionDetailsNotFoundException.class)
            .isThrownBy(() -> registrar.registerBeanDefinitions(beanFactory, this.source))
            .withMessage("fail. You may need to add a 'name' to your @ServiceConnection annotation");
  }

  @Test
  void registerBeanDefinitionsWhenExistingBeanSkipsRegistration() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("testbean", new RootBeanDefinition(CustomTestConnectionDetails.class));
    ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory, this.factories);
    given(this.factories.getConnectionDetails(this.source, true))
            .willReturn(Map.of(TestConnectionDetails.class, new TestConnectionDetails()));
    registrar.registerBeanDefinitions(beanFactory, this.source);
    assertThat(beanFactory.getBean(TestConnectionDetails.class)).isInstanceOf(CustomTestConnectionDetails.class);
  }

  @Test
  void registerBeanDefinitionsRegistersDefinition() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    ConnectionDetailsRegistrar registrar = new ConnectionDetailsRegistrar(beanFactory, this.factories);
    given(this.factories.getConnectionDetails(this.source, true))
            .willReturn(Map.of(TestConnectionDetails.class, new TestConnectionDetails()));
    registrar.registerBeanDefinitions(beanFactory, this.source);
    assertThat(beanFactory.getBean(TestConnectionDetails.class)).isNotNull();
  }

  static class TestConnectionDetails implements ConnectionDetails {

  }

  static class CustomTestConnectionDetails extends TestConnectionDetails {

  }

}
