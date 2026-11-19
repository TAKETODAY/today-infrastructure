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

package infra.testcontainers.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.util.stream.Stream;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.container.ContainerImageMetadata;
import infra.test.context.DynamicPropertyRegistry;
import infra.test.context.DynamicPropertySource;
import infra.testcontainers.beans.TestcontainerBeanDefinition;
import infra.testcontainers.properties.TestcontainersPropertySourceAutoConfiguration;
import infra.testcontainers.service.connection.ServiceConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class ImportTestcontainersRegistrarTests {

  @Test
  void importsAnnotatedFieldsFromExplicitSourceAndStartsContainerOnPropertyLookup() {
    given(Containers.database.getDockerImageName()).willReturn("postgres:17");
    given(Containers.database.getFirstMappedPort()).willReturn(15432);
    try (var context = new AnnotationConfigApplicationContext(ExplicitImport.class,
            TestcontainersPropertySourceAutoConfiguration.class)) {
      assertImportedContainer(context, Containers.database);
      var order = inOrder(Containers.database);
      assertThat(context.getEnvironment().getProperty("database.port", Integer.class)).isEqualTo(15432);
      order.verify(Containers.database).start();
      order.verify(Containers.database).getFirstMappedPort();
    }
  }

  @Test
  void importsFromTheAnnotatedClassWhenNoSourceIsSpecified() {
    given(ImplicitImport.database.getDockerImageName()).willReturn("postgres:17");
    try (var context = new AnnotationConfigApplicationContext(ImplicitImport.class)) {
      assertImportedContainer(context, ImplicitImport.database);
      assertThat(context.getBeanNamesForType(String.class)).isEmpty();
    }
  }

  @ParameterizedTest
  @MethodSource("invalidDeclarations")
  void rejectsInvalidDeclarations(Class<?> configuration, String message) {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(configuration);
      assertThatIllegalStateException().isThrownBy(context::refresh).withMessage(message);
    }
  }

  static Stream<Arguments> invalidDeclarations() {
    return Stream.of(
            Arguments.of(NullField.class, "Container field 'database' must not have a null value"),
            Arguments.of(InstanceField.class, "Container field 'database' must be static"),
            Arguments.of(InstanceMethod.class, "@DynamicPropertySource method 'properties' must be static"),
            Arguments.of(NoArgument.class,
                    "@DynamicPropertySource method 'properties' must accept a single DynamicPropertyRegistry argument"),
            Arguments.of(WrongArgument.class,
                    "@DynamicPropertySource method 'properties' must accept a single DynamicPropertyRegistry argument"));
  }

  private void assertImportedContainer(AnnotationConfigApplicationContext context, Container<?> container) {
    String[] names = context.getBeanNamesForType(Container.class);
    assertThat(names).hasSize(1);
    assertThat(context.getBean(names[0])).isSameAs(container);
    var definition = (TestcontainerBeanDefinition) context.getBeanDefinition(names[0]);
    assertThat(definition.getContainerImageName()).isEqualTo("postgres:17");
    assertThat(ContainerImageMetadata.getFrom(definition)).isEqualTo(new ContainerImageMetadata("postgres:17"));
    assertThat(definition.getAnnotations().isPresent(ServiceConnection.class)).isTrue();
  }

  @ImportTestcontainers(Containers.class)
  static class ExplicitImport { }

  static class Containers {

    @ServiceConnection
    private static final GenericContainer<?> database = mock(GenericContainer.class);

    @DynamicPropertySource
    private static void properties(DynamicPropertyRegistry registry) {
      registry.add("database.port", database::getFirstMappedPort);
    }

  }

  @ImportTestcontainers
  static class ImplicitImport {

    @ServiceConnection
    private static final GenericContainer<?> database = mock(GenericContainer.class);

    private static final String ignored = "not a container";

  }

  @ImportTestcontainers
  static class NullField {

    static Container<?> database;

  }

  @ImportTestcontainers
  static class InstanceField {

    Container<?> database;

  }

  @ImportTestcontainers
  static class InstanceMethod {

    @DynamicPropertySource
    void properties(DynamicPropertyRegistry registry) { }

  }

  @ImportTestcontainers
  static class WrongArgument {

    @DynamicPropertySource
    static void properties(String wrongType) { }

  }

  @ImportTestcontainers
  static class NoArgument {

    @DynamicPropertySource
    static void properties() { }

  }

}
