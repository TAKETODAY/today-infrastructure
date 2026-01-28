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

import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.pattern.ValidatePattern;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import infra.beans.BeanProperty;
import infra.beans.BeanWrapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlywayProperties}.
 *
 * @author Stephane Nicoll
 * @author Chris Bono
 */
class FlywayPropertiesTests {

  @Test
  void defaultValuesAreConsistent() {
    FlywayProperties properties = new FlywayProperties();
    Configuration configuration = new FluentConfiguration();
    assertThat(properties.failOnMissingLocations).isEqualTo(configuration.isFailOnMissingLocations());
    assertThat(properties.locations.stream().map(this::toLocation).toArray(Location[]::new)).isEqualTo(configuration.getLocations());
    assertThat(properties.callbackLocations.stream().map(this::toLocation).toArray(Location[]::new)).isEqualTo(configuration.getCallbackLocations());
    assertThat(properties.encoding).isEqualTo(configuration.getEncoding());
    assertThat(properties.connectRetries).isEqualTo(configuration.getConnectRetries());
    assertThat(properties.connectRetriesInterval).extracting(Duration::getSeconds)
            .extracting(Long::intValue)
            .isEqualTo(configuration.getConnectRetriesInterval());
    assertThat(properties.lockRetryCount).isEqualTo(configuration.getLockRetryCount());
    assertThat(properties.defaultSchema).isEqualTo(configuration.getDefaultSchema());
    assertThat(properties.schemas).isEqualTo(Arrays.asList(configuration.getSchemas()));
    assertThat(properties.createSchemas).isEqualTo(configuration.isCreateSchemas());
    assertThat(properties.table).isEqualTo(configuration.getTable());
    assertThat(properties.baselineDescription).isEqualTo(configuration.getBaselineDescription());
    assertThat(MigrationVersion.fromVersion(properties.baselineVersion)).isEqualTo(configuration.getBaselineVersion());
    assertThat(properties.installedBy).isEqualTo(configuration.getInstalledBy());
    assertThat(properties.placeholders).isEqualTo(configuration.getPlaceholders());
    assertThat(properties.placeholderPrefix).isEqualToIgnoringWhitespace(configuration.getPlaceholderPrefix());
    assertThat(properties.placeholderSuffix).isEqualTo(configuration.getPlaceholderSuffix());
    assertThat(properties.placeholderReplacement).isEqualTo(configuration.isPlaceholderReplacement());
    assertThat(properties.powershellExecutable).isEqualTo(configuration.getPowershellExecutable());
    assertThat(properties.sqlMigrationPrefix).isEqualTo(configuration.getSqlMigrationPrefix());
    assertThat(properties.sqlMigrationSuffixes).containsExactly(configuration.getSqlMigrationSuffixes());
    assertThat(properties.sqlMigrationSeparator).isEqualTo(configuration.getSqlMigrationSeparator());
    assertThat(properties.repeatableSqlMigrationPrefix).isEqualTo(configuration.getRepeatableSqlMigrationPrefix());
    assertThat(MigrationVersion.fromVersion(properties.target)).isEqualTo(configuration.getTarget());
    assertThat(configuration.getInitSql()).isNull();
    assertThat(properties.initSqls).isEmpty();
    assertThat(properties.baselineOnMigrate).isEqualTo(configuration.isBaselineOnMigrate());
    assertThat(properties.cleanDisabled).isEqualTo(configuration.isCleanDisabled());
    assertThat(properties.group).isEqualTo(configuration.isGroup());
    assertThat(properties.mixed).isEqualTo(configuration.isMixed());
    assertThat(properties.outOfOrder).isEqualTo(configuration.isOutOfOrder());
    assertThat(properties.skipDefaultCallbacks).isEqualTo(configuration.isSkipDefaultCallbacks());
    assertThat(properties.skipDefaultResolvers).isEqualTo(configuration.isSkipDefaultResolvers());
    assertThat(properties.validateMigrationNaming).isEqualTo(configuration.isValidateMigrationNaming());
    assertThat(properties.validateOnMigrate).isEqualTo(configuration.isValidateOnMigrate());
    assertThat(properties.detectEncoding).isNull();
    assertThat(properties.placeholderSeparator).isEqualTo(configuration.getPlaceholderSeparator());
    assertThat(properties.scriptPlaceholderPrefix).isEqualTo(configuration.getScriptPlaceholderPrefix());
    assertThat(properties.scriptPlaceholderSuffix).isEqualTo(configuration.getScriptPlaceholderSuffix());
    assertThat(properties.executeInTransaction).isEqualTo(configuration.isExecuteInTransaction());
    assertThat(properties.communityDbSupportEnabled).isNull();
    assertThat(properties.ignoreMigrationPatterns.stream().map(ValidatePattern::fromPattern))
            .containsExactly(configuration.getIgnoreMigrationPatterns());
    assertThat(properties.enabled).isTrue();
  }

  @Test
  void loggersIsOverriddenToSlf4j() {
    assertThat(new FluentConfiguration().getLoggers()).containsExactly("auto");
    assertThat(new FlywayProperties().loggers).containsExactly("slf4j");
  }

  @Test
  void expectedPropertiesAreManaged() {
    Map<String, BeanProperty> properties = indexProperties(BeanWrapper.forBeanPropertyAccess(new FlywayProperties()));
    Map<String, BeanProperty> configuration = indexProperties(BeanWrapper.forBeanPropertyAccess(new ClassicConfiguration()));
    // Properties specific settings
    ignoreProperties(properties, "url", "driverClassName", "user", "password", "enabled");
    // Properties that are managed by specific extensions
    ignoreProperties(properties, "oracle", "postgresql", "sqlserver");
    // Properties that are only used on the command line
    ignoreProperties(configuration, "jarDirs");
    // https://github.com/flyway/flyway/issues/3732
    ignoreProperties(configuration, "environment");
    // High level object we can't set with properties
    ignoreProperties(configuration, "callbacks", "classLoader", "dataSource", "javaMigrations",
            "javaMigrationClassProvider", "pluginRegister", "resourceProvider", "resolvers");
    // Properties we don't want to expose
    ignoreProperties(configuration, "resolversAsClassNames", "callbacksAsClassNames", "driver", "modernConfig",
            "currentResolvedEnvironment", "reportFilename", "reportEnabled", "workingDirectory",
            "cachedDataSources", "cachedResolvedEnvironments", "currentEnvironmentName", "allEnvironments",
            "environmentProvisionMode", "provisionMode", "cleanOnValidationError");
    // Handled by the conversion service
    ignoreProperties(configuration, "baselineVersionAsString", "encodingAsString", "locationsAsStrings",
            "callbackLocationsAsStrings", "targetAsString");
    // Handled as initSql array
    ignoreProperties(configuration, "initSql");
    ignoreProperties(properties, "initSqls");
    // Handled as dryRunOutput
    ignoreProperties(configuration, "dryRunOutputAsFile", "dryRunOutputAsFileName");
    // Handled as createSchemas
    ignoreProperties(configuration, "shouldCreateSchemas");
    // Getters for the DataSource settings rather than actual properties
    ignoreProperties(configuration, "databaseType", "password", "url", "user");
    // Properties not exposed by Flyway
    ignoreProperties(configuration, "failOnMissingTarget");
    // Properties managed by a proprietary extension
    ignoreProperties(configuration, "cherryPick");

    ignoreProperties(configuration, "classScanner");
    ignoreProperties(configuration, "dataSources");
    ignoreProperties(configuration, "resolvedEnvironments");
    ignoreProperties(configuration, "environmentResolver");

    aliasProperty(configuration, "communityDBSupportEnabled", "communityDbSupportEnabled");
    List<String> configurationKeys = new ArrayList<>(configuration.keySet());
    Collections.sort(configurationKeys);
    List<String> propertiesKeys = new ArrayList<>(properties.keySet());
    Collections.sort(propertiesKeys);
    assertThat(configurationKeys).containsExactlyElementsOf(propertiesKeys);
  }

  @SuppressWarnings("deprecation")
  private Location toLocation(String location) {
    return new Location(location);
  }

  private void ignoreProperties(Map<String, ?> index, String... propertyNames) {
    for (String propertyName : propertyNames) {
      assertThat(index.remove(propertyName)).describedAs("Property to ignore should be present " + propertyName)
              .isNotNull();
    }
  }

  private void aliasProperty(Map<String, BeanProperty> index, String originalName, String alias) {
    BeanProperty descriptor = index.remove(originalName);
    assertThat(descriptor).describedAs("Property to alias should be present " + originalName).isNotNull();
    index.put(alias, descriptor);
  }

  private Map<String, BeanProperty> indexProperties(BeanWrapper beanWrapper) {
    Map<String, BeanProperty> descriptor = new HashMap<>();
    for (BeanProperty property : beanWrapper.getBeanProperties()) {
      descriptor.put(property.getName(), property);
    }
    ignoreProperties(descriptor, "class");
    return descriptor;
  }

}
