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
import infra.stereotype.Component;

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


