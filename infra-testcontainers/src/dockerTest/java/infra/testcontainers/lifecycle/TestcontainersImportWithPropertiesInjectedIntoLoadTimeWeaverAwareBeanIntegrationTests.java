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

package infra.testcontainers.lifecycle;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

import infra.app.test.context.TestConfiguration;
import infra.context.annotation.Bean;
import infra.context.properties.ConfigurationProperties;
import infra.context.properties.EnableConfigurationProperties;
import infra.context.weaving.LoadTimeWeaverAware;
import infra.instrument.classloading.LoadTimeWeaver;
import infra.test.context.DynamicPropertyRegistry;
import infra.test.context.DynamicPropertySource;
import infra.test.context.junit.jupiter.InfraExtension;
import infra.test.testcontainers.DisabledIfDockerUnavailable;
import infra.test.testcontainers.TestImage;
import infra.testcontainers.context.ImportTestcontainers;
import infra.testcontainers.lifecycle.TestcontainersImportWithPropertiesInjectedIntoLoadTimeWeaverAwareBeanIntegrationTests.Containers;

/**
 * Tests for {@link ImportTestcontainers} when properties are being injected into a
 * {@link LoadTimeWeaverAware} bean.
 *
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
@DisabledIfDockerUnavailable
@ImportTestcontainers(Containers.class)
class TestcontainersImportWithPropertiesInjectedIntoLoadTimeWeaverAwareBeanIntegrationTests {

  // gh-38913

  @Test
  void starts() {
  }

  @TestConfiguration
  @EnableConfigurationProperties(MockDataSourceProperties.class)
  static class Config {

    @Bean
    MockEntityManager mockEntityManager(MockDataSourceProperties properties) {
      return new MockEntityManager();
    }

  }

  static class MockEntityManager implements LoadTimeWeaverAware {

    @Override
    public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
    }

  }

  @ConfigurationProperties("datasource")
  public static class MockDataSourceProperties {

    private @Nullable String url;

    public @Nullable String getUrl() {
      return this.url;
    }

    public void setUrl(@Nullable String url) {
      this.url = url;
    }

  }

  static class Containers {

    @Container
    static PostgreSQLContainer container = TestImage.container(PostgreSQLContainer.class);

    @DynamicPropertySource
    static void setConnectionProperties(DynamicPropertyRegistry registry) {
      registry.add("datasource.url", container::getJdbcUrl);
      registry.add("datasource.password", container::getPassword);
      registry.add("datasource.username", container::getUsername);
    }

  }

}
