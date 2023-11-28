/*
 * Copyright 2017 - 2023 the original author or authors.
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

import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.jdbc.core.JdbcOperations;
import cn.taketoday.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import cn.taketoday.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/13 15:28
 */
class JdbcClientAutoConfigurationTests {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
          .withPropertyValues("datasource.generate-unique-name=true")
          .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
                  JdbcClientAutoConfiguration.class));

  @Test
  void jdbcClientWhenNoAvailableJdbcTemplateIsNotCreated() {
    new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
            .run((context) -> assertThat(context).doesNotHaveBean(JdbcClient.class));
  }

  @Test
  void jdbcClientWhenExistingJdbcTemplateIsCreated() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(JdbcClient.class);
      NamedParameterJdbcTemplate namedParameterJdbcTemplate = context.getBean(NamedParameterJdbcTemplate.class);
      assertThat(namedParameterJdbcTemplate.getJdbcOperations()).isEqualTo(context.getBean(JdbcOperations.class));
    });
  }

  @Test
  void jdbcClientWithCustomJdbcClientIsNotCreated() {
    this.contextRunner.withBean("customJdbcClient", JdbcClient.class, () -> mock(JdbcClient.class))
            .run((context) -> {
              assertThat(context).hasSingleBean(JdbcClient.class);
              assertThat(context.getBean(JdbcClient.class)).isEqualTo(context.getBean("customJdbcClient"));
            });
  }

}