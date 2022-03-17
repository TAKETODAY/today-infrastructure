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

package cn.taketoday.orm.hibernate5;

import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.ServiceRegistry;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.aware.ResourceLoaderAware;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.PathMatchingPatternResourceLoader;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.core.type.filter.TypeFilter;
import cn.taketoday.lang.Nullable;

/**
 * {@link FactoryBean} that creates a Hibernate {@link SessionFactory}. This is the usual
 * way to set up a shared Hibernate SessionFactory in a Framework application context; the
 * SessionFactory can then be passed to data access objects via dependency injection.
 *
 * <p>Compatible with Hibernate 5.5/5.6.
 * This Hibernate-specific {@code LocalSessionFactoryBean} can be an immediate alternative
 * to {@link cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean} for
 * common JPA purposes: The Hibernate {@code SessionFactory} will natively expose the JPA
 * {@code EntityManagerFactory} interface as well, and Hibernate {@code BeanContainer}
 * integration will be registered out of the box. In combination with
 * {@link HibernateTransactionManager}, this naturally allows for mixing JPA access code
 * with native Hibernate access code within the same transaction.
 *
 * @author Juergen Hoeller
 * @see #setDataSource
 * @see #setPackagesToScan
 * @see HibernateTransactionManager
 * @see LocalSessionFactoryBuilder
 * @see cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean
 * @since 4.0
 */
public class LocalSessionFactoryBean extends HibernateExceptionTranslator
        implements FactoryBean<SessionFactory>, ResourceLoaderAware, BeanFactoryAware, InitializingBean, DisposableBean {

  @Nullable
  private DataSource dataSource;

  @Nullable
  private Resource[] configLocations;

  @Nullable
  private String[] mappingResources;

  @Nullable
  private Resource[] mappingLocations;

  @Nullable
  private Resource[] cacheableMappingLocations;

  @Nullable
  private Resource[] mappingJarLocations;

  @Nullable
  private Resource[] mappingDirectoryLocations;

  @Nullable
  private Interceptor entityInterceptor;

  @Nullable
  private ImplicitNamingStrategy implicitNamingStrategy;

  @Nullable
  private PhysicalNamingStrategy physicalNamingStrategy;

  @Nullable
  private Object jtaTransactionManager;

  @Nullable
  private RegionFactory cacheRegionFactory;

  @Nullable
  private MultiTenantConnectionProvider multiTenantConnectionProvider;

  @Nullable
  private CurrentTenantIdentifierResolver currentTenantIdentifierResolver;

  @Nullable
  private Properties hibernateProperties;

  @Nullable
  private TypeFilter[] entityTypeFilters;

  @Nullable
  private Class<?>[] annotatedClasses;

  @Nullable
  private String[] annotatedPackages;

  @Nullable
  private String[] packagesToScan;

  @Nullable
  private AsyncTaskExecutor bootstrapExecutor;

  @Nullable
  private Integrator[] hibernateIntegrators;

  private boolean metadataSourcesAccessed = false;

  @Nullable
  private MetadataSources metadataSources;

  @Nullable
  private PatternResourceLoader patternResourceLoader;

  @Nullable
  private ConfigurableBeanFactory beanFactory;

  @Nullable
  private Configuration configuration;

  @Nullable
  private SessionFactory sessionFactory;

  /**
   * Set the DataSource to be used by the SessionFactory.
   * If set, this will override corresponding settings in Hibernate properties.
   * <p>If this is set, the Hibernate settings should not define
   * a connection provider to avoid meaningless double configuration.
   */
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  /**
   * Set the location of a single Hibernate XML config file, for example as
   * classpath resource "classpath:hibernate.cfg.xml".
   * <p>Note: Can be omitted when all necessary properties and mapping
   * resources are specified locally via this bean.
   *
   * @see Configuration#configure(java.net.URL)
   */
  public void setConfigLocation(Resource configLocation) {
    this.configLocations = new Resource[] { configLocation };
  }

  /**
   * Set the locations of multiple Hibernate XML config files, for example as
   * classpath resources "classpath:hibernate.cfg.xml,classpath:extension.cfg.xml".
   * <p>Note: Can be omitted when all necessary properties and mapping
   * resources are specified locally via this bean.
   *
   * @see Configuration#configure(java.net.URL)
   */
  public void setConfigLocations(Resource... configLocations) {
    this.configLocations = configLocations;
  }

  /**
   * Set Hibernate mapping resources to be found in the class path,
   * like "example.hbm.xml" or "mypackage/example.hbm.xml".
   * Analogous to mapping entries in a Hibernate XML config file.
   * Alternative to the more generic setMappingLocations method.
   * <p>Can be used to add to mappings from a Hibernate XML config file,
   * or to specify all mappings locally.
   *
   * @see #setMappingLocations
   * @see Configuration#addResource
   */
  public void setMappingResources(String... mappingResources) {
    this.mappingResources = mappingResources;
  }

  /**
   * Set locations of Hibernate mapping files, for example as classpath
   * resource "classpath:example.hbm.xml". Supports any resource location
   * via Framework's resource abstraction, for example relative paths like
   * "WEB-INF/mappings/example.hbm.xml" when running in an application context.
   * <p>Can be used to add to mappings from a Hibernate XML config file,
   * or to specify all mappings locally.
   *
   * @see Configuration#addInputStream
   */
  public void setMappingLocations(Resource... mappingLocations) {
    this.mappingLocations = mappingLocations;
  }

  /**
   * Set locations of cacheable Hibernate mapping files, for example as web app
   * resource "/WEB-INF/mapping/example.hbm.xml". Supports any resource location
   * via Framework's resource abstraction, as long as the resource can be resolved
   * in the file system.
   * <p>Can be used to add to mappings from a Hibernate XML config file,
   * or to specify all mappings locally.
   *
   * @see Configuration#addCacheableFile(File)
   */
  public void setCacheableMappingLocations(Resource... cacheableMappingLocations) {
    this.cacheableMappingLocations = cacheableMappingLocations;
  }

  /**
   * Set locations of jar files that contain Hibernate mapping resources,
   * like "WEB-INF/lib/example.hbm.jar".
   * <p>Can be used to add to mappings from a Hibernate XML config file,
   * or to specify all mappings locally.
   *
   * @see Configuration#addJar(File)
   */
  public void setMappingJarLocations(Resource... mappingJarLocations) {
    this.mappingJarLocations = mappingJarLocations;
  }

  /**
   * Set locations of directories that contain Hibernate mapping resources,
   * like "WEB-INF/mappings".
   * <p>Can be used to add to mappings from a Hibernate XML config file,
   * or to specify all mappings locally.
   *
   * @see Configuration#addDirectory(File)
   */
  public void setMappingDirectoryLocations(Resource... mappingDirectoryLocations) {
    this.mappingDirectoryLocations = mappingDirectoryLocations;
  }

  /**
   * Set a Hibernate entity interceptor that allows to inspect and change
   * property values before writing to and reading from the database.
   * Will get applied to any new Session created by this factory.
   *
   * @see Configuration#setInterceptor
   */
  public void setEntityInterceptor(Interceptor entityInterceptor) {
    this.entityInterceptor = entityInterceptor;
  }

  /**
   * Set a Hibernate 5 {@link ImplicitNamingStrategy} for the SessionFactory.
   *
   * @see Configuration#setImplicitNamingStrategy
   */
  public void setImplicitNamingStrategy(ImplicitNamingStrategy implicitNamingStrategy) {
    this.implicitNamingStrategy = implicitNamingStrategy;
  }

  /**
   * Set a Hibernate 5 {@link PhysicalNamingStrategy} for the SessionFactory.
   *
   * @see Configuration#setPhysicalNamingStrategy
   */
  public void setPhysicalNamingStrategy(PhysicalNamingStrategy physicalNamingStrategy) {
    this.physicalNamingStrategy = physicalNamingStrategy;
  }

  /**
   * Set the Framework {@link cn.taketoday.transaction.jta.JtaTransactionManager}
   * or the JTA {@link jakarta.transaction.TransactionManager} to be used with Hibernate,
   * if any. Implicitly sets up {@code JtaPlatform}.
   *
   * @see LocalSessionFactoryBuilder#setJtaTransactionManager
   */
  public void setJtaTransactionManager(Object jtaTransactionManager) {
    this.jtaTransactionManager = jtaTransactionManager;
  }

  /**
   * Set the Hibernate {@link RegionFactory} to use for the SessionFactory.
   * Allows for using a Framework-managed {@code RegionFactory} instance.
   * <p>Note: If this is set, the Hibernate settings should not define a
   * cache provider to avoid meaningless double configuration.
   *
   * @see LocalSessionFactoryBuilder#setCacheRegionFactory
   * @since 4.0
   */
  public void setCacheRegionFactory(RegionFactory cacheRegionFactory) {
    this.cacheRegionFactory = cacheRegionFactory;
  }

  /**
   * Set a {@link MultiTenantConnectionProvider} to be passed on to the SessionFactory.
   *
   * @see LocalSessionFactoryBuilder#setMultiTenantConnectionProvider
   * @since 4.0
   */
  public void setMultiTenantConnectionProvider(MultiTenantConnectionProvider multiTenantConnectionProvider) {
    this.multiTenantConnectionProvider = multiTenantConnectionProvider;
  }

  /**
   * Set a {@link CurrentTenantIdentifierResolver} to be passed on to the SessionFactory.
   *
   * @see LocalSessionFactoryBuilder#setCurrentTenantIdentifierResolver
   */
  public void setCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver currentTenantIdentifierResolver) {
    this.currentTenantIdentifierResolver = currentTenantIdentifierResolver;
  }

  /**
   * Set Hibernate properties, such as "hibernate.dialect".
   * <p>Note: Do not specify a transaction provider here when using
   * Framework-driven transactions. It is also advisable to omit connection
   * provider settings and use a Framework-set DataSource instead.
   *
   * @see #setDataSource
   */
  public void setHibernateProperties(Properties hibernateProperties) {
    this.hibernateProperties = hibernateProperties;
  }

  /**
   * Return the Hibernate properties, if any. Mainly available for
   * configuration through property paths that specify individual keys.
   */
  public Properties getHibernateProperties() {
    if (hibernateProperties == null) {
      this.hibernateProperties = new Properties();
    }
    return hibernateProperties;
  }

  /**
   * Specify custom type filters for Framework-based scanning for entity classes.
   * <p>Default is to search all specified packages for classes annotated with
   * {@code @jakarta.persistence.Entity}, {@code @jakarta.persistence.Embeddable}
   * or {@code @jakarta.persistence.MappedSuperclass}.
   *
   * @see #setPackagesToScan
   */
  public void setEntityTypeFilters(TypeFilter... entityTypeFilters) {
    this.entityTypeFilters = entityTypeFilters;
  }

  /**
   * Specify annotated entity classes to register with this Hibernate SessionFactory.
   *
   * @see Configuration#addAnnotatedClass(Class)
   */
  public void setAnnotatedClasses(Class<?>... annotatedClasses) {
    this.annotatedClasses = annotatedClasses;
  }

  /**
   * Specify the names of annotated packages, for which package-level
   * annotation metadata will be read.
   *
   * @see Configuration#addPackage(String)
   */
  public void setAnnotatedPackages(String... annotatedPackages) {
    this.annotatedPackages = annotatedPackages;
  }

  /**
   * Specify packages to search for autodetection of your entity classes in the
   * classpath. This is analogous to Framework's component-scan feature
   * ({@link cn.taketoday.context.annotation.ClassPathBeanDefinitionScanner}).
   */
  public void setPackagesToScan(String... packagesToScan) {
    this.packagesToScan = packagesToScan;
  }

  /**
   * Specify an asynchronous executor for background bootstrapping,
   * e.g. a {@link cn.taketoday.core.task.SimpleAsyncTaskExecutor}.
   * <p>{@code SessionFactory} initialization will then switch into background
   * bootstrap mode, with a {@code SessionFactory} proxy immediately returned for
   * injection purposes instead of waiting for Hibernate's bootstrapping to complete.
   * However, note that the first actual call to a {@code SessionFactory} method will
   * then block until Hibernate's bootstrapping completed, if not ready by then.
   * For maximum benefit, make sure to avoid early {@code SessionFactory} calls
   * in init methods of related beans, even for metadata introspection purposes.
   *
   * @see LocalSessionFactoryBuilder#buildSessionFactory(AsyncTaskExecutor)
   * @since 4.0
   */
  public void setBootstrapExecutor(AsyncTaskExecutor bootstrapExecutor) {
    this.bootstrapExecutor = bootstrapExecutor;
  }

  /**
   * Specify one or more Hibernate {@link Integrator} implementations to apply.
   * <p>This will only be applied for an internally built {@link MetadataSources}
   * instance. {@link #setMetadataSources} effectively overrides such settings,
   * with integrators to be applied to the externally built {@link MetadataSources}.
   *
   * @see #setMetadataSources
   * @see BootstrapServiceRegistryBuilder#applyIntegrator
   * @since 4.0
   */
  public void setHibernateIntegrators(Integrator... hibernateIntegrators) {
    this.hibernateIntegrators = hibernateIntegrators;
  }

  /**
   * Specify a Hibernate {@link MetadataSources} service to use (e.g. reusing an
   * existing one), potentially populated with a custom Hibernate bootstrap
   * {@link ServiceRegistry} as well.
   *
   * @see MetadataSources#MetadataSources(ServiceRegistry)
   * @see BootstrapServiceRegistryBuilder#build()
   * @since 4.0
   */
  public void setMetadataSources(MetadataSources metadataSources) {
    this.metadataSourcesAccessed = true;
    this.metadataSources = metadataSources;
  }

  /**
   * Determine the Hibernate {@link MetadataSources} to use.
   * <p>Can also be externally called to initialize and pre-populate a {@link MetadataSources}
   * instance which is then going to be used for {@link SessionFactory} building.
   *
   * @return the MetadataSources to use (never {@code null})
   * @see LocalSessionFactoryBuilder#LocalSessionFactoryBuilder(DataSource, ResourceLoader, MetadataSources)
   * @since 4.0
   */
  public MetadataSources getMetadataSources() {
    this.metadataSourcesAccessed = true;
    if (metadataSources == null) {
      BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
      if (patternResourceLoader != null) {
        builder = builder.applyClassLoader(patternResourceLoader.getClassLoader());
      }
      if (hibernateIntegrators != null) {
        for (Integrator integrator : hibernateIntegrators) {
          builder = builder.applyIntegrator(integrator);
        }
      }
      this.metadataSources = new MetadataSources(builder.build());
    }
    return metadataSources;
  }

  /**
   * Specify a Framework {@link ResourceLoader} to use for Hibernate metadata.
   *
   * @param resourceLoader the ResourceLoader to use (never {@code null})
   */
  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.patternResourceLoader = PatternResourceLoader.fromResourceLoader(resourceLoader);
  }

  /**
   * Determine the Framework {@link ResourceLoader} to use for Hibernate metadata.
   *
   * @return the ResourceLoader to use (never {@code null})
   */
  public ResourceLoader getResourceLoader() {
    if (patternResourceLoader == null) {
      this.patternResourceLoader = new PathMatchingPatternResourceLoader();
    }
    return patternResourceLoader;
  }

  /**
   * Accept the containing {@link BeanFactory}, registering corresponding Hibernate
   * {@link org.hibernate.resource.beans.container.spi.BeanContainer} integration for
   * it if possible. This requires a Framework {@link ConfigurableBeanFactory}.
   *
   * @see FrameworkBeanContainer
   * @see LocalSessionFactoryBuilder#setBeanContainer
   */
  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    if (beanFactory instanceof ConfigurableBeanFactory) {
      this.beanFactory = (ConfigurableBeanFactory) beanFactory;
    }
  }

  @Override
  public void afterPropertiesSet() throws IOException {
    if (metadataSources != null && !metadataSourcesAccessed) {
      // Repeated initialization with no user-customized MetadataSources -> clear it.
      this.metadataSources = null;
    }

    LocalSessionFactoryBuilder sfb = new LocalSessionFactoryBuilder(
            dataSource, getResourceLoader(), getMetadataSources());

    if (configLocations != null) {
      for (Resource resource : configLocations) {
        // Load Hibernate configuration from given location.
        sfb.configure(resource.getLocation());
      }
    }

    if (mappingResources != null) {
      // Register given Hibernate mapping definitions, contained in resource files.
      for (String mapping : mappingResources) {
        Resource mr = new ClassPathResource(mapping.trim(), getResourceLoader().getClassLoader());
        sfb.addInputStream(mr.getInputStream());
      }
    }

    if (mappingLocations != null) {
      // Register given Hibernate mapping definitions, contained in resource files.
      for (Resource resource : mappingLocations) {
        sfb.addInputStream(resource.getInputStream());
      }
    }

    if (cacheableMappingLocations != null) {
      // Register given cacheable Hibernate mapping definitions, read from the file system.
      for (Resource resource : cacheableMappingLocations) {
        sfb.addCacheableFile(resource.getFile());
      }
    }

    if (mappingJarLocations != null) {
      // Register given Hibernate mapping definitions, contained in jar files.
      for (Resource resource : mappingJarLocations) {
        sfb.addJar(resource.getFile());
      }
    }

    if (mappingDirectoryLocations != null) {
      // Register all Hibernate mapping definitions in the given directories.
      for (Resource resource : mappingDirectoryLocations) {
        File file = resource.getFile();
        if (!file.isDirectory()) {
          throw new IllegalArgumentException(
                  "Mapping directory location [" + resource + "] does not denote a directory");
        }
        sfb.addDirectory(file);
      }
    }

    if (entityInterceptor != null) {
      sfb.setInterceptor(entityInterceptor);
    }

    if (implicitNamingStrategy != null) {
      sfb.setImplicitNamingStrategy(implicitNamingStrategy);
    }

    if (physicalNamingStrategy != null) {
      sfb.setPhysicalNamingStrategy(physicalNamingStrategy);
    }

    if (jtaTransactionManager != null) {
      sfb.setJtaTransactionManager(jtaTransactionManager);
    }

    if (beanFactory != null) {
      sfb.setBeanContainer(beanFactory);
    }

    if (cacheRegionFactory != null) {
      sfb.setCacheRegionFactory(cacheRegionFactory);
    }

    if (multiTenantConnectionProvider != null) {
      sfb.setMultiTenantConnectionProvider(multiTenantConnectionProvider);
    }

    if (currentTenantIdentifierResolver != null) {
      sfb.setCurrentTenantIdentifierResolver(currentTenantIdentifierResolver);
    }

    if (hibernateProperties != null) {
      sfb.addProperties(hibernateProperties);
    }

    if (entityTypeFilters != null) {
      sfb.setEntityTypeFilters(entityTypeFilters);
    }

    if (annotatedClasses != null) {
      sfb.addAnnotatedClasses(annotatedClasses);
    }

    if (annotatedPackages != null) {
      sfb.addPackages(annotatedPackages);
    }

    if (packagesToScan != null) {
      sfb.scanPackages(packagesToScan);
    }

    // Build SessionFactory instance.
    this.configuration = sfb;
    this.sessionFactory = buildSessionFactory(sfb);
  }

  /**
   * Subclasses can override this method to perform custom initialization
   * of the SessionFactory instance, creating it via the given Configuration
   * object that got prepared by this LocalSessionFactoryBean.
   * <p>The default implementation invokes LocalSessionFactoryBuilder's buildSessionFactory.
   * A custom implementation could prepare the instance in a specific way (e.g. applying
   * a custom ServiceRegistry) or use a custom SessionFactoryImpl subclass.
   *
   * @param sfb a LocalSessionFactoryBuilder prepared by this LocalSessionFactoryBean
   * @return the SessionFactory instance
   * @see LocalSessionFactoryBuilder#buildSessionFactory
   */
  protected SessionFactory buildSessionFactory(LocalSessionFactoryBuilder sfb) {
    return (bootstrapExecutor != null ? sfb.buildSessionFactory(bootstrapExecutor) :
            sfb.buildSessionFactory());
  }

  /**
   * Return the Hibernate Configuration object used to build the SessionFactory.
   * Allows for access to configuration metadata stored there (rarely needed).
   *
   * @throws IllegalStateException if the Configuration object has not been initialized yet
   */
  public final Configuration getConfiguration() {
    if (configuration == null) {
      throw new IllegalStateException("Configuration not initialized yet");
    }
    return configuration;
  }

  @Override
  @Nullable
  public SessionFactory getObject() {
    return sessionFactory;
  }

  @Override
  public Class<?> getObjectType() {
    return (sessionFactory != null ? sessionFactory.getClass() : SessionFactory.class);
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void destroy() {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
  }

}
