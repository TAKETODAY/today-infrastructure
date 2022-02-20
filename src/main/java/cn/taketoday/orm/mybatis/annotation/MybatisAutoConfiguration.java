/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.ImportBeanDefinitionRegistrar;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.auto.AutoConfigurationPackages;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.condition.ConditionalOnSingleCandidate;
import cn.taketoday.context.loader.DefinitionLoadingContext;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.orm.mybatis.SqlSessionFactoryBean;
import cn.taketoday.orm.mybatis.SqlSessionTemplate;
import cn.taketoday.orm.mybatis.mapper.MapperFactoryBean;
import cn.taketoday.orm.mybatis.mapper.MapperScannerConfigurer;
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
@cn.taketoday.context.annotation.Configuration
@ConditionalOnSingleCandidate(DataSource.class)
@ConditionalOnClass({ SqlSessionFactory.class, SqlSessionFactoryBean.class })
public class MybatisAutoConfiguration implements InitializingBean {
  private static final Logger log = LoggerFactory.getLogger(MybatisAutoConfiguration.class);

  private final MybatisProperties properties;

  private final Interceptor[] interceptors;

  private final TypeHandler[] typeHandlers;

  private final LanguageDriver[] languageDrivers;

  private final ResourceLoader resourceLoader;

  private final DatabaseIdProvider databaseIdProvider;

  private final List<ConfigurationCustomizer> configurationCustomizers;

  private final List<SqlSessionFactoryBeanCustomizer> sqlSessionFactoryBeanCustomizers;

  public MybatisAutoConfiguration(
          ResourceLoader resourceLoader,
          ObjectProvider<Interceptor[]> interceptorsProvider,
          ObjectProvider<TypeHandler[]> typeHandlersProvider,
          ObjectProvider<LanguageDriver[]> languageDriversProvider,
          ObjectProvider<DatabaseIdProvider> databaseIdProvider,
          ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider,
          @Props(prefix = MybatisProperties.MYBATIS_PREFIX) MybatisProperties properties,
          ObjectProvider<List<SqlSessionFactoryBeanCustomizer>> sqlSessionFactoryBeanCustomizers) {
    this.properties = properties;
    this.interceptors = interceptorsProvider.getIfAvailable();
    this.typeHandlers = typeHandlersProvider.getIfAvailable();
    this.languageDrivers = languageDriversProvider.getIfAvailable();
    this.resourceLoader = resourceLoader;
    this.databaseIdProvider = databaseIdProvider.getIfAvailable();
    this.configurationCustomizers = configurationCustomizersProvider.getIfAvailable();
    this.sqlSessionFactoryBeanCustomizers = sqlSessionFactoryBeanCustomizers.getIfAvailable();
  }

  @Override
  public void afterPropertiesSet() {
    checkConfigFileExists();
  }

  private void checkConfigFileExists() {
    if (this.properties.isCheckConfigLocation() && StringUtils.hasText(this.properties.getConfigLocation())) {
      Resource resource = this.resourceLoader.getResource(this.properties.getConfigLocation());
      Assert.state(resource.exists(),
              "Cannot find config location: " + resource + " (please add config file or check your Mybatis configuration)");
    }
  }

  @Component
  @ConditionalOnMissingBean
  public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
    SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
    factory.setDataSource(dataSource);
    factory.setVfs(ResourceLoaderVFS.class);
    if (StringUtils.hasText(this.properties.getConfigLocation())) {
      factory.setConfigLocation(this.resourceLoader.getResource(this.properties.getConfigLocation()));
    }
    applyConfiguration(factory);
    if (this.properties.getConfigurationProperties() != null) {
      factory.setConfigurationProperties(this.properties.getConfigurationProperties());
    }
    if (ObjectUtils.isNotEmpty(this.interceptors)) {
      factory.setPlugins(this.interceptors);
    }
    if (this.databaseIdProvider != null) {
      factory.setDatabaseIdProvider(this.databaseIdProvider);
    }
    if (StringUtils.isNotEmpty(this.properties.getTypeAliasesPackage())) {
      factory.setTypeAliasesPackage(this.properties.getTypeAliasesPackage());
    }
    if (this.properties.getTypeAliasesSuperType() != null) {
      factory.setTypeAliasesSuperType(this.properties.getTypeAliasesSuperType());
    }
    if (StringUtils.isNotEmpty(this.properties.getTypeHandlersPackage())) {
      factory.setTypeHandlersPackage(this.properties.getTypeHandlersPackage());
    }
    if (ObjectUtils.isNotEmpty(this.typeHandlers)) {
      factory.setTypeHandlers(this.typeHandlers);
    }
    Resource[] mapperLocations = properties.resolveMapperLocations();
    if (ObjectUtils.isNotEmpty(mapperLocations)) {
      factory.setMapperLocations(mapperLocations);
    }

    BeanMetadata metadata = BeanMetadata.from(SqlSessionFactoryBean.class);
    Set<String> factoryPropertyNames = metadata.getBeanProperties().keySet();
    Class<? extends LanguageDriver> defaultLanguageDriver = this.properties.getDefaultScriptingLanguageDriver();
    if (factoryPropertyNames.contains("scriptingLanguageDrivers") && ObjectUtils.isNotEmpty(this.languageDrivers)) {
      // Need to mybatis-spring 2.0.2+
      factory.setScriptingLanguageDrivers(this.languageDrivers);
      if (defaultLanguageDriver == null && this.languageDrivers.length == 1) {
        defaultLanguageDriver = this.languageDrivers[0].getClass();
      }
    }
    if (factoryPropertyNames.contains("defaultScriptingLanguageDriver")) {
      factory.setDefaultScriptingLanguageDriver(defaultLanguageDriver);
    }
    applySqlSessionFactoryBeanCustomizers(factory);
    return factory.getObject();
  }

  private void applyConfiguration(SqlSessionFactoryBean factory) {
    Configuration configuration = this.properties.getConfiguration();
    if (configuration == null && !StringUtils.hasText(this.properties.getConfigLocation())) {
      configuration = new Configuration();
    }
    if (configuration != null && CollectionUtils.isNotEmpty(this.configurationCustomizers)) {
      for (ConfigurationCustomizer customizer : this.configurationCustomizers) {
        customizer.customize(configuration);
      }
    }
    factory.setConfiguration(configuration);
  }

  private void applySqlSessionFactoryBeanCustomizers(SqlSessionFactoryBean factory) {
    if (CollectionUtils.isNotEmpty(this.sqlSessionFactoryBeanCustomizers)) {
      for (SqlSessionFactoryBeanCustomizer customizer : this.sqlSessionFactoryBeanCustomizers) {
        customizer.customize(factory);
      }
    }
  }

  @Bean
  @ConditionalOnMissingBean
  public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    ExecutorType executorType = this.properties.getExecutorType();
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
   * correctly, out-of-the-box, similar to using Spring Data JPA repositories.
   */
  public static class AutoConfiguredMapperScannerRegistrar
          implements BeanFactoryAware, EnvironmentAware, ImportBeanDefinitionRegistrar {

    private BeanFactory beanFactory;
    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, DefinitionLoadingContext context) {

      if (!AutoConfigurationPackages.has(this.beanFactory)) {
        log.debug("Could not determine auto-configuration package, automatic mapper scanning disabled.");
        return;
      }

      log.debug("Searching for mappers annotated with @Mapper");

      List<String> packages = AutoConfigurationPackages.get(this.beanFactory);
      if (log.isDebugEnabled()) {
        for (String pkg : packages) {
          log.debug("Using auto-configuration base package '{}'", pkg);
        }
      }

      BeanDefinition definition = new BeanDefinition(MapperScannerConfigurer.class);
      definition.addPropertyValue("processPropertyPlaceHolders", true);
      definition.addPropertyValue("annotationClass", Mapper.class);
      definition.addPropertyValue("basePackage", StringUtils.collectionToString(packages));
      BeanMetadata metadata = BeanMetadata.from(MapperScannerConfigurer.class);
      Set<String> propertyNames = metadata.getBeanProperties().keySet();

      if (propertyNames.contains("lazyInitialization")) {
        definition.addPropertyValue("lazyInitialization", "${mybatis.lazy-initialization:false}");
      }
      if (propertyNames.contains("defaultScope")) {
        definition.addPropertyValue("defaultScope", "${mybatis.mapper-default-scope:}");
      }

      // for spring-native
      boolean injectSqlSession = environment.getProperty(
              "mybatis.inject-sql-session-on-mapper-scan", Boolean.class, Boolean.TRUE);
      if (injectSqlSession && this.beanFactory != null) {
        Optional<String> sqlSessionTemplateBeanName =
                Optional.ofNullable(getBeanNameForType(SqlSessionTemplate.class, beanFactory));
        Optional<String> sqlSessionFactoryBeanName =
                Optional.ofNullable(getBeanNameForType(SqlSessionFactory.class, beanFactory));
        if (sqlSessionTemplateBeanName.isPresent()
                || sqlSessionFactoryBeanName.isEmpty()) {
          definition.addPropertyValue("sqlSessionTemplateBeanName",
                  sqlSessionTemplateBeanName.orElse("sqlSessionTemplate"));
        }
        else {
          definition.addPropertyValue("sqlSessionFactoryBeanName", sqlSessionFactoryBeanName.get());
        }
      }
      definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

      context.registerBeanDefinition(MapperScannerConfigurer.class.getName(), definition);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }

    private String getBeanNameForType(Class<?> type, BeanFactory factory) {
      Set<String> beanNames = factory.getBeanNamesForType(type);
      return beanNames.size() > 0 ? CollectionUtils.firstElement(beanNames) : null;
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
      log.debug(
              "Not found configuration for registering mapper bean using @MapperScan, MapperFactoryBean and MapperScannerConfigurer.");
    }

  }

}
