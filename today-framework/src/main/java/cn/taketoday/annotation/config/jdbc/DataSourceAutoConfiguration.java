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
import javax.sql.XADataSource;

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.config.DisableDIAutoConfiguration;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.AnyNestedCondition;
import cn.taketoday.context.condition.ConditionMessage;
import cn.taketoday.context.condition.ConditionOutcome;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnProperty;
import cn.taketoday.context.condition.InfraCondition;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.jdbc.config.DataSourceBuilder;
import cn.taketoday.jdbc.datasource.embedded.EmbeddedDatabaseType;
import cn.taketoday.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link DataSource}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/23 17:12
 */
@DisableDIAutoConfiguration
@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })
@EnableConfigurationProperties(DataSourceProperties.class)
@Import(DataSourcePoolMetadataProvidersConfiguration.class)
public class DataSourceAutoConfiguration {

  @Configuration(proxyBeanMethods = false)
  @Conditional(EmbeddedDatabaseCondition.class)
  @ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
  @Import(EmbeddedDataSourceConfiguration.class)
  protected static class EmbeddedDatabaseConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(PooledDataSourceCondition.class)
  @ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
  @Import({ DataSourceConfiguration.Hikari.class, DataSourceConfiguration.Tomcat.class,
          DataSourceConfiguration.Dbcp2.class, DataSourceConfiguration.OracleUcp.class,
          DataSourceConfiguration.Generic.class, DataSourceJmxConfiguration.class })
  protected static class PooledDataSourceConfiguration {

  }

  /**
   * {@link AnyNestedCondition} that checks that either {@code datasource.type}
   * is set or {@link PooledDataSourceAvailableCondition} applies.
   */
  static class PooledDataSourceCondition extends AnyNestedCondition {

    PooledDataSourceCondition() {
      super(ConfigurationPhase.PARSE_CONFIGURATION);
    }

    @ConditionalOnProperty(prefix = "datasource", name = "type")
    static class ExplicitType {

    }

    @Conditional(PooledDataSourceAvailableCondition.class)
    static class PooledDataSourceAvailable {

    }

  }

  /**
   * {@link Condition} to test if a supported connection pool is available.
   */
  static class PooledDataSourceAvailableCondition extends InfraCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
      ConditionMessage.Builder message = ConditionMessage.forCondition("PooledDataSource");
      if (DataSourceBuilder.findType(context.getClassLoader()) != null) {
        return ConditionOutcome.match(message.foundExactly("supported DataSource"));
      }
      return ConditionOutcome.noMatch(message.didNotFind("supported DataSource").atAll());
    }

  }

  /**
   * {@link Condition} to detect when an embedded {@link DataSource} type can be used.
   * If a pooled {@link DataSource} is available, it will always be preferred to an
   * {@code EmbeddedDatabase}.
   */
  static class EmbeddedDatabaseCondition extends InfraCondition {

    private static final String DATASOURCE_URL_PROPERTY = "datasource.url";

    private final InfraCondition pooledCondition = new PooledDataSourceCondition();

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
      ConditionMessage.Builder message = ConditionMessage.forCondition("EmbeddedDataSource");
      if (hasDataSourceUrlProperty(context)) {
        return ConditionOutcome.noMatch(message.because(DATASOURCE_URL_PROPERTY + " is set"));
      }
      if (anyMatches(context, metadata, this.pooledCondition)) {
        return ConditionOutcome.noMatch(message.foundExactly("supported pooled data source"));
      }
      EmbeddedDatabaseType type = EmbeddedDatabaseConnection.get(context.getClassLoader()).getType();
      if (type == null) {
        return ConditionOutcome.noMatch(message.didNotFind("embedded database").atAll());
      }
      return ConditionOutcome.match(message.found("embedded database").items(type));
    }

    private boolean hasDataSourceUrlProperty(ConditionContext context) {
      Environment environment = context.getEnvironment();
      if (environment.containsProperty(DATASOURCE_URL_PROPERTY)) {
        try {
          return StringUtils.hasText(environment.getProperty(DATASOURCE_URL_PROPERTY));
        }
        catch (IllegalArgumentException ex) {
          // Ignore unresolvable placeholder errors
        }
      }
      return false;
    }

  }

}
