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

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.factory.BeanCreationException;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Specific tests for {@link TestDatabaseAutoConfiguration} when no embedded database is
 * available.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@ClassPathExclusions({ "h2-*.jar", "hsqldb-*.jar", "derby-*.jar" })
class TestDatabaseAutoConfigurationNoEmbeddedTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withUserConfiguration(ExistingDataSourceConfiguration.class)
          .withConfiguration(AutoConfigurations.of(TestDatabaseAutoConfiguration.class));

  @Test
  void applyAnyReplace() {
    this.contextRunner.run((context) -> assertThat(context).getFailure()
            .isInstanceOf(BeanCreationException.class)
            .hasMessageContaining("Failed to replace DataSource with an embedded database for tests.")
            .hasMessageContaining("If you want an embedded database please put a supported one on the classpath")
            .hasMessageContaining("or tune the replace attribute of @AutoConfigureTestDatabase."));
  }

  @Test
  void applyNoReplace() {
    this.contextRunner.withPropertyValues("infra.test.database.replace=NONE").run((context) -> {
      assertThat(context).hasSingleBean(DataSource.class);
      assertThat(context).getBean(DataSource.class).isSameAs(context.getBean("myCustomDataSource"));
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class ExistingDataSourceConfiguration {

    @Bean
    DataSource myCustomDataSource() {
      return mock(DataSource.class);
    }

  }

}
