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

package cn.taketoday.annotation.config.jpa;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.transaction.TransactionManagerCustomizers;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.annotation.config.AutoConfigurationPackages;
import cn.taketoday.context.annotation.config.EnableAutoConfiguration;
import cn.taketoday.context.condition.ConditionalOnMissingBean;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.domain.EntityScanPackages;
import cn.taketoday.lang.Nullable;
import cn.taketoday.orm.jpa.JpaTransactionManager;
import cn.taketoday.orm.jpa.JpaVendorAdapter;
import cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean;
import cn.taketoday.orm.jpa.persistenceunit.PersistenceManagedTypes;
import cn.taketoday.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
import cn.taketoday.orm.jpa.persistenceunit.PersistenceUnitManager;
import cn.taketoday.orm.jpa.support.EntityManagerFactoryBuilder;
import cn.taketoday.orm.jpa.support.EntityManagerFactoryBuilderCustomizer;
import cn.taketoday.orm.jpa.vendor.AbstractJpaVendorAdapter;
import cn.taketoday.stereotype.Component;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionManager;
import cn.taketoday.transaction.jta.JtaTransactionManager;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import jakarta.persistence.EntityManagerFactory;

/**
 * Base {@link EnableAutoConfiguration Auto-configuration} for JPA.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @author Eddú Meléndez
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JpaProperties.class)
public abstract class JpaBaseConfiguration {

  protected final DataSource dataSource;

  /**
   * the {@link JpaProperties}.
   */
  protected final JpaProperties properties;

  /**
   * the JTA transaction manager or {@code null}
   */
  @Nullable
  protected final JtaTransactionManager jtaTransactionManager;

  protected JpaBaseConfiguration(DataSource dataSource, JpaProperties properties,
          @Nullable JtaTransactionManager jtaTransactionManager) {
    this.dataSource = dataSource;
    this.properties = properties;
    this.jtaTransactionManager = jtaTransactionManager;
  }

  @Component
  @ConditionalOnMissingBean(TransactionManager.class)
  public PlatformTransactionManager transactionManager(
          @Nullable TransactionManagerCustomizers transactionManagerCustomizers) {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    if (transactionManagerCustomizers != null) {
      transactionManagerCustomizers.customize(transactionManager);
    }
    return transactionManager;
  }

  @Component
  @ConditionalOnMissingBean
  public JpaVendorAdapter jpaVendorAdapter() {
    AbstractJpaVendorAdapter adapter = createJpaVendorAdapter();
    adapter.setShowSql(this.properties.isShowSql());
    if (this.properties.getDatabase() != null) {
      adapter.setDatabase(this.properties.getDatabase());
    }
    if (this.properties.getDatabasePlatform() != null) {
      adapter.setDatabasePlatform(this.properties.getDatabasePlatform());
    }
    adapter.setGenerateDdl(this.properties.isGenerateDdl());
    return adapter;
  }

  @Component
  @ConditionalOnMissingBean
  public EntityManagerFactoryBuilder entityManagerFactoryBuilder(
          JpaVendorAdapter jpaVendorAdapter,
          @Nullable PersistenceUnitManager persistenceUnitManager,
          List<EntityManagerFactoryBuilderCustomizer> customizers) {
    var builder = new EntityManagerFactoryBuilder(
            jpaVendorAdapter, properties.getProperties(), persistenceUnitManager);
    for (EntityManagerFactoryBuilderCustomizer customizer : customizers) {
      customizer.customize(builder);
    }
    return builder;
  }

  @Primary
  @Component
  @ConditionalOnMissingBean({ LocalContainerEntityManagerFactoryBean.class, EntityManagerFactory.class })
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
          EntityManagerFactoryBuilder factoryBuilder, PersistenceManagedTypes persistenceManagedTypes) {
    Map<String, Object> vendorProperties = getVendorProperties();
    customizeVendorProperties(vendorProperties);
    return factoryBuilder.dataSource(this.dataSource)
            .managedTypes(persistenceManagedTypes)
            .properties(vendorProperties)
            .mappingResources(getMappingResources())
            .jta(isJta())
            .build();
  }

  protected abstract AbstractJpaVendorAdapter createJpaVendorAdapter();

  protected abstract Map<String, Object> getVendorProperties();

  /**
   * Customize vendor properties before they are used. Allows for post-processing (for
   * example to configure JTA specific settings).
   *
   * @param vendorProperties the vendor properties to customize
   */
  protected void customizeVendorProperties(Map<String, Object> vendorProperties) {

  }

  private String[] getMappingResources() {
    List<String> mappingResources = this.properties.getMappingResources();
    return (ObjectUtils.isNotEmpty(mappingResources) ? StringUtils.toStringArray(mappingResources) : null);
  }

  /**
   * Returns if a JTA {@link PlatformTransactionManager} is being used.
   *
   * @return if a JTA transaction manager is being used
   */
  protected final boolean isJta() {
    return jtaTransactionManager != null;
  }

  /**
   * Return the {@link JpaProperties}.
   *
   * @return the properties
   */
  protected final JpaProperties getProperties() {
    return this.properties;
  }

  /**
   * Return the {@link DataSource}.
   *
   * @return the data source
   */
  protected final DataSource getDataSource() {
    return this.dataSource;
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnMissingBean({ LocalContainerEntityManagerFactoryBean.class, EntityManagerFactory.class })
  static class PersistenceManagedTypesConfiguration {

    @Primary
    @Component
    @ConditionalOnMissingBean
    static PersistenceManagedTypes persistenceManagedTypes(BeanFactory beanFactory, ResourceLoader resourceLoader) {
      String[] packagesToScan = getPackagesToScan(beanFactory);
      return new PersistenceManagedTypesScanner(resourceLoader).scan(packagesToScan);
    }

    private static String[] getPackagesToScan(BeanFactory beanFactory) {
      List<String> packages = EntityScanPackages.get(beanFactory).getPackageNames();
      if (packages.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
        packages = AutoConfigurationPackages.get(beanFactory);
      }
      return StringUtils.toStringArray(packages);
    }

  }

}
