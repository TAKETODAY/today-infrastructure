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

package infra.jdbc.config.health;

import org.jspecify.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import infra.app.health.config.contributor.ConditionalOnEnabledHealthIndicator;
import infra.app.health.contributor.CompositeHealthContributor;
import infra.app.health.contributor.HealthContributor;
import infra.app.health.contributor.HealthContributors;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.SimpleAutowireCandidateResolver;
import infra.context.annotation.config.DisableDIAutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.properties.EnableConfigurationProperties;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.datasource.lookup.AbstractRoutingDataSource;
import infra.jdbc.health.DataSourceHealthIndicator;
import infra.jdbc.metadata.CompositeDataSourcePoolMetadataProvider;
import infra.jdbc.metadata.DataSourcePoolMetadata;
import infra.jdbc.metadata.DataSourcePoolMetadataProvider;
import infra.lang.Assert;
import infra.stereotype.Component;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DataSourceHealthIndicator}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Arthur Kalimullin
 * @author Julio Gomez
 * @author Safeer Ansari
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@DisableDIAutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass(ConditionalOnEnabledHealthIndicator.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnEnabledHealthIndicator("db")
@EnableConfigurationProperties(DataSourceHealthIndicatorProperties.class)
public final class DataSourceHealthContributorAutoConfiguration {

  @Component
  @ConditionalOnMissingBean(name = { "dbHealthIndicator", "dbHealthContributor" })
  static HealthContributor dbHealthContributor(ConfigurableBeanFactory beanFactory,
          DataSourceHealthIndicatorProperties properties, List<DataSourcePoolMetadataProvider> metadataProviders) {
    Map<String, DataSource> dataSources = SimpleAutowireCandidateResolver.resolveAutowireCandidates(beanFactory,
            DataSource.class, false, true);

    DataSourcePoolMetadataProvider poolMetadataProvider = new CompositeDataSourcePoolMetadataProvider(metadataProviders);

    if (properties.ignoreRoutingDataSources) {
      Map<String, DataSource> filtered = dataSources.entrySet()
              .stream()
              .filter((e) -> !isRoutingDataSource(e.getValue()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      return createContributor(filtered, poolMetadataProvider);
    }
    return createContributor(dataSources, poolMetadataProvider);
  }

  private static HealthContributor createContributor(Map<String, DataSource> beans, DataSourcePoolMetadataProvider poolMetadataProvider) {
    Assert.notEmpty(beans, "'beans' must not be empty");
    if (beans.size() == 1) {
      return createContributor(beans.values().iterator().next(), poolMetadataProvider);
    }
    return CompositeHealthContributor.fromMap(beans, dataSource -> createContributor(dataSource, poolMetadataProvider));
  }

  private static HealthContributor createContributor(DataSource source, DataSourcePoolMetadataProvider poolMetadataProvider) {
    if (isRoutingDataSource(source)) {
      return new RoutingDataSourceHealthContributor(extractRoutingDataSource(source), dataSource -> createContributor(dataSource, poolMetadataProvider));
    }
    return new DataSourceHealthIndicator(source, getValidationQuery(source, poolMetadataProvider));
  }

  private static @Nullable String getValidationQuery(DataSource source, DataSourcePoolMetadataProvider poolMetadataProvider) {
    DataSourcePoolMetadata poolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(source);
    return (poolMetadata != null) ? poolMetadata.getValidationQuery() : null;
  }

  private static boolean isRoutingDataSource(DataSource dataSource) {
    if (dataSource instanceof AbstractRoutingDataSource) {
      return true;
    }
    try {
      return dataSource.isWrapperFor(AbstractRoutingDataSource.class);
    }
    catch (SQLException ex) {
      return false;
    }
  }

  private static AbstractRoutingDataSource extractRoutingDataSource(DataSource dataSource) {
    if (dataSource instanceof AbstractRoutingDataSource routingDataSource) {
      return routingDataSource;
    }
    try {
      return dataSource.unwrap(AbstractRoutingDataSource.class);
    }
    catch (SQLException ex) {
      throw new IllegalStateException("Failed to unwrap AbstractRoutingDataSource from " + dataSource, ex);
    }
  }

  /**
   * {@link CompositeHealthContributor} used for {@link AbstractRoutingDataSource} beans
   * where the overall health is composed of a {@link DataSourceHealthIndicator} for
   * each routed datasource.
   */
  static class RoutingDataSourceHealthContributor implements CompositeHealthContributor {

    private final CompositeHealthContributor delegate;

    private static final String UNNAMED_DATASOURCE_KEY = "unnamed";

    RoutingDataSourceHealthContributor(AbstractRoutingDataSource routingDataSource,
            Function<DataSource, HealthContributor> contributorFunction) {
      Map<String, DataSource> routedDataSources = routingDataSource.getResolvedDataSources()
              .entrySet()
              .stream()
              .collect(Collectors.toMap((e) -> Objects.toString(e.getKey(), UNNAMED_DATASOURCE_KEY),
                      Map.Entry::getValue));
      this.delegate = CompositeHealthContributor.fromMap(routedDataSources, contributorFunction);
    }

    @Override
    public @Nullable HealthContributor getContributor(String name) {
      return this.delegate.getContributor(name);
    }

    @Override
    public Stream<HealthContributors.Entry> stream() {
      return this.delegate.stream();
    }

  }

}
