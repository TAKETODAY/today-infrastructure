/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.orm.hibernate5;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.InfrastructureProxy;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.core.type.classreading.CachingMetadataReaderFactory;
import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;
import cn.taketoday.core.type.filter.AnnotationTypeFilter;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.jta.JtaTransactionManager;
import cn.taketoday.util.ClassUtils;
import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import jakarta.transaction.TransactionManager;

/**
 * A Framework-provided extension of the standard Hibernate {@link Configuration} class,
 * adding {@link HibernateSessionContext} as a default and providing convenient ways
 * to specify a JDBC {@link DataSource} and an application class loader.
 *
 * <p>This is designed for programmatic use, e.g. in {@code @Bean} factory methods;
 * consider using {@link LocalSessionFactoryBean} for XML bean definition files.
 * Typically combined with {@link HibernateTransactionManager} for declarative
 * transactions against the {@code SessionFactory} and its JDBC {@code DataSource}.
 *
 * <p>Compatible with Hibernate 5.5/5.6.
 * This Hibernate-specific factory builder can also be a convenient way to set up
 * a JPA {@code EntityManagerFactory} since the Hibernate {@code SessionFactory}
 * natively exposes the JPA {@code EntityManagerFactory} interface as well now.
 *
 * <p>This builder supports Hibernate {@code BeanContainer} integration,
 * {@link MetadataSources} from custom {@link BootstrapServiceRegistryBuilder}
 * setup, as well as other advanced Hibernate configuration options beyond the
 * standard JPA bootstrap contract.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see HibernateTransactionManager
 * @see LocalSessionFactoryBean
 * @see #setBeanContainer
 * @see #LocalSessionFactoryBuilder(DataSource, ResourceLoader, MetadataSources)
 * @see BootstrapServiceRegistryBuilder
 * @since 4.0
 */
@SuppressWarnings("serial")
public class LocalSessionFactoryBuilder extends Configuration {

  private static final String RESOURCE_PATTERN = "/**/*.class";

  private static final String PACKAGE_INFO_SUFFIX = ".package-info";

  private static final TypeFilter[] DEFAULT_ENTITY_TYPE_FILTERS = new TypeFilter[] {
          new AnnotationTypeFilter(Entity.class, false),
          new AnnotationTypeFilter(Embeddable.class, false),
          new AnnotationTypeFilter(MappedSuperclass.class, false)
  };

  private static final TypeFilter CONVERTER_TYPE_FILTER = new AnnotationTypeFilter(Converter.class, false);

  private final PatternResourceLoader patternResourceLoader;

  @Nullable
  private TypeFilter[] entityTypeFilters = DEFAULT_ENTITY_TYPE_FILTERS;

  /**
   * Create a new LocalSessionFactoryBuilder for the given DataSource.
   *
   * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
   * (may be {@code null})
   */
  public LocalSessionFactoryBuilder(@Nullable DataSource dataSource) {
    this(dataSource, new PathMatchingPatternResourceLoader());
  }

  /**
   * Create a new LocalSessionFactoryBuilder for the given DataSource.
   *
   * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
   * (may be {@code null})
   * @param classLoader the ClassLoader to load application classes from
   */
  public LocalSessionFactoryBuilder(@Nullable DataSource dataSource, ClassLoader classLoader) {
    this(dataSource, new PathMatchingPatternResourceLoader(classLoader));
  }

  /**
   * Create a new LocalSessionFactoryBuilder for the given DataSource.
   *
   * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
   * (may be {@code null})
   * @param resourceLoader the ResourceLoader to load application classes from
   */
  public LocalSessionFactoryBuilder(@Nullable DataSource dataSource, ResourceLoader resourceLoader) {
    this(dataSource, resourceLoader, new MetadataSources(
            new BootstrapServiceRegistryBuilder().applyClassLoader(resourceLoader.getClassLoader()).build()));
  }

  /**
   * Create a new LocalSessionFactoryBuilder for the given DataSource.
   *
   * @param dataSource the JDBC DataSource that the resulting Hibernate SessionFactory should be using
   * (may be {@code null})
   * @param resourceLoader the ResourceLoader to load application classes from
   * @param metadataSources the Hibernate MetadataSources service to use (e.g. reusing an existing one)
   */
  public LocalSessionFactoryBuilder(
          @Nullable DataSource dataSource, ResourceLoader resourceLoader, MetadataSources metadataSources) {
    super(metadataSources);
    Properties properties = getProperties();
    properties.put(AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, HibernateSessionContext.class.getName());
    if (dataSource != null) {
      properties.put(AvailableSettings.DATASOURCE, dataSource);
    }
    properties.put(AvailableSettings.CONNECTION_HANDLING,
            PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD);

    properties.put(AvailableSettings.CLASSLOADERS, Collections.singleton(resourceLoader.getClassLoader()));
    this.patternResourceLoader = PatternResourceLoader.fromResourceLoader(resourceLoader);
  }

  /**
   * Set the Framework {@link JtaTransactionManager} or the JTA {@link TransactionManager}
   * to be used with Hibernate, if any. Allows for using a Framework-managed transaction
   * manager for Hibernate 5's session and cache synchronization, with the
   * "hibernate.transaction.jta.platform" automatically set to it.
   * <p>A passed-in Framework {@link JtaTransactionManager} needs to contain a JTA
   * {@link TransactionManager} reference to be usable here, except for the WebSphere
   * case where we'll automatically set {@code WebSphereExtendedJtaPlatform} accordingly.
   * <p>Note: If this is set, the Hibernate settings should not contain a JTA platform
   * setting to avoid meaningless double configuration.
   */
  public LocalSessionFactoryBuilder setJtaTransactionManager(Object jtaTransactionManager) {
    Assert.notNull(jtaTransactionManager, "Transaction manager reference must not be null");

    Properties properties = getProperties();
    if (jtaTransactionManager instanceof JtaTransactionManager) {
      boolean webspherePresent = ClassUtils.isPresent("com.ibm.wsspi.uow.UOWManager", getClass().getClassLoader());
      if (webspherePresent) {
        properties.put(AvailableSettings.JTA_PLATFORM,
                "org.hibernate.engine.transaction.jta.platform.internal.WebSphereExtendedJtaPlatform");
      }
      else {
        JtaTransactionManager jtaTm = (JtaTransactionManager) jtaTransactionManager;
        if (jtaTm.getTransactionManager() == null) {
          throw new IllegalArgumentException(
                  "Can only apply JtaTransactionManager which has a TransactionManager reference set");
        }
        properties.put(AvailableSettings.JTA_PLATFORM,
                new ConfigurableJtaPlatform(jtaTm.getTransactionManager(), jtaTm.getUserTransaction(),
                        jtaTm.getTransactionSynchronizationRegistry()));
      }
    }
    else if (jtaTransactionManager instanceof TransactionManager) {
      properties.put(AvailableSettings.JTA_PLATFORM,
              new ConfigurableJtaPlatform((TransactionManager) jtaTransactionManager, null, null));
    }
    else {
      throw new IllegalArgumentException(
              "Unknown transaction manager type: " + jtaTransactionManager.getClass().getName());
    }

    properties.put(AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta");
    properties.put(AvailableSettings.CONNECTION_HANDLING,
            PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT);

    return this;
  }

  /**
   * Set a Hibernate {@link org.hibernate.resource.beans.container.spi.BeanContainer}
   * for the given Framework {@link ConfigurableBeanFactory}.
   * <p>This enables autowiring of Hibernate attribute converters and entity listeners.
   *
   * @see HibernateBeanContainer
   * @see AvailableSettings#BEAN_CONTAINER
   */
  public LocalSessionFactoryBuilder setBeanContainer(ConfigurableBeanFactory beanFactory) {
    getProperties().put(AvailableSettings.BEAN_CONTAINER, new HibernateBeanContainer(beanFactory));
    return this;
  }

  /**
   * Set the Hibernate {@link RegionFactory} to use for the SessionFactory.
   * Allows for using a Framework-managed {@code RegionFactory} instance.
   * <p>Note: If this is set, the Hibernate settings should not define a
   * cache provider to avoid meaningless double configuration.
   *
   * @see AvailableSettings#CACHE_REGION_FACTORY
   */
  public LocalSessionFactoryBuilder setCacheRegionFactory(RegionFactory cacheRegionFactory) {
    getProperties().put(AvailableSettings.CACHE_REGION_FACTORY, cacheRegionFactory);
    return this;
  }

  /**
   * Set a {@link MultiTenantConnectionProvider} to be passed on to the SessionFactory.
   *
   * @see AvailableSettings#MULTI_TENANT_CONNECTION_PROVIDER
   */
  public LocalSessionFactoryBuilder setMultiTenantConnectionProvider(MultiTenantConnectionProvider multiTenantConnectionProvider) {
    getProperties().put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProvider);
    return this;
  }

  /**
   * Overridden to reliably pass a {@link CurrentTenantIdentifierResolver} to the SessionFactory.
   *
   * @see AvailableSettings#MULTI_TENANT_IDENTIFIER_RESOLVER
   */
  @Override
  public void setCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
    getProperties().put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, currentTenantIdentifierResolver);
    super.setCurrentTenantIdentifierResolver(currentTenantIdentifierResolver);
  }

  /**
   * Specify custom type filters for Framework-based scanning for entity classes.
   * <p>Default is to search all specified packages for classes annotated with
   * {@code @jakarta.persistence.Entity}, {@code @jakarta.persistence.Embeddable}
   * or {@code @jakarta.persistence.MappedSuperclass}.
   *
   * @see #scanPackages
   */
  public LocalSessionFactoryBuilder setEntityTypeFilters(TypeFilter... entityTypeFilters) {
    this.entityTypeFilters = entityTypeFilters;
    return this;
  }

  /**
   * Add the given annotated classes in a batch.
   *
   * @see #addAnnotatedClass
   * @see #scanPackages
   */
  public LocalSessionFactoryBuilder addAnnotatedClasses(Class<?>... annotatedClasses) {
    for (Class<?> annotatedClass : annotatedClasses) {
      addAnnotatedClass(annotatedClass);
    }
    return this;
  }

  /**
   * Add the given annotated packages in a batch.
   *
   * @see #addPackage
   * @see #scanPackages
   */
  public LocalSessionFactoryBuilder addPackages(String... annotatedPackages) {
    for (String annotatedPackage : annotatedPackages) {
      addPackage(annotatedPackage);
    }
    return this;
  }

  /**
   * Perform Framework-based scanning for entity classes, registering them
   * as annotated classes with this {@code Configuration}.
   *
   * @param packagesToScan one or more Java package names
   * @throws HibernateException if scanning fails for any reason
   */
  public LocalSessionFactoryBuilder scanPackages(String... packagesToScan) throws HibernateException {
    TreeSet<String> packageNames = new TreeSet<>();
    TreeSet<String> entityClassNames = new TreeSet<>();
    TreeSet<String> converterClassNames = new TreeSet<>();
    try {
      for (String pkg : packagesToScan) {
        String pattern = PatternResourceLoader.CLASSPATH_ALL_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(pkg) + RESOURCE_PATTERN;
        var readerFactory = new CachingMetadataReaderFactory(patternResourceLoader);
        for (Resource resource : patternResourceLoader.getResources(pattern)) {
          try {
            MetadataReader reader = readerFactory.getMetadataReader(resource);
            String className = reader.getClassMetadata().getClassName();
            if (matchesEntityTypeFilter(reader, readerFactory)) {
              entityClassNames.add(className);
            }
            else if (CONVERTER_TYPE_FILTER.match(reader, readerFactory)) {
              converterClassNames.add(className);
            }
            else if (className.endsWith(PACKAGE_INFO_SUFFIX)) {
              packageNames.add(className.substring(0, className.length() - PACKAGE_INFO_SUFFIX.length()));
            }
          }
          catch (FileNotFoundException ex) {
            // Ignore non-readable resource
          }
        }
      }
    }
    catch (IOException ex) {
      throw new MappingException("Failed to scan classpath for unlisted classes", ex);
    }
    try {
      ClassLoader cl = this.patternResourceLoader.getClassLoader();
      for (String className : entityClassNames) {
        addAnnotatedClass(ClassUtils.forName(className, cl));
      }
      for (String className : converterClassNames) {
        addAttributeConverter(ClassUtils.forName(className, cl));
      }
      for (String packageName : packageNames) {
        addPackage(packageName);
      }
    }
    catch (ClassNotFoundException ex) {
      throw new MappingException("Failed to load annotated classes from classpath", ex);
    }
    return this;
  }

  /**
   * Check whether any of the configured entity type filters matches
   * the current class descriptor contained in the metadata reader.
   */
  private boolean matchesEntityTypeFilter(
          MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {
    if (entityTypeFilters != null) {
      for (TypeFilter filter : entityTypeFilters) {
        if (filter.match(reader, readerFactory)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Build the Hibernate {@code SessionFactory} through background bootstrapping,
   * using the given executor for a parallel initialization phase
   * (e.g. a {@link cn.taketoday.core.task.SimpleAsyncTaskExecutor}).
   * <p>{@code SessionFactory} initialization will then switch into background
   * bootstrap mode, with a {@code SessionFactory} proxy immediately returned for
   * injection purposes instead of waiting for Hibernate's bootstrapping to complete.
   * However, note that the first actual call to a {@code SessionFactory} method will
   * then block until Hibernate's bootstrapping completed, if not ready by then.
   * For maximum benefit, make sure to avoid early {@code SessionFactory} calls
   * in init methods of related beans, even for metadata introspection purposes.
   *
   * @see #buildSessionFactory()
   */
  public SessionFactory buildSessionFactory(AsyncTaskExecutor bootstrapExecutor) {
    Assert.notNull(bootstrapExecutor, "AsyncTaskExecutor must not be null");
    return (SessionFactory) Proxy.newProxyInstance(patternResourceLoader.getClassLoader(),
            new Class<?>[] { SessionFactoryImplementor.class, InfrastructureProxy.class },
            new BootstrapSessionFactoryInvocationHandler(bootstrapExecutor));
  }

  /**
   * Proxy invocation handler for background bootstrapping, only enforcing
   * a fully initialized target {@code SessionFactory} when actually needed.
   */
  private class BootstrapSessionFactoryInvocationHandler implements InvocationHandler {

    private final Future<SessionFactory> sessionFactoryFuture;

    public BootstrapSessionFactoryInvocationHandler(AsyncTaskExecutor bootstrapExecutor) {
      this.sessionFactoryFuture = bootstrapExecutor.submit(
              (Callable<SessionFactory>) LocalSessionFactoryBuilder.this::buildSessionFactory);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      switch (method.getName()) {
        case "equals" -> {
          // Only consider equal when proxies are identical.
          return (proxy == args[0]);
        }
        case "hashCode" -> {
          // Use hashCode of EntityManagerFactory proxy.
          return System.identityHashCode(proxy);
        }
        case "getProperties" -> {
          return getProperties();
        }
        case "getWrappedObject" -> {
          // Call coming in through InfrastructureProxy interface...
          return getSessionFactory();
        }
      }

      // Regular delegation to the target SessionFactory,
      // enforcing its full initialization...
      try {
        return method.invoke(getSessionFactory(), args);
      }
      catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
    }

    private SessionFactory getSessionFactory() {
      try {
        return sessionFactoryFuture.get();
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted during initialization of Hibernate SessionFactory", ex);
      }
      catch (ExecutionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof HibernateException) {
          // Rethrow a provider configuration exception (possibly with a nested cause) directly
          throw (HibernateException) cause;
        }
        throw new IllegalStateException("Failed to asynchronously initialize Hibernate SessionFactory: " +
                ex.getMessage(), cause);
      }
    }
  }

}
