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

package cn.taketoday.annotation.config.jpa;

import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.sql.DataSource;

import cn.taketoday.annotation.config.TestAutoConfigurationPackage;
import cn.taketoday.annotation.config.jdbc.DataSourceAutoConfiguration;
import cn.taketoday.annotation.config.jdbc.DataSourceTransactionManagerAutoConfiguration;
import cn.taketoday.annotation.config.jpa.domain.country.Country;
import cn.taketoday.annotation.config.jpa.test.City;
import cn.taketoday.annotation.config.transaction.TransactionAutoConfiguration;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.annotation.config.AutoConfigurations;
import cn.taketoday.framework.test.context.assertj.AssertableApplicationContext;
import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.framework.test.context.runner.ContextConsumer;
import cn.taketoday.jdbc.config.DataSourceBuilder;
import cn.taketoday.orm.jpa.JpaTransactionManager;
import cn.taketoday.orm.jpa.JpaVendorAdapter;
import cn.taketoday.orm.jpa.LocalContainerEntityManagerFactoryBean;
import cn.taketoday.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import cn.taketoday.orm.jpa.persistenceunit.PersistenceManagedTypes;
import cn.taketoday.orm.jpa.persistenceunit.PersistenceUnitManager;
import cn.taketoday.orm.jpa.support.EntityManagerFactoryBuilderCustomizer;
import cn.taketoday.test.BuildOutput;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.spi.PersistenceUnitInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for JPA tests and tests for {@link JpaBaseConfiguration}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 */
abstract class AbstractJpaAutoConfigurationTests {

  private final Class<?> autoConfiguredClass;

  private final ApplicationContextRunner contextRunner;

  protected AbstractJpaAutoConfigurationTests(Class<?> autoConfiguredClass) {
    this.autoConfiguredClass = autoConfiguredClass;
    this.contextRunner = new ApplicationContextRunner()
            .withPropertyValues("datasource.generate-unique-name=true",
                    "infra.jta.log-dir="
                            + new File(new BuildOutput(getClass()).getRootLocation(), "transaction-logs"))
            .withUserConfiguration(TestConfiguration.class)
            .withConfiguration(
                    AutoConfigurations.of(
                            DataSourceAutoConfiguration.class,
                            TransactionAutoConfiguration.class,
                            autoConfiguredClass
                    )
            );
  }

  protected ApplicationContextRunner contextRunner() {
    return this.contextRunner;
  }

  @Test
  void notConfiguredIfDataSourceIsNotAvailable() {
    new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(this.autoConfiguredClass))
            .run(assertJpaIsNotAutoConfigured());
  }

  @Test
  void notConfiguredIfNoSingleDataSourceCandidateIsAvailable() {
    new ApplicationContextRunner().withUserConfiguration(TestTwoDataSourcesConfiguration.class)
            .withConfiguration(AutoConfigurations.of(this.autoConfiguredClass)).run(assertJpaIsNotAutoConfigured());
  }

  protected ContextConsumer<AssertableApplicationContext> assertJpaIsNotAutoConfigured() {
    return (context) -> {
      assertThat(context).hasNotFailed();
      assertThat(context).hasSingleBean(JpaProperties.class);
      assertThat(context).doesNotHaveBean(TransactionManager.class);
      assertThat(context).doesNotHaveBean(EntityManagerFactory.class);
    };
  }

  @Test
  void configuredWithAutoConfiguredDataSource() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(DataSource.class);
      assertThat(context).hasSingleBean(JpaTransactionManager.class);
      assertThat(context).hasSingleBean(EntityManagerFactory.class);
      assertThat(context).hasSingleBean(PersistenceManagedTypes.class);
    });
  }

  @Test
  void configuredWithSingleCandidateDataSource() {
    this.contextRunner.withUserConfiguration(TestTwoDataSourcesAndPrimaryConfiguration.class).run((context) -> {
      assertThat(context).getBeans(DataSource.class).hasSize(2);
      assertThat(context).hasSingleBean(JpaTransactionManager.class);
      assertThat(context).hasSingleBean(EntityManagerFactory.class);
      assertThat(context).hasSingleBean(PersistenceManagedTypes.class);
    });
  }

  @Test
  void jpaTransactionManagerTakesPrecedenceOverSimpleDataSourceOne() {
    this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceTransactionManagerAutoConfiguration.class))
            .run((context) -> {
              assertThat(context).hasSingleBean(DataSource.class);
              assertThat(context).hasSingleBean(JpaTransactionManager.class);
              assertThat(context).getBean("transactionManager").isInstanceOf(JpaTransactionManager.class);
            });
  }

  @Test
  void customJpaProperties() {
    contextRunner.withPropertyValues("jpa.properties.a:b", "jpa.properties.a.b:c", "jpa.properties.c:d")
            .run((context) -> {
              LocalContainerEntityManagerFactoryBean bean = context
                      .getBean(LocalContainerEntityManagerFactoryBean.class);
              Map<String, Object> map = bean.getJpaPropertyMap();
              assertThat(map.get("a")).isEqualTo("b");
              assertThat(map.get("c")).isEqualTo("d");
              assertThat(map.get("a.b")).isEqualTo("c");
            });
  }

  @Test
  void usesManuallyDefinedLocalContainerEntityManagerFactoryBeanIfAvailable() {
    this.contextRunner.withUserConfiguration(TestConfigurationWithLocalContainerEntityManagerFactoryBean.class)
            .run((context) -> {
              LocalContainerEntityManagerFactoryBean factoryBean = context
                      .getBean(LocalContainerEntityManagerFactoryBean.class);
              Map<String, Object> map = factoryBean.getJpaPropertyMap();
              assertThat(map.get("configured")).isEqualTo("manually");
            });
  }

  @Test
  void usesManuallyDefinedEntityManagerFactoryIfAvailable() {
    this.contextRunner.withUserConfiguration(TestConfigurationWithLocalContainerEntityManagerFactoryBean.class)
            .run((context) -> {
              EntityManagerFactory factoryBean = context.getBean(EntityManagerFactory.class);
              Map<String, Object> map = factoryBean.getProperties();
              assertThat(map.get("configured")).isEqualTo("manually");
            });
  }

  @Test
  void usesManuallyDefinedTransactionManagerBeanIfAvailable() {
    this.contextRunner.withUserConfiguration(TestConfigurationWithTransactionManager.class).run((context) -> {
      assertThat(context).hasSingleBean(TransactionManager.class);
      TransactionManager txManager = context.getBean(TransactionManager.class);
      assertThat(txManager).isInstanceOf(CustomJpaTransactionManager.class);
    });
  }

  @Test
  void defaultPersistenceManagedTypes() {
    this.contextRunner.run((context) -> {
      assertThat(context).hasSingleBean(PersistenceManagedTypes.class);
      EntityManager entityManager = context.getBean(EntityManagerFactory.class).createEntityManager();
      assertThat(getManagedJavaTypes(entityManager)).contains(City.class).doesNotContain(Country.class);
    });
  }

  @Test
  void customPersistenceManagedTypes() {
    this.contextRunner
            .withBean(PersistenceManagedTypes.class, () -> PersistenceManagedTypes.of(Country.class.getName()))
            .run((context) -> {
              assertThat(context).hasSingleBean(PersistenceManagedTypes.class);
              EntityManager entityManager = context.getBean(EntityManagerFactory.class).createEntityManager();
              assertThat(getManagedJavaTypes(entityManager)).contains(Country.class).doesNotContain(City.class);
            });
  }

  @Test
  void customPersistenceUnitManager() {
    this.contextRunner.withUserConfiguration(TestConfigurationWithCustomPersistenceUnitManager.class)
            .run((context) -> {
              LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = context
                      .getBean(LocalContainerEntityManagerFactoryBean.class);
              assertThat(entityManagerFactoryBean).hasFieldOrPropertyWithValue("persistenceUnitManager",
                      context.getBean(PersistenceUnitManager.class));
            });
  }

  @Test
  void customPersistenceUnitPostProcessors() {
    this.contextRunner.withUserConfiguration(TestConfigurationWithCustomPersistenceUnitPostProcessors.class)
            .run((context) -> {
              LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = context
                      .getBean(LocalContainerEntityManagerFactoryBean.class);
              PersistenceUnitInfo persistenceUnitInfo = entityManagerFactoryBean.getPersistenceUnitInfo();
              assertThat(persistenceUnitInfo).isNotNull();
              assertThat(persistenceUnitInfo.getManagedClassNames())
                      .contains("customized.attribute.converter.class.name");
            });
  }

  private Class<?>[] getManagedJavaTypes(EntityManager entityManager) {
    Set<ManagedType<?>> managedTypes = entityManager.getMetamodel().getManagedTypes();
    return managedTypes.stream().map(ManagedType::getJavaType).toArray(Class<?>[]::new);
  }

  @Configuration(proxyBeanMethods = false)
  static class TestTwoDataSourcesConfiguration {

    @Bean
    DataSource firstDataSource() {
      return createRandomDataSource();
    }

    @Bean
    DataSource secondDataSource() {
      return createRandomDataSource();
    }

    private DataSource createRandomDataSource() {
      String url = "jdbc:h2:mem:init-" + UUID.randomUUID();
      return DataSourceBuilder.create().url(url).build();
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestTwoDataSourcesAndPrimaryConfiguration {

    @Bean
    @Primary
    DataSource firstDataSource() {
      return createRandomDataSource();
    }

    @Bean
    DataSource secondDataSource() {
      return createRandomDataSource();
    }

    private DataSource createRandomDataSource() {
      String url = "jdbc:h2:mem:init-" + UUID.randomUUID();
      return DataSourceBuilder.create().url(url).build();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @TestAutoConfigurationPackage(City.class)
  static class TestConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfigurationWithLocalContainerEntityManagerFactoryBean extends TestConfiguration {

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, JpaVendorAdapter adapter) {
      LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
      factoryBean.setJpaVendorAdapter(adapter);
      factoryBean.setDataSource(dataSource);
      factoryBean.setPersistenceUnitName("manually-configured");
      Map<String, Object> properties = new HashMap<>();
      properties.put("configured", "manually");
      properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
      factoryBean.setJpaPropertyMap(properties);
      return factoryBean;
    }

  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfigurationWithEntityManagerFactory extends TestConfiguration {

    @Bean
    EntityManagerFactory entityManagerFactory(DataSource dataSource, JpaVendorAdapter adapter) {
      LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
      factoryBean.setJpaVendorAdapter(adapter);
      factoryBean.setDataSource(dataSource);
      factoryBean.setPersistenceUnitName("manually-configured");
      Map<String, Object> properties = new HashMap<>();
      properties.put("configured", "manually");
      properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
      factoryBean.setJpaPropertyMap(properties);
      factoryBean.afterPropertiesSet();
      return factoryBean.getObject();
    }

    @Bean
    PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
      JpaTransactionManager transactionManager = new JpaTransactionManager();
      transactionManager.setEntityManagerFactory(emf);
      return transactionManager;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @TestAutoConfigurationPackage(City.class)
  static class TestConfigurationWithTransactionManager {

    @Bean
    TransactionManager testTransactionManager() {
      return new CustomJpaTransactionManager();
    }

  }

  @Configuration(proxyBeanMethods = false)
  @TestAutoConfigurationPackage(AbstractJpaAutoConfigurationTests.class)
  static class TestConfigurationWithCustomPersistenceUnitManager {

    private final DataSource dataSource;

    TestConfigurationWithCustomPersistenceUnitManager(DataSource dataSource) {
      this.dataSource = dataSource;
    }

    @Bean
    PersistenceUnitManager persistenceUnitManager() {
      DefaultPersistenceUnitManager persistenceUnitManager = new DefaultPersistenceUnitManager();
      persistenceUnitManager.setDefaultDataSource(this.dataSource);
      persistenceUnitManager.setPackagesToScan(City.class.getPackage().getName());
      return persistenceUnitManager;
    }

  }

  @Configuration(proxyBeanMethods = false)
  @TestAutoConfigurationPackage(AbstractJpaAutoConfigurationTests.class)
  static class TestConfigurationWithCustomPersistenceUnitPostProcessors {

    @Bean
    EntityManagerFactoryBuilderCustomizer entityManagerFactoryBuilderCustomizer() {
      return (builder) -> builder.setPersistenceUnitPostProcessors(
              (pui) -> pui.addManagedClassName("customized.attribute.converter.class.name"));
    }

  }

  static class CustomJpaTransactionManager extends JpaTransactionManager {

  }

}
