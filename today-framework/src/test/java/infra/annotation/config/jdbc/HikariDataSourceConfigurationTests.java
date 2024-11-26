/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.annotation.config.jdbc;

import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import infra.beans.BeansException;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.config.BeanPostProcessor;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.config.AutoConfigurations;
import infra.app.jdbc.HikariCheckpointRestoreLifecycle;
import infra.app.test.context.runner.ApplicationContextRunner;
import infra.jdbc.datasource.DelegatingDataSource;
import infra.test.classpath.ClassPathExclusions;

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
  void whenCheckpointRestoreIsAvailableHikariAutoConfigRegistersLifecycleBean() {
    this.contextRunner.withPropertyValues("datasource.type=" + HikariDataSource.class.getName())
            .run((context) -> assertThat(context).hasSingleBean(HikariCheckpointRestoreLifecycle.class));
  }

  @Test
  void whenCheckpointRestoreIsAvailableAndDataSourceHasBeenWrappedHikariAutoConfigRegistersLifecycleBean() {
    this.contextRunner.withUserConfiguration(DataSourceWrapperConfiguration.class)
            .withPropertyValues("datasource.type=" + HikariDataSource.class.getName())
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
