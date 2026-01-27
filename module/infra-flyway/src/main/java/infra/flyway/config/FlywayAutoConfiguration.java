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

package infra.flyway.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.extensibility.ConfigurationExtension;
import org.flywaydb.database.oracle.OracleConfigurationExtension;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.flywaydb.database.sqlserver.SQLServerConfigurationExtension;
import org.jspecify.annotations.Nullable;

import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.beans.factory.ObjectProvider;
import infra.context.annotation.Bean;
import infra.context.annotation.Conditional;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportRuntimeHints;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.annotation.config.EnableAutoConfiguration;
import infra.context.condition.AnyNestedCondition;
import infra.context.condition.ConditionalOnBean;
import infra.context.condition.ConditionalOnBooleanProperty;
import infra.context.condition.ConditionalOnClass;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.properties.ConfigurationPropertiesBinding;
import infra.context.properties.EnableConfigurationProperties;
import infra.core.Ordered;
import infra.core.TypeDescriptor;
import infra.core.annotation.Order;
import infra.core.conversion.GenericConverter;
import infra.core.io.ResourceLoader;
import infra.flyway.config.FlywayProperties.Oracle;
import infra.flyway.config.FlywayProperties.Postgresql;
import infra.flyway.config.FlywayProperties.Sqlserver;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.config.DataSourceBuilder;
import infra.jdbc.config.DatabaseDriver;
import infra.jdbc.config.JdbcConnectionDetails;
import infra.jdbc.datasource.SimpleDriverDataSource;
import infra.jdbc.support.JdbcUtils;
import infra.jdbc.support.MetaDataAccessException;
import infra.lang.Assert;
import infra.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;
import infra.util.PropertyMapper;
import infra.util.StringUtils;
import infra.util.function.SingletonSupplier;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Flyway database migrations.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Vedran Pavic
 * @author Stephane Nicoll
 * @author Jacques-Etienne Beaudet
 * @author Eddú Meléndez
 * @author Dominic Gunn
 * @author Dan Zheng
 * @author András Deák
 * @author Semyon Danilov
 * @author Chris Bono
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass(Flyway.class)
@Conditional(FlywayAutoConfiguration.FlywayDataSourceCondition.class)
@ConditionalOnBooleanProperty(name = "flyway.enabled", matchIfMissing = true)
@Import(DatabaseInitializationDependencyConfigurer.class)
@ImportRuntimeHints(FlywayAutoConfiguration.FlywayAutoConfigurationRuntimeHints.class)
public final class FlywayAutoConfiguration {

  @Bean
  @ConfigurationPropertiesBinding
  static StringOrNumberToMigrationVersionConverter stringOrNumberMigrationVersionConverter() {
    return new StringOrNumberToMigrationVersionConverter();
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(JdbcUtils.class)
  @ConditionalOnMissingBean(Flyway.class)
  @EnableConfigurationProperties(FlywayProperties.class)
  static class FlywayConfiguration {

    private final FlywayProperties properties;

    FlywayConfiguration(FlywayProperties properties) {
      this.properties = properties;
    }

    @Bean
    static ResourceProviderCustomizer resourceProviderCustomizer() {
      return new ResourceProviderCustomizer();
    }

    @Bean
    @ConditionalOnMissingBean(FlywayConnectionDetails.class)
    static PropertiesFlywayConnectionDetails flywayConnectionDetails(FlywayProperties properties) {
      return new PropertiesFlywayConnectionDetails(properties);
    }

    @Bean
    Flyway flyway(FlywayConnectionDetails connectionDetails, ResourceLoader resourceLoader,
            ObjectProvider<DataSource> dataSource,
            @FlywayDataSource @Nullable DataSource flywayDataSource,
            ObjectProvider<FlywayConfigurationCustomizer> fluentConfigurationCustomizers,
            ObjectProvider<JavaMigration> javaMigrations, ObjectProvider<Callback> callbacks,
            ResourceProviderCustomizer resourceProviderCustomizer) {
      FluentConfiguration configuration = new FluentConfiguration(resourceLoader.getClassLoader());
      configureDataSource(configuration, flywayDataSource, dataSource.getIfUnique(),
              connectionDetails);
      configureProperties(configuration, this.properties);
      configureCallbacks(configuration, callbacks.orderedStream().toList());
      configureJavaMigrations(configuration, javaMigrations.orderedStream().toList());
      fluentConfigurationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configuration));
      resourceProviderCustomizer.customize(configuration);
      return configuration.load();
    }

    private void configureDataSource(FluentConfiguration configuration, @Nullable DataSource flywayDataSource,
            @Nullable DataSource dataSource, FlywayConnectionDetails connectionDetails) {
      DataSource migrationDataSource = getMigrationDataSource(flywayDataSource, dataSource, connectionDetails);
      configuration.dataSource(migrationDataSource);
    }

    private DataSource getMigrationDataSource(@Nullable DataSource flywayDataSource,
            @Nullable DataSource dataSource, FlywayConnectionDetails connectionDetails) {
      if (flywayDataSource != null) {
        return flywayDataSource;
      }
      String url = connectionDetails.getJdbcUrl();
      if (url != null) {
        DataSourceBuilder<?> builder = DataSourceBuilder.create().type(SimpleDriverDataSource.class);
        builder.url(url);
        applyConnectionDetails(connectionDetails, builder);
        return builder.build();
      }
      String user = connectionDetails.getUsername();
      if (user != null && dataSource != null) {
        DataSourceBuilder<?> builder = DataSourceBuilder.derivedFrom(dataSource)
                .type(SimpleDriverDataSource.class);
        applyConnectionDetails(connectionDetails, builder);
        return builder.build();
      }
      Assert.state(dataSource != null, "Flyway migration DataSource missing");
      return dataSource;
    }

    private void applyConnectionDetails(FlywayConnectionDetails connectionDetails, DataSourceBuilder<?> builder) {
      builder.username(connectionDetails.getUsername());
      builder.password(connectionDetails.getPassword());
      String driverClassName = connectionDetails.getDriverClassName();
      if (StringUtils.hasText(driverClassName)) {
        builder.driverClassName(driverClassName);
      }
    }

    /**
     * Configure the given {@code configuration} using the given {@code properties}.
     * <p>
     * To maximize forwards- and backwards-compatibility method references are not
     * used.
     *
     * @param configuration the configuration
     * @param properties the properties
     */
    private void configureProperties(FluentConfiguration configuration, FlywayProperties properties) {
      // NOTE: Using method references in the mapper methods can break
      // back-compatibility (see gh-38164)
      PropertyMapper map = PropertyMapper.get();
      String[] locations = new LocationResolver(configuration.getDataSource())
              .resolveLocations(properties.getLocations())
              .toArray(new String[0]);
      configuration.locations(locations);
      map.from(properties.getCallbackLocations())
              .when((callbackLocations) -> !ObjectUtils.isEmpty(callbackLocations))
              .to((callbackLocations) -> configuration.callbackLocations(
                      new LocationResolver(configuration.getDataSource()).resolveLocations(callbackLocations)
                              .toArray(new String[0])));
      map.from(properties.isFailOnMissingLocations()).to(configuration::failOnMissingLocations);
      map.from(properties.getEncoding()).to(configuration::encoding);
      map.from(properties.getConnectRetries()).to(configuration::connectRetries);
      map.from(properties.getConnectRetriesInterval()).as(Duration::getSeconds).as(Long::intValue).to(configuration::connectRetriesInterval);
      map.from(properties.getLockRetryCount()).to(configuration::lockRetryCount);
      map.from(properties.getDefaultSchema()).to(configuration::defaultSchema);
      map.from(properties.getSchemas()).as(StringUtils::toStringArray).to(configuration::schemas);
      map.from(properties.isCreateSchemas()).to(configuration::createSchemas);
      map.from(properties.getTable()).to(configuration::table);
      map.from(properties.getTablespace()).to(configuration::tablespace);
      map.from(properties.getBaselineDescription()).to(configuration::baselineDescription);
      map.from(properties.getBaselineVersion()).to(configuration::baselineVersion);
      map.from(properties.getInstalledBy()).to(configuration::installedBy);
      map.from(properties.getPlaceholders()).to(configuration::placeholders);
      map.from(properties.getPlaceholderPrefix()).to(configuration::placeholderPrefix);
      map.from(properties.getPlaceholderSuffix()).to(configuration::placeholderSuffix);
      map.from(properties.getPlaceholderSeparator()).to(configuration::placeholderSeparator);
      map.from(properties.isPlaceholderReplacement()).to(configuration::placeholderReplacement);
      map.from(properties.getSqlMigrationPrefix()).to(configuration::sqlMigrationPrefix);
      map.from(properties.getSqlMigrationSuffixes()).as(StringUtils::toStringArray).to(configuration::sqlMigrationSuffixes);
      map.from(properties.getSqlMigrationSeparator()).to(configuration::sqlMigrationSeparator);
      map.from(properties.getRepeatableSqlMigrationPrefix()).to(configuration::repeatableSqlMigrationPrefix);
      map.from(properties.getTarget()).to(configuration::target);
      map.from(properties.isBaselineOnMigrate()).to(configuration::baselineOnMigrate);
      map.from(properties.isCleanDisabled()).to(configuration::cleanDisabled);
      map.from(properties.isGroup()).to(configuration::group);
      map.from(properties.isMixed()).to(configuration::mixed);
      map.from(properties.isOutOfOrder()).to(configuration::outOfOrder);
      map.from(properties.isSkipDefaultCallbacks()).to(configuration::skipDefaultCallbacks);
      map.from(properties.isSkipDefaultResolvers()).to(configuration::skipDefaultResolvers);
      map.from(properties.isValidateMigrationNaming()).to(configuration::validateMigrationNaming);
      map.from(properties.isValidateOnMigrate()).to(configuration::validateOnMigrate);
      map.from(properties.getInitSqls())
              .whenNot(CollectionUtils::isEmpty)
              .as((initSqls) -> StringUtils.collectionToDelimitedString(initSqls, "\n"))
              .to(configuration::initSql);
      map.from(properties.getScriptPlaceholderPrefix()).to(configuration::scriptPlaceholderPrefix);
      map.from(properties.getScriptPlaceholderSuffix()).to(configuration::scriptPlaceholderSuffix);
      map.from(properties.getPowershellExecutable()).to(configuration::powershellExecutable);
      configureExecuteInTransaction(configuration, properties, map);
      map.from(properties::getLoggers).to(configuration::loggers);
      map.from(properties::getCommunityDbSupportEnabled).to(configuration::communityDBSupportEnabled);
      map.from(properties.getBatch()).to(configuration::batch);
      map.from(properties.getDryRunOutput()).to(configuration::dryRunOutput);
      map.from(properties.getErrorOverrides()).to(configuration::errorOverrides);
      map.from(properties.getStream()).to(configuration::stream);
      map.from(properties.getJdbcProperties()).whenNot(Map::isEmpty).to(configuration::jdbcProperties);
      map.from(properties.getKerberosConfigFile()).to(configuration::kerberosConfigFile);
      map.from(properties.getOutputQueryResults()).to(configuration::outputQueryResults);
      map.from(properties.getSkipExecutingMigrations()).to(configuration::skipExecutingMigrations);
      map.from(properties.getIgnoreMigrationPatterns())
              .to((ignoreMigrationPatterns) -> configuration.ignoreMigrationPatterns(ignoreMigrationPatterns.toArray(new String[0])));
      map.from(properties.getDetectEncoding()).to(configuration::detectEncoding);
    }

    private void configureExecuteInTransaction(FluentConfiguration configuration, FlywayProperties properties,
            PropertyMapper map) {
      try {
        map.from(properties.isExecuteInTransaction()).to(configuration::executeInTransaction);
      }
      catch (NoSuchMethodError ex) {
        // Flyway < 9.14
      }
    }

    private void configureCallbacks(FluentConfiguration configuration, List<Callback> callbacks) {
      if (!callbacks.isEmpty()) {
        configuration.callbacks(callbacks.toArray(new Callback[0]));
      }
    }

    private void configureJavaMigrations(FluentConfiguration flyway, List<JavaMigration> migrations) {
      if (!migrations.isEmpty()) {
        flyway.javaMigrations(migrations.toArray(new JavaMigration[0]));
      }
    }

    @Bean
    @ConditionalOnMissingBean
    FlywayMigrationInitializer flywayInitializer(Flyway flyway,
            ObjectProvider<FlywayMigrationStrategy> migrationStrategy) {
      return new FlywayMigrationInitializer(flyway, migrationStrategy.getIfAvailable());
    }

    @ConditionalOnClass(name = "org.flywaydb.database.sqlserver.SQLServerConfigurationExtension")
    @Configuration(proxyBeanMethods = false)
    static class SqlServerConfiguration {

      @Bean
      SqlServerFlywayConfigurationCustomizer sqlServerFlywayConfigurationCustomizer(FlywayProperties properties) {
        return new SqlServerFlywayConfigurationCustomizer(properties);
      }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.flywaydb.database.oracle.OracleConfigurationExtension")
    static class OracleConfiguration {

      @Bean
      OracleFlywayConfigurationCustomizer oracleFlywayConfigurationCustomizer(FlywayProperties properties) {
        return new OracleFlywayConfigurationCustomizer(properties);
      }

    }

    @ConditionalOnClass(name = "org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension")
    @Configuration(proxyBeanMethods = false)
    static class PostgresqlConfiguration {

      @Bean
      PostgresqlFlywayConfigurationCustomizer postgresqlFlywayConfigurationCustomizer(
              FlywayProperties properties) {
        return new PostgresqlFlywayConfigurationCustomizer(properties);
      }

    }

  }

  private static class LocationResolver {

    private static final String VENDOR_PLACEHOLDER = "{vendor}";

    private final DataSource dataSource;

    LocationResolver(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    List<String> resolveLocations(List<String> locations) {
      if (usesVendorLocation(locations)) {
        DatabaseDriver databaseDriver = getDatabaseDriver();
        return replaceVendorLocations(locations, databaseDriver);
      }
      return locations;
    }

    private List<String> replaceVendorLocations(List<String> locations, DatabaseDriver databaseDriver) {
      if (databaseDriver == DatabaseDriver.UNKNOWN) {
        return locations;
      }
      String vendor = databaseDriver.getId();
      return locations.stream().map((location) -> location.replace(VENDOR_PLACEHOLDER, vendor)).toList();
    }

    private DatabaseDriver getDatabaseDriver() {
      try {
        String url = JdbcUtils.extractDatabaseMetaData(this.dataSource, DatabaseMetaData::getURL);
        return DatabaseDriver.fromJdbcUrl(url);
      }
      catch (MetaDataAccessException ex) {
        throw new IllegalStateException(ex);
      }

    }

    private boolean usesVendorLocation(Collection<String> locations) {
      for (String location : locations) {
        if (location.contains(VENDOR_PLACEHOLDER)) {
          return true;
        }
      }
      return false;
    }

  }

  /**
   * Convert a String or Number to a {@link MigrationVersion}.
   */
  static class StringOrNumberToMigrationVersionConverter implements GenericConverter {

    private static final Set<ConvertiblePair> CONVERTIBLE_TYPES;

    static {
      Set<ConvertiblePair> types = new HashSet<>(2);
      types.add(new ConvertiblePair(String.class, MigrationVersion.class));
      types.add(new ConvertiblePair(Number.class, MigrationVersion.class));
      CONVERTIBLE_TYPES = Collections.unmodifiableSet(types);
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
      return CONVERTIBLE_TYPES;
    }

    @Override
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
      String value = ObjectUtils.nullSafeToString(source);
      return MigrationVersion.fromVersion(value);
    }

  }

  static final class FlywayDataSourceCondition extends AnyNestedCondition {

    FlywayDataSourceCondition() {
      super(ConfigurationPhase.REGISTER_BEAN);
    }

    @ConditionalOnBean(DataSource.class)
    private static final class DataSourceBeanCondition {

    }

    @ConditionalOnBean(JdbcConnectionDetails.class)
    private static final class JdbcConnectionDetailsCondition {

    }

    @ConditionalOnProperty("flyway.url")
    private static final class FlywayUrlCondition {

    }

  }

  static class FlywayAutoConfigurationRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
      hints.resources().registerPattern("db/migration/*");
    }

  }

  /**
   * Adapts {@link FlywayProperties} to {@link FlywayConnectionDetails}.
   */
  static final class PropertiesFlywayConnectionDetails implements FlywayConnectionDetails {

    private final FlywayProperties properties;

    PropertiesFlywayConnectionDetails(FlywayProperties properties) {
      this.properties = properties;
    }

    @Override
    public @Nullable String getUsername() {
      return this.properties.getUser();
    }

    @Override
    public @Nullable String getPassword() {
      return this.properties.getPassword();
    }

    @Override
    public @Nullable String getJdbcUrl() {
      return this.properties.getUrl();
    }

    @Override
    public @Nullable String getDriverClassName() {
      return this.properties.getDriverClassName();
    }

  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  static final class OracleFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

    private final FlywayProperties properties;

    OracleFlywayConfigurationCustomizer(FlywayProperties properties) {
      this.properties = properties;
    }

    @Override
    public void customize(FluentConfiguration configuration) {
      Extension<OracleConfigurationExtension> extension = new Extension<>(configuration,
              OracleConfigurationExtension.class, "Oracle");
      Oracle properties = this.properties.getOracle();
      PropertyMapper map = PropertyMapper.get();
      map.from(properties::getSqlplus).to(extension.via(OracleConfigurationExtension::setSqlplus));
      map.from(properties::getSqlplusWarn)
              .to(extension.via(OracleConfigurationExtension::setSqlplusWarn));
      map.from(properties::getWalletLocation)
              .to(extension.via(OracleConfigurationExtension::setWalletLocation));
      map.from(properties::getKerberosCacheFile)
              .to(extension.via(OracleConfigurationExtension::setKerberosCacheFile));
    }

  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  static final class PostgresqlFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

    private final FlywayProperties properties;

    PostgresqlFlywayConfigurationCustomizer(FlywayProperties properties) {
      this.properties = properties;
    }

    @Override
    public void customize(FluentConfiguration configuration) {
      Extension<PostgreSQLConfigurationExtension> extension = new Extension<>(configuration,
              PostgreSQLConfigurationExtension.class, "PostgreSQL");
      Postgresql properties = this.properties.getPostgresql();
      PropertyMapper map = PropertyMapper.get();
      map.from(properties::getTransactionalLock)
              .to(extension.via(PostgreSQLConfigurationExtension::setTransactionalLock));
    }

  }

  @Order(Ordered.HIGHEST_PRECEDENCE)
  static final class SqlServerFlywayConfigurationCustomizer implements FlywayConfigurationCustomizer {

    private final FlywayProperties properties;

    SqlServerFlywayConfigurationCustomizer(FlywayProperties properties) {
      this.properties = properties;
    }

    @Override
    public void customize(FluentConfiguration configuration) {
      Extension<SQLServerConfigurationExtension> extension = new Extension<>(configuration,
              SQLServerConfigurationExtension.class, "SQL Server");
      Sqlserver properties = this.properties.getSqlserver();
      PropertyMapper map = PropertyMapper.get();
      map.from(properties::getKerberosLoginFile).to(extension.via(this::setKerberosLoginFile));
    }

    private void setKerberosLoginFile(SQLServerConfigurationExtension configuration, String file) {
      configuration.getKerberos().getLogin().setFile(file);
    }

  }

  /**
   * Helper class used to map properties to a {@link ConfigurationExtension}.
   *
   * @param <E> the extension type
   */
  static class Extension<E extends ConfigurationExtension> {

    private final Supplier<E> extension;

    Extension(FluentConfiguration configuration, Class<E> type, String name) {
      this.extension = SingletonSupplier.of(() -> {
        E extension = configuration.getPluginRegister().getExact(type);
        Assert.state(extension != null, () -> "Flyway %s extension missing".formatted(name));
        return extension;
      });
    }

    <T> Consumer<T> via(BiConsumer<E, T> action) {
      return (value) -> action.accept(this.extension.get(), value);
    }

  }

}
