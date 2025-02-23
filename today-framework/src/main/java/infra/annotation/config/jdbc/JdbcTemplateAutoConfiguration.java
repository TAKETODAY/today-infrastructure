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

import javax.sql.DataSource;

import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Lazy;
import infra.context.annotation.Primary;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnSingleCandidate;
import infra.context.properties.EnableConfigurationProperties;
import infra.jdbc.core.JdbcOperations;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.namedparam.NamedParameterJdbcOperations;
import infra.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import infra.jdbc.support.SQLExceptionTranslator;
import infra.lang.Nullable;
import infra.stereotype.Component;
import infra.util.CollectionUtils;

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

  private JdbcTemplateAutoConfiguration() {
  }

  @Primary
  @Component
  @ConditionalOnMissingBean(JdbcOperations.class)
  public static JdbcTemplate jdbcTemplate(DataSource dataSource, JdbcProperties properties,
          ObjectProvider<SQLExceptionTranslator> sqlExceptionTranslators) {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    JdbcProperties.Template template = properties.getTemplate();
    jdbcTemplate.setFetchSize(template.getFetchSize());
    jdbcTemplate.setMaxRows(template.getMaxRows());
    if (template.getQueryTimeout() != null) {
      jdbcTemplate.setQueryTimeout((int) template.getQueryTimeout().getSeconds());
    }

    SQLExceptionTranslator sqlExceptionTranslator = sqlExceptionTranslators.getIfUnique();
    if (sqlExceptionTranslator != null) {
      jdbcTemplate.setExceptionTranslator(sqlExceptionTranslator);
    }
    return jdbcTemplate;
  }

  @Primary
  @Component
  @ConditionalOnSingleCandidate(JdbcTemplate.class)
  @ConditionalOnMissingBean(NamedParameterJdbcOperations.class)
  public static NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
    return new NamedParameterJdbcTemplate(jdbcTemplate);
  }

}


