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

package infra.docker.compose.service.connection;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import infra.context.ApplicationContext;
import infra.context.container.ContainerImageMetadata;
import infra.context.service.connection.ConnectionDetails;
import infra.context.service.connection.ConnectionDetailsFactories;
import infra.context.support.GenericApplicationContext;
import infra.docker.compose.core.ImageReference;
import infra.docker.compose.core.RunningService;
import infra.docker.compose.lifecycle.DockerComposeServicesReadyEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class DockerComposeServiceConnectionsApplicationListenerTests {

  @Test
  void registersOriginalAndAdaptedDetailsWithImageMetadata() {
    ConnectionDetailsFactories factories = mock();
    var listener = new DockerComposeServiceConnectionsApplicationListener(factories);
    var service = service("catalog-db", "postgres:17");
    var unrelatedService = service("unrelated", "redis:7");
    var database = new DatabaseDetails("jdbc:postgresql://localhost/catalog");
    var migration = new MigrationDetails(database.jdbcUrl());
    given(factories.getConnectionDetails(database, false)).willReturn(Map.of(MigrationDetails.class, migration));
    try (var context = new GenericApplicationContext()) {
      given(factories.getConnectionDetails(any(DockerComposeConnectionSource.class), eq(false))).willAnswer(invocation -> {
        DockerComposeConnectionSource source = invocation.getArgument(0);
        assertThat(source.getEnvironment()).isSameAs(context.getEnvironment());
        return (source.getRunningService() == service) ? Map.of(DatabaseDetails.class, database) : Map.of();
      });

      listener.onApplicationEvent(event(context, List.of(service, unrelatedService)));
      context.refresh();

      String databaseBeanName = "databaseDetailsForCatalogDb";
      String migrationBeanName = "migrationDetailsForCatalogDb";
      assertThat(context.getBeansOfType(ConnectionDetails.class)).containsOnlyKeys(databaseBeanName, migrationBeanName);
      assertThat(context.getBean(databaseBeanName)).isSameAs(database);
      assertThat(context.getBean(migrationBeanName)).isSameAs(migration);
      var imageMetadata = new ContainerImageMetadata("docker.io/library/postgres:17");
      assertThat(ContainerImageMetadata.getFrom(context.getBeanDefinition(databaseBeanName))).isEqualTo(imageMetadata);
      assertThat(ContainerImageMetadata.getFrom(context.getBeanDefinition(migrationBeanName))).isEqualTo(imageMetadata);
    }
  }

  @Test
  void ignoresContextsThatCannotRegisterBeanDefinitions() {
    ApplicationContext context = mock();
    var event = event(context, List.of(service("catalog-db", "postgres:17")));

    new DockerComposeServiceConnectionsApplicationListener().onApplicationEvent(event);

    then(context).shouldHaveNoInteractions();
  }

  private static DockerComposeServicesReadyEvent event(ApplicationContext context, List<RunningService> services) {
    DockerComposeServicesReadyEvent event = mock();
    given(event.getSource()).willReturn(context);
    given(event.getRunningServices()).willReturn(services);
    return event;
  }

  private static RunningService service(String name, String image) {
    RunningService service = mock();
    given(service.name()).willReturn(name);
    given(service.image()).willReturn(ImageReference.of(image));
    return service;
  }

  record DatabaseDetails(String jdbcUrl) implements ConnectionDetails {
  }

  record MigrationDetails(String jdbcUrl) implements ConnectionDetails {
  }
}
