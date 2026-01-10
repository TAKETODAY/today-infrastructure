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

package infra.annotation.config.jdbc;

import org.junit.jupiter.api.Test;

import infra.app.test.context.runner.ApplicationContextRunner;
import infra.context.annotation.config.AutoConfigurations;
import infra.jdbc.core.JdbcOperations;
import infra.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import infra.jdbc.core.simple.JdbcClient;

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