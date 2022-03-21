/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.orm.mybatis;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.event.ContextRefreshedEvent;
import cn.taketoday.context.loader.ClassPathScanningComponentProvider;
import cn.taketoday.context.loader.MetadataReaderConsumer;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.type.ClassMetadata;
import cn.taketoday.core.type.filter.AssignableTypeFilter;
import cn.taketoday.jdbc.datasource.TransactionAwareDataSourceProxy;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.orm.mybatis.transaction.ManagedTransactionFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@code FactoryBean} that creates a MyBatis {@code SqlSessionFactory}. This is the usual way to set up a shared
 * MyBatis {@code SqlSessionFactory} in a application context; the SqlSessionFactory can then be passed to
 * MyBatis-based DAOs via dependency injection.
 *
 * Either {@code DataSourceTransactionManager} or {@code JtaTransactionManager} can be used for transaction demarcation
 * in combination with a {@code SqlSessionFactory}. JTA should be used for transactions which span multiple databases or
 * when container managed transactions (CMT) are being used.
 *
 * @author Putthiphong Boonphong
 * @author Hunter Presnall
 * @author Eduardo Macarron
 * @author Eddú Meléndez
 * @author Kazuki Shimizu
 * @see #setConfigLocation
 * @see #setDataSource
 * @since 4.0
 */
public class SqlSessionFactoryBean
        implements FactoryBean<SqlSessionFactory>, InitializingBean, ApplicationListener<ContextRefreshedEvent> {

  private static final Logger log = LoggerFactory.getLogger(SqlSessionFactoryBean.class);

  private Resource configLocation;

  private Configuration configuration;

  private Resource[] mapperLocations;

  private DataSource dataSource;

  private TransactionFactory transactionFactory;

  private Properties configurationProperties;

  private SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

  private SqlSessionFactory sqlSessionFactory;

  private String environment = SqlSessionFactoryBean.class.getSimpleName();

  private boolean failFast;

  private Interceptor[] plugins;

  private TypeHandler<?>[] typeHandlers;

  private String typeHandlersPackage;

  @SuppressWarnings("rawtypes")
  private Class<? extends TypeHandler> defaultEnumTypeHandler;

  private Class<?>[] typeAliases;

  private String typeAliasesPackage;

  private Class<?> typeAliasesSuperType;

  @Nullable
  private LanguageDriver[] scriptingLanguageDrivers;

  @Nullable
  private Class<? extends LanguageDriver> defaultScriptingLanguageDriver;

  // issue #19. No default provider.
  @Nullable
  private DatabaseIdProvider databaseIdProvider;

  private Class<? extends VFS> vfs;

  private Cache cache;

  private ObjectFactory objectFactory;

  private ObjectWrapperFactory objectWrapperFactory;

  private ClassLoader classLoader = ClassUtils.getDefaultClassLoader();

  private ClassPathScanningComponentProvider componentProvider;

  /**
   * Set the ClassLoader for loading (scanning) TypeHandlers or typeAliases classes
   *
   * @param classLoader ClassLoader
   */
  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public void setComponentProvider(ClassPathScanningComponentProvider componentProvider) {
    this.componentProvider = componentProvider;
  }

  /**
   * Sets the ObjectFactory.
   *
   * @param objectFactory a custom ObjectFactory
   */
  public void setObjectFactory(ObjectFactory objectFactory) {
    this.objectFactory = objectFactory;
  }

  /**
   * Sets the ObjectWrapperFactory.
   *
   * @param objectWrapperFactory a specified ObjectWrapperFactory
   */
  public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
    this.objectWrapperFactory = objectWrapperFactory;
  }

  /**
   * Gets the DatabaseIdProvider
   *
   * @return a specified DatabaseIdProvider
   */
  public DatabaseIdProvider getDatabaseIdProvider() {
    return databaseIdProvider;
  }

  /**
   * Sets the DatabaseIdProvider. this variable is not initialized by default.
   *
   * @param databaseIdProvider a DatabaseIdProvider
   */
  public void setDatabaseIdProvider(@Nullable DatabaseIdProvider databaseIdProvider) {
    this.databaseIdProvider = databaseIdProvider;
  }

  /**
   * Gets the VFS.
   *
   * @return a specified VFS
   */
  public Class<? extends VFS> getVfs() {
    return this.vfs;
  }

  /**
   * Sets the VFS.
   *
   * @param vfs a VFS
   */
  public void setVfs(Class<? extends VFS> vfs) {
    this.vfs = vfs;
  }

  /**
   * Gets the Cache.
   *
   * @return a specified Cache
   */
  public Cache getCache() {
    return this.cache;
  }

  /**
   * Sets the Cache.
   *
   * @param cache a Cache
   */
  public void setCache(Cache cache) {
    this.cache = cache;
  }

  /**
   * Mybatis plugin list.
   *
   * @param plugins list of plugins
   */
  public void setPlugins(Interceptor... plugins) {
    this.plugins = plugins;
  }

  /**
   * Packages to search for type aliases.
   *
   * <p>
   * allow to specify a wildcard such as {@code com.example.*.model}.
   *
   * @param typeAliasesPackage package to scan for domain objects
   */
  public void setTypeAliasesPackage(String typeAliasesPackage) {
    this.typeAliasesPackage = typeAliasesPackage;
  }

  /**
   * Super class which domain objects have to extend to have a type alias created. No effect if there is no package to
   * scan configured.
   *
   * @param typeAliasesSuperType super class for domain objects
   */
  public void setTypeAliasesSuperType(Class<?> typeAliasesSuperType) {
    this.typeAliasesSuperType = typeAliasesSuperType;
  }

  /**
   * Packages to search for type handlers.
   *
   * <p> allow to specify a wildcard such as {@code com.example.*.typehandler}.
   *
   * @param typeHandlersPackage package to scan for type handlers
   */
  public void setTypeHandlersPackage(String typeHandlersPackage) {
    this.typeHandlersPackage = typeHandlersPackage;
  }

  /**
   * Set type handlers. They must be annotated with {@code MappedTypes} and optionally with {@code MappedJdbcTypes}
   *
   * @param typeHandlers Type handler list
   */
  public void setTypeHandlers(TypeHandler<?>... typeHandlers) {
    this.typeHandlers = typeHandlers;
  }

  /**
   * Set the default type handler class for enum.
   *
   * @param defaultEnumTypeHandler The default type handler class for enum
   */
  public void setDefaultEnumTypeHandler(
          @SuppressWarnings("rawtypes") Class<? extends TypeHandler> defaultEnumTypeHandler) {
    this.defaultEnumTypeHandler = defaultEnumTypeHandler;
  }

  /**
   * List of type aliases to register. They can be annotated with {@code Alias}
   *
   * @param typeAliases Type aliases list
   */
  public void setTypeAliases(Class<?>... typeAliases) {
    this.typeAliases = typeAliases;
  }

  /**
   * If true, a final check is done on Configuration to assure that all
   * mapped statements are fully loaded and there is no one still pending
   * to resolve includes. Defaults to false.
   *
   * @param failFast enable failFast
   */
  public void setFailFast(boolean failFast) {
    this.failFast = failFast;
  }

  /**
   * Set the location of the MyBatis {@code SqlSessionFactory} config file. A typical value is
   * "WEB-INF/mybatis-configuration.xml".
   *
   * @param configLocation a location the MyBatis config file
   */
  public void setConfigLocation(Resource configLocation) {
    this.configLocation = configLocation;
  }

  /**
   * Set a customized MyBatis configuration.
   *
   * @param configuration MyBatis configuration
   */
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Set locations of MyBatis mapper files that are going to be merged into the {@code SqlSessionFactory} configuration
   * at runtime.
   *
   * This is an alternative to specifying "&lt;sqlmapper&gt;" entries in an MyBatis config file. This property being
   * based on Framework's resource abstraction also allows for specifying resource patterns here: e.g.
   * "classpath*:sqlmap/*-mapper.xml".
   *
   * @param mapperLocations location of MyBatis mapper files
   */
  public void setMapperLocations(Resource... mapperLocations) {
    this.mapperLocations = mapperLocations;
  }

  /**
   * Set optional properties to be passed into the SqlSession configuration, as alternative to a
   * {@code &lt;properties&gt;} tag in the configuration xml file. This will be used to resolve placeholders in the
   * config file.
   *
   * @param sqlSessionFactoryProperties optional properties for the SqlSessionFactory
   */
  public void setConfigurationProperties(Properties sqlSessionFactoryProperties) {
    this.configurationProperties = sqlSessionFactoryProperties;
  }

  /**
   * Set the JDBC {@code DataSource} that this instance should manage transactions for. The {@code DataSource} should
   * match the one used by the {@code SqlSessionFactory}: for example, you could specify the same JNDI DataSource for
   * both.
   *
   * A transactional JDBC {@code Connection} for this {@code DataSource} will be provided to application code accessing
   * this {@code DataSource} directly via {@code DataSourceUtils} or {@code DataSourceTransactionManager}.
   *
   * The {@code DataSource} specified here should be the target {@code DataSource} to manage transactions for, not a
   * {@code TransactionAwareDataSourceProxy}. Only data access code may work with
   * {@code TransactionAwareDataSourceProxy}, while the transaction manager needs to work on the underlying target
   * {@code DataSource}. If there's nevertheless a {@code TransactionAwareDataSourceProxy} passed in, it will be
   * unwrapped to extract its target {@code DataSource}.
   *
   * @param dataSource a JDBC {@code DataSource}
   */
  public void setDataSource(DataSource dataSource) {
    if (dataSource instanceof TransactionAwareDataSourceProxy proxy) {
      // If we got a TransactionAwareDataSourceProxy, we need to perform
      // transactions for its underlying target DataSource, else data
      // access code won't see properly exposed transactions (i.e.
      // transactions for the target DataSource).
      this.dataSource = proxy.getTargetDataSource();
    }
    else {
      this.dataSource = dataSource;
    }
  }

  /**
   * Sets the {@code SqlSessionFactoryBuilder} to use when creating the {@code SqlSessionFactory}.
   *
   * This is mainly meant for testing so that mock SqlSessionFactory classes can be injected. By default,
   * {@code SqlSessionFactoryBuilder} creates {@code DefaultSqlSessionFactory} instances.
   *
   * @param sqlSessionFactoryBuilder a SqlSessionFactoryBuilder
   */
  public void setSqlSessionFactoryBuilder(SqlSessionFactoryBuilder sqlSessionFactoryBuilder) {
    this.sqlSessionFactoryBuilder = sqlSessionFactoryBuilder;
  }

  /**
   * Set the MyBatis TransactionFactory to use. Default is {@code ManagedTransactionFactory}
   *
   * The default {@code ManagedTransactionFactory} should be appropriate for all cases: be it transaction
   * management, EJB CMT or plain JTA. If there is no active transaction, SqlSession operations will execute SQL
   * statements non-transactionally.
   *
   * <b>It is strongly recommended to use the default {@code TransactionFactory}.</b> If not used, any attempt at
   * getting an SqlSession through Framework's MyBatis framework will throw an exception if a transaction is active.
   *
   * @param transactionFactory the MyBatis TransactionFactory
   * @see ManagedTransactionFactory
   */
  public void setTransactionFactory(TransactionFactory transactionFactory) {
    this.transactionFactory = transactionFactory;
  }

  /**
   * <b>NOTE:</b> This class <em>overrides</em> any {@code Environment} you have set in the MyBatis config file. This is
   * used only as a placeholder name. The default value is {@code SqlSessionFactoryBean.class.getSimpleName()}.
   *
   * @param environment the environment name
   */
  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  /**
   * Set scripting language drivers.
   *
   * @param scriptingLanguageDrivers scripting language drivers
   */
  public void setScriptingLanguageDrivers(@Nullable LanguageDriver... scriptingLanguageDrivers) {
    this.scriptingLanguageDrivers = scriptingLanguageDrivers;
  }

  /**
   * Set a default scripting language driver class.
   *
   * @param languageDriver A default scripting language driver class
   */
  public void setDefaultScriptingLanguageDriver(@Nullable Class<? extends LanguageDriver> languageDriver) {
    this.defaultScriptingLanguageDriver = languageDriver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.notNull(dataSource, "Property 'dataSource' is required");
    Assert.notNull(sqlSessionFactoryBuilder, "Property 'sqlSessionFactoryBuilder' is required");
    Assert.state((configuration == null && configLocation == null) || !(configuration != null && configLocation != null),
            "Property 'configuration' and 'configLocation' can not specified with together");

    this.sqlSessionFactory = buildSqlSessionFactory();
  }

  /**
   * Build a {@code SqlSessionFactory} instance.
   *
   * The default implementation uses the standard MyBatis {@code XMLConfigBuilder} API to build a
   * {@code SqlSessionFactory} instance based on a Reader. Since 1.3.0, it can be specified a {@link Configuration}
   * instance directly(without config file).
   *
   * @return SqlSessionFactory
   * @throws Exception if configuration is failed
   */
  protected SqlSessionFactory buildSqlSessionFactory() throws Exception {
    final Configuration targetConfiguration;
    XMLConfigBuilder xmlConfigBuilder = null;
    if (configuration != null) {
      targetConfiguration = this.configuration;
      if (targetConfiguration.getVariables() == null) {
        targetConfiguration.setVariables(configurationProperties);
      }
      else if (configurationProperties != null) {
        targetConfiguration.getVariables().putAll(configurationProperties);
      }
    }
    else if (configLocation != null) {
      xmlConfigBuilder = new XMLConfigBuilder(configLocation.getInputStream(), null, configurationProperties);
      targetConfiguration = xmlConfigBuilder.getConfiguration();
    }
    else {
      log.debug("Property 'configuration' or 'configLocation' not specified, using default MyBatis Configuration");
      targetConfiguration = new Configuration();
      if (configurationProperties != null) {
        targetConfiguration.setVariables(configurationProperties);
      }
    }

    if (objectFactory != null) {
      targetConfiguration.setObjectFactory(objectFactory);
    }

    if (objectWrapperFactory != null) {
      targetConfiguration.setObjectWrapperFactory(objectWrapperFactory);
    }
    if (vfs != null) {
      targetConfiguration.setVfsImpl(vfs);
    }

    TypeAliasRegistry aliasRegistry = targetConfiguration.getTypeAliasRegistry();
    if (StringUtils.isNotEmpty(typeAliasesPackage)) {
      scanClassPath(typeAliasesPackage, typeAliasesSuperType, (metadataReader, factory) -> {
        ClassMetadata classMetadata = metadataReader.getClassMetadata();
        if (classMetadata.isIndependent() && !classMetadata.isAbstract() && !classMetadata.isInterface()) {
          aliasRegistry.registerAlias(ClassUtils.resolveClassName(classMetadata.getClassName(), classLoader));
        }
      });
    }

    if (ObjectUtils.isNotEmpty(typeAliases)) {
      for (Class<?> typeAlias : typeAliases) {
        aliasRegistry.registerAlias(typeAlias);
        log.debug("Registered type alias: '{}'", typeAlias);
      }
    }

    if (ObjectUtils.isNotEmpty(plugins)) {
      for (Interceptor plugin : plugins) {
        targetConfiguration.addInterceptor(plugin);
        log.debug("Registered plugin: '{}'", plugin);
      }
    }

    TypeHandlerRegistry handlerRegistry = targetConfiguration.getTypeHandlerRegistry();
    if (StringUtils.isNotEmpty(typeHandlersPackage)) {
      scanClassPath(typeHandlersPackage, TypeHandler.class, (metadataReader, factory) -> {
        ClassMetadata classMetadata = metadataReader.getClassMetadata();
        if (classMetadata.isIndependent() && !classMetadata.isAbstract() && !classMetadata.isInterface()) {
          handlerRegistry.register(ClassUtils.resolveClassName(classMetadata.getClassName(), classLoader));
        }
      });
    }

    if (ObjectUtils.isNotEmpty(typeHandlers)) {
      for (TypeHandler<?> typeHandler : typeHandlers) {
        handlerRegistry.register(typeHandler);
        log.debug("Registered type handler: '{}'", typeHandler);
      }
    }

    targetConfiguration.setDefaultEnumTypeHandler(defaultEnumTypeHandler);

    if (ObjectUtils.isNotEmpty(scriptingLanguageDrivers)) {
      for (LanguageDriver languageDriver : scriptingLanguageDrivers) {
        targetConfiguration.getLanguageRegistry().register(languageDriver);
        log.debug("Registered scripting language driver: '{}'", languageDriver);
      }
    }

    if (defaultScriptingLanguageDriver != null) {
      targetConfiguration.setDefaultScriptingLanguage(defaultScriptingLanguageDriver);
    }

    if (databaseIdProvider != null) {// fix #64 set databaseId before parse mapper xmls
      try {
        targetConfiguration.setDatabaseId(databaseIdProvider.getDatabaseId(dataSource));
      }
      catch (SQLException e) {
        throw new IOException("Failed getting a databaseId", e);
      }
    }
    if (cache != null) {
      targetConfiguration.addCache(cache);
    }

    if (xmlConfigBuilder != null) {
      try {
        xmlConfigBuilder.parse();
        log.debug("Parsed configuration file: '{}'", configLocation);
      }
      catch (Exception ex) {
        throw new IOException("Failed to parse config resource: " + this.configLocation, ex);
      }
      finally {
        ErrorContext.instance().reset();
      }
    }

    targetConfiguration.setEnvironment(new Environment(environment,
            transactionFactory == null ? new ManagedTransactionFactory() : transactionFactory, dataSource));

    if (mapperLocations != null) {
      if (mapperLocations.length == 0) {
        log.warn("Property 'mapperLocations' was specified but matching resources are not found.");
      }
      else {
        for (Resource mapperLocation : mapperLocations) {
          if (mapperLocation == null) {
            continue;
          }
          try {
            XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapperLocation.getInputStream(),
                    targetConfiguration, mapperLocation.toString(), targetConfiguration.getSqlFragments());
            xmlMapperBuilder.parse();
          }
          catch (Exception e) {
            throw new IOException("Failed to parse mapping resource: '" + mapperLocation + "'", e);
          }
          finally {
            ErrorContext.instance().reset();
          }
          log.debug("Parsed mapper file: '{}'", mapperLocation);
        }
      }
    }
    else {
      log.debug("Property 'mapperLocations' was not specified.");
    }

    return sqlSessionFactoryBuilder.build(targetConfiguration);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SqlSessionFactory getObject() throws Exception {
    if (this.sqlSessionFactory == null) {
      afterPropertiesSet();
    }
    return this.sqlSessionFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends SqlSessionFactory> getObjectType() {
    return this.sqlSessionFactory == null ? SqlSessionFactory.class : this.sqlSessionFactory.getClass();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isSingleton() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (failFast) {
      // fail-fast -> check all statements are completed
      this.sqlSessionFactory.getConfiguration().getMappedStatementNames();
    }
  }

  private void scanClassPath(String packagePatterns,
          @Nullable Class<?> assignableType, MetadataReaderConsumer consumer) throws IOException {

    String[] packagePatternArray = StringUtils.tokenizeToStringArray(packagePatterns,
            ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);

    if (componentProvider == null) {
      componentProvider = new ClassPathScanningComponentProvider();
    }
    AssignableTypeFilter typeFilter = getTypeFilter(assignableType);
    for (String packagePattern : packagePatternArray) {
      componentProvider.scan(packagePattern, (metadataReader, factory) -> {
        try {
          if (typeFilter == null || typeFilter.match(metadataReader, factory)) {
            consumer.accept(metadataReader, factory);
          }
        }
        catch (Throwable e) {
          log.warn("Cannot load the '{}'. Cause by {}", metadataReader, e.toString());
        }
      });
    }
  }

  @Nullable
  private AssignableTypeFilter getTypeFilter(@Nullable Class<?> assignableType) {
    AssignableTypeFilter typeFilter = null;
    if (assignableType != null) {
      typeFilter = new AssignableTypeFilter(assignableType);
    }
    return typeFilter;
  }

}
