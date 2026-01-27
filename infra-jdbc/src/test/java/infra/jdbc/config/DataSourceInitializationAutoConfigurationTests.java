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

package infra.jdbc.config;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.jdbc.datasource.init.DatabasePopulator;
import infra.jdbc.init.DataSourceScriptDatabaseInitializer;
import infra.sql.config.init.ApplicationScriptDatabaseInitializer;
import infra.sql.init.AbstractScriptDatabaseInitializer;
import infra.sql.init.DatabaseInitializationSettings;
import infra.sql.init.dependency.DependsOnDatabaseInitialization;
import infra.test.context.FilteredClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceInitializationAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DataSourceInitializationAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DataSourceInitializationAutoConfiguration.class))
          .withPropertyValues("datasource.generate-unique-name:true");

  @Test
  void whenNoDataSourceIsAvailableThenAutoConfigurationBacksOff() {
    this.contextRunner
            .run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
  }

  @Test
  void whenDataSourceIsAvailableThenDataSourceInitializerIsAutoConfigured() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .run((context) -> assertThat(context).hasSingleBean(DataSourceScriptDatabaseInitializer.class));
  }

  @Test
  void whenDataSourceIsAvailableAndModeIsNeverThenInitializerIsNotAutoConfigured() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withPropertyValues("sql.init.mode:never")
            .run((context) -> assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class));
  }

  @Test
  void whenAnApplicationInitializerIsDefinedThenInitializerIsNotAutoConfigured() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withUserConfiguration(ApplicationDatabaseInitializerConfiguration.class)
            .run((context) -> assertThat(context).hasSingleBean(ApplicationScriptDatabaseInitializer.class)
                    .hasBean("customInitializer"));
  }

  @Test
  void whenAnInitializerIsDefinedThenApplicationInitializerIsStillAutoConfigured() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withUserConfiguration(DatabaseInitializerConfiguration.class)
            .run((context) -> assertThat(context).hasSingleBean(ApplicationDataSourceScriptDatabaseInitializer.class)
                    .hasBean("customInitializer"));
  }

  @Test
  void whenBeanIsAnnotatedAsDependingOnDatabaseInitializationThenItDependsOnDataSourceScriptDatabaseInitializer() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withUserConfiguration(DependsOnInitializedDatabaseConfiguration.class)
            .run((context) -> {
              ConfigurableBeanFactory beanFactory = context.getBeanFactory();
              BeanDefinition beanDefinition = beanFactory.getBeanDefinition(
                      "dataSourceInitializationAutoConfigurationTests.DependsOnInitializedDatabaseConfiguration");
              assertThat(beanDefinition.getDependsOn())
                      .containsExactlyInAnyOrder("dataSourceScriptDatabaseInitializer");
            });
  }

  @Test
  void whenADataSourceIsAvailableAndInfraJdbcIsNotThenAutoConfigurationBacksOff() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .withClassLoader(new FilteredClassLoader(DatabasePopulator.class))
            .run((context) -> {
              assertThat(context).hasSingleBean(DataSource.class);
              assertThat(context).doesNotHaveBean(AbstractScriptDatabaseInitializer.class);
            });
  }

  @Configuration(proxyBeanMethods = false)
  static class ApplicationDatabaseInitializerConfiguration {

    @Bean
    ApplicationScriptDatabaseInitializer customInitializer() {
      return mock(ApplicationScriptDatabaseInitializer.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class DatabaseInitializerConfiguration {

    @Bean
    DataSourceScriptDatabaseInitializer customInitializer() {
      return new DataSourceScriptDatabaseInitializer(mock(DataSource.class),
              new DatabaseInitializationSettings()) {

        @Override
        protected void runScripts(Scripts scripts) {
          // No-op
        }

        @Override
        protected boolean isEmbeddedDatabase() {
          return true;
        }

      };
    }

  }

  @Configuration(proxyBeanMethods = false)
  @DependsOnDatabaseInitialization
  static class DependsOnInitializedDatabaseConfiguration {

    DependsOnInitializedDatabaseConfiguration() {

    }

  }

}
