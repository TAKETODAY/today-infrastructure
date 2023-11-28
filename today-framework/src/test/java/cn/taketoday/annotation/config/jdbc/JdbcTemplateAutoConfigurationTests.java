/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.annotation.config.jdbc;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import javax.sql.DataSource;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.jdbc.core.JdbcOperations;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.core.namedparam.NamedParameterJdbcOperations;
import cn.taketoday.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdbcTemplateAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author Dan Zheng
 */
class JdbcTemplateAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withPropertyValues("datasource.generate-unique-name=true").withConfiguration(
                  AutoConfigurations.of(DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class));

  @Test
  void testJdbcTemplateExists() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(JdbcOperations.class);
      JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
      assertThat(jdbcTemplate.getDataSource()).isEqualTo(context.getBean(DataSource.class));
      assertThat(jdbcTemplate.getFetchSize()).isEqualTo(-1);
      assertThat(jdbcTemplate.getQueryTimeout()).isEqualTo(-1);
      assertThat(jdbcTemplate.getMaxRows()).isEqualTo(-1);
    });
  }

  @Test
  void testJdbcTemplateWithCustomProperties() {
    this.contextRunner.withPropertyValues("jdbc.template.fetch-size:100",
            "jdbc.template.query-timeout:60", "jdbc.template.max-rows:1000").run((context) -> {
      assertThat(context).hasSingleBean(JdbcOperations.class);
      JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
      assertThat(jdbcTemplate.getDataSource()).isNotNull();
      assertThat(jdbcTemplate.getFetchSize()).isEqualTo(100);
      assertThat(jdbcTemplate.getQueryTimeout()).isEqualTo(60);
      assertThat(jdbcTemplate.getMaxRows()).isEqualTo(1000);
    });
  }

  @Test
  void testJdbcTemplateExistsWithCustomDataSource() {
    this.contextRunner.withUserConfiguration(TestDataSourceConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(JdbcOperations.class);
      JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
      assertThat(jdbcTemplate.getDataSource()).isEqualTo(context.getBean("customDataSource"));
    });
  }

  @Test
  void testNamedParameterJdbcTemplateExists() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
      NamedParameterJdbcTemplate namedParameterJdbcTemplate = context.getBean(NamedParameterJdbcTemplate.class);
      assertThat(namedParameterJdbcTemplate.getJdbcOperations()).isEqualTo(context.getBean(JdbcOperations.class));
    });
  }

  @Test
  void testMultiDataSource() {
    this.contextRunner.withUserConfiguration(MultiDataSourceConfiguration.class).run((context) -> {
      assertThat(context).doesNotHaveBean(JdbcOperations.class);
      assertThat(context).doesNotHaveBean(NamedParameterJdbcOperations.class);
    });
  }

  @Test
  void testMultiJdbcTemplate() {
    this.contextRunner.withUserConfiguration(MultiJdbcTemplateConfiguration.class)
            .run((context) -> assertThat(context).doesNotHaveBean(NamedParameterJdbcOperations.class));
  }

  @Test
  void testMultiDataSourceUsingPrimary() {
    this.contextRunner.withUserConfiguration(MultiDataSourceUsingPrimaryConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(JdbcOperations.class);
      assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
      assertThat(context.getBean(JdbcTemplate.class).getDataSource())
              .isEqualTo(context.getBean("test1DataSource"));
    });
  }

  @Test
  void testMultiJdbcTemplateUsingPrimary() {
    this.contextRunner.withUserConfiguration(MultiJdbcTemplateUsingPrimaryConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
      assertThat(context.getBean(NamedParameterJdbcTemplate.class).getJdbcOperations())
              .isEqualTo(context.getBean("test1Template"));
    });
  }

  @Test
  void testExistingCustomJdbcTemplate() {
    this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(JdbcOperations.class);
      assertThat(context.getBean(JdbcOperations.class)).isEqualTo(context.getBean("customJdbcOperations"));
    });
  }

  @Test
  void testExistingCustomNamedParameterJdbcTemplate() {
    this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
      assertThat(context).hasSingleBean(NamedParameterJdbcOperations.class);
      assertThat(context.getBean(NamedParameterJdbcOperations.class))
              .isEqualTo(context.getBean("customNamedParameterJdbcOperations"));
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomConfiguration {

    @Bean
    JdbcOperations customJdbcOperations(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    @Bean
    NamedParameterJdbcOperations customNamedParameterJdbcOperations(DataSource dataSource) {
      return new NamedParameterJdbcTemplate(dataSource);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestDataSourceConfiguration {

    @Bean
    DataSource customDataSource() {
      return new TestDataSource();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MultiJdbcTemplateConfiguration {

    @Bean
    JdbcTemplate test1Template() {
      return mock(JdbcTemplate.class);
    }

    @Bean
    JdbcTemplate test2Template() {
      return mock(JdbcTemplate.class);
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class MultiJdbcTemplateUsingPrimaryConfiguration {

    @Bean
    @Primary
    JdbcTemplate test1Template() {
      return mock(JdbcTemplate.class);
    }

    @Bean
    JdbcTemplate test2Template() {
      return mock(JdbcTemplate.class);
    }

  }

  static class DataSourceInitializationValidator {

    private final Integer count;

    DataSourceInitializationValidator(JdbcTemplate jdbcTemplate) {
      this.count = jdbcTemplate.queryForObject("SELECT COUNT(*) from BAR", Integer.class);
    }

  }

  static class DataSourceMigrationValidator {

    private final Integer count;

    DataSourceMigrationValidator(JdbcTemplate jdbcTemplate) {
      this.count = jdbcTemplate.queryForObject("SELECT COUNT(*) from CITY", Integer.class);
    }

  }

  static class NamedParameterDataSourceMigrationValidator {

    private final Integer count;

    NamedParameterDataSourceMigrationValidator(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
      this.count = namedParameterJdbcTemplate.queryForObject("SELECT COUNT(*) from CITY", Collections.emptyMap(),
              Integer.class);
    }

  }

}
