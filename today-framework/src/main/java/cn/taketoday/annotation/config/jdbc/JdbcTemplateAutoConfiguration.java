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

import javax.sql.DataSource;

import cn.taketoday.context.annotation.Lazy;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnSingleCandidate;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.jdbc.core.JdbcOperations;
import cn.taketoday.jdbc.core.JdbcTemplate;
import cn.taketoday.jdbc.core.namedparam.NamedParameterJdbcOperations;
import cn.taketoday.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import cn.taketoday.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JdbcTemplate} and
 * {@link NamedParameterJdbcTemplate}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/23 17:56
 */
@Lazy
@ConditionalOnClass({ DataSource.class, JdbcTemplate.class })
@ConditionalOnSingleCandidate(DataSource.class)
@EnableConfigurationProperties(JdbcProperties.class)
@DisableDIAutoConfiguration(after = DataSourceAutoConfiguration.class)
public class JdbcTemplateAutoConfiguration {

  @Primary
  @Component
  @ConditionalOnMissingBean(JdbcOperations.class)
  static JdbcTemplate jdbcTemplate(DataSource dataSource, JdbcProperties properties) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    JdbcProperties.Template template = properties.getTemplate();
    jdbcTemplate.setFetchSize(template.getFetchSize());
    jdbcTemplate.setMaxRows(template.getMaxRows());
    if (template.getQueryTimeout() != null) {
      jdbcTemplate.setQueryTimeout((int) template.getQueryTimeout().getSeconds());
    }
    return jdbcTemplate;
  }

  @Primary
  @Component
  @ConditionalOnSingleCandidate(JdbcTemplate.class)
  @ConditionalOnMissingBean(NamedParameterJdbcOperations.class)
  static NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
    return new NamedParameterJdbcTemplate(jdbcTemplate);
  }

}


