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

package infra.jdbc.test.config;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;
import infra.jdbc.test.config.AutoConfigureTestDatabaseDockerComposeIntegrationTests.SetupDockerCompose;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import infra.app.test.config.OverrideAutoConfiguration;
import infra.app.test.context.InfraTest;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.ImportAutoConfiguration;
import infra.core.io.ClassPathResource;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.datasource.embedded.EmbeddedDatabase;
import infra.test.context.ContextConfiguration;
import infra.test.context.support.TestPropertySourceUtils;
import infra.test.testcontainers.DisabledIfDockerUnavailable;
import infra.test.testcontainers.TestImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigureTestDatabase} with Docker Compose.
 *
 * @author Phillip Webb
 */
@InfraTest
@ContextConfiguration(initializers = SetupDockerCompose.class)
@AutoConfigureTestDatabase
@OverrideAutoConfiguration(enabled = false)
@DisabledIfDockerUnavailable
class AutoConfigureTestDatabaseDockerComposeIntegrationTests {

  @Autowired
  private DataSource dataSource;

  @Test
  void dataSourceIsNotReplaced() {
    assertThat(this.dataSource).isInstanceOf(HikariDataSource.class).isNotInstanceOf(EmbeddedDatabase.class);
  }

  @Configuration
  @ImportAutoConfiguration(DataSourceAutoConfiguration.class)
  static class Config {

  }

  static class SetupDockerCompose implements ApplicationContextInitializer {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      try {
        Path composeFile = Files.createTempFile("", "-postgres-compose");
        String composeFileContent = new ClassPathResource("postgres-compose.yaml")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("{imageName}", TestImage.POSTGRESQL.toString());
        Files.writeString(composeFile, composeFileContent);
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
                "infra.docker.compose.skip.in-tests=false", "infra.docker.compose.stop.command=down",
                "infra.docker.compose.file=" + composeFile.toAbsolutePath());
      }
      catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

  }

}
