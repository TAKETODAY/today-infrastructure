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

package infra.jdbc.config;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.beans.BeansException;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.jdbc.datasource.DelegatingDataSource;
import infra.test.classpath.ClassPathExclusions;
import infra.test.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceAutoConfiguration} with Hikari.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class HikariDataSourceConfigurationTests {

  private static final String PREFIX = "datasource.hikari.";

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
          .withPropertyValues("datasource.type=" + HikariDataSource.class.getName());

  @Test
  void testDataSourceExists() {
    this.contextRunner.run((context) -> {
      assertThat(context.getBeansOfType(DataSource.class)).hasSize(1);
      assertThat(context.getBeansOfType(HikariDataSource.class)).hasSize(1);
    });
  }

  @Test
  void testDataSourcePropertiesOverridden() {
    this.contextRunner
            .withPropertyValues(PREFIX + "jdbc-url=jdbc:foo//bar/spam", "datasource.hikari.max-lifetime=1234")
            .run((context) -> {
              HikariDataSource ds = context.getBean(HikariDataSource.class);
              assertThat(ds.getJdbcUrl()).isEqualTo("jdbc:foo//bar/spam");
              assertThat(ds.getMaxLifetime()).isEqualTo(1234);
            });
  }

  @Test
  void testDataSourceGenericPropertiesOverridden() {
    this.contextRunner
            .withPropertyValues(PREFIX + "data-source-properties.dataSourceClassName=org.h2.JDBCDataSource")
            .run((context) -> {
              HikariDataSource ds = context.getBean(HikariDataSource.class);
              assertThat(ds.getDataSourceProperties().getProperty("dataSourceClassName"))
                      .isEqualTo("org.h2.JDBCDataSource");

            });
  }

  @Test
  void testDataSourceDefaultsPreserved() {
    this.contextRunner.run((context) -> {
      HikariDataSource ds = context.getBean(HikariDataSource.class);
      assertThat(ds.getMaxLifetime()).isEqualTo(1800000);
    });
  }

  @Test
  void nameIsAliasedToPoolName() {
    this.contextRunner.withPropertyValues("datasource.name=myDS").run((context) -> {
      HikariDataSource ds = context.getBean(HikariDataSource.class);
      assertThat(ds.getPoolName()).isEqualTo("myDS");

    });
  }

  @Test
  void poolNameTakesPrecedenceOverName() {
    this.contextRunner.withPropertyValues("datasource.name=myDS", PREFIX + "pool-name=myHikariDS")
            .run((context) -> {
              HikariDataSource ds = context.getBean(HikariDataSource.class);
              assertThat(ds.getPoolName()).isEqualTo("myHikariDS");
            });
  }

  @Test
  @ClassPathOverrides("org.crac:crac:1.3.0")
  void whenCheckpointRestoreIsAvailableHikariAutoConfigRegistersLifecycleBean() {
    this.contextRunner.withPropertyValues("datasource.type=" + HikariDataSource.class.getName())
            .run((context) -> assertThat(context).hasSingleBean(HikariCheckpointRestoreLifecycle.class));
  }

  @Test
  @ClassPathOverrides("org.crac:crac:1.3.0")
  void whenCheckpointRestoreIsAvailableAndDataSourceHasBeenWrappedHikariAutoConfigRegistersLifecycleBean() {
    this.contextRunner.withUserConfiguration(DataSourceWrapperConfiguration.class)
            .run((context) -> assertThat(context).hasSingleBean(HikariCheckpointRestoreLifecycle.class));
  }

  @Test
  @ClassPathExclusions("crac-*.jar")
  void whenCheckpointRestoreIsNotAvailableHikariAutoConfigDoesNotRegisterLifecycleBean() {
    this.contextRunner.withPropertyValues("datasource.type=" + HikariDataSource.class.getName())
            .run((context) -> assertThat(context).doesNotHaveBean(HikariCheckpointRestoreLifecycle.class));
  }

  @Configuration(proxyBeanMethods = false)
  static class DataSourceWrapperConfiguration {

    @Bean
    static BeanPostProcessor dataSourceWrapper() {
      return new InitializationBeanPostProcessor() {

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
          if (bean instanceof DataSource dataSource) {
            return new DelegatingDataSource(dataSource);
          }
          return bean;
        }

      };
    }

  }

}
