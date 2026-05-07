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

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServiceConnectionAutoConfigurationRegistrar}.
 *
 * @author Daeho Kwon
 */
class ServiceConnectionAutoConfigurationRegistrarTests {

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
