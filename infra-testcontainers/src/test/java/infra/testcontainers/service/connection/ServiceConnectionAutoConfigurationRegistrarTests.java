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

import org.junit.jupiter.api.Test;
import infra.testcontainers.service.connection.ContainerConnectionDetailsFactory.ContainerConnectionDetails;
import org.testcontainers.postgresql.PostgreSQLContainer;

import infra.beans.factory.config.BeanDefinition;
import infra.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.context.container.ContainerImageMetadata;
import infra.origin.Origin;
import infra.testcontainers.context.ImportTestcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceConnectionAutoConfigurationRegistrar}.
 *
 * @author Daeho Kwon
 */
class ServiceConnectionAutoConfigurationRegistrarTests {

  @Test
  void beanOriginUsesTheBeanNameForIdentityAndIncludesItsDefinitionInDiagnostics() {
    BeanDefinition definition = mock(BeanDefinition.class);
    given(definition.getResourceDescription()).willReturn("test configuration");
    var origin = new BeanOrigin("database", definition);

    assertThat(origin).isEqualTo(origin).isEqualTo(new BeanOrigin("database", null))
            .hasSameHashCodeAs(new BeanOrigin("database", null))
            .isNotEqualTo(new BeanOrigin("other", definition)).isNotEqualTo("database").isNotEqualTo(null)
            .hasToString("Bean 'database' defined in test configuration");
  }

  @Test
  void importedContainerContributesItsImageAndFieldAnnotationsToConnectionDetails() {
    given(ImportedContainerConfiguration.database.getDockerImageName()).willReturn("postgres:17");
    new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServiceConnectionAutoConfiguration.class))
            .withUserConfiguration(ImportedContainerConfiguration.class)
            .run(context -> {
              String[] names = context.getBeanNamesForType(DatabaseConnectionDetails.class);
              assertThat(names).hasSize(1);
              var details = (ContainerConnectionDetails<?>) context.getBean(names[0]);
              assertThat(details.hasAnnotation(JksTrustStore.class)).isTrue();
              assertThat(ContainerImageMetadata.getFrom(context.getBeanFactory().getBeanDefinition(names[0])))
                      .isEqualTo(new ContainerImageMetadata("postgres:17"));
              assertThat(Origin.from(details)).hasToString("Bean 'importTestContainer.%s.database'"
                      .formatted(ImportedContainerConfiguration.class.getName()));
            });
  }

  @Test
  void manuallyRegisteredSingletonDoesNotRequireABeanDefinition() {
    new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServiceConnectionAutoConfiguration.class))
            .withInitializer(context -> context.getBeanFactory().registerSingleton("database", mock(PostgreSQLContainer.class)))
            .run(context -> assertThat(context).doesNotHaveBean(DatabaseConnectionDetails.class));
  }

  @Test
  void sslAnnotationOnBeanMethodShouldBeDetectedInContainerConnectionSource() {
    new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ServiceConnectionAutoConfiguration.class))
            .withUserConfiguration(ContainerConfiguration.class)
            .run((context) -> {
              ContainerConnectionDetails<?> connectionDetails = (ContainerConnectionDetails<?>) context
                      .getBean(DatabaseConnectionDetails.class);
              assertThat(connectionDetails.hasAnnotation(Ssl.class)).isTrue();
            });
  }

  @ImportTestcontainers
  static class ImportedContainerConfiguration {

    @ServiceConnection
    @JksTrustStore("ca.jks")
    private static final PostgreSQLContainer database = mock(PostgreSQLContainer.class);

  }

  @Configuration(proxyBeanMethods = false)
  static class ContainerConfiguration {

    @Bean
    @ServiceConnection
    @Ssl
    PostgreSQLContainer container() {
      return mock();
    }

  }

}
