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

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.cfg.AvailableSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.DisableAllDependencyInjection;
import cn.taketoday.beans.factory.annotation.DisableDependencyInjection;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.condition.ConditionalOnSingleCandidate;
import cn.taketoday.context.properties.EnableConfigurationProperties;
import cn.taketoday.framework.jdbc.SchemaManagementProvider;
import cn.taketoday.framework.jdbc.metadata.CompositeDataSourcePoolMetadataProvider;
import cn.taketoday.framework.jdbc.metadata.DataSourcePoolMetadataProvider;
import cn.taketoday.jndi.JndiLocatorDelegate;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.orm.hibernate5.HibernateBeanContainer;
import cn.taketoday.orm.hibernate5.support.HibernateJtaPlatform;
import cn.taketoday.orm.jpa.vendor.AbstractJpaVendorAdapter;
import cn.taketoday.orm.jpa.vendor.HibernateJpaVendorAdapter;
import cn.taketoday.transaction.jta.JtaTransactionManager;
import cn.taketoday.util.ClassUtils;

/**
 * {@link JpaBaseConfiguration} implementation for Hibernate.
 *
 * @author Phillip Webb
 * @author Josh Long
 * @author Manuel Doninger
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@DisableDependencyInjection
@DisableAllDependencyInjection
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HibernateProperties.class)
@ConditionalOnSingleCandidate(DataSource.class)
class HibernateJpaConfiguration extends JpaBaseConfiguration {

  private static final Logger log = LoggerFactory.getLogger(HibernateJpaConfiguration.class);

  private static final String JTA_PLATFORM = "hibernate.transaction.jta.platform";

  private static final String PROVIDER_DISABLES_AUTOCOMMIT = "hibernate.connection.provider_disables_autocommit";

  /**
   * {@code NoJtaPlatform} implementations for various Hibernate versions.
   */
  private static final String[] NO_JTA_PLATFORM_CLASSES = {
          "org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform",
          "org.hibernate.service.jta.platform.internal.NoJtaPlatform"
  };

  private final HibernateProperties hibernateProperties;
  private final DataSourcePoolMetadataProvider poolMetadataProvider;
  private final HibernateDefaultDdlAutoProvider defaultDdlAutoProvider;
  private final List<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers;

  HibernateJpaConfiguration(DataSource dataSource, JpaProperties jpaProperties,
          ConfigurableBeanFactory beanFactory, HibernateProperties hibernateProperties,
          @Nullable JtaTransactionManager jtaTransactionManager,
          @Nullable PhysicalNamingStrategy physicalNamingStrategy,
          @Nullable ImplicitNamingStrategy implicitNamingStrategy,
          ObjectProvider<SchemaManagementProvider> providers,
          ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders,
          ObjectProvider<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
    super(dataSource, jpaProperties, jtaTransactionManager);
    this.hibernateProperties = hibernateProperties;
    this.defaultDdlAutoProvider = new HibernateDefaultDdlAutoProvider(providers, dataSource);
    this.poolMetadataProvider = new CompositeDataSourcePoolMetadataProvider(metadataProviders);
    this.hibernatePropertiesCustomizers = determineHibernatePropertiesCustomizers(
            beanFactory, physicalNamingStrategy, implicitNamingStrategy, hibernatePropertiesCustomizers);
  }

  private List<HibernatePropertiesCustomizer> determineHibernatePropertiesCustomizers(
          ConfigurableBeanFactory beanFactory,
          @Nullable PhysicalNamingStrategy physicalNamingStrategy,
          @Nullable ImplicitNamingStrategy implicitNamingStrategy,
          ObjectProvider<HibernatePropertiesCustomizer> hibernatePropertiesCustomizers) {
    var customizers = new ArrayList<HibernatePropertiesCustomizer>();
    if (ClassUtils.isPresent("org.hibernate.resource.beans.container.spi.BeanContainer",
            getClass().getClassLoader())) {
      customizers.add(properties ->
              properties.put(AvailableSettings.BEAN_CONTAINER, new HibernateBeanContainer(beanFactory)));
    }
    if (physicalNamingStrategy != null || implicitNamingStrategy != null) {
      customizers.add(new NamingStrategiesHibernatePropertiesCustomizer(
              physicalNamingStrategy, implicitNamingStrategy));
    }

    hibernatePropertiesCustomizers.addOrderedTo(customizers);
    return customizers;
  }

  @Override
  protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
    return new HibernateJpaVendorAdapter();
  }

  @Override
  protected Map<String, Object> getVendorProperties() {
    return new LinkedHashMap<>(hibernateProperties
            .determineHibernateProperties(properties.getProperties(),
                    new HibernateSettings()
                            .ddlAuto(defaultDdlAutoProvider)
                            .hibernatePropertiesCustomizers(hibernatePropertiesCustomizers)));
  }

  @Override
  protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
    super.customizeVendorProperties(vendorProperties);
    if (!vendorProperties.containsKey(JTA_PLATFORM)) {
      configureJtaPlatform(vendorProperties);
    }
    if (!vendorProperties.containsKey(PROVIDER_DISABLES_AUTOCOMMIT)) {
      configureProviderDisablesAutocommit(vendorProperties);
    }
  }

  private void configureJtaPlatform(Map<String, Object> vendorProperties) throws LinkageError {
    // Make sure Hibernate doesn't attempt to auto-detect a JTA platform
    if (jtaTransactionManager == null) {
      vendorProperties.put(JTA_PLATFORM, getNoJtaPlatformManager());
    }
    // As of Hibernate 5.2, Hibernate can fully integrate with the WebSphere
    // transaction manager on its own.
    else if (!runningOnWebSphere()) {
      configureInfraJtaPlatform(vendorProperties, jtaTransactionManager);
    }
  }

  private void configureProviderDisablesAutocommit(Map<String, Object> vendorProperties) {
    if (isDataSourceAutoCommitDisabled() && !isJta()) {
      vendorProperties.put(PROVIDER_DISABLES_AUTOCOMMIT, "true");
    }
  }

  private boolean isDataSourceAutoCommitDisabled() {
    var poolMetadata = poolMetadataProvider.getDataSourcePoolMetadata(dataSource);
    return poolMetadata != null && Boolean.FALSE.equals(poolMetadata.getDefaultAutoCommit());
  }

  private boolean runningOnWebSphere() {
    return ClassUtils.isPresent("com.ibm.websphere.jtaextensions.ExtendedJTATransaction",
            getClass().getClassLoader());
  }

  private void configureInfraJtaPlatform(
          Map<String, Object> vendorProperties, JtaTransactionManager jtaTransactionManager) {
    try {
      vendorProperties.put(JTA_PLATFORM, new HibernateJtaPlatform(jtaTransactionManager));
    }
    catch (LinkageError ex) {
      // NoClassDefFoundError can happen if Hibernate 4.2 is used and some
      // containers (e.g. JBoss EAP 6) wrap it in the superclass LinkageError
      if (!isUsingJndi()) {
        throw new IllegalStateException(
                "Unable to set Hibernate JTA platform, are you using the correct version of Hibernate?", ex);
      }
      // Assume that Hibernate will use JNDI
      if (log.isDebugEnabled()) {
        log.debug("Unable to set Hibernate JTA platform : {}", ex.getMessage());
      }
    }
  }

  private boolean isUsingJndi() {
    try {
      return JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable();
    }
    catch (Error ex) {
      return false;
    }
  }

  private Object getNoJtaPlatformManager() {
    for (String candidate : NO_JTA_PLATFORM_CLASSES) {
      try {
        return Class.forName(candidate).getDeclaredConstructor().newInstance();
      }
      catch (Exception ex) {
        // Continue searching
      }
    }
    throw new IllegalStateException(
            "No available JtaPlatform candidates amongst " + Arrays.toString(NO_JTA_PLATFORM_CLASSES));
  }

  private static class NamingStrategiesHibernatePropertiesCustomizer implements HibernatePropertiesCustomizer {

    @Nullable
    private final PhysicalNamingStrategy physicalNamingStrategy;

    @Nullable
    private final ImplicitNamingStrategy implicitNamingStrategy;

    NamingStrategiesHibernatePropertiesCustomizer(
            @Nullable PhysicalNamingStrategy physicalNamingStrategy,
            @Nullable ImplicitNamingStrategy implicitNamingStrategy) {
      this.physicalNamingStrategy = physicalNamingStrategy;
      this.implicitNamingStrategy = implicitNamingStrategy;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
      if (this.physicalNamingStrategy != null) {
        hibernateProperties.put("hibernate.physical_naming_strategy", this.physicalNamingStrategy);
      }
      if (this.implicitNamingStrategy != null) {
        hibernateProperties.put("hibernate.implicit_naming_strategy", this.implicitNamingStrategy);
      }
    }

  }

}
