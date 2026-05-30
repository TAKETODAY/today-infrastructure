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

package infra.jdbc.test.config;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import infra.aot.AotDetector;
import infra.beans.BeansException;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.annotation.AnnotatedBeanDefinition;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.EnvironmentAware;
import infra.context.annotation.Bean;
import infra.context.annotation.Role;
import infra.context.annotation.config.AutoConfiguration;
import infra.context.condition.ConditionalOnMissingBean;
import infra.context.condition.ConditionalOnProperty;
import infra.context.container.ContainerImageMetadata;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.bind.BoundPropertiesTrackingBindHandler;
import infra.context.properties.source.ConfigurationProperty;
import infra.context.properties.source.ConfigurationPropertyName;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.MapPropertySource;
import infra.core.env.PropertySource;
import infra.core.type.MethodMetadata;
import infra.jdbc.config.DataSourceAutoConfiguration;
import infra.jdbc.config.EmbeddedDatabaseConnection;
import infra.jdbc.config.JdbcConnectionDetails;
import infra.jdbc.datasource.embedded.EmbeddedDatabase;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import infra.jdbc.datasource.embedded.EmbeddedDatabaseType;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.origin.PropertySourceOrigin;
import infra.util.ObjectUtils;

import static infra.jdbc.test.config.AutoConfigureTestDatabase.Replace;

/**
 * Auto-configuration for a test database.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AutoConfigureTestDatabase
 * @since 5.0
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
public final class TestDatabaseAutoConfiguration {

  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnProperty(name = "infra.test.database.replace", havingValue = "NON_TEST", matchIfMissing = true)
  static EmbeddedDataSourceBeanFactoryPostProcessor nonTestEmbeddedDataSourceBeanFactoryPostProcessor(
          ConfigurableEnvironment environment) {
    return new EmbeddedDataSourceBeanFactoryPostProcessor(environment, Replace.NON_TEST);
  }

  @Bean
  @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
  @ConditionalOnProperty(name = "infra.test.database.replace", havingValue = "ANY")
  static EmbeddedDataSourceBeanFactoryPostProcessor embeddedDataSourceBeanFactoryPostProcessor(
          ConfigurableEnvironment environment) {
    return new EmbeddedDataSourceBeanFactoryPostProcessor(environment, Replace.ANY);
  }

  @Bean
  @ConditionalOnProperty(name = "infra.test.database.replace", havingValue = "AUTO_CONFIGURED")
  @ConditionalOnMissingBean
  DataSource dataSource(Environment environment) {
    return new EmbeddedDataSourceFactory(environment).getEmbeddedDatabase();
  }

  @Order(Ordered.LOWEST_PRECEDENCE)
  static class EmbeddedDataSourceBeanFactoryPostProcessor implements BeanDefinitionRegistryPostProcessor {

    private static final ConfigurationPropertyName DATASOURCE_URL_PROPERTY = ConfigurationPropertyName
            .of("datasource.url");

    private static final Bindable<String> BINDABLE_STRING = Bindable.of(String.class);

    private static final String DYNAMIC_VALUES_PROPERTY_SOURCE_CLASS = "infra.test.context.support.DynamicValuesPropertySource";

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedDataSourceBeanFactoryPostProcessor.class);

    private final ConfigurableEnvironment environment;

    private final Replace replace;

    EmbeddedDataSourceBeanFactoryPostProcessor(ConfigurableEnvironment environment, Replace replace) {
      this.environment = environment;
      this.replace = replace;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
      if (AotDetector.useGeneratedArtifacts()) {
        return;
      }
      Assert.isTrue(registry instanceof ConfigurableBeanFactory,
              "'registry' must be a ConfigurableBeanFactory");
      process(registry, (ConfigurableBeanFactory) registry);
    }

    private void process(BeanDefinitionRegistry registry, ConfigurableBeanFactory beanFactory) {
      BeanDefinitionHolder holder = getDataSourceBeanDefinition(beanFactory);
      if (holder != null && isReplaceable(beanFactory, holder)) {
        String beanName = holder.getBeanName();
        boolean primary = holder.getBeanDefinition().isPrimary();
        logger.info("Replacing '" + beanName + "' DataSource bean with " + (primary ? "primary " : "")
                + "embedded version");
        registry.removeBeanDefinition(beanName);
        registry.registerBeanDefinition(beanName, createEmbeddedBeanDefinition(primary));
      }
    }

    private BeanDefinition createEmbeddedBeanDefinition(boolean primary) {
      BeanDefinition beanDefinition = new RootBeanDefinition(EmbeddedDataSourceFactoryBean.class);
      beanDefinition.setPrimary(primary);
      return beanDefinition;
    }

    private @Nullable BeanDefinitionHolder getDataSourceBeanDefinition(
            ConfigurableBeanFactory beanFactory) {
      String[] beanNames = beanFactory.getBeanNamesForType(DataSource.class);
      if (ObjectUtils.isEmpty(beanNames)) {
        logger.warn("No DataSource beans found, embedded version will not be used");
        return null;
      }
      if (beanNames.length == 1) {
        String beanName = beanNames[0];
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        return new BeanDefinitionHolder(beanDefinition, beanName);
      }
      for (String beanName : beanNames) {
        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
        if (beanDefinition.isPrimary()) {
          return new BeanDefinitionHolder(beanDefinition, beanName);
        }
      }
      logger.warn("No primary DataSource found, embedded version will not be used");
      return null;
    }

    private boolean isReplaceable(ConfigurableBeanFactory beanFactory, BeanDefinitionHolder holder) {
      if (this.replace == Replace.NON_TEST) {
        return !isAutoConfigured(holder) || !isConnectingToTestDatabase(beanFactory);
      }
      return true;
    }

    private boolean isAutoConfigured(BeanDefinitionHolder holder) {
      if (holder.getBeanDefinition() instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
        MethodMetadata factoryMethodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();
        return (factoryMethodMetadata != null) && (factoryMethodMetadata.getDeclaringClassName()
                .startsWith("infra.jdbc.config."));
      }
      return false;
    }

    private boolean isConnectingToTestDatabase(ConfigurableBeanFactory beanFactory) {
      return isUsingTestServiceConnection(beanFactory) || isUsingTestDatasourceUrl();
    }

    private boolean isUsingTestServiceConnection(ConfigurableBeanFactory beanFactory) {
      for (String beanName : beanFactory.getBeanNamesForType(JdbcConnectionDetails.class)) {
        try {
          BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
          if (ContainerImageMetadata.isPresent(beanDefinition)) {
            return true;
          }
        }
        catch (NoSuchBeanDefinitionException ex) {
          // Ignore
        }
      }
      return false;
    }

    private boolean isUsingTestDatasourceUrl() {
      List<ConfigurationProperty> bound = new ArrayList<>();
      Binder.get(this.environment, new BoundPropertiesTrackingBindHandler(bound::add))
              .bind(DATASOURCE_URL_PROPERTY, BINDABLE_STRING);
      return !bound.isEmpty() && isUsingTestDatasourceUrl(bound.get(0));
    }

    private boolean isUsingTestDatasourceUrl(ConfigurationProperty configurationProperty) {
      return isBoundToDynamicValuesPropertySource(configurationProperty)
              || isTestcontainersUrl(configurationProperty);
    }

    private boolean isBoundToDynamicValuesPropertySource(ConfigurationProperty configurationProperty) {
      if (configurationProperty.getOrigin() instanceof PropertySourceOrigin origin) {
        return isDynamicValuesPropertySource(origin.getPropertySource());
      }
      return false;
    }

    private boolean isDynamicValuesPropertySource(@Nullable PropertySource<?> propertySource) {
      return propertySource != null
              && DYNAMIC_VALUES_PROPERTY_SOURCE_CLASS.equals(propertySource.getClass().getName());
    }

    private boolean isTestcontainersUrl(ConfigurationProperty configurationProperty) {
      Object value = configurationProperty.getValue();
      return (value != null) && value.toString().startsWith("jdbc:tc:");
    }

  }

  static class EmbeddedDataSourceFactoryBean implements FactoryBean<DataSource>, EnvironmentAware, InitializingBean {

    @SuppressWarnings("NullAway.Init")
    private EmbeddedDataSourceFactory factory;

    @SuppressWarnings("NullAway.Init")
    private EmbeddedDatabase embeddedDatabase;

    @Override
    public void setEnvironment(Environment environment) {
      this.factory = new EmbeddedDataSourceFactory(environment);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
      this.embeddedDatabase = this.factory.getEmbeddedDatabase();
    }

    @Override
    public DataSource getObject() throws Exception {
      return this.embeddedDatabase;
    }

    @Override
    public Class<?> getObjectType() {
      return EmbeddedDatabase.class;
    }

  }

  static class EmbeddedDataSourceFactory {

    private final Environment environment;

    EmbeddedDataSourceFactory(Environment environment) {
      this.environment = environment;
      if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
        Map<String, Object> source = new HashMap<>();
        source.put("datasource.schema-username", "");
        source.put("sql.init.username", "");
        configurableEnvironment.getPropertySources().addFirst(new MapPropertySource("testDatabase", source));
      }
    }

    EmbeddedDatabase getEmbeddedDatabase() {
      EmbeddedDatabaseConnection connection = this.environment.getProperty("infra.test.database.connection",
              EmbeddedDatabaseConnection.class, EmbeddedDatabaseConnection.NONE);
      if (EmbeddedDatabaseConnection.NONE.equals(connection)) {
        connection = EmbeddedDatabaseConnection.get(getClass().getClassLoader());
      }
      Assert.state(connection != EmbeddedDatabaseConnection.NONE,
              "Failed to replace DataSource with an embedded database for tests. If "
                      + "you want an embedded database please put a supported one "
                      + "on the classpath or tune the replace attribute of @AutoConfigureTestDatabase.");
      EmbeddedDatabaseType type = connection.getType();
      Assert.state(type != null, "'type' is required");
      return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(type).build();
    }

  }

}
