/*
 * Copyright 2017 - 2025 the original author or authors.
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

import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnSingleCandidate;
import infra.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import infra.jdbc.core.simple.JdbcClient;
import infra.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JdbcClient}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/13 15:26
 */
@DisableDIAutoConfiguration(after = JdbcTemplateAutoConfiguration.class)
@ConditionalOnSingleCandidate(NamedParameterJdbcTemplate.class)
@ConditionalOnMissingBean(JdbcClient.class)
public class JdbcClientAutoConfiguration {

  private JdbcClientAutoConfiguration() {
  }

  @Component
  public static JdbcClient jdbcClient(NamedParameterJdbcTemplate jdbcTemplate) {
    return JdbcClient.create(jdbcTemplate);
  }

}
