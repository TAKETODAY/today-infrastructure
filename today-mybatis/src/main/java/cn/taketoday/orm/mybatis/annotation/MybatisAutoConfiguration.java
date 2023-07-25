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

package cn.taketoday.orm.mybatis.annotation;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;

import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.jdbc.DataSourceAutoConfiguration;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.config.AutoConfiguration;
import cn.taketoday.context.annotation.config.AutoConfigurationPackages;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnSingleCandidate;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.orm.mybatis.SqlSessionFactoryBean;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
import cn.taketoday.orm.mybatis.mapper.MapperFactoryBean;
import cn.taketoday.orm.mybatis.mapper.MapperScannerConfigurer;
import cn.taketoday.stereotype.Component;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Auto-Configuration for Mybatis. Contributes a {@link SqlSessionFactory} and a
 * {@link SqlSessionTemplate}.
 *
 * If {@link MapperScan} is used, or a configuration file is specified as a property,
 * those will be considered, otherwise this auto-configuration will attempt to
 * register mappers based on the interface definitions in or under the root auto-configuration package.
 *
 * @author Eddú Meléndez
 * @author Josh Long
 * @author Kazuki Shimizu
 * @author Eduardo Macarrón
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/1 02:17
 */
@ConditionalOnSingleCandidate(DataSource.class)
@EnableConfigurationProperties(MybatisProperties.class)
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class })
public class MybatisAutoConfiguration implements InitializingBean {
  private static final Logger log = LoggerFactory.getLogger(MybatisAutoConfiguration.class);

  private final MybatisProperties properties;
  private final ResourceLoader resourceLoader;

  @SuppressWarnings("rawtypes")
  private final ObjectProvider<TypeHandler[]> typeHandlers;
  private final ObjectProvider<Interceptor[]> interceptors;
  private final ObjectProvider<LanguageDriver[]> languageDrivers;
  private final ObjectProvider<DatabaseIdProvider> databaseIdProvider;
  private final ObjectProvider<ConfigurationCustomizer> configurationCustomizers;
  private final ObjectProvider<SqlSessionFactoryBeanCustomizer> sqlSessionFactoryBeanCustomizers;

  @SuppressWarnings("rawtypes")
  public MybatisAutoConfiguration(MybatisProperties properties, ResourceLoader resourceLoader,
          ObjectProvider<Interceptor[]> interceptorsProvider,
          ObjectProvider<DatabaseIdProvider> databaseIdProvider,
          ObjectProvider<TypeHandler[]> typeHandlersProvider,
          ObjectProvider<LanguageDriver[]> languageDriversProvider,
          ObjectProvider<ConfigurationCustomizer> configurationCustomizersProvider,
          ObjectProvider<SqlSessionFactoryBeanCustomizer> sqlSessionFactoryBeanCustomizers) {
    this.properties = properties;
    this.resourceLoader = resourceLoader;
    this.interceptors = interceptorsProvider;
    this.typeHandlers = typeHandlersProvider;
    this.languageDrivers = languageDriversProvider;
    this.databaseIdProvider = databaseIdProvider;
    this.configurationCustomizers = configurationCustomizersProvider;
    this.sqlSessionFactoryBeanCustomizers = sqlSessionFactoryBeanCustomizers;
  }

  @Override
  public void afterPropertiesSet() {
    checkConfigFileExists();
  }

  private void checkConfigFileExists() {
    if (properties.isCheckConfigLocation() && StringUtils.hasText(properties.getConfigLocation())) {
      Resource resource = resourceLoader.getResource(properties.getConfigLocation());
      if (!resource.exists()) {
        throw new IllegalStateException("Cannot find config location: " +
                resource + " (please add config file or check your Mybatis configuration)");
      }
    }
  }

  @Component
  @ConditionalOnMissingBean
  public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
    SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
    factory.setDataSource(dataSource);
    factory.setVfs(ResourceLoaderVFS.class);
    if (StringUtils.hasText(properties.getConfigLocation())) {
      factory.setConfigLocation(resourceLoader.getResource(properties.getConfigLocation()));
    }
    applyConfiguration(factory);
    if (properties.getConfigurationProperties() != null) {
      factory.setConfigurationProperties(properties.getConfigurationProperties());
    }

    if (StringUtils.isNotEmpty(properties.getTypeAliasesPackage())) {
      factory.setTypeAliasesPackage(properties.getTypeAliasesPackage());
    }
    if (properties.getTypeAliasesSuperType() != null) {
      factory.setTypeAliasesSuperType(properties.getTypeAliasesSuperType());
    }
    if (StringUtils.isNotEmpty(properties.getTypeHandlersPackage())) {
      factory.setTypeHandlersPackage(properties.getTypeHandlersPackage());
    }

    Resource[] mapperLocations = properties.resolveMapperLocations();
    if (ObjectUtils.isNotEmpty(mapperLocations)) {
      factory.setMapperLocations(mapperLocations);
    }

    interceptors.ifAvailable(factory::setPlugins);
    typeHandlers.ifAvailable(factory::setTypeHandlers);
    databaseIdProvider.ifAvailable(factory::setDatabaseIdProvider);

    Class<? extends LanguageDriver> defaultLanguageDriver = properties.getDefaultScriptingLanguageDriver();
    factory.setDefaultScriptingLanguageDriver(defaultLanguageDriver);
    languageDrivers.ifAvailable(languageDrivers -> {
      factory.setScriptingLanguageDrivers(languageDrivers);
      if (defaultLanguageDriver == null && languageDrivers.length == 1) {
        // override
        factory.setDefaultScriptingLanguageDriver(languageDrivers[0].getClass());
      }
    });

    applySqlSessionFactoryBeanCustomizers(factory);
    return factory.getObject();
  }

  private void applyConfiguration(SqlSessionFactoryBean factory) {
    Configuration configuration = properties.getConfiguration();
    if (configuration == null && StringUtils.isBlank(properties.getConfigLocation())) {
      configuration = new Configuration();
    }

    if (configuration != null) {
      for (ConfigurationCustomizer customizer : configurationCustomizers) {
        customizer.customize(configuration);
      }
    }
    factory.setConfiguration(configuration);
  }

  private void applySqlSessionFactoryBeanCustomizers(SqlSessionFactoryBean factory) {
    for (SqlSessionFactoryBeanCustomizer customizer : sqlSessionFactoryBeanCustomizers) {
      customizer.customize(factory);
    }
  }

  @Component
  @ConditionalOnMissingBean
  SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    ExecutorType executorType = properties.getExecutorType();
    if (executorType != null) {
      return new SqlSessionTemplate(sqlSessionFactory, executorType);
    }
    else {
      return new SqlSessionTemplate(sqlSessionFactory);
    }
  }

  /**
   * This will just scan the same base package as Framework does. If you want more power,
   * you can explicitly use {@link MapperScan} but this will get typed mappers working
   * correctly, out-of-the-box, similar to using Framework Data JPA repositories.
   */
  public static class AutoConfiguredMapperScannerRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
      BeanFactory beanFactory = context.getBeanFactory();
      if (!AutoConfigurationPackages.has(beanFactory)) {
        log.debug("Could not determine auto-configuration package, automatic mapper scanning disabled.");
        return;
      }

      log.debug("Searching for mappers annotated with @Mapper");

      List<String> packages = AutoConfigurationPackages.get(beanFactory);
      if (log.isDebugEnabled()) {
        for (String pkg : packages) {
          log.debug("Using auto-configuration base package '{}'", pkg);
        }
      }

      var builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
      builder.addPropertyValue("annotationClass", Mapper.class);
      builder.addPropertyValue("processPropertyPlaceHolders", true);
      builder.addPropertyValue("defaultScope", "${mybatis.mapper-default-scope:}");
      builder.addPropertyValue("lazyInitialization", "${mybatis.lazy-initialization:false}");
      builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(packages));

      Environment environment = context.getEnvironment();
      if (environment.getFlag("mybatis.inject-sql-session-on-mapper-scan", true)) {
        String sqlSessionFactoryBeanName = getBeanNameForType(SqlSessionFactory.class, beanFactory);
        String sqlSessionTemplateBeanName = getBeanNameForType(SqlSessionTemplate.class, beanFactory);
        if (sqlSessionTemplateBeanName != null || sqlSessionFactoryBeanName == null) {
          builder.addPropertyValue("sqlSessionTemplateBeanName",
                  sqlSessionTemplateBeanName == null ? "sqlSessionTemplate" : sqlSessionTemplateBeanName);
        }
        else {
          builder.addPropertyValue("sqlSessionFactoryBeanName", sqlSessionFactoryBeanName);
        }
      }
      builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

      context.registerBeanDefinition(MapperScannerConfigurer.class.getName(), builder.getRawBeanDefinition());
    }

    @Nullable
    private String getBeanNameForType(Class<?> type, BeanFactory factory) {
      return CollectionUtils.firstElement(factory.getBeanNamesForType(type));
    }

  }

  /**
   * If mapper registering configuration or mapper scanning configuration not present, this configuration allow to scan
   * mappers based on the same component-scanning path asFramework itself.
   */
  @cn.taketoday.context.annotation.Configuration
  @Import(AutoConfiguredMapperScannerRegistrar.class)
  @ConditionalOnMissingBean({ MapperFactoryBean.class, MapperScannerConfigurer.class })
  public static class MapperScannerRegistrarNotFoundConfiguration implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
      log.debug("Not found configuration for registering mapper bean using @MapperScan, MapperFactoryBean and MapperScannerConfigurer.");
    }

  }

}
