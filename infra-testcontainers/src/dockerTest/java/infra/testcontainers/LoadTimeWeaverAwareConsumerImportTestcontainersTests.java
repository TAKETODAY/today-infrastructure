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

package infra.testcontainers;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.weaving.LoadTimeWeaverAware;
import infra.instrument.classloading.LoadTimeWeaver;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseType;
import infra.test.testcontainers.DisabledIfDockerUnavailable;
import infra.testcontainers.context.ImportTestcontainers;
import infra.testcontainers.service.connection.DatabaseConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

@InfraTest
@DisabledIfDockerUnavailable
@ImportTestcontainers(LoadTimeWeaverAwareConsumerContainers.class)
class LoadTimeWeaverAwareConsumerImportTestcontainersTests implements LoadTimeWeaverAwareConsumerContainers {

  @Autowired
  private LoadTimeWeaverAwareConsumer consumer;

  @Test
  void loadTimeWeaverAwareBeanCanUseJdbcUrlFromContainerBasedConnectionDetails() {
    assertThat(this.consumer.jdbcUrl).isNotNull();
  }

  @Configuration
  static class TestConfiguration {

    @Bean
    DataSource dataSource() {
      EmbeddedDatabaseFactory embeddedDatabaseFactory = new EmbeddedDatabaseFactory();
      embeddedDatabaseFactory.setGenerateUniqueDatabaseName(true);
      embeddedDatabaseFactory.setDatabaseType(EmbeddedDatabaseType.H2);
      return embeddedDatabaseFactory.getDatabase();
    }

    @Bean
    LoadTimeWeaverAwareConsumer loadTimeWeaverAwareConsumer(DatabaseConnectionDetails connectionDetails) {
      return new LoadTimeWeaverAwareConsumer(connectionDetails);
    }

  }

  static class LoadTimeWeaverAwareConsumer implements LoadTimeWeaverAware {

    private final String jdbcUrl;

    LoadTimeWeaverAwareConsumer(DatabaseConnectionDetails connectionDetails) {
      this.jdbcUrl = connectionDetails.getJdbcUrl();
    }

    @Override
    public void setLoadTimeWeaver(LoadTimeWeaver loadTimeWeaver) {
    }

  }

}
