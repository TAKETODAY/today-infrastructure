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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import javax.sql.DataSource;

import infra.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.core.env.Environment;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.config.EmbeddedDatabaseConnection;
import infra.jdbc.config.JdbcConnectionDetails;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.datasource.DriverManagerDataSource;
import infra.jdbc.datasource.embedded.EmbeddedDatabase;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseType;
import infra.jdbc.test.config.TestDatabaseAutoConfiguration.EmbeddedDataSourceFactory;
import infra.jdbc.test.config.TestDatabaseAutoConfiguration.EmbeddedDataSourceFactoryBean;
import infra.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TestDatabaseAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class TestDatabaseAutoConfigurationTests {

  private static final String TESTCONTAINERS_URL = "jdbc:tc:postgresql:16:///test";

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(TestDatabaseAutoConfiguration.class));

  @Test
  void replaceWithNoDataSourceAvailable() {
    this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(DataSource.class));
  }

  @Test
  void multipleDataSourcesWithoutPrimaryAreNotReplaced() {
    DataSource first = mock(DataSource.class);
    DataSource second = mock(DataSource.class);
    this.contextRunner.withBean("firstDataSource", DataSource.class, () -> first)
            .withBean("secondDataSource", DataSource.class, () -> second)
            .run(context -> assertThat(context.getBeansOfType(DataSource.class))
                    .containsExactlyInAnyOrderEntriesOf(Map.of("firstDataSource", first, "secondDataSource", second)));
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void programmaticallyRegisteredDataSourceIsReplacedEvenWithTestcontainersUrl(boolean annotatedDefinition) {
    var definition = annotatedDefinition ? new AnnotatedGenericBeanDefinition(DriverManagerDataSource.class)
            : new RootBeanDefinition(DriverManagerDataSource.class);
    this.contextRunner.withInitializer(context -> ((BeanDefinitionRegistry) context.getBeanFactory())
                    .registerBeanDefinition("customDataSource", definition))
            .withPropertyValues("datasource.url=" + TESTCONTAINERS_URL)
            .run(context -> assertThat(context).getBean("customDataSource").isInstanceOf(EmbeddedDatabase.class));
  }

  @Test
  void nonTestPreservesAutoConfiguredDataSourceWithTestcontainersUrl() {
    withAutoConfiguredDataSource().withPropertyValues("datasource.url=" + TESTCONTAINERS_URL)
            .run(context -> assertThat(context).getBean(DriverManagerDataSource.class)
                    .extracting(DriverManagerDataSource::getUrl).isEqualTo(TESTCONTAINERS_URL));
  }

  @Test
  void anyReplacesAutoConfiguredDataSourceWithTestcontainersUrl() {
    withAutoConfiguredDataSource().withPropertyValues("datasource.url=" + TESTCONTAINERS_URL, "test.database.replace=ANY")
            .run(context -> assertThat(context).getBean(DataSource.class).isInstanceOf(EmbeddedDatabase.class));
  }

  @Test
  void connectionDetailsRegisteredAsSingletonDoNotPreventReplacement() {
    withAutoConfiguredDataSource().withPropertyValues("datasource.url=jdbc:h2:mem:application")
            .withInitializer(context -> context.getBeanFactory()
                    .registerSingleton("customJdbcConnectionDetails", mock(JdbcConnectionDetails.class)))
            .run(context -> assertThat(context).getBean(DataSource.class).isInstanceOf(EmbeddedDatabase.class));
  }

  @Test
  void embeddedDatabaseCanBeCreatedWithNonConfigurableEnvironment() throws Exception {
    Environment environment = mock(Environment.class);
    given(environment.getProperty("test.database.connection", EmbeddedDatabaseConnection.class, EmbeddedDatabaseConnection.NONE))
            .willReturn(EmbeddedDatabaseConnection.HSQLDB);
    EmbeddedDatabase database = new EmbeddedDataSourceFactory(environment).getEmbeddedDatabase();
    try (var connection = database.getConnection()) {
      assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("HSQL Database Engine");
    }
    finally {
      database.shutdown();
    }
  }

  @Test
  void replaceWithUniqueDatabase() {
    this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(EmbeddedDataSourceFactoryBean.class);
      DataSource datasource = context.getBean(DataSource.class);
      JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
      jdbcTemplate.execute("create table example (id int, name varchar);");
      this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class).run((secondContext) -> {
        DataSource anotherDatasource = secondContext.getBean(DataSource.class);
        JdbcTemplate anotherJdbcTemplate = new JdbcTemplate(anotherDatasource);
        anotherJdbcTemplate.execute("create table example (id int, name varchar);");
      });
    });
  }

  @Test
  void whenUsingAotGeneratedArtifactsEmbeddedDataSourceFactoryBeanIsNotDefined() {
    this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class)
            .withSystemProperties("infra.aot.enabled=true")
            .run((context) -> assertThat(context).doesNotHaveBean(EmbeddedDataSourceFactoryBean.class));
  }

  private ApplicationContextRunner withAutoConfiguredDataSource() {
    return this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withPropertyValues("datasource.type=" + DriverManagerDataSource.class.getName());
  }

  @Configuration(proxyBeanMethods = false)
  static class ExistingDataSourceConfiguration {

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.HSQL).build();
    }

  }

}
