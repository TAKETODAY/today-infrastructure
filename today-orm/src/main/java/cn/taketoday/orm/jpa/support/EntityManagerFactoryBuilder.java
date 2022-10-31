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

package cn.taketoday.orm.jpa.support;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import cn.taketoday.core.task.AsyncTaskExecutor;
import cn.taketoday.orm.jpa.JpaVendorAdapter;
import cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean;
import cn.taketoday.orm.jpa.persistenceunit.PersistenceManagedTypes;
import cn.taketoday.orm.jpa.persistenceunit.PersistenceUnitManager;
import cn.taketoday.orm.jpa.persistenceunit.PersistenceUnitPostProcessor;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Convenient builder for JPA EntityManagerFactory instances. Collects common
 * configuration when constructed and then allows you to create one or more
 * {@link LocalContainerEntityManagerFactoryBean} through a fluent builder pattern. The
 * most common options are covered in the builder, but you can always manipulate the
 * product of the builder if you need more control, before returning it from a
 * {@code @Bean} definition.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/31 10:21
 */
public class EntityManagerFactoryBuilder {

  private final JpaVendorAdapter jpaVendorAdapter;

  private final PersistenceUnitManager persistenceUnitManager;

  private final Map<String, Object> jpaProperties;

  private final URL persistenceUnitRootLocation;

  private AsyncTaskExecutor bootstrapExecutor;

  private PersistenceUnitPostProcessor[] persistenceUnitPostProcessors;

  /**
   * Create a new instance passing in the common pieces that will be shared if multiple
   * EntityManagerFactory instances are created.
   *
   * @param jpaVendorAdapter a vendor adapter
   * @param jpaProperties the JPA properties to be passed to the persistence provider
   * @param persistenceUnitManager optional source of persistence unit information (can
   * be null)
   */
  public EntityManagerFactoryBuilder(JpaVendorAdapter jpaVendorAdapter, Map<String, ?> jpaProperties,
          PersistenceUnitManager persistenceUnitManager) {
    this(jpaVendorAdapter, jpaProperties, persistenceUnitManager, null);
  }

  /**
   * Create a new instance passing in the common pieces that will be shared if multiple
   * EntityManagerFactory instances are created.
   *
   * @param jpaVendorAdapter a vendor adapter
   * @param jpaProperties the JPA properties to be passed to the persistence provider
   * @param persistenceUnitManager optional source of persistence unit information (can
   * be null)
   * @param persistenceUnitRootLocation the persistence unit root location to use as a
   * fallback (can be null)
   */
  public EntityManagerFactoryBuilder(JpaVendorAdapter jpaVendorAdapter, Map<String, ?> jpaProperties,
          PersistenceUnitManager persistenceUnitManager, URL persistenceUnitRootLocation) {
    this.jpaVendorAdapter = jpaVendorAdapter;
    this.persistenceUnitManager = persistenceUnitManager;
    this.jpaProperties = new LinkedHashMap<>(jpaProperties);
    this.persistenceUnitRootLocation = persistenceUnitRootLocation;
  }

  public Builder dataSource(DataSource dataSource) {
    return new Builder(dataSource);
  }

  /**
   * Configure the bootstrap executor to be used by the
   * {@link LocalContainerEntityManagerFactoryBean}.
   *
   * @param bootstrapExecutor the executor
   */
  public void setBootstrapExecutor(AsyncTaskExecutor bootstrapExecutor) {
    this.bootstrapExecutor = bootstrapExecutor;
  }

  /**
   * Set the {@linkplain PersistenceUnitPostProcessor persistence unit post processors}
   * to be applied to the PersistenceUnitInfo used for creating the
   * {@link LocalContainerEntityManagerFactoryBean}.
   *
   * @param persistenceUnitPostProcessors the persistence unit post processors to use
   */
  public void setPersistenceUnitPostProcessors(PersistenceUnitPostProcessor... persistenceUnitPostProcessors) {
    this.persistenceUnitPostProcessors = persistenceUnitPostProcessors;
  }

  /**
   * A fluent builder for a LocalContainerEntityManagerFactoryBean.
   */
  public final class Builder {

    private DataSource dataSource;

    private PersistenceManagedTypes managedTypes;

    private String[] packagesToScan;

    private String persistenceUnit;

    private Map<String, Object> properties = new HashMap<>();

    private String[] mappingResources;

    private boolean jta;

    private Builder(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    /**
     * The persistence managed types, providing both the managed entities and packages
     * the entity manager should consider.
     *
     * @param managedTypes managed types.
     * @return the builder for fluent usage
     */
    public Builder managedTypes(PersistenceManagedTypes managedTypes) {
      this.managedTypes = managedTypes;
      return this;
    }

    /**
     * The names of packages to scan for {@code @Entity} annotations.
     *
     * @param packagesToScan packages to scan
     * @return the builder for fluent usage
     * @see #managedTypes(PersistenceManagedTypes)
     */
    public Builder packages(String... packagesToScan) {
      this.packagesToScan = packagesToScan;
      return this;
    }

    /**
     * The classes whose packages should be scanned for {@code @Entity} annotations.
     *
     * @param basePackageClasses the classes to use
     * @return the builder for fluent usage
     * @see #managedTypes(PersistenceManagedTypes)
     */
    public Builder packages(Class<?>... basePackageClasses) {
      Set<String> packages = new HashSet<>();
      for (Class<?> type : basePackageClasses) {
        packages.add(ClassUtils.getPackageName(type));
      }
      this.packagesToScan = StringUtils.toStringArray(packages);
      return this;
    }

    /**
     * The name of the persistence unit. If only building one EntityManagerFactory you
     * can omit this, but if there are more than one in the same application you
     * should give them distinct names.
     *
     * @param persistenceUnit the name of the persistence unit
     * @return the builder for fluent usage
     */
    public Builder persistenceUnit(String persistenceUnit) {
      this.persistenceUnit = persistenceUnit;
      return this;
    }

    /**
     * Generic properties for standard JPA or vendor-specific configuration. These
     * properties override any values provided in the constructor.
     *
     * @param properties the properties to use
     * @return the builder for fluent usage
     */
    public Builder properties(Map<String, ?> properties) {
      this.properties.putAll(properties);
      return this;
    }

    /**
     * The mapping resources (equivalent to {@code <mapping-file>} entries in
     * {@code persistence.xml}) for the persistence unit.
     * <p>
     * Note that mapping resources must be relative to the classpath root, e.g.
     * "META-INF/mappings.xml" or "com/mycompany/repository/mappings.xml", so that
     * they can be loaded through {@code ClassLoader.getResource()}.
     *
     * @param mappingResources the mapping resources to use
     * @return the builder for fluent usage
     */
    public Builder mappingResources(String... mappingResources) {
      this.mappingResources = mappingResources;
      return this;
    }

    /**
     * Configure if using a JTA {@link DataSource}, i.e. if
     * {@link LocalContainerEntityManagerFactoryBean#setDataSource(DataSource)
     * setDataSource} or
     * {@link LocalContainerEntityManagerFactoryBean#setJtaDataSource(DataSource)
     * setJtaDataSource} should be called on the
     * {@link LocalContainerEntityManagerFactoryBean}.
     *
     * @param jta if the data source is JTA
     * @return the builder for fluent usage
     */
    public Builder jta(boolean jta) {
      this.jta = jta;
      return this;
    }

    public LocalContainerEntityManagerFactoryBean build() {
      LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
      if (EntityManagerFactoryBuilder.this.persistenceUnitManager != null) {
        entityManagerFactoryBean
                .setPersistenceUnitManager(EntityManagerFactoryBuilder.this.persistenceUnitManager);
      }
      if (this.persistenceUnit != null) {
        entityManagerFactoryBean.setPersistenceUnitName(this.persistenceUnit);
      }
      entityManagerFactoryBean.setJpaVendorAdapter(EntityManagerFactoryBuilder.this.jpaVendorAdapter);

      if (this.jta) {
        entityManagerFactoryBean.setJtaDataSource(this.dataSource);
      }
      else {
        entityManagerFactoryBean.setDataSource(this.dataSource);
      }
      if (this.managedTypes != null) {
        entityManagerFactoryBean.setManagedTypes(this.managedTypes);
      }
      else {
        entityManagerFactoryBean.setPackagesToScan(this.packagesToScan);
      }
      entityManagerFactoryBean.getJpaPropertyMap().putAll(EntityManagerFactoryBuilder.this.jpaProperties);
      entityManagerFactoryBean.getJpaPropertyMap().putAll(this.properties);
      if (!ObjectUtils.isEmpty(this.mappingResources)) {
        entityManagerFactoryBean.setMappingResources(this.mappingResources);
      }
      URL rootLocation = EntityManagerFactoryBuilder.this.persistenceUnitRootLocation;
      if (rootLocation != null) {
        entityManagerFactoryBean.setPersistenceUnitRootLocation(rootLocation.toString());
      }
      if (EntityManagerFactoryBuilder.this.bootstrapExecutor != null) {
        entityManagerFactoryBean.setBootstrapExecutor(EntityManagerFactoryBuilder.this.bootstrapExecutor);
      }
      if (EntityManagerFactoryBuilder.this.persistenceUnitPostProcessors != null) {
        entityManagerFactoryBean.setPersistenceUnitPostProcessors(
                EntityManagerFactoryBuilder.this.persistenceUnitPostProcessors);
      }
      return entityManagerFactoryBean;
    }

  }

}
